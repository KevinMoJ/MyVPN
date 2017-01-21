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
    private long mStartTime;
    private int remain;

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
            remain = 0;
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

        public void setRemainTime(int remain){
            BaseService.this.remain = remain;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mTrafficMonitor = new TrafficMonitor();
        handler = new Handler(getMainLooper());
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
        Intent intent = new Intent();
        long c= System.currentTimeMillis();
        switch (s){
            case INIT:
                intent.setAction(Action.INIT);
                break;
            case CONNECTING:
                intent.setAction(Action.CONNECTING);
                if(mStartTime > 0){
                    intent.putExtra(SharedPreferenceKey.DURATION, c - mStartTime);
                }
                mStartTime = c;
                break;
            case CONNECTED:
                intent.setAction(Action.CONNECTED);
                if(mStartTime > 0){
                    intent.putExtra(SharedPreferenceKey.DURATION, c - mStartTime);
                }
                mStartTime = c;
                break;
            case STOPPING:
                intent.setAction(Action.STOPPING);
                if(mStartTime > 0){
                    intent.putExtra(SharedPreferenceKey.DURATION, c - mStartTime);
                }
                mStartTime = c;
                break;
            case STOPPED:
                intent.setAction(Action.STOPPED);
                if(mStartTime > 0){
                    intent.putExtra(SharedPreferenceKey.DURATION, c - mStartTime);
                }
                mStartTime = 0;
                break;
            case ERROR:
                intent.setAction(Action.ERROR);
                if(mStartTime > 0){
                    intent.putExtra(SharedPreferenceKey.DURATION, c - mStartTime);
                }
                mStartTime = 0;
                break;
        }
        sendBroadcast(intent);
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
//                        if(mTrafficMonitor.updateRate()){
//                            updateTrafficRate();
//                        }
                        mTrafficMonitor.updateRate();
                        remain -= 1;
                        if(state.equals(Constants.State.CONNECTED)) {
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

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void initLastResetMonth(){
        Calendar currentMonth = Calendar.getInstance();
        Calendar lastResetMonth = DefaultSharedPrefeencesUtil.getLastResetMonth(this);
        if(currentMonth.before(lastResetMonth)){
            DefaultSharedPrefeencesUtil.setLastResetMonth(this, currentMonth);
        }

    }

    protected abstract void enableNotification(boolean enable);

    public int getRemain(){
        return remain;
    }

}
