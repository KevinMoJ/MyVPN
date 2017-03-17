package com.androapplite.shadowsocks;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.androapplite.shadowsocks.activity.ConnectivityActivity;
import com.androapplite.shadowsocks.broadcast.ReportUseTimeReceiver;
import com.androapplite.shadowsocks.util.IabBroadcastReceiver;
import com.androapplite.shadowsocks.util.IabHelper;
import com.androapplite.vpn3.BuildConfig;
import com.bestgo.adsplugin.ads.AbstractFirebase;
import com.bestgo.adsplugin.ads.AdAppHelper;
import com.crashlytics.android.Crashlytics;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;

import io.fabric.sdk.android.Fabric;

/**
 * Created by jim on 16/5/2.
 */
public class ShadowsocksApplication extends Application implements Application.ActivityLifecycleCallbacks {
    IabHelper mHelper;
    IabBroadcastReceiver mBroadcastReceiver;
    private int mRunningActivityNum;
    private ArrayList<Activity> mActivitys;


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
//        FacebookSdk.setIsDebugEnabled(BuildConfig.DEB

        registerActivityLifecycleCallbacks(this);
        
        AdAppHelper.FIREBASE = new FirebaseAdapter(this);
        final AdAppHelper adAppHelper = AdAppHelper.getInstance(getApplicationContext());
        adAppHelper.init();
        mActivitys = new ArrayList<>();
        reportDailyUseTime(this);
    }

    private static class FirebaseAdapter extends AbstractFirebase{
        private WeakReference<Context> mContextReference;

        FirebaseAdapter(Context context){
            mContextReference = new WeakReference<Context>(context);
        }

        @Override
        public void logEvent(String key, Bundle values) {
            Context context = mContextReference.get();
            if(context != null){
                FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(context);
                analytics.logEvent(key, values);
            }
        }

        @Override
        public void logEvent(String key, long value) {
            Bundle bundle = new Bundle();
            bundle.putLong("Value", value);
            logEvent(key, bundle);
        }

        @Override
        public void logEvent(String key, String name, long value) {
            Bundle bundle = new Bundle();
            bundle.putString("Name", name);
            bundle.putLong("Value", value);
            logEvent(key, bundle);
        }

        @Override
        public void logEvent(String key, String name, String value) {
            Bundle bundle = new Bundle();
            bundle.putString("Name", name);
            bundle.putString("Value", value);
            logEvent(key, bundle);
        }

        @Override
        public void logEvent(String key, String value) {
            Bundle bundle = new Bundle();
            bundle.putString("Value", value);
            logEvent(key, bundle);
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
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        mActivitys.add(activity);
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
            for(Activity activity1 : mActivitys){
                activity1.finish();
            }
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        mActivitys.remove(activity);
    }

    public int getRunningActivityCount(){
        return mRunningActivityNum;
    }

    public static void reportDailyUseTime(Context context){
        Intent intent = new Intent(context, ReportUseTimeReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 24);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        alarmManager.setRepeating(AlarmManager.RTC, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
    }
}
