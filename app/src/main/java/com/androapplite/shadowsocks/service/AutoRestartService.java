package com.androapplite.shadowsocks.service;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.ShadowsockServiceHelper;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.activity.ConnectivityActivity;
import com.androapplite.shadowsocks.model.ServerConfig;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.bestgo.adsplugin.daemon.Daemon;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.lang.ref.WeakReference;

import yyf.shadowsocks.Config;
import yyf.shadowsocks.IShadowsocksService;
import yyf.shadowsocks.IShadowsocksServiceCallback;
import yyf.shadowsocks.utils.Constants;


public class AutoRestartService extends Service implements ServiceConnection{
    private volatile IShadowsocksService mShadowsocksService;
    private volatile IShadowsocksServiceCallback.Stub mShadowsocksServiceCallbackBinder;
    private volatile int mState;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private long mRemoteConfigFetchStart;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mShadowsocksServiceCallbackBinder = new ShadowsocksServiceCallback(this);
        SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
        mState = sharedPreferences.getInt(SharedPreferenceKey.VPN_STATE, Constants.State.INIT.ordinal());
        Firebase.getInstance(this).logEvent("自动重启","创建");
        Daemon.run(getApplicationContext(), AutoRestartService.class, 10);
        ShadowsockServiceHelper.startService(this);
        ShadowsockServiceHelper.bindService(this, this);
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mShadowsocksService = IShadowsocksService.Stub.asInterface(service);
        startVpnService();
    }

    private void startVpnService() {
        synchronized (this) {
            try {
                mShadowsocksService.registerCallback(mShadowsocksServiceCallbackBinder);
                int currentState = mShadowsocksService.getState();
                final int connectedState = Constants.State.CONNECTED.ordinal();
                if (mState == connectedState && currentState != connectedState) {
                    SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
                    ServerConfig serverConfig = ServerConfig.loadFromSharedPreference(sharedPreferences);
                    if (serverConfig != null) {
                        Config config = new Config(serverConfig.server, serverConfig.port);
                        mShadowsocksService.start(config);
                        Firebase.getInstance(this).logEvent("自动重启","恢复连接");
                    }
                }else if(mState != connectedState){
                    Firebase.getInstance(this).logEvent("自动重启","断开-不需恢复连接");
                }else{
                    Firebase.getInstance(this).logEvent("自动重启","连接-不需恢复连接");
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

    private static class RemoteConfigFetchListener implements OnCompleteListener<Void> {
        private WeakReference<AutoRestartService> mServiceReference;
        RemoteConfigFetchListener(AutoRestartService service){
            mServiceReference = new WeakReference<AutoRestartService>(service);
        }
        @Override
        public void onComplete(@NonNull Task<Void> task) {
            AutoRestartService service = mServiceReference.get();
            if(service != null){
                if (task.isSuccessful()) {
                    service.mFirebaseRemoteConfig.activateFetched();
                    Firebase.getInstance(service).logEvent("获取远程配置", "成功", System.currentTimeMillis() - service.mRemoteConfigFetchStart);
                } else {
                    Firebase.getInstance(service).logEvent("获取远程配置", "失败", System.currentTimeMillis() - service.mRemoteConfigFetchStart);
                }
            }

        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mRemoteConfigFetchStart = System.currentTimeMillis();
        mFirebaseRemoteConfig.fetch(1800).addOnCompleteListener(new RemoteConfigFetchListener(this));
        return super.onStartCommand(intent, flags, startId);
    }
}
