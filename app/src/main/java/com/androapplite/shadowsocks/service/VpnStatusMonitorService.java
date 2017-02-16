package com.androapplite.shadowsocks.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import com.androapplite.shadowsocks.ShadowsockServiceHelper;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import yyf.shadowsocks.IShadowsocksService;
import yyf.shadowsocks.IShadowsocksServiceCallback;
import yyf.shadowsocks.service.ShadowsocksVpnService;
import yyf.shadowsocks.utils.Constants;

public class VpnStatusMonitorService extends Service implements ServiceConnection {
    private String mUuid;
    private IShadowsocksService mVpnService;
    private boolean mHasRate;
    private long mStartMilli;


    public VpnStatusMonitorService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
        mUuid = sharedPreferences.getString(SharedPreferenceKey.UUID, null);
        if(mUuid == null){
            mUuid = UUID.randomUUID().toString();
            mUuid = mUuid.replace("-","");
            sharedPreferences.edit().putString(SharedPreferenceKey.UUID, mUuid).commit();
        }
        ShadowsockServiceHelper.bindService(this, this);
        mHasRate = false;
        mStartMilli = System.currentTimeMillis();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        long current = System.currentTimeMillis();
        long spanSecond = (current - mStartMilli)/1000;
        if(mHasRate){
            Log.d("vpn 状态", "速度时长 " + spanSecond);
        }else{
            Log.d("vpn 状态", "没速度时长 " + spanSecond);
        }
        removeNoRateDetectHandler();
        if(mVpnService != null) {
            try {
                mVpnService.unregisterCallback(mCallback);
            } catch (RemoteException e) {
                ShadowsocksApplication.handleException(e);
            }
        }
        unbindService(this);
    }

    public static void startService(Context context){
        Intent intent = new Intent(context, VpnStatusMonitorService.class);
        context.startService(intent);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mVpnService = IShadowsocksService.Stub.asInterface(service);
        try {
            mVpnService.registerCallback(mCallback);
        } catch (RemoteException e) {
            ShadowsocksApplication.handleException(e);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    private static final long ONE_MINUTE = 1000 * 60;

    private IShadowsocksServiceCallback.Stub mCallback = new IShadowsocksServiceCallback.Stub() {
        @Override
        public void stateChanged(int state, String msg) throws RemoteException {
            if(state == Constants.State.ERROR.ordinal() || state == Constants.State.STOPPED.ordinal()){
                stopSelf();
            }
        }

        @Override
        public void trafficUpdated(long txRate, long rxRate, long txTotal, long rxTotal) throws RemoteException {
            if(txRate == 0 && rxRate == 0){
                startNoRateDetectHandler();
            }else{
                if(mHasRate == false){
                    long current = System.currentTimeMillis();
                    long spanSecond = (current - mStartMilli)/1000;
                    mStartMilli = current;
                    mHasRate = true;
                    Log.d("vpn 状态", "没速度时长 " + spanSecond);
                }else {
                    removeNoRateDetectHandler();
                }

            }
        }
    };


    private Handler mNoRateDetectHander;
    private Runnable mNoRateDetectRunnable = new Runnable() {
        @Override
        public void run() {
            if(mHasRate == true){
                long current = System.currentTimeMillis() - ONE_MINUTE;
                long spanSecond = (current - mStartMilli)/1000;
                mStartMilli = current;
                Log.d("vpn 状态", "速度时长 " + spanSecond);
            }
            mHasRate = false;
        }
    };

    private void startNoRateDetectHandler() {
        if(mNoRateDetectHander == null) {
            mNoRateDetectHander = new Handler(Looper.getMainLooper());
            mNoRateDetectHander.postDelayed(mNoRateDetectRunnable, ONE_MINUTE);
        }
    }

    private void removeNoRateDetectHandler() {
        if(mNoRateDetectHander != null){
            mNoRateDetectHander.removeCallbacks(mNoRateDetectRunnable);
            mNoRateDetectHander = null;
        }
    }

}
