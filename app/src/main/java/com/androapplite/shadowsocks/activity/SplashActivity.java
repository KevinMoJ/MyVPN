package com.androapplite.shadowsocks.activity;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.ProgressBar;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.NotificationsUtils;
import com.androapplite.shadowsocks.service.AutoRestartService;
import com.androapplite.vpn3.R;
import com.androapplite.shadowsocks.ShadowsockServiceHelper;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.service.ServerListFetcherService;
import com.bestgo.adsplugin.ads.AdAppHelper;

import java.lang.ref.WeakReference;

import yyf.shadowsocks.IShadowsocksService;
import yyf.shadowsocks.utils.Constants;

public class SplashActivity extends BaseShadowsocksActivity implements ServiceConnection {
    private IShadowsocksService mShadowsocksService;
    private Handler mAdLoadedCheckHandler;
    private Runnable mAdLoadedCheckRunable;
    private ObjectAnimator mProgressbarAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initBackgroundReceiver();
        initBackgroundReceiverIntentFilter();

        checkAndCopyAsset();
//        AutoRestartService.startService(this);
//        ShadowsockServiceHelper.startService(this);
        ShadowsockServiceHelper.bindService(this, this);
        startProgressBarAnimation();


        final AdAppHelper adAppHelper = AdAppHelper.getInstance(SplashActivity.this);
        adAppHelper.loadNewInterstitial();
        adAppHelper.loadNewNative();

        mAdLoadedCheckRunable = new AdLoadedCheckRunnable(this, adAppHelper);
        mAdLoadedCheckHandler = new Handler();
        mAdLoadedCheckHandler.postDelayed(mAdLoadedCheckRunable, 1000);


    }

    private static class AdLoadedCheckRunnable implements Runnable{
        private WeakReference<SplashActivity> mActivityReference;
        private AdAppHelper mAdAppHelper;

        AdLoadedCheckRunnable(SplashActivity activity, AdAppHelper adAppHelper){
            mActivityReference = new WeakReference<SplashActivity>(activity);
            mAdAppHelper = adAppHelper;
        }

        @Override
        public void run() {
            SplashActivity activity = mActivityReference.get();
            if(activity != null){
                if(mAdAppHelper.isFullAdLoaded()){
                    activity.mProgressbarAnimator.setDuration(100);
                    activity.mProgressbarAnimator.start();
                }else{
                    activity.mAdLoadedCheckHandler.postDelayed(activity.mAdLoadedCheckRunable, 1000);
                }
            }

        }
    }

    private void startProgressBarAnimation(){
        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progress_bar);
        final PropertyValuesHolder start = PropertyValuesHolder.ofInt("progress", 0);
        PropertyValuesHolder end = PropertyValuesHolder.ofInt("progress", 100);
        mProgressbarAnimator = ObjectAnimator.ofPropertyValuesHolder(progressBar, start, end);
        mProgressbarAnimator.setDuration(5000);
        mProgressbarAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                startActivity(new Intent(SplashActivity.this, ConnectivityActivity.class));
                finish();
            }
        });
        mProgressbarAnimator.start();

    }


    private void checkAndCopyAsset() {
        new Thread() {
            @Override
            public void run() {
                try {
                    ShadowsockServiceHelper.checkAndCopyAsset(getAssets(), yyf.shadowsocks.jni.System.getABI());
                }catch (Exception e){
                    ShadowsocksApplication.handleException(e);
                }
            }
        }.start();
    }

    private void initBackgroundReceiver(){
        mBackgroundReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if(action.equals(Action.CONNECTION_ACTIVITY_SHOW) || action.equals(Action.NEW_USER_GUIDE_ACTIVITY_SHOW)){
                    finish();
                }
            }
        };
    }

    private void initBackgroundReceiverIntentFilter(){
        mBackgroundReceiverIntentFilter = new IntentFilter();
        mBackgroundReceiverIntentFilter.addAction(Action.CONNECTION_ACTIVITY_SHOW);
        mBackgroundReceiverIntentFilter.addAction(Action.NEW_USER_GUIDE_ACTIVITY_SHOW);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(this);
        mAdLoadedCheckHandler.removeCallbacks(mAdLoadedCheckRunable);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mShadowsocksService = IShadowsocksService.Stub.asInterface(service);
        try {
            int s = mShadowsocksService.getState();
            Constants.State state = Constants.State.values()[s];
            if(state == Constants.State.INIT || state == Constants.State.STOPPED || state == Constants.State.ERROR){
                ServerListFetcherService.fetchServerListAsync(this);
            }
        } catch (RemoteException e) {
            ShadowsocksApplication.handleException(e);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mShadowsocksService = null;
    }


}
