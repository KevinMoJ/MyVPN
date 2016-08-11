package com.androapplite.shadowsocks.ad;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import com.androapplite.shadowsocks.BuildConfig;
import com.androapplite.shadowsocks.GAHelper;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.facebook.ads.AbstractAdListener;
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
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
    private static final String[] AD_STATUS_TEXT = {"初始化", "加载中", "加载完", "打开", "关闭", "错误", "显示中", "加载超时"};

    public static final int AD_BANNER = 0;
    public static final int AD_INTERSTItiAL = 1;
    public static final int AD_NATIVE = 2;
    @IntDef({AD_BANNER, AD_INTERSTItiAL, AD_NATIVE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface AdType{}
    private static final String[] AD_TYPE_TEXT = {"AD_BANNER", "AD_INTERSTITIAL", "AD_NATIVE"};

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
    private Context mApplicationContext;
    private int mMaxRetryCount;
    private int mRetryCount;

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
                sendLoadingTimeToGA();
            }
        };
        mApplicationContext = context.getApplicationContext();
        mMaxRetryCount = 1;

    }
    protected AdBase(Context context, @AdPlatform int platform, @AdType int type, String tag){
        this(context, platform, type);
        mTag = tag;
    }

    private void sendLoadingTimeToGA() {
        if(mLoadingDuration != 0) {
            String uniqueTag = getFullTag();
            GAHelper.sendTimingEvent(mApplicationContext, uniqueTag, getAdStatusText(), mLoadingDuration, getAdId());
        }
    }


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
        ShadowsocksApplication.debug("广告", getAdStatusText());
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
                AdBase.this.onAdClosed();
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                AdBase.this.onAdError(errorCode);
            }

            @Override
            public void onAdOpened() {
                AdBase.this.onAdOpened();
            }

            @Override
            public void onAdLoaded() {
                AdBase.this.onAdLoaded();
            }
        };
    }

    protected AbstractAdListener createFacebookAdListener(){
        return new AbstractAdListener() {
            @Override
            public void onError(Ad ad, AdError adError) {
                AdBase.this.onAdError(adError.getErrorCode());
            }

            @Override
            public void onAdLoaded(Ad ad) {
                AdBase.this.onAdLoaded();
            }

            @Override
            public void onAdClicked(Ad ad) {
                AdBase.this.onAdOpened();
            }

            @Override
            public void onInterstitialDisplayed(Ad ad) {
                AdBase.this.onAdOpened();
            }

            @Override
            public void onInterstitialDismissed(Ad ad) {
                AdBase.this.onAdClosed();
            }
        };
    }


    protected void onAdClosed(){
        setAdStatus(AD_CLOSED);
        if(mListener != null){
            mListener.onAdClosed(this);
        }
        load();
//        ShadowsocksApplication.debug("广告Status", mAdStatus + "");
    }

    protected void onAdError(int errorCode){
        setAdStatus(AD_ERROR);
        if(mListener != null){
            mListener.onAdError(this, errorCode);
        }
        clearTimeout();
        sendLoadingTimeToGA();
        //banner广告自己会重试的
        if(mRetryCount < mMaxRetryCount){
            load();
            mRetryCount++;
        }
//        ShadowsocksApplication.debug("广告Status", mAdStatus + " " + errorCode);
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
        setAdStatus(AD_OPENED);
        if(mListener != null){
            mListener.onAdOpened(this);
        }
        ShadowsocksApplication.debug("广告Status", mAdStatus + "");

    }

    protected void onAdLoaded(){
        setAdStatus(AD_LOADED);
        if(mListener != null){
            mListener.onAdLoaded(this);
        }
        clearTimeout();
        mRetryCount = 0;
        sendLoadingTimeToGA();
        ShadowsocksApplication.debug("广告Status", mAdStatus + "");
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
        String uniqueTag = getFullTag();
        mSharedPreference.edit().putInt(uniqueTag, mDisplayCount).apply();
    }

    @NonNull
    private String getFullTag() {
        return String.format("%s_%s_%s", getAdPlatformText(), getAdTypeText(), getTag());
    }

    protected void loadDisplayCount(){
        String uniqueTag = getFullTag();
        mDisplayCount = mSharedPreference.getInt(uniqueTag, 0);
    }


    public String getAdTypeText(){
        return AD_TYPE_TEXT[mAdType];
    }

    public String getAdStatusText(){
        return AD_STATUS_TEXT[mAdStatus];
    }

    public String getAdPlatformText(){
        return AD_PLATFORM_TEXT[mAdplatform];
    }


    public abstract String getAdId();

    public void setMaxRetryCount(int retryCount){
        mRetryCount = retryCount;
    }

    public int getMaxRetryCount(){
        return  mRetryCount;
    }

}
