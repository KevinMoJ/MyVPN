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
        if(!mInterstitial.isLoading() ){
            final int status = getAdStatus();
            if(status == AD_INIT || status == AD_CLOSED || status == AD_TIMEOUT || status == AD_RETRY_FAILED) {
                ShadowsocksApplication.debug("广告load", mInterstitial.isLoading() + " " + getAdStatusText());
                try {
                    mInterstitial.loadAd(createAdmobRequest());
                    setAdStatus(AD_LOADING);
                    super.load();
                    ShadowsocksApplication.debug("广告load", mInterstitial.isLoading() + " " + getAdStatusText());
                } catch (Exception e) {
                    ShadowsocksApplication.handleException(e);
                    setAdStatus(AD_ERROR);
                }
            }
        }
    }

    @Override
    public void show(){
        if(mInterstitial.isLoaded() && getAdStatus() == AD_LOADED) {
            ShadowsocksApplication.debug("广告show", mInterstitial.isLoading() + " " + getAdStatusText());
            mInterstitial.show();
            setAdStatus(AD_SHOWING);
            setShowWhenLoaded(false);
            ShadowsocksApplication.debug("广告show", mInterstitial.isLoading() + " " + getAdStatusText());

        }else{
            setShowWhenLoaded(true);
        }
    }

    @Override
    public String getAdId() {
        return mInterstitial.getAdUnitId();
    }
}
