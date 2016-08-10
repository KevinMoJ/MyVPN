package com.androapplite.shadowsocks.ad.admob;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.ad.Banner;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

/**
 * Created by jim on 16/8/4.
 */
public class AdMobBanner extends Banner<AdView>{

    public AdMobBanner(Context context, String adUnitId, AdSize size){
        super(context, AD_ADMOB);
        init(context, adUnitId, size);
    }

    private void init(Context context, String adUnitId, AdSize size) {
        AdView adView = new AdView(context);
        adView.setAdSize(size);
        adView.setAdUnitId(adUnitId);
        adView.setAdListener(createAdmobAdListener());
        setAdView(adView);
    }

    public AdMobBanner(Context context, String adUnitId, AdSize size, String tag){
        super(context, AD_ADMOB, tag);
        init(context, adUnitId, size);
    }

    @Override
    public void load(){
        AdView mAdView = getAdView();
        if(!mAdView.isLoading() && getAdStatus() != AD_LOADED) {
            try {
                mAdView.loadAd(createAdmobRequest());
                setAdStatus(AD_LOADING);
                super.load();
            }catch (Exception e){
                ShadowsocksApplication.handleException(e);
                setAdStatus(AD_ERROR);
            }
        }
    }

    @Override
    public void resume(){
        getAdView().resume();
    }

    @Override
    public void pause(){
        getAdView().pause();
    }

    @Override
    public void destory() {
        AdView adView = getAdView();
        ((ViewGroup)adView.getParent()).removeView(adView);
    }

    @Override
    public String getAdId() {
        return getAdView().getAdUnitId();
    }

}
