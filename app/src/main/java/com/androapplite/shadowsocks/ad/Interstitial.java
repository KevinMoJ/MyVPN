package com.androapplite.shadowsocks.ad;

import com.google.android.gms.ads.AdListener;

/**
 * Created by jim on 16/8/4.
 */
public abstract class Interstitial extends AdBase{
    private int mDisplayCount;


    protected Interstitial(@AdPlatform int platform){
        super(platform, AD_INTERSTItiAL);
        mDisplayCount = 0;
    }


    public abstract void show();

//    protected Interstitial(@AdPlatform int platform, String tag){
//        super(platform, AD_INTERSTItiAL, tag);
//    }

    @Override
    protected void onAdOpened(){
        super.onAdOpened();
        mDisplayCount++;
    }
}
