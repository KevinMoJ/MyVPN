package com.androapplite.shadowsocks.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.androapplite.shadowsocks.ShadowsockServiceHelper;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

import yyf.shadowsocks.IShadowsocksService;

public class TimeCountDownService extends Service implements ServiceConnection{
    private SharedPreferences mSharedPreference;
    private TimeUpReceiver mTimeUpReceiver;
    private DisconnectReceiver mDisconnectReceiver;
    private Timer mTimeTickTimer;
    private IShadowsocksService mShadowsocksService;
    private int m1hCountDown;
    private long mLastTickTime;

    public TimeCountDownService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mSharedPreference = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
        registerTimeTickTimer();
        registerTimeUpBroadcast();
        registerDisconnectReceiver();
        m1hCountDown = mSharedPreference.getInt(SharedPreferenceKey.TIME_COUNT_DOWN, 0);
        if(m1hCountDown <= 0) m1hCountDown = 1800;
        mLastTickTime = System.currentTimeMillis();
        ShadowsockServiceHelper.bindService(this, this);
    }

    private void registerTimeTickTimer(){
        mTimeTickTimer = new Timer();
        mTimeTickTimer.schedule(new TimeCountDownTask(this), 100, 1000);
    }

    private void registerTimeUpBroadcast(){
        IntentFilter intentFilter = new IntentFilter(Action.TIME_UP);
        mTimeUpReceiver = new TimeUpReceiver(this);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.registerReceiver(mTimeUpReceiver, intentFilter);
    }

    private void registerDisconnectReceiver(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(yyf.shadowsocks.broadcast.Action.STOPPED);
        intentFilter.addAction(yyf.shadowsocks.broadcast.Action.ERROR);
        mDisconnectReceiver = new DisconnectReceiver(this);
        registerReceiver(mDisconnectReceiver, intentFilter);
    }


    @Override
    public void onDestroy() {
        mTimeTickTimer.cancel();
        mTimeTickTimer.purge();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mTimeUpReceiver);
        unregisterReceiver(mDisconnectReceiver);
        if(mShadowsocksService != null) {
            unbindService(this);
        }
        //手工停止的重新计时
        mSharedPreference.edit().putInt(SharedPreferenceKey.TIME_COUNT_DOWN, 0).apply();
        super.onDestroy();
    }

    private static class TimeCountDownTask extends TimerTask{
        WeakReference<TimeCountDownService> mServiceReference;
        TimeCountDownTask(TimeCountDownService service){
            mServiceReference = new WeakReference<TimeCountDownService>(service);
        }

        @Override
        public void run() {
            TimeCountDownService service = mServiceReference.get();
            if(service != null){
                long countDown = service.mSharedPreference.getLong(SharedPreferenceKey.USE_TIME, 0);
                //处理系统休眠的情况
//            Log.d("TimeCountDownService", mLastTickTime + " " + System.currentTimeMillis());
                long differ = System.currentTimeMillis() - service.mLastTickTime;
                service.mLastTickTime = System.currentTimeMillis();
                if(differ > 60 * 1000){
                    countDown += differ/1000;
                    service.mSharedPreference.edit().putLong(SharedPreferenceKey.USE_TIME, countDown).apply();

                    service.m1hCountDown -= differ/1000;
                    if(service.m1hCountDown < 0){
                        service.m1hCountDown = 0;
                    }
                }else{
                    service.mSharedPreference.edit().putLong(SharedPreferenceKey.USE_TIME, ++countDown).apply();
                }

                if(--service.m1hCountDown <= 0){
                    service.sendTimeUpBroadcast();
                }
                service.mSharedPreference.edit().putInt(SharedPreferenceKey.TIME_COUNT_DOWN, service.m1hCountDown).apply();
                Log.d("CountDownService", "剩余时间 " + service.m1hCountDown);
            }
        }
    }

    private void sendTimeUpBroadcast(){
        Intent intent = new Intent(Action.TIME_UP);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
    }

    private static class TimeUpReceiver extends BroadcastReceiver{
        WeakReference<TimeCountDownService> mServiceReference;
        TimeUpReceiver(TimeCountDownService service){
            mServiceReference = new WeakReference<TimeCountDownService>(service);
        }
        @Override
        public void onReceive(Context context, Intent intent) {
            TimeCountDownService service = mServiceReference.get();
            if(service != null) {
                service.stopVPNConnection();
            }
        }
    }

    private void stopVPNConnection() {
        if(mShadowsocksService != null) {
            try {
                mShadowsocksService.stop();
            } catch (RemoteException e) {
                ShadowsocksApplication.handleException(e);
            }
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mShadowsocksService = IShadowsocksService.Stub.asInterface(service);
        try {
            mShadowsocksService.setRemainTime(m1hCountDown);
        } catch (RemoteException e) {
            ShadowsocksApplication.handleException(e);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }


    public static void start(Context context){
        try {
            context.startService(new Intent(context, TimeCountDownService.class));
        }catch (Exception e){
            ShadowsocksApplication.handleException(e);
        }
    }

    private static class DisconnectReceiver extends BroadcastReceiver{
        WeakReference<TimeCountDownService> mServiceReference;
        DisconnectReceiver(TimeCountDownService service){
            mServiceReference = new WeakReference<TimeCountDownService>(service);
        }
        @Override
        public void onReceive(Context context, Intent intent) {
            TimeCountDownService service = mServiceReference.get();
            if(service != null) {
                service.stopSelf();
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(mShadowsocksService != null){
            try {
                mShadowsocksService.setRemainTime(m1hCountDown);
            } catch (RemoteException e) {
                ShadowsocksApplication.handleException(e);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }
}
