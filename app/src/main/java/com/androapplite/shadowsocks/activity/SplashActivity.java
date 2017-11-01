package com.androapplite.shadowsocks.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ProgressBar;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.R;
import com.androapplite.shadowsocks.broadcast.Action;
import com.bestgo.adsplugin.ads.AdAppHelper;

import java.lang.ref.WeakReference;

public class SplashActivity extends BaseShadowsocksActivity {
    private ObjectAnimator mProgressbarAnimator;
    private Handler mAdLoadedCheckHandler;
    private Runnable mAdLoadedCheckRunable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initBackgroundReceiver();
        initBackgroundReceiverIntentFilter();
        startProgressBarAnimation();

        final AdAppHelper adAppHelper = AdAppHelper.getInstance(SplashActivity.this);
        adAppHelper.loadNewInterstitial();
//        adAppHelper.loadNewBanner();
        adAppHelper.loadNewNative();

        mAdLoadedCheckRunable = new AdLoadedCheckRunnable(this, adAppHelper);
        mAdLoadedCheckHandler = new Handler();
        mAdLoadedCheckHandler.postDelayed(mAdLoadedCheckRunable, 1000);
        Firebase.getInstance(this).logEvent("屏幕","闪幕屏幕");

    }

    private static class AdLoadedCheckRunnable implements Runnable{
        private WeakReference<SplashActivity> mActivityReference;
        AdAppHelper mAdAppHelper;
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
        mAdLoadedCheckHandler.removeCallbacks(mAdLoadedCheckRunable);
    }
}
