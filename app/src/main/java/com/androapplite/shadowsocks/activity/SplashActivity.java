package com.androapplite.shadowsocks.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.ViewPropertyAnimator;
import android.widget.ProgressBar;

import com.androapplite.shadowsocks.CheckInAlarm;
import com.androapplite.shadowsocks.GAHelper;
import com.androapplite.vpn3.R;
import com.androapplite.shadowsocks.ShadowsockServiceHelper;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.service.AppCheckService;
import com.androapplite.shadowsocks.service.ServerListFetcherService;

import java.util.concurrent.TimeUnit;

import yyf.shadowsocks.IShadowsocksService;
import yyf.shadowsocks.utils.Constants;

public class SplashActivity extends BaseShadowsocksActivity implements ServiceConnection {
    private IShadowsocksService mShadowsocksService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initBackgroundReceiver();
        initBackgroundReceiverIntentFilter();

//        startNewUserGuideActivityOrConnectionActivity();
        checkAndCopyAsset();
        ShadowsockServiceHelper.startService(this);

        GAHelper.sendScreenView(this, "启动屏幕");

        ShadowsockServiceHelper.bindService(this, this);
        CheckInAlarm.checkIn(this);
        AppCheckService.startAppCheckService(this);
        startProgressBarAnimation();
    }
    private void startProgressBarAnimation(){
        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progress_bar);
        PropertyValuesHolder start = PropertyValuesHolder.ofInt("progress", 0);
        PropertyValuesHolder end = PropertyValuesHolder.ofInt("progress", 100);
        ObjectAnimator objectAnimator = ObjectAnimator.ofPropertyValuesHolder(progressBar, start, end);
        objectAnimator.setDuration(5000);
        objectAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                startNewUserGuideActivityOrConnectionActivity();
            }
        });
        objectAnimator.start();
    }

    private void startNewUserGuideActivityOrConnectionActivity() {
        startActivity(new Intent(this, ConnectivityActivity.class));
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
