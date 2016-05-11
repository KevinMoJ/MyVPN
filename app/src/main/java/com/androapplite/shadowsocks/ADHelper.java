package com.androapplite.shadowsocks;

import android.content.Context;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.NativeAd;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Created by jim on 16/5/11.
 */
public final class AdHelper {
    private Context mContext;
    private NativeAd mFacebookAd;
    private AdView mAdmobAd;
    private static final String ADMOB_ID = "ca-app-pub-5330675173640052/3927500528";
    private static final String FACEBOOK_ID = "";
    public static final int AD_INIT = 0;
    public static final int AD_LOADING = 1;
    public static final int AD_LOADED = 2;
    public static final int AD_CLICKED = 3;
    public static final int AD_CLOSED = 4;
    public static final int AD_ERROR = 5;
    private @AdState int mFacebookAdState;
    private @AdState int mAdmobAdState;

    @IntDef({AD_INIT, AD_LOADING, AD_LOADED, AD_CLICKED, AD_CLOSED, AD_ERROR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface AdState{}


    public AdHelper(@NonNull Context context){
        mContext = context;
        mAdmobAd = new AdView(context);
        mFacebookAd = new NativeAd(context, FACEBOOK_ID);
        initAdmob();
        mFacebookAdState = AD_INIT;
        mAdmobAdState = AD_INIT;

    }

    public void initAdmob(){
        mAdmobAd.setAdSize(AdSize.SMART_BANNER);
        mAdmobAd.setAdUnitId(ADMOB_ID);

    }

    private static AdRequest createAdmobRequest(){
        AdRequest.Builder builder = new AdRequest.Builder();
        return  builder.build();
    }

    public void setAdmobAdListener(final OnAdLoadListener listener){
        mAdmobAd.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(int errorCode) {
                mAdmobAdState = AD_ERROR;
                if(listener != null){
                    listener.onError(mAdmobAd);
                }
            }

            @Override
            public void onAdLoaded() {
                mAdmobAdState = AD_LOADED;
                if(listener != null){
                    listener.onAdLoaded(mAdmobAd);
                }
            }

            @Override
            public void onAdOpened() {
                mAdmobAdState = AD_CLICKED;
                if(listener != null){
                    listener.onAdClicked(mAdmobAd);
                }
            }

            @Override
            public void onAdClosed() {
                mAdmobAdState = AD_CLOSED;
            }
        });
    }

    public void loadAdmobAd(){
        mAdmobAd.loadAd(createAdmobRequest());
        mAdmobAdState = AD_LOADING;
    }

    public void setFacebookAdListener(final OnAdLoadListener listener){
        mFacebookAd.setAdListener(new com.facebook.ads.AdListener() {
            @Override
            public void onError(Ad ad, AdError adError) {
                mFacebookAdState = AD_ERROR;
                if(listener != null){
                    listener.onError(ad);
                }
            }

            @Override
            public void onAdLoaded(Ad ad) {
                mFacebookAdState = AD_LOADED;
                if(listener != null){
                    listener.onAdLoaded(ad);
                }
            }

            @Override
            public void onAdClicked(Ad ad) {
                mFacebookAdState = AD_CLICKED;
                if(listener != null){
                    listener.onAdClicked(ad);
                }
            }

        });
    }

    public void loadFacebookAd(){
        mFacebookAd.loadAd();
        mFacebookAdState = AD_LOADING;
    }

    public interface OnAdLoadListener{
        void onAdLoaded(Object ad);
        void onAdClicked(Object ad);
        void onError(Object ad);
    }



}
