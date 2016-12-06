package yyf.shadowsocks.service;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Handler;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import com.androapplite.shadowsocks.ShadowsocksApplication;

import java.lang.System;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import yyf.shadowsocks.IShadowsocksService;
import yyf.shadowsocks.broadcast.Action;
import yyf.shadowsocks.jni.*;
import yyf.shadowsocks.preferences.DefaultSharedPrefeencesUtil;
import yyf.shadowsocks.preferences.SharedPreferenceKey;
import yyf.shadowsocks.utils.Constants;
import yyf.shadowsocks.Config;
import yyf.shadowsocks.IShadowsocksServiceCallback;
import yyf.shadowsocks.utils.TrafficMonitor;
import yyf.shadowsocks.utils.TrafficMonitorThread;

/**
 * Created by yyf on 2015/6/18.
 */
public abstract class BaseService extends VpnService {
    volatile private Constants.State state = Constants.State.INIT;
    volatile private int callbacksCount = 0;
    private TrafficMonitorThread trafficMonitorThread;
    private TrafficMonitor mTrafficMonitor;
    private Timer timer;
    private Handler handler;
    protected Config config = null;
    private Timer mHoulyUpdater;
    private long mStartTime;

    final RemoteCallbackList<IShadowsocksServiceCallback> callbacks = new RemoteCallbackList<IShadowsocksServiceCallback>();
    IShadowsocksService.Stub binder = new IShadowsocksService.Stub(){
        public int getMode(){
            return getServiceMode().ordinal();
        }

        public int getState(){
            return state.ordinal();
        }

        @Override
        public void registerCallback(IShadowsocksServiceCallback cb) throws RemoteException {
            BaseService.this.registerCallback(cb);
        }

        @Override
        public void unregisterCallback(IShadowsocksServiceCallback cb) throws RemoteException {
            BaseService.this.unregisterCallback(cb);
        }

        public void stop() {
//            if (state != Constants.State.CONNECTING && state != Constants.State.STOPPING) {
//                stopRunner();
//            }
            if(state != Constants.State.STOPPING){
                stopRunner();
            }
        }

        public void start(Config config) {
            if (state != Constants.State.CONNECTING && state != Constants.State.STOPPING) {
                startRunner(config);
            }
        }

        public long getTxTotalMonthly(){
            return DefaultSharedPrefeencesUtil.getTxTotal(BaseService.this);
        }

        public long getRxTotalMonthly(){
            return DefaultSharedPrefeencesUtil.getRxTotal(BaseService.this);
        }

        public void enableNotification(boolean enable){
            BaseService.this.enableNotification(enable);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mTrafficMonitor = new TrafficMonitor();
        handler = new Handler(getMainLooper());
        mHoulyUpdater = new Timer(true);
        initHourlyUpdater();
        initLastResetMonth();
    }

    public abstract void stopBackgroundService();
    public void startRunner(Config config){
        this.config = config;
        mTrafficMonitor.reset();
        trafficMonitorThread = new TrafficMonitorThread(this);
        trafficMonitorThread.start();
    }
    public void stopRunner(){
        updateTrafficTotal(mTrafficMonitor.txTotal, mTrafficMonitor.rxTotal);
        mTrafficMonitor.reset();
        if (trafficMonitorThread != null) {
            trafficMonitorThread.stopThread();
            trafficMonitorThread = null;
        }
        // stop the service if no callback registered
        if (callbacksCount == 0) {
            stopSelf();
        }
    }

    private void updateTrafficTotal(long tx, long rx){
        DefaultSharedPrefeencesUtil.accumulateTxTotalAndRxToal(this, tx, rx);
    }

    public abstract Constants.Mode getServiceMode();
    public abstract String getTag();
    public abstract Context getContext();

    public Constants.State getState(){
        return state;
    }
    public void changeState(Constants.State s) {
        changeState(s, null);

        if(s == Constants.State.CONNECTED){
            mStartTime = System.currentTimeMillis();
        }else if(s == Constants.State.STOPPED){
            //send broadcast
            Intent intent = new Intent(Action.STOPPED);
            if(mStartTime != 0) {
                long c = System.currentTimeMillis();
                intent.putExtra(SharedPreferenceKey.DURATION, c - mStartTime);
                mStartTime = 0;
            }
            sendBroadcast(intent);
        }else if(s == Constants.State.ERROR){
            //send broadcast
            Intent intent = new Intent(Action.ERROR);
            if(mStartTime != 0) {
                long c = System.currentTimeMillis();
                intent.putExtra(SharedPreferenceKey.DURATION, c - mStartTime);
                mStartTime = 0;
            }
            sendBroadcast(intent);
        }
    }

    protected void changeState(final Constants.State s,final String msg) {
        Handler handler = new Handler(getContext().getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                if (state != s) {
                    if (callbacksCount > 0) {
                        int n = callbacks.beginBroadcast();
                        for (int i = 0; i < n; i++) {
                            try {
                                callbacks.getBroadcastItem(i).stateChanged(s.ordinal(), msg);
                            } catch (RemoteException e) {
                                ShadowsocksApplication.handleException(e);
                            }
                        }
                        callbacks.finishBroadcast();
                    }
                    state = s;
                }
            }
        });

    }

    void initSoundVibrateLights(Notification notification) {
        notification.sound = null;
    }

    public void registerCallback(IShadowsocksServiceCallback callback){
        if(callback != null && callbacks.register(callback)) {
            callbacksCount += 1;
            if (callbacksCount != 0 && timer == null) {
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        if(mTrafficMonitor.updateRate()){
                            updateTrafficRate();
                        }
                    }
                };
                timer = new Timer(true);
                timer.schedule(task, 1000, 1000);
            }
            mTrafficMonitor.updateRate();
            try {
                callback.trafficUpdated(mTrafficMonitor.txRate, mTrafficMonitor.rxRate, mTrafficMonitor.txTotal, mTrafficMonitor.rxTotal);
            } catch (RemoteException e) {
                ShadowsocksApplication.handleException(e);
            }
        }
    }

    private void updateTrafficRate(){
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(callbacksCount > 0){
                    long txRate = mTrafficMonitor.txRate;
                    long rxRate = mTrafficMonitor.rxRate;
                    long txTotal = mTrafficMonitor.txTotal;
                    long rxTotal = mTrafficMonitor.rxTotal;
                    int n = callbacks.beginBroadcast();
                    for(int i=0; i<n; i++){
                        try {
                            callbacks.getBroadcastItem(i).trafficUpdated(txRate, rxRate, txTotal, rxTotal);
                        } catch (RemoteException e) {
                            ShadowsocksApplication.handleException(e);
                        }
                    }
                    callbacks.finishBroadcast();
                }
            }
        });
    }

    public void unregisterCallback(IShadowsocksServiceCallback callback){
        if (callback != null && callbacks.unregister(callback)) {
            callbacksCount -= 1;
            if (callbacksCount == 0 && timer != null) {
                timer.cancel();
                timer = null;
            }
        }
    }

    public int getCallbacksCount() {
        return callbacksCount;
    }

    private void updateTrafficTotal(){

    }

    public TrafficMonitor getTrafficMonitor(){
        return mTrafficMonitor;
    }

    private void initHourlyUpdater(){
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                checkLastResetMonth();
            }
        };
