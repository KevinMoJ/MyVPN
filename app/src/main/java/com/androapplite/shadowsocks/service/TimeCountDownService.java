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
import android.text.format.DateUtils;
import android.util.Log;

import com.androapplite.shadowsocks.ShadowsockServiceHelper;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.activity.CommonAlertActivity;
import com.androapplite.shadowsocks.activity.WatchVideoADDialogActivity;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;

import java.util.Timer;
import java.util.TimerTask;

import yyf.shadowsocks.IShadowsocksService;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class TimeCountDownService extends Service implements ServiceConnection{
    private SharedPreferences mSharedPreference;
    private TimeUpReceiver mTimeUpReceiver;
    private DisconnectReceiver mDisconnectReceiver;
    private Timer mTimeTickTimer;
    private IShadowsocksService mShadowsocksService;
    private int m1hCountDown;
    private static final String KEEP_CONNECT = "KEEP_CONNECT";

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
    }

    private void registerTimeTickTimer(){
        mTimeTickTimer = new Timer();
        mTimeTickTimer.schedule(new TimeCountDownTask(), 0, 1000);
    }

    private void registerTimeUpBroadcast(){
        IntentFilter intentFilter = new IntentFilter(Action.TIME_UP);
        mTimeUpReceiver = new TimeUpReceiver();
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.registerReceiver(mTimeUpReceiver, intentFilter);
    }

    private void registerDisconnectReceiver(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(yyf.shadowsocks.broadcast.Action.STOPPING);
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
    }

    private class TimeCountDownTask extends TimerTask{
        @Override
        public void run() {
            int countDown = mSharedPreference.getInt(SharedPreferenceKey.TIME_COUNT_DOWN, 0);
            if(countDown > 0) {
                mSharedPreference.edit().putInt(SharedPreferenceKey.TIME_COUNT_DOWN, countDown - 1).commit();

                if(--m1hCountDown <= 0){
                    sendTimeUpBroadcast();
                }
                Log.d("m1hCountDown", m1hCountDown + " ");
                if(countDown > m1hCountDown){
                    if(m1hCountDown == 300 || m1hCountDown == 180 || m1hCountDown == 60){
                        //提示用户延长一小时
                    }
                }else{
                    if(countDown == 3601 || countDown == 1800 || countDown == 900 || countDown == 300){
                        CommonAlertActivity.showAlert(TimeCountDownService.this, CommonAlertActivity.TIME_UP);
                    }
                }
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
        WatchVideoADDialogActivity.showTimeUsedUpDialog(TimeCountDownService.this);
        ShadowsockServiceHelper.bindService(this, this);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mShadowsocksService = IShadowsocksService.Stub.asInterface(service);
        try {
            mShadowsocksService.stop();
        } catch (RemoteException e) {
            ShadowsocksApplication.handleException(e);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }


    public static void start(Context context){
        context.startService(new Intent(context, TimeCountDownService.class));
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
        m1hCountDown += countDown > 3600 ? 3600 : countDown;
        return super.onStartCommand(intent, flags, startId);
    }
}
