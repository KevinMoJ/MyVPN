package com.androapplite.shadowsocks.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import com.androapplite.shadowsocks.ShadowsockServiceHelper;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.activity.ConnectivityActivity;
import com.androapplite.shadowsocks.model.ServerConfig;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;

import java.lang.ref.WeakReference;

import yyf.shadowsocks.Config;
import yyf.shadowsocks.IShadowsocksService;
import yyf.shadowsocks.IShadowsocksServiceCallback;
import yyf.shadowsocks.utils.Constants;


public class AutoRestartService extends Service implements ServiceConnection{
    private volatile IShadowsocksService mShadowsocksService;
    private volatile IShadowsocksServiceCallback.Stub mShadowsocksServiceCallbackBinder;
    private volatile int mState;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ShadowsockServiceHelper.bindService(this, this);
        mShadowsocksServiceCallbackBinder = new ShadowsocksServiceCallback(this);
        SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
        mState = sharedPreferences.getInt(SharedPreferenceKey.VPN_STATE, Constants.State.INIT.ordinal());
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mShadowsocksService = IShadowsocksService.Stub.asInterface(service);
        startVpnService();
    }

    private void startVpnService() {
        synchronized (AutoRestartService.class) {
            try {
                mShadowsocksService.registerCallback(mShadowsocksServiceCallbackBinder);
                int currentState = mShadowsocksService.getState();
                if (mState == Constants.State.CONNECTED.ordinal() && currentState != Constants.State.CONNECTED.ordinal()) {
                    SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
                    ServerConfig serverConfig = ServerConfig.loadFromSharedPreference(sharedPreferences);
                    if (serverConfig != null) {
                        Config config = new Config(serverConfig.server, serverConfig.port);
                        mShadowsocksService.start(config);
                    }
                }

            } catch (RemoteException e) {
                ShadowsocksApplication.handleException(e);
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        try {
            mShadowsocksService.unregisterCallback(mShadowsocksServiceCallbackBinder);
        } catch (RemoteException e) {
            ShadowsocksApplication.handleException(e);
        }
        mShadowsocksService = null;
    }

    private static class ShadowsocksServiceCallback extends IShadowsocksServiceCallback.Stub{
        private WeakReference<AutoRestartService> mServiceReference;

        ShadowsocksServiceCallback(AutoRestartService service){
            mServiceReference = new WeakReference<AutoRestartService>(service);
        }

        @Override
        public void stateChanged(int state, String msg) throws RemoteException {
            AutoRestartService service = mServiceReference.get();
            if(service != null && service.mState != state){
                SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(service);
                sharedPreferences.edit().putInt(SharedPreferenceKey.VPN_STATE, state).commit();
                service.mState = state;
                if(state == Constants.State.CONNECTED.ordinal()){
                    TimeCountDownService.start(service);
                }
            }
        }

        @Override
        public void trafficUpdated(long txRate, long rxRate, long txTotal, long rxTotal) throws RemoteException {

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            mShadowsocksService.unregisterCallback(mShadowsocksServiceCallbackBinder);
        } catch (RemoteException e) {
            ShadowsocksApplication.handleException(e);
        }
        unbindService(this);
    }

    public static void startService(Context context){
        context.startService(new Intent(context, AutoRestartService.class));
    }

}
