package com.androapplite.shadowsocks;

import android.app.Application;
import android.os.Build;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.util.Log;

import com.androapplite.shadowsocks.util.IabBroadcastReceiver;
import com.androapplite.shadowsocks.util.IabHelper;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import io.fabric.sdk.android.Fabric;

/**
 * Created by jim on 16/5/2.
 */
public class ShadowsocksApplication extends Application {
    private Tracker mTracker;
    IabHelper mHelper;
    IabBroadcastReceiver mBroadcastReceiver;
    private InterstitialAd mInterstitialAd;


    @NonNull
    public Tracker getTracker(){
        if(mTracker == null){
            synchronized(this){
                if(mTracker == null){
                    final GoogleAnalytics googleAnalytics = GoogleAnalytics.getInstance(this);
                    googleAnalytics.setDryRun(BuildConfig.DEBUG);
                    mTracker = googleAnalytics.newTracker(R.xml.ga_tracker);
                    mTracker.enableAdvertisingIdCollection(true);
                    return mTracker;
                }else{
                    return mTracker;
                }
            }
        }else{
            return mTracker;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        if (BuildConfig.DEBUG) {
            //谷歌插页广告导致资源泄露
//            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
//                    .detectAll()
//                    .penaltyLog()
//                    .build());
//            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
//                    .detectAll()
//                    .penaltyLog()
//                    .penaltyDeath()
//                    .build());
        }
//        mHelper = new IabHelper(this, base64EncodedPublicKey);
//        mHelper.enableDebugLogging(BuildConfig.DEBUG);

        initInterstitialAd();
        loadInterstitialAd();
    }

    public static final void debug(@NonNull String tag, @NonNull String msg){
        if(BuildConfig.DEBUG){
            Log.d(tag, msg);
        }
    }

    public static final void handleException(@NonNull Throwable throwable){
        if(BuildConfig.DEBUG){
            throwable.printStackTrace();
        }else{
            Crashlytics.logException(throwable);
        }
    }

    private void initInterstitialAd() {
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.admob_interstitial_id));
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                ShadowsocksApplication.debug("插页广告", "关闭");
            }

            @Override
            public void onAdLoaded() {
                ShadowsocksApplication.debug("插页广告", "加载完成");
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                ShadowsocksApplication.debug("插页广告", "加载错误" + errorCode);
            }


        });
    }

    public void loadInterstitialAd(){
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("8FB883F20089D8E653BB8D6D06A1EB3A")
                .build();

        mInterstitialAd.loadAd(adRequest);
    }

    public InterstitialAd getInterstitialAd(){
        return mInterstitialAd;
    }
}
