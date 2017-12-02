package com.androapplite.shadowsocks.activity;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.androapplite.vpn3.R;
import com.androapplite.shadowsocks.Firebase;

import com.androapplite.shadowsocks.service.ServerListFetcherService;
import com.androapplite.shadowsocks.service.VpnManageService;
import com.bestgo.adsplugin.ads.AdAppHelper;


public class SplashActivity extends AppCompatActivity implements Handler.Callback,
        Animator.AnimatorListener{
    private Handler mAdLoadedCheckHandler;
    private ObjectAnimator mProgressbarAnimator;
    private static final int MSG_AD_LOADED_CHECK = 1;
    private AdAppHelper mAdAppHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        startProgressBarAnimation();
        mAdAppHelper = AdAppHelper.getInstance(this);
        mAdAppHelper.loadNewInterstitial();
        mAdAppHelper.loadNewNative();
        mAdAppHelper.loadNewSplashAd();

        mAdLoadedCheckHandler = new Handler(this);
        mAdLoadedCheckHandler.sendEmptyMessageDelayed(MSG_AD_LOADED_CHECK, 3000);
        ServerListFetcherService.fetchServerListAsync(this);
        VpnManageService.start(this);
        Firebase.getInstance(this).logEvent("屏幕","闪屏屏幕");
        Intent intent = getIntent();
        if (intent != null) {
            String source = intent.getStringExtra("source");
            if (source != null && source.equals("notificaiton")) {
                Firebase.getInstance(this).logEvent("打开app来源","通知");
            } else {
                Firebase.getInstance(this).logEvent("打开app来源","图标");
            }
        }

        AdAppHelper.getInstance(getApplicationContext()).loadNewSplashAd();

        FrameLayout frameLayout = (FrameLayout)findViewById(R.id.splash_ad_ll);
        LinearLayout centerLogoLL = (LinearLayout)findViewById(R.id.center_logo_ll);
        LinearLayout bottomLogoLL = (LinearLayout)findViewById(R.id.bottom_logo_ll);
        if (AdAppHelper.getInstance(getApplicationContext()).isSplashReady()) {
            View view = AdAppHelper.getInstance(getApplicationContext()).getSplashAd();
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP);
            frameLayout.addView(view, layoutParams);

            centerLogoLL.setVisibility(View.GONE);
            frameLayout.setVisibility(View.VISIBLE);
            bottomLogoLL.setVisibility(View.VISIBLE);
        } else {
            centerLogoLL.setVisibility(View.VISIBLE);
            frameLayout.setVisibility(View.GONE);
            bottomLogoLL.setVisibility(View.GONE);
        }

    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_AD_LOADED_CHECK:
                Log.d("SplanshActivity", "mAdAppHelper.isFullAdLoaded() " + mAdAppHelper.isFullAdLoaded());
                if(mAdAppHelper.isFullAdLoaded()){
                    mProgressbarAnimator.setDuration(100);
                }else{
                    mAdLoadedCheckHandler.sendEmptyMessageDelayed(MSG_AD_LOADED_CHECK, 1000);
                }
                break;
        }
        return true;
    }

    private void startProgressBarAnimation(){
        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progress_bar);
        final PropertyValuesHolder start = PropertyValuesHolder.ofInt("progress", 0);
        PropertyValuesHolder end = PropertyValuesHolder.ofInt("progress", 100);
        mProgressbarAnimator = ObjectAnimator.ofPropertyValuesHolder(progressBar, start, end);
        mProgressbarAnimator.setDuration(5000);
        mProgressbarAnimator.addListener(this);
        mProgressbarAnimator.start();

    }

    @Override
    protected void onDestroy() {
        mAdLoadedCheckHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public void onAnimationStart(Animator animation) {

    }

    @Override
    public void onAnimationEnd(Animator animation) {
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
        finish();
    }

    @Override
    public void onAnimationCancel(Animator animation) {

    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }
}
