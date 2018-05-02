package com.androapplite.shadowsocks;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDex;
import android.util.Log;

import com.androapplite.shadowsocks.activity.MainActivity;
import com.androapplite.shadowsocks.model.VpnState;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;

import com.bestgo.adsplugin.ads.AdAppHelper;
import com.crashlytics.android.Crashlytics;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.google.firebase.FirebaseApp;
import com.vm.shadowsocks.core.VpnNotification;

import io.fabric.sdk.android.Fabric;


/**
 * Created by jim on 16/5/2.
 */
public class ShadowsocksApplication extends Application implements HomeWatcher.OnHomePressedListener, Application.ActivityLifecycleCallbacks{
    private HomeWatcher mHomeWathcer;
    private int mOpenActivities;
    private boolean mIsFirstOpen;

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

        FirebaseApp.initializeApp(this);
        AdAppHelper.GA_RESOURCE_ID = R.xml.ga_tracker;
        AdAppHelper.FIREBASE = Firebase.getInstance(this);

        final AdAppHelper adAppHelper = AdAppHelper.getInstance(getApplicationContext());
        adAppHelper.init(null);
        mHomeWathcer = new HomeWatcher(this);
        mHomeWathcer.setOnHomePressedListener(this);
        checkVpnState();
        VpnNotification.showVpnStoppedNotificationGlobe(this, false);
        mHomeWathcer = new HomeWatcher(this);
        mHomeWathcer.startWatch();
        mIsFirstOpen = true;
        registerActivityLifecycleCallbacks(this);
    }

    private void checkVpnState(){
        SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
        int stateValue = sharedPreferences.getInt(SharedPreferenceKey.VPN_STATE, VpnState.Init.ordinal());
        Log.d("ShadowsocksApplication", "vpn state: " + stateValue);
        if (stateValue >= 0 && stateValue < VpnState.values().length) {
            VpnState state = VpnState.values()[stateValue];
            if (state == VpnState.Connected) {
                Firebase.getInstance(this).logEvent("异常", "进程刚启动", "上次启动异常中止");
            }
        }
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


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onHomePressed() {
        Firebase.getInstance(this).logEvent("按键","Home");
    }

    @Override
    public void onHomeLongPressed() {

    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {
        mOpenActivities++;
        if (activity instanceof MainActivity) {
            if (mIsFirstOpen) {
                mIsFirstOpen = false;
                PromotionTracking.getInstance(this).reportOpenMainPageCount();
            }
            PromotionTracking.getInstance(this).reportContinuousDayCount();
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {
        mOpenActivities--;
        if (mOpenActivities == 0) {
            mIsFirstOpen = true;
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}
