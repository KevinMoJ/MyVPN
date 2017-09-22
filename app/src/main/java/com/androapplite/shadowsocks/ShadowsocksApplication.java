package com.androapplite.shadowsocks;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDex;
import android.util.Log;

import com.androapplite.shadowsocks.activity.ConnectivityActivity;
import com.androapplite.shadowsocks.broadcast.ReportUseTimeReceiver;
import com.androapplite.shadowsocks.service.AutoRestartService;
import com.androapplite.shadowsocks.util.IabBroadcastReceiver;
import com.androapplite.shadowsocks.util.IabHelper;
import com.bestgo.adsplugin.ads.AdAppHelper;
import com.crashlytics.android.Crashlytics;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.google.firebase.FirebaseApp;

import java.util.ArrayList;
import java.util.Calendar;

import io.fabric.sdk.android.Fabric;

/**
 * Created by jim on 16/5/2.
 */
public class ShadowsocksApplication extends Application implements Application.ActivityLifecycleCallbacks
    ,HomeWatcher.OnHomePressedListener{
    IabHelper mHelper;
    IabBroadcastReceiver mBroadcastReceiver;
    private int mRunningActivityNum;
    private ArrayList<Activity> mActivitys;
    private HomeWatcher mHomeWathcer;
    private boolean mIsHomeKeyPressed;


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
        FirebaseApp.initializeApp(this);
        AdAppHelper.FIREBASE = Firebase.getInstance(this);
        AdAppHelper.GA_RESOURCE_ID = R.xml.ga_tracker;
//        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
//        int widthDp = (int) (displayMetrics.widthPixels / displayMetrics.density);
//        AdAppHelper.NEWS_ADMOB_WIDTH = widthDp - 10;
//        AdAppHelper.NEWS_TITLE_COLOR = Color.BLACK;
//        AdAppHelper.NEWS_DETAIL_COLOR = Color.GRAY;
//        AdAppHelper.NATIVE_ADMOB_WIDTH_LIST = getResources().getDimensionPixelOffset(R.dimen.half_standard_margin);
        final AdAppHelper adAppHelper = AdAppHelper.getInstance(getApplicationContext());
        adAppHelper.init();
//        FacebookSdk.setIsDebugEnabled(BuildConfig.DEBUG);
        registerActivityLifecycleCallbacks(this);
        AdAppHelper.getInstance(getApplicationContext()).init();
        mActivitys = new ArrayList<>();
        reportDailyUseTime();
        AutoRestartService.startService(this);

        mHomeWathcer = new HomeWatcher(this);
        mHomeWathcer.setOnHomePressedListener(this);

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
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        mActivitys.add(activity);
        if(mActivitys.size() == 1){
            mHomeWathcer.startWatch();
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {
        mRunningActivityNum++;
    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {
        mRunningActivityNum--;
        if(mRunningActivityNum == 0){
            if(mIsHomeKeyPressed){
                for (Activity activity1 : mActivitys) {
                    activity1.finish();
                }
            }
            mIsHomeKeyPressed = false;
        }

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        mActivitys.remove(activity);
        if(mActivitys.isEmpty()){
            mHomeWathcer.stopWatch();
        }
    }

    public void reportDailyUseTime(){
        Intent intent = new Intent(this, ReportUseTimeReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

        AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        alarmManager.setRepeating(AlarmManager.RTC, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }


    @Override
    public void onHomePressed() {
        mIsHomeKeyPressed = true;
    }

    @Override
    public void onHomeLongPressed() {

    }
}