//        mHoulyUpdater.schedule(timerTask, TimeUnit.SECONDS.toMillis(1), TimeUnit.MINUTES.toMillis(1));
        mHoulyUpdater.schedule(timerTask, TimeUnit.SECONDS.toMillis(1), TimeUnit.HOURS.toMillis(1));

    }

    private void checkLastResetMonth() {
        long txTotal = DefaultSharedPrefeencesUtil.getTxTotal(this);
        long rxTotal = DefaultSharedPrefeencesUtil.getRxTotal(this);
        Calendar lastResetMonth = DefaultSharedPrefeencesUtil.getLastResetMonth(this);
        Calendar currentMonth = Calendar.getInstance();
//        if(currentMonth.getTimeInMillis() - lastResetMonth.getTimeInMillis() > 10000){
        if(lastResetMonth.get(Calendar.MONTH) != currentMonth.get(Calendar.MONTH) ){
            DefaultSharedPrefeencesUtil.resetTxTotalAndRxTotal(this);
            DefaultSharedPrefeencesUtil.setLastResetMonth(this, currentMonth);
            mTrafficMonitor.reset();

            //send broadcast
            Intent intent = new Intent(Action.RESET_TOTAL);
            intent.putExtra(SharedPreferenceKey.TX_TOTAL, txTotal);
            intent.putExtra(SharedPreferenceKey.RX_TOTAL, rxTotal);
            intent.putExtra(SharedPreferenceKey.LAST_RESET_MONTH, lastResetMonth.getTimeInMillis());
            sendBroadcast(intent);

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mHoulyUpdater != null){
            mHoulyUpdater.cancel();
            mHoulyUpdater = null;
        }
    }

    private void initLastResetMonth(){
        Calendar currentMonth = Calendar.getInstance();
        Calendar lastResetMonth = DefaultSharedPrefeencesUtil.getLastResetMonth(this);
        if(currentMonth.before(lastResetMonth)){
            DefaultSharedPrefeencesUtil.setLastResetMonth(this, currentMonth);
        }

    }

    protected abstract void enableNotification(boolean enable);

}
