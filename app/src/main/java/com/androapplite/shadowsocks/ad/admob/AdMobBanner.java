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
        mAdview = new AdView(context);
        mAdview.setAdSize(size);
        mAdview.setAdUnitId(adUnitId);
        mAdview.setAdListener(createAdmobAdListener());
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

    public void resume(){
        mAdview.resume();
    }

    public void pause(){
        mAdview.pause();
    }

    @Override
    public void addToViewGroup(ViewGroup container, ViewGroup.LayoutParams layoutParams) {
        super.addToViewGroup(container, layoutParams);
        container.addView(mAdview, layoutParams);
    }
}
