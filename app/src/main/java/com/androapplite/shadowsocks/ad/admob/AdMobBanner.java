package com.androapplite.shadowsocks.ad.admob;

import android.content.Context;

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
        super(AD_ADMOB);
        mAdview = new AdView(context);
        mAdview.setAdSize(size);
        mAdview.setAdUnitId(adUnitId);
        mAdview.setAdListener(createAdmobAdListener());
    }

    @Override
    public void load(){
        if(!mAdview.isLoading() && getAdStatus() != AD_LOADED) {
            mAdview.loadAd(createAdmobRequest());
            setAdStatus(AD_LOADING);
        }
    }


}
