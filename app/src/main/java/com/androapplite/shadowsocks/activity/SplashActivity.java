package com.androapplite.shadowsocks.activity;

import android.animation.Animator;
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

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.service.ServerListFetcherService;
import com.androapplite.shadowsocks.service.VpnManageService;
import com.androapplite.shadowsocks.service.WarnDialogShowService;
import com.androapplite.shadowsocks.utils.InternetUtil;
import com.androapplite.vpn3.R;
import com.bestgo.adsplugin.ads.AdAppHelper;


public class SplashActivity extends AppCompatActivity implements Handler.Callback,
        Animator.AnimatorListener{
    private Handler mAdLoadedCheckHandler;
    private static final int MSG_AD_LOADED_CHECK = 1;
    private AdAppHelper mAdAppHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        mAdAppHelper = AdAppHelper.getInstance(this);
        mAdAppHelper.loadNewInterstitial();
        mAdAppHelper.loadNewNative();
        mAdAppHelper.loadNewSplashAd();

        WarnDialogShowService.start(this);
        mAdLoadedCheckHandler = new Handler(this);
        Message msg = Message.obtain();
        msg.what = MSG_AD_LOADED_CHECK;
        msg.arg1 = 1;

        ServerListFetcherService.fetchServerListAsync(this);
        VpnManageService.start(this);
        Firebase firebase = Firebase.getInstance(this);
        firebase.logEvent("屏幕","闪屏屏幕");
        Intent intent = getIntent();
        if (intent != null) {
            String source = intent.getStringExtra("source");
            if (source != null) {
                firebase.logEvent("打开app来源", source);
            } else {
                firebase.logEvent("打开app来源", "图标");
            }
        }

        FrameLayout frameLayout = (FrameLayout)findViewById(R.id.splash_ad_ll);
        LinearLayout centerLogoLL = (LinearLayout)findViewById(R.id.center_logo_ll);
        LinearLayout bottomLogoLL = (LinearLayout)findViewById(R.id.bottom_logo_ll);
        if (mAdAppHelper.isSplashReady()) {
            View view = mAdAppHelper.getSplashAd();
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP);
            frameLayout.addView(view, layoutParams);

            centerLogoLL.setVisibility(View.GONE);
            frameLayout.setVisibility(View.VISIBLE);
            bottomLogoLL.setVisibility(View.VISIBLE);

            mAdLoadedCheckHandler.sendMessageDelayed(msg, 3000);
        } else {
            centerLogoLL.setVisibility(View.VISIBLE);
            frameLayout.setVisibility(View.GONE);
            bottomLogoLL.setVisibility(View.GONE);

            mAdLoadedCheckHandler.sendMessageDelayed(msg, 1000);
        }
        Firebase.getInstance(this).logEvent("当前网络类型", "类型", InternetUtil.getNetworkState(SplashActivity.this));
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_AD_LOADED_CHECK:
                Log.d("SplanshActivity", "mAdAppHelper.isFullAdLoaded() " + mAdAppHelper.isFullAdLoaded());
                if(mAdAppHelper.isFullAdLoaded() || msg.arg1 > 4){
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                }else{
                    Message message = Message.obtain();
                    message.what = MSG_AD_LOADED_CHECK;
                    message.arg1 = 1 + msg.arg1;
                    mAdLoadedCheckHandler.sendMessageDelayed(message, 1000);
                }
                break;
        }
        return true;
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
