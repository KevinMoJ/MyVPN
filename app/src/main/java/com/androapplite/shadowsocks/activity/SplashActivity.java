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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ProgressBar;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.NotificationsUtils;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.androapplite.shadowsocks.service.AutoRestartService;
import com.androapplite.vpn3.R;
import com.androapplite.shadowsocks.ShadowsockServiceHelper;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.service.ServerListFetcherService;
import com.bestgo.adsplugin.ads.AdAppHelper;

import java.lang.ref.WeakReference;
import java.util.Random;

import yyf.shadowsocks.IShadowsocksService;
import yyf.shadowsocks.utils.Constants;

public class SplashActivity extends AppCompatActivity implements Handler.Callback, Animator.AnimatorListener{
    private Handler mAdLoadedCheckHandler;
    private ObjectAnimator mProgressbarAnimator;
    private static final int MSG_CHECK_ADS = 1;
    private Constants.State mState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        checkAndCopyAsset();
        startProgressBarAnimation();

        mAdLoadedCheckHandler = new Handler(this);
        mAdLoadedCheckHandler.sendEmptyMessageDelayed(MSG_CHECK_ADS, 1000);
        Firebase.getInstance(this).logEvent("屏幕","闪屏屏幕");
        AutoRestartService.startService(this);

        final SharedPreferences preferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
        int state = preferences.getInt(SharedPreferenceKey.VPN_STATE, Constants.State.INIT.ordinal());
        mState = Constants.State.values()[state];
        if(mState == Constants.State.INIT || mState == Constants.State.STOPPED || mState == Constants.State.ERROR){
            ServerListFetcherService.fetchServerListAsync(this);
        }

        final AdAppHelper adAppHelper = AdAppHelper.getInstance(SplashActivity.this);
        boolean shouldLoadAd = true;
        if(mState == Constants.State.CONNECTED || true){
            String defaultChange = adAppHelper.getCustomCtrlValue("default", "1");
            String city = preferences.getString(SharedPreferenceKey.CONNECTING_VPN_NAME, null);
            if(city != null){
                String chanceString = adAppHelper.getCustomCtrlValue(city, defaultChange);
                float chance = 1;
                try {
                    chance = Float.parseFloat(chanceString);
                    if(chance < 0){
                        chance = 0;
                    }else if(chance > 1){
                        chance = 1;
                    }
                }catch (Exception e){
                    ShadowsocksApplication.handleException(e);
                }

                float random = (float) Math.random();
                shouldLoadAd = random < chance;
            }
        }

        if(shouldLoadAd){
            adAppHelper.loadNewInterstitial();
            adAppHelper.loadNewNative();
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        if(msg.what == MSG_CHECK_ADS){
            final AdAppHelper adAppHelper = AdAppHelper.getInstance(SplashActivity.this);
            if(adAppHelper.isFullAdLoaded() && adAppHelper.isNativeLoaded()){
                mProgressbarAnimator.setDuration(100);
            }else {
                mAdLoadedCheckHandler.sendEmptyMessageDelayed(MSG_CHECK_ADS, 1000);
            }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAdLoadedCheckHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onAnimationStart(Animator animation) {

    }

    @Override
    public void onAnimationEnd(Animator animation) {
        startActivity(new Intent(SplashActivity.this, ConnectivityActivity.class));
        finish();
    }

    @Override
    public void onAnimationCancel(Animator animation) {

    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }
}
