package com.androapplite.shadowsocks;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

/**
 * Created by jim on 16/5/2.
 */
public class ShadowsocksApplication extends Application {
    private Tracker mTracker;

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
}
