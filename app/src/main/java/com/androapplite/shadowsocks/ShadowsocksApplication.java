package com.androapplite.shadowsocks;

import android.app.Application;
import android.os.Build;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.util.Log;

import com.androapplite.shadowsocks.util.IabBroadcastReceiver;
import com.androapplite.shadowsocks.util.IabHelper;
import com.androapplite.vpn3.BuildConfig;
import com.androapplite.vpn3.R;
import com.crashlytics.android.Crashlytics;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.smartads.plugin.GameApplication;
import com.umeng.analytics.game.UMGameAgent;

import io.fabric.sdk.android.Fabric;

/**
 * Created by jim on 16/5/2.
 */
public class ShadowsocksApplication extends GameApplication {
    private Tracker mTracker;
    IabHelper mHelper;
    IabBroadcastReceiver mBroadcastReceiver;

    @NonNull
    public Tracker getTracker(){
        if(mTracker == null){
            synchronized(this){
                if(mTracker == null){
                    final GoogleAnalytics googleAnalytics = GoogleAnalytics.getInstance(this);
                    googleAnalytics.setDryRun(BuildConfig.DEBUG);
                    mTracker = googleAnalytics.newTracker(R.xml.ga_tracker);
                    mTracker.enableAdvertisingIdCollection(true);
                    mTracker.enableExceptionReporting(!BuildConfig.DEBUG);
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
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);
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
        // Initialize the SDK before executing any other operations,
//        FacebookSdk.setIsDebugEnabled(BuildConfig.DEBUG);

        UMGameAgent.init(this);
        UMGameAgent.setTraceSleepTime(false);
        UMGameAgent.setSessionContinueMillis(60000L);
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

}
