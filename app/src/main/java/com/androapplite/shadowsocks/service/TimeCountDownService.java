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
        m1hCountDown = 0;
        mLastTickTime = System.currentTimeMillis();
        ShadowsockServiceHelper.bindService(this, this);
    }

    private void registerTimeTickTimer(){
        mTimeTickTimer = new Timer();
        mTimeTickTimer.schedule(new TimeCountDownTask(), 100, 1000);
    }

    private void registerTimeUpBroadcast(){
        IntentFilter intentFilter = new IntentFilter(Action.TIME_UP);
        mTimeUpReceiver = new TimeUpReceiver();
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.registerReceiver(mTimeUpReceiver, intentFilter);
    }

    private void registerDisconnectReceiver(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(yyf.shadowsocks.broadcast.Action.STOPPED);
        intentFilter.addAction(yyf.shadowsocks.broadcast.Action.ERROR);
        mDisconnectReceiver = new DisconnectReceiver();
        registerReceiver(mDisconnectReceiver, intentFilter);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mTimeTickTimer.cancel();
        mTimeTickTimer.purge();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mTimeUpReceiver);
        unregisterReceiver(mDisconnectReceiver);
        if(mShadowsocksService != null) {
            unbindService(this);
        }
        mSharedPreference.edit().putBoolean(SharedPreferenceKey.EXTENT_1H_ALERT, false).commit();
    }

    private class TimeCountDownTask extends TimerTask{
        @Override
        public void run() {
            int countDown = mSharedPreference.getInt(SharedPreferenceKey.TIME_COUNT_DOWN, 0);
            //处理系统休眠的情况
            Log.d("TimeCountDownService", mLastTickTime + " " + System.currentTimeMillis());
            long differ = System.currentTimeMillis() - mLastTickTime;
            mLastTickTime = System.currentTimeMillis();
            if(differ > 60 * 1000){
                countDown -= differ/1000;
                if(countDown < 0){
                    countDown = 0;
                }
                mSharedPreference.edit().putInt(SharedPreferenceKey.TIME_COUNT_DOWN, countDown).commit();

                m1hCountDown -= differ/1000;
                if(m1hCountDown < 0){
                    m1hCountDown = 0;
                }
            }

            if(countDown > 0) {
                mSharedPreference.edit().putInt(SharedPreferenceKey.TIME_COUNT_DOWN, --countDown).commit();
                if(--m1hCountDown <= 0){
                    sendTimeUpBroadcast();
                }
                Log.d("countDown2", countDown + " " + m1hCountDown);

                //注意是小于号不是小于等于号

            }else {
                sendTimeUpBroadcast();
            }
        }
    }

    private void sendTimeUpBroadcast(){
        Intent intent = new Intent(Action.TIME_UP);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
    }

    private class TimeUpReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            stopVPNConnection();
        }
    }

    private void stopVPNConnection() {
        int countDown = mSharedPreference.getInt(SharedPreferenceKey.TIME_COUNT_DOWN, 0);
        try {
            mShadowsocksService.stop();
        } catch (RemoteException e) {
            ShadowsocksApplication.handleException(e);
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

    private class DisconnectReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int countDown = mSharedPreference.getInt(SharedPreferenceKey.TIME_COUNT_DOWN, 0);
        m1hCountDown += countDown > 1800 ? 1800 : countDown;
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
