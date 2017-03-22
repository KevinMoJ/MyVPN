package com.androapplite.shadowsocks;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.bestgo.adsplugin.ads.AbstractFirebase;
import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * Created by LScorpio on 2017/3/7.
 */
public class Firebase extends AbstractFirebase {

    private static Firebase instance;
    private Context context;
    private FirebaseAnalytics mFireInstance;

    public Firebase(Context context) {
        this.context = context;
    }

    public static Firebase getInstance(Context context) {
        if(instance == null){
            synchronized (Firebase.class){
                if (instance == null) {
                    instance = new Firebase(context);
                }
            }
        }
        return instance;
    }

    private FirebaseAnalytics getFirebaseAnalytics() {
        if (mFireInstance == null) {
            try {
                mFireInstance = FirebaseAnalytics.getInstance(context);
            } catch (Exception e) {

            }
        }
        return mFireInstance;
    }

    public void logEvent(String category, String action) {
        FirebaseAnalytics firebaseAnalytics = instance.getFirebaseAnalytics();
        Bundle bundle = new Bundle();
        bundle.putString("action", cutStringIfNecessary(action));
        firebaseAnalytics.logEvent(category, bundle);
    }

    public void logEvent(String category, String action, String label) {
        FirebaseAnalytics firebaseAnalytics = instance.getFirebaseAnalytics();
        Bundle bundle = new Bundle();
        bundle.putString("action", cutStringIfNecessary(action));
        bundle.putString("label", cutStringIfNecessary(label));
        firebaseAnalytics.logEvent(category, bundle);
    }

    public void logEvent(String category, String action, String label, long value) {
        FirebaseAnalytics firebaseAnalytics = instance.getFirebaseAnalytics();
        Bundle bundle = new Bundle();
        bundle.putString("action", cutStringIfNecessary(action));
        bundle.putString("label", cutStringIfNecessary(label));
        bundle.putLong("value", value);
        firebaseAnalytics.logEvent(category, bundle);
    }

    public void logEvent(String category, String action, String label, double value) {
        FirebaseAnalytics firebaseAnalytics = instance.getFirebaseAnalytics();
        Bundle bundle = new Bundle();
        bundle.putString("action", cutStringIfNecessary(action));
        bundle.putString("label", cutStringIfNecessary(label));
        bundle.putDouble("value", value);
        firebaseAnalytics.logEvent(category, bundle);
    }

    public void logEvent(String category, String action, long value) {
        FirebaseAnalytics firebaseAnalytics = instance.getFirebaseAnalytics();
        Bundle bundle = new Bundle();
        bundle.putString("action", cutStringIfNecessary(action));
        bundle.putLong("value", value);
        firebaseAnalytics.logEvent(category, bundle);
    }

    public void logEvent(String category, String action, double value) {
        FirebaseAnalytics firebaseAnalytics = instance.getFirebaseAnalytics();
        Bundle bundle = new Bundle();
        bundle.putString("action", cutStringIfNecessary(action));
        bundle.putDouble("value", value);
        firebaseAnalytics.logEvent(category, bundle);
    }

    public void logEvent(String category, long value) {
        FirebaseAnalytics firebaseAnalytics = instance.getFirebaseAnalytics();
        Bundle bundle = new Bundle();
        bundle.putString("action", cutStringIfNecessary(category));
        bundle.putLong("value", value);
        firebaseAnalytics.logEvent(category, bundle);
    }

    public void logEvent(String category, double value) {
        FirebaseAnalytics firebaseAnalytics = instance.getFirebaseAnalytics();
        Bundle bundle = new Bundle();
        bundle.putString("action", cutStringIfNecessary(category));
        bundle.putDouble("value", value);
        firebaseAnalytics.logEvent(category, bundle);
    }

    public void logEvent(String category, Bundle values) {
        FirebaseAnalytics firebaseAnalytics = instance.getFirebaseAnalytics();
        firebaseAnalytics.logEvent(category, values);
    }

    private String cutStringIfNecessary(String v) {
        if (!TextUtils.isEmpty(v) && v.length() > 100) {
            return v.substring(0, 100);
        }
        return v;
    }

    //设置是否开启数据收集功能
    public void setAnalyticsCollectionEnabled(boolean enabled) {
        instance.getFirebaseAnalytics().setAnalyticsCollectionEnabled(enabled);
    }

    public void setMinimumSessionDuration(long milliseconds) {
        instance.getFirebaseAnalytics().setMinimumSessionDuration(milliseconds);
    }

    public void setUserId(String userId) {
        instance.getFirebaseAnalytics().setUserId(userId);
    }

    public void setSessionTimeoutDuration(long milliseconds) {
        instance.getFirebaseAnalytics().setSessionTimeoutDuration(milliseconds);
    }

    public void setUserProperty(String name, String value) {
        instance.getFirebaseAnalytics().setUserProperty(name, value);
    }

}
