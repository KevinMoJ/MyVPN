package com.androapplite.shadowsocks.ad;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;

import com.androapplite.shadowsocks.BuildConfig;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;

import java.lang.System;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.TimeUnit;

/**
 * Created by jim on 16/8/4.
 */
public abstract class AdBase {
    public static final int AD_INIT = 0;
    public static final int AD_LOADING = 1;
    public static final int AD_LOADED = 2;
    public static final int AD_OPENED = 3;
    public static final int AD_CLOSED = 4;
    public static final int AD_ERROR = 5;
    public static final int AD_SHOWING = 6;
    public static final int AD_TIMEOUT = 7;


    @IntDef({AD_INIT, AD_LOADING, AD_LOADED, AD_OPENED, AD_CLOSED, AD_ERROR, AD_SHOWING, AD_TIMEOUT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface AdStatus{}

    public static final int AD_BANNER = 0;
    public static final int AD_INTERSTItiAL = 1;
    public static final int AD_NATIVE = 2;
    @IntDef({AD_BANNER, AD_INTERSTItiAL, AD_NATIVE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface AdType{}
    private static final String[] AD_TYPE_TEXT = {"AD_BANNER", "AD_INTERSTItiAL", "AD_NATIVE"};

    public static final int AD_ADMOB = 0;
    public static final int AD_FACEBOOK = 1;
    @IntDef({AD_ADMOB, AD_FACEBOOK})
    @Retention(RetentionPolicy.SOURCE)
    public @interface AdPlatform{}
    private static final String[] AD_PLATFORM_TEXT = {"AD_ADMOB", "AD_FACEBOOK"};

    private volatile @AdStatus int mAdStatus;
    private @AdType int mAdType;
    private @AdPlatform int mAdplatform;
    private OnAdLoadListener mListener;
    private String mTag;
    private int mDisplayCount;
    private SharedPreferences mSharedPreference;
    private Handler mTimeoutHandler;
    private Runnable mTimeoutCallback;
    private long mLoadingStartTime;
    private long mLoadingDuration;

    protected AdBase(Context context, @AdPlatform int platform, @AdType int type){
        mAdplatform = platform;
        mAdType = type;
        mAdStatus = AD_INIT;
        mDisplayCount = 0;
        mSharedPreference = PreferenceManager.getDefaultSharedPreferences(context);
        mTimeoutCallback = new Runnable() {
            @Override
            public void run() {
                setAdStatus(AD_TIMEOUT);
            }
        };

    }

//    protected AdBase(@AdPlatform int platform, @AdType int type, String tag){
//        this(platform, type);
//        mTag = tag;
//    }

    public interface OnAdLoadListener{
        void onAdLoaded(AdBase adBase);
        void onAdOpened(AdBase adBase);
        void onAdClosed(AdBase adBase);
        void onAdError(AdBase adBase, int errorCode);
    }

    protected AdRequest createAdmobRequest(){
        AdRequest.Builder builder = new AdRequest.Builder();
        if(BuildConfig.DEBUG) {
            //nexus 4 android 4.4
            builder.addTestDevice("7CE6E7BA2138D164DA9BE6641B0F5BDD");
            //我的手机 nexus 5 android 6
            builder.addTestDevice("8FB883F20089D8E653BB8D6D06A1EB3A");
        }
        return  builder.build();
    }

    protected synchronized void setAdStatus(@AdStatus int status){
        mAdStatus = status;
        ShadowsocksApplication.debug("广告Status", mAdStatus + "");
    }

    public synchronized  @AdStatus int getAdStatus(){
        return  mAdStatus;
    }

    public void setAdLoadListener(OnAdLoadListener listener){
        mListener = listener;
    }

    public OnAdLoadListener getAdLoadListener(){
        return mListener;
    }

    public void setTag(String tag){
        mTag = tag;
    }

    public String getTag(){
        return mTag != null ? mTag : "";
    }

    protected AdListener createAdmobAdListener(){
        return new AdListener() {
            @Override
            public void onAdClosed() {
                setAdStatus(AD_CLOSED);
                AdBase.this.onAdClosed();
                ShadowsocksApplication.debug("广告Status", mAdStatus + "");

            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                setAdStatus(AD_ERROR);
                AdBase.this.onAdError(errorCode);
                ShadowsocksApplication.debug("广告Status", mAdStatus + " " + errorCode);

            }

            @Override
            public void onAdOpened() {
                setAdStatus(AD_OPENED);
                AdBase.this.onAdOpened();
                ShadowsocksApplication.debug("广告Status", mAdStatus + "");

            }

            @Override
            public void onAdLoaded() {
                setAdStatus(AD_LOADED);
                AdBase.this.onAdLoaded();
                ShadowsocksApplication.debug("广告Status", mAdStatus + "");

            }
        };
    }

    protected void onAdClosed(){
        if(mListener != null){
            mListener.onAdClosed(this);
        }
        load();
    }

    protected void onAdError(int errorCode){
        if(mListener != null){
            mListener.onAdError(this, errorCode);
        }
        clearTimeout();
    }

    private void clearTimeout(){
        if(mTimeoutHandler != null){
            mTimeoutHandler.removeCallbacks(mTimeoutCallback);
            mTimeoutHandler = null;

            if(mLoadingStartTime != 0){
                mLoadingDuration = System.currentTimeMillis() - mLoadingStartTime;
                mLoadingStartTime = 0;
            }
        }
    }

    protected void onAdOpened(){
        if(mListener != null){
            mListener.onAdOpened(this);
        }
    }

    protected void onAdLoaded(){
        if(mListener != null){
            mListener.onAdLoaded(this);
        }
        clearTimeout();
    }

    public void load(){
        setTiemeout();
    }

    private void setTiemeout(){
        if(mTimeoutHandler != null){
            mTimeoutHandler.removeCallbacks(mTimeoutCallback);
        }
        mTimeoutHandler = new Handler();
        mTimeoutHandler.postDelayed(mTimeoutCallback, TimeUnit.MINUTES.toMillis(3));
        mLoadingStartTime = System.currentTimeMillis();
    }

    public int getDisplayCount(){
        return mDisplayCount;
    }

    protected void increaseDisplayCount(){
        if(mDisplayCount < Integer.MAX_VALUE) {
            mDisplayCount++;
        }
    }

    protected void saveDisplayCount(){
        String uniqueTag = getAdPlatformText() + getAdTypeText() + getTag();
        mSharedPreference.edit().putInt(uniqueTag, mDisplayCount).apply();
    }

    protected void loadDisplayCount(){
        String uniqueTag = getAdPlatformText() + getAdTypeText() + getTag();
        mDisplayCount = mSharedPreference.getInt(uniqueTag, 0);
    }


    public String getAdTypeText(){
        return AD_TYPE_TEXT[mAdType];
    }

    public String getAdPlatformText(){
        return AD_PLATFORM_TEXT[mAdplatform];
    }



}
