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
public class AdMobBanner extends Banner{
    private AdView mAdview;

    public AdMobBanner(Context context, String adUnitId, AdSize size){
        super(context, AD_ADMOB);
        init(context, adUnitId, size);
    }

    private void init(Context context, String adUnitId, AdSize size) {
        mAdview = new AdView(context);
        mAdview.setAdSize(size);
        mAdview.setAdUnitId(adUnitId);
        mAdview.setAdListener(createAdmobAdListener());
    }

    public AdMobBanner(Context context, String adUnitId, AdSize size, String tag){
        super(context, AD_ADMOB, tag);
        init(context, adUnitId, size);
    }

    @Override
    public void load(){
        if(!mAdview.isLoading() && getAdStatus() != AD_LOADED) {
            try {
                mAdview.loadAd(createAdmobRequest());
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
        mAdview.resume();
    }

    @Override
    public void pause(){
        mAdview.pause();
    }

    @Override
    public void addToViewGroup(ViewGroup container, ViewGroup.LayoutParams layoutParams) {
        super.addToViewGroup();
        container.addView(mAdview, layoutParams);
    }

    @Override
    public void addToViewGroup(ViewGroup container){
        super.addToViewGroup();
        container.addView(mAdview);
    }

    @Override
    public void destory() {
        ((ViewGroup)mAdview.getParent()).removeView(mAdview);
    }

    @Override
    public String getAdId() {
        return mAdview.getAdUnitId();
    }

}
