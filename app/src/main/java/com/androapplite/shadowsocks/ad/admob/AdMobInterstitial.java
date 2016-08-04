package com.androapplite.shadowsocks.ad.admob;

import android.content.Context;

import com.androapplite.shadowsocks.ad.AdBase;
import com.androapplite.shadowsocks.ad.Interstitial;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.InterstitialAd;

/**
 * Created by jim on 16/8/4.
 */
public class AdMobInterstitial extends Interstitial{
    private InterstitialAd mInterstitial;

    public AdMobInterstitial(Context context, String adUnitId){
        super(AD_ADMOB);
        mInterstitial = new InterstitialAd(context);
        mInterstitial.setAdUnitId(adUnitId);
        mInterstitial.setAdListener(createAdmobAdListener());
    }

    public AdMobInterstitial(Context context, String adUnitId, String tag){
        this(context, adUnitId);
        setTag(tag);
    }

    @Override
    public void load(){
        if(!mInterstitial.isLoading() && getAdStatus() != AD_LOADED){
            mInterstitial.loadAd(createAdmobRequest());
            setAdStatus(AD_LOADING);
        }
    }

    @Override
    public void show(){
        mInterstitial.show();
    }

/*
new AdListener() {
            @Override
            public void onAdClosed() {
                setAdStatus(AD_CLOSED);
                OnAdLoadListener listener = getAdLoadListener();
                if(listener != null){
                    listener.onAdClosed();
                }
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                setAdStatus(AD_ERROR);
                OnAdLoadListener listener = getAdLoadListener();
                if(listener != null){
                    listener.onError(errorCode);
                }
            }

            @Override
            public void onAdOpened() {
                setAdStatus(AD_OPENED);
                OnAdLoadListener listener = getAdLoadListener();
                if(listener != null){
                    listener.onAdOpened();
                }
            }

            @Override
            public void onAdLoaded() {
                setAdStatus(AD_LOADED);
                OnAdLoadListener listener = getAdLoadListener();
                if(listener != null){
                    listener.onAdLoaded();
                }
            }
        }
 */


}
