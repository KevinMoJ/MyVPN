package com.androapplite.shadowsocks.ad.facebook;

import android.content.Context;
import android.view.ViewGroup;

import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.ad.AdBase;
import com.androapplite.shadowsocks.ad.Banner;
import com.facebook.ads.AdSize;
import com.facebook.ads.AdView;

/**
 * Created by jim on 16/8/10.
 */
public class FacebookBanner extends Banner {
    private AdView mAdview;

    public FacebookBanner(Context context, String adUnitId, AdSize size){
        super(context, AD_FACEBOOK);
        init(context, adUnitId, size);
    }

    private void init(Context context, String adUnitId, AdSize size) {
        mAdview = new AdView(context, adUnitId, size);
        mAdview.setAdListener(createFacebookAdListener());
    }

    public FacebookBanner(Context context, String adUnitId, AdSize size, String tag){
        super(context, AD_FACEBOOK, tag);
        init(context, adUnitId, size);
    }

    @Override
    public void resume() {

    }

    @Override
    public void pause() {
    }

    @Override
    public void destory() {
        ((ViewGroup)mAdview.getParent()).removeView(mAdview);
    }

    @Override
    public String getAdId() {
        return getAdId();
    }

    @Override
    public void load(){
        if(getAdStatus() != AD_LOADED) {
            try {
                mAdview.loadAd();
                setAdStatus(AD_LOADING);
                super.load();
            }catch (Exception e){
                ShadowsocksApplication.handleException(e);
                setAdStatus(AD_ERROR);
            }
        }
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
}
