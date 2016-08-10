package com.androapplite.shadowsocks.ad.facebook;

import android.content.Context;

import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.ad.AdBase;
import com.androapplite.shadowsocks.ad.Interstitial;
import com.facebook.ads.InterstitialAd;
import com.facebook.ads.InterstitialAdListener;

/**
 * Created by jim on 16/8/10.
 */
public class FacebookInterstitial extends Interstitial {
    private InterstitialAd mInterstitial;
    private String mAdUnitId;

    public FacebookInterstitial(Context context, String adUnitId){
        super(context, AD_FACEBOOK);
        init(context, adUnitId);
    }

    private void init(Context context, String adUnitId){
        mInterstitial = new InterstitialAd(context, adUnitId);
        mInterstitial.setAdListener(createFacebookAdListener());
        mAdUnitId = adUnitId;
    }

    public FacebookInterstitial(Context context, String adUnitId, String tag) {
        super(context, AD_FACEBOOK, tag);
        init(context, adUnitId);
    }

    @Override
    public void load(){
        if(getAdStatus() != AD_LOADED){
            ShadowsocksApplication.debug("广告load", "" + getAdStatus());
            try{
                mInterstitial.loadAd();
                setAdStatus(AD_LOADING);
                super.load();
            }catch (Exception e){
                ShadowsocksApplication.handleException(e);
                setAdStatus(AD_ERROR);
            }
        }
    }

    @Override
    public void show() {
        if(mInterstitial.isAdLoaded()) {
            mInterstitial.show();
            setAdStatus(AD_SHOWING);
            setShowWhenLoaded(false);
        }else{
            setShowWhenLoaded(true);
        }
    }

    @Override
    public String getAdId() {
        return mAdUnitId;
    }
}
