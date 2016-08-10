package com.androapplite.shadowsocks.ad.admob;

import android.content.Context;

import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.ad.AdBase;
import com.androapplite.shadowsocks.ad.Interstitial;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.InterstitialAd;

/**
 * Created by jim on 16/8/4.
 */
public class AdMobInterstitial extends Interstitial{
    private volatile InterstitialAd mInterstitial;

    public AdMobInterstitial(Context context, String adUnitId){
        super(context, AD_ADMOB);
        init(context, adUnitId);
    }

    private void init(Context context, String adUnitId) {
        mInterstitial = new InterstitialAd(context);
        mInterstitial.setAdUnitId(adUnitId);
        mInterstitial.setAdListener(createAdmobAdListener());
    }

    public AdMobInterstitial(Context context, String adUnitId, String tag){
        super(context, AD_ADMOB, tag);
        init(context, adUnitId);
    }

    @Override
    public void load(){
        if(!mInterstitial.isLoading() && getAdStatus() != AD_LOADED){
            ShadowsocksApplication.debug("广告load", mInterstitial.isLoading() + " " + getAdStatus());
            try{
                mInterstitial.loadAd(createAdmobRequest());
                setAdStatus(AD_LOADING);
                super.load();
            }catch (Exception e){
                ShadowsocksApplication.handleException(e);
                setAdStatus(AD_ERROR);
            }
        }
    }

    @Override
    public void show(){
        if(mInterstitial.isLoaded()) {
            mInterstitial.show();
            setAdStatus(AD_SHOWING);
            setShowWhenLoaded(false);
        }else{
            setShowWhenLoaded(true);
        }
    }

    @Override
    public String getAdId() {
        return mInterstitial.getAdUnitId();
    }
}
