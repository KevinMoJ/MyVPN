package com.androapplite.shadowsocks.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;

import com.androapplite.shadowsocks.GAHelper;
import com.androapplite.shadowsocks.ad.AdHelper;
import com.androapplite.shadowsocks.R;
import com.androapplite.shadowsocks.ShadowsockServiceHelper;
import com.androapplite.shadowsocks.ad.Banner;
import com.androapplite.shadowsocks.ad.admob.AdMobBanner;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.google.android.gms.ads.AdSize;

import java.util.concurrent.TimeUnit;

public class SplashActivity extends BaseShadowsocksActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initBackgroundReceiver();
        initBackgroundReceiverIntentFilter();

        startNewUserGuideActivityOrConnectionActivity();
        checkAndCopyAsset();
        ShadowsockServiceHelper.startService(this);

        initAdHelper();
        GAHelper.sendScreenView(this, "启动屏幕");
    }

    private void initAdHelper() {
        final AdHelper adHelper = AdHelper.getInstance(getApplicationContext());
        Banner banner = null;

        try {
            int screenDensity = getResources().getConfiguration().densityDpi;
            if (screenDensity > 240) {
                banner = new AdMobBanner(this, getString(R.string.banner_ad_unit_id),
                        AdSize.LARGE_BANNER, getString(R.string.tag_banner));
            } else {
                banner = new AdMobBanner(this, getString(R.string.banner_ad_unit_id),
                        AdSize.SMART_BANNER, getString(R.string.tag_banner));
            }
        }catch (NoSuchFieldError e){
            banner = new AdMobBanner(this, getString(R.string.banner_ad_unit_id),
                    AdSize.SMART_BANNER, getString(R.string.tag_banner));
        }
        adHelper.addAd(banner);
        adHelper.loadAll();
    }

    private void startNewUserGuideActivityOrConnectionActivity() {
        getWindow().getDecorView().postDelayed(new Runnable() {
            @Override
            public void run() {
                Activity activity = SplashActivity.this;
                startActivity(new Intent(activity,
                        DefaultSharedPrefeencesUtil.isNewUser(activity) ? NewUserGuideActivity.class : ConnectivityActivity.class
                ));
            }
        }, TimeUnit.SECONDS.toMillis(2));
    }

    private void checkAndCopyAsset() {
        new Thread() {
            @Override
            public void run() {
                ShadowsockServiceHelper.checkAndCopyAsset(getAssets(), yyf.shadowsocks.jni.System.getABI());
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
}
