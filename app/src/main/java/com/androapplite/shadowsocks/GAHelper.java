package com.androapplite.shadowsocks;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * Created by jim on 16/5/2.
 */
public final class GAHelper {
    public static final void sendEvent(@NonNull Context context,
                                       @NonNull String category,
                                       @NonNull String action,
                                       @NonNull String label,
                                       long value){
        FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(context);
        Bundle params = new Bundle();
        params.putString("Action", action);
        params.putString("Label", label);
        params.putLong("Value", value);
        analytics.logEvent(category, params);
    }

    public static final void sendEvent(@NonNull Context context,
                                       @NonNull String category,
                                       @NonNull String action,
                                       @NonNull String label){
        FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(context);
        Bundle params = new Bundle();
        params.putString("Action", action);
        params.putString("Label", label);
        analytics.logEvent(category, params);
    }

    public static final void sendEvent(@NonNull Context context,
                                       @NonNull String category,
                                       @NonNull String action){
        FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(context);
        Bundle params = new Bundle();
        params.putString("Action", action);
        analytics.logEvent(category, params);
    }

    public static final void sendTimingEvent(@NonNull Context context,
                                             @NonNull String category,
                                             @NonNull String variable,
                                             long value,
                                             @NonNull String label){
        FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(context);
        Bundle params = new Bundle();
        params.putString("Variable", variable);
        params.putLong("Value", value);
        params.putString("Label", label);
        analytics.logEvent(category, params);
    }

    public static final void sendTimingEvent(@NonNull Context context,
                                             @NonNull String category,
                                             @NonNull String variable,
                                             long value){
        FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(context);
        Bundle params = new Bundle();
        params.putString("Variable", variable);
        params.putLong("Value", value);
        analytics.logEvent(category, params);
    }

    public static final void sendScreenView(@NonNull Context context, @NonNull String screenView){

    }
}
