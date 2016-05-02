package com.androapplite.shadowsocks;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

/**
 * Created by jim on 16/5/2.
 */
public final class GAHelper {
    @NonNull
    public static final Tracker getTracker(@NonNull Context context){
        return ((ShadowsocksApplication)context.getApplicationContext()).getTracker();
    }

    public static final void sendEvent(@NonNull Context context,
                                       @NonNull String category,
                                       @NonNull String action,
                                       @NonNull String label,
                                       long value){
        getTracker(context).send(new HitBuilders.EventBuilder(category, action)
                .setLabel(label).setValue(value).build());
    }

    public static final void sendEvent(@NonNull Context context,
                                       @NonNull String category,
                                       @NonNull String action,
                                       @NonNull String label){
        getTracker(context).send(new HitBuilders.EventBuilder(category, action)
                .setLabel(label).build());
    }

    public static final void sendEvent(@NonNull Context context,
                                       @NonNull String category,
                                       @NonNull String action){
        getTracker(context).send(new HitBuilders.EventBuilder(category, action).build());
    }

    public static final void sendTimingEvent(@NonNull Context context,
                                             @NonNull String category,
                                             @NonNull String variable,
                                             long value){
        getTracker(context).send(new HitBuilders.TimingBuilder(category, variable, value).build());
    }
}
