package com.androapplite.shadowsocks.ad;

import android.content.Context;
import android.os.Handler;

import com.google.android.gms.ads.AdListener;

import java.util.concurrent.TimeUnit;

/**
 * Created by jim on 16/8/4.
 */
public abstract class Interstitial extends AdBase{
    private Handler mTimeoutHandler;
    private Runnable mTimeoutCallback;


    protected Interstitial(Context context, @AdPlatform int platform){
        super(context, platform, AD_INTERSTItiAL);
        mTimeoutCallback = new Runnable() {
            @Override
            public void run() {
                 setAdStatus(AD_TIMEOUT);
            }
        };
    }


    public abstract void show();

    @Override
    public void load() {
       setTiemeout();
    }


    @Override
    protected void onAdOpened(){
        super.onAdOpened();
        increaseDisplayCount();
    }

    @Override
    protected void onAdError(int errorCode) {
        super.onAdError(errorCode);
        if(mTimeoutHandler != null){
            mTimeoutHandler.removeCallbacks(mTimeoutCallback);
            mTimeoutHandler = null;
        }
    }

    @Override
    protected void onAdLoaded() {
        super.onAdLoaded();
        if(mTimeoutHandler != null){
            mTimeoutHandler.removeCallbacks(mTimeoutCallback);
            mTimeoutHandler = null;
        }
    }

    @Override
    protected void onAdClosed() {
        super.onAdClosed();
        load();
    }

    private void setTiemeout(){
        if(mTimeoutHandler != null){
            mTimeoutHandler.removeCallbacks(mTimeoutCallback);
        }
        mTimeoutHandler = new Handler();
        mTimeoutHandler.postDelayed(mTimeoutCallback, TimeUnit.MINUTES.toMillis(3));
    }
}
