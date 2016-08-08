package com.androapplite.shadowsocks.ad;

import android.content.Context;
import android.os.Handler;

import com.google.android.gms.ads.AdListener;

import java.util.concurrent.TimeUnit;

/**
 * Created by jim on 16/8/4.
 */
public abstract class Interstitial extends AdBase{
    private boolean mShowWhenLoaded;

    protected Interstitial(Context context, @AdPlatform int platform){
        super(context, platform, AD_INTERSTItiAL);
    }


    public abstract void show();

    @Override
    protected void onAdOpened(){
        super.onAdOpened();
        increaseDisplayCount();
    }

    protected void setShowWhenLoaded(boolean showWhenLoaded){
        mShowWhenLoaded = showWhenLoaded;
    }

    @Override
    protected void onAdLoaded() {
        super.onAdLoaded();
        if(mShowWhenLoaded){
            show();
        }
    }
}
