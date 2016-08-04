package com.androapplite.shadowsocks.ad;

import android.support.annotation.IntDef;

import com.androapplite.shadowsocks.BuildConfig;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

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
    @IntDef({AD_INIT, AD_LOADING, AD_LOADED, AD_OPENED, AD_CLOSED, AD_ERROR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface AdStatus{}

    public static final int AD_BANNER = 0;
    public static final int AD_INTERSTItiAL = 1;
    public static final int AD_NATIVE = 2;
    @IntDef({AD_BANNER, AD_INTERSTItiAL, AD_NATIVE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface AdType{}

    public static final int AD_ADMOB = 0;
    public static final int AD_FACEBOOK = 1;
    @IntDef({AD_ADMOB, AD_FACEBOOK})
    @Retention(RetentionPolicy.SOURCE)
    public @interface AdPlatform{}

    private @AdStatus int mAdStatus;
    private @AdType int mAdType;
    private @AdPlatform int mAdplatform;
    private OnAdLoadListener mListener;
    private String mTag;

    protected AdBase(@AdPlatform int platform, @AdType int type){
        mAdplatform = platform;
        mAdType = type;
        mAdStatus = AD_INIT;
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
            builder.addTestDevice("c35b50cfe853bf4d75a49754dc0e4c48");
        }
        return  builder.build();
    }

    protected void setAdStatus(@AdStatus int status){
        mAdStatus = status;
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
        return mTag;
    }

    protected AdListener createAdmobAdListener(){
        return new AdListener() {
            @Override
            public void onAdClosed() {
                mAdStatus = AD_CLOSED;
                AdBase.this.onAdClosed();
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                mAdStatus = AD_ERROR;
                AdBase.this.onAdError(errorCode);
            }

            @Override
            public void onAdOpened() {
                mAdStatus = AD_OPENED;
                AdBase.this.onAdOpened();
            }

            @Override
            public void onAdLoaded() {
                mAdStatus = AD_LOADED;
            }
        };
    }

    protected void onAdClosed(){
        if(mListener != null){
            mListener.onAdClosed(this);
        }
    }

    protected void onAdError(int errorCode){
        if(mListener != null){
            mListener.onAdError(this, errorCode);
        }
    }

    protected void onAdOpened(){
        if(mListener != null){
            mListener.onAdOpened(this);
        }
    }

    private void onAdLoaded(){
        if(mListener != null){
            mListener.onAdLoaded(this);
        }
    }

    abstract public void load();

}
