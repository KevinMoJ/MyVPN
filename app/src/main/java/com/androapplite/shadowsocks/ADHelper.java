package com.androapplite.shadowsocks;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import com.androapplite.shadowsocks.broadcast.Action;
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.NativeAd;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by jim on 16/5/11.
 */
public final class AdHelper {
    private Context mContext;
    private NativeAd mFacebookAd;
    private AdView mAdmobAd;
    private static final String ADMOB_ID = "ca-app-pub-5330675173640052/3927500528";
    private static final String FACEBOOK_ID = "504824046367667_504825153034223";
    public static final int AD_INIT = 0;
    public static final int AD_LOADING = 1;
    public static final int AD_LOADED = 2;
    public static final int AD_CLICKED = 3;
    public static final int AD_CLOSED = 4;
    public static final int AD_ERROR = 5;

    @IntDef({AD_INIT, AD_LOADING, AD_LOADED, AD_CLICKED, AD_CLOSED, AD_ERROR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface AdState{}

    private @AdState int mFacebookAdState;
    private @AdState int mAdmobAdState;

    public static final int AD_FACEBOOK = 0;
    public static final int AD_ADMOB = 1;

    @IntDef({AD_FACEBOOK, AD_ADMOB})
    @Retention(RetentionPolicy.SOURCE)
    public @interface AdType{}

    private LocalBroadcastManager mLocalBroadcastManager;

    public static final String AD_TYPE = "AD_TYPE";
    public static final String AD_STATE = "AD_STATE";

    private OnAdLoadListener mFaceBookAdListener;
    private OnAdLoadListener mAdmobAdListener;

    private static AdHelper mInstance;

    private AdHelper(@NonNull Context context){
        mContext = context;
        mAdmobAd = new AdView(context);
        mFacebookAd = new NativeAd(context, FACEBOOK_ID);
        mFacebookAdState = AD_INIT;
        mAdmobAdState = AD_INIT;
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        initAdmob();
        initFacebookAd();

    }

    public void initAdmob(){
        mAdmobAd.setAdSize(AdSize.SMART_BANNER);
        mAdmobAd.setAdUnitId(ADMOB_ID);
        mAdmobAd.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(int errorCode) {
                mAdmobAdState = AD_ERROR;
                if (mAdmobAdListener != null) {
                    mAdmobAdListener.onError(mAdmobAd);
                }
                Intent intent = createBroadcastIntent(AD_ERROR);
                mLocalBroadcastManager.sendBroadcast(intent);
            }

            @Override
            public void onAdLoaded() {
                mAdmobAdState = AD_LOADED;
                if (mAdmobAdListener != null) {
                    mAdmobAdListener.onAdLoaded(mAdmobAd);
                }
                Intent intent = createBroadcastIntent(AD_LOADED);
                mLocalBroadcastManager.sendBroadcast(intent);
            }

            @Override
            public void onAdOpened() {
                mAdmobAdState = AD_CLICKED;
                if (mAdmobAdListener != null) {
                    mAdmobAdListener.onAdClicked(mAdmobAd);
                }
                Intent intent = createBroadcastIntent(AD_CLICKED);
                mLocalBroadcastManager.sendBroadcast(intent);
            }

            @Override
            public void onAdClosed() {
                mAdmobAdState = AD_CLOSED;
                Intent intent = createBroadcastIntent(AD_CLOSED);
                mLocalBroadcastManager.sendBroadcast(intent);
            }

            @NonNull
            private Intent createBroadcastIntent(@AdState int adState) {
                Intent intent = new Intent(Action.AD_LOADED);
                intent.putExtra(AD_TYPE, AD_ADMOB);
                intent.putExtra(AD_STATE, adState);
                return intent;
            }
        });

    }

    private static AdRequest createAdmobRequest(){
        AdRequest.Builder builder = new AdRequest.Builder();
        return  builder.build();
    }

    public void setAdmobAdListener(OnAdLoadListener listener){
        mAdmobAdListener = listener;
    }

    public void loadAdmobAd(){
        mAdmobAd.loadAd(createAdmobRequest());
        mAdmobAdState = AD_LOADING;
    }

    public void initFacebookAd(){
        mFacebookAd.setAdListener(new com.facebook.ads.AdListener() {
            @Override
            public void onError(Ad ad, AdError adError) {
                mFacebookAdState = AD_ERROR;
                if(mFaceBookAdListener != null){
                    mFaceBookAdListener.onError(ad);
                }
                Intent intent = createBroadcastIntent(AD_ERROR);
                mLocalBroadcastManager.sendBroadcast(intent);
            }

            @Override
            public void onAdLoaded(Ad ad) {
                mFacebookAdState = AD_LOADED;
                if(mFaceBookAdListener != null){
                    mFaceBookAdListener.onAdLoaded(ad);
                }
                Intent intent = createBroadcastIntent(AD_LOADED);
                mLocalBroadcastManager.sendBroadcast(intent);
            }

            @Override
            public void onAdClicked(Ad ad) {
                mFacebookAdState = AD_CLICKED;
                if(mFaceBookAdListener != null){
                    mFaceBookAdListener.onAdClicked(ad);
                }
                Intent intent = createBroadcastIntent(AD_CLICKED);
                mLocalBroadcastManager.sendBroadcast(intent);
            }

            @NonNull
            private Intent createBroadcastIntent(@AdState int adState){
                Intent intent = new Intent(Action.AD_LOADED);
                intent.putExtra(AD_TYPE, AD_FACEBOOK);
                intent.putExtra(AD_STATE, adState);
                return intent;
            }

        });
    }

    public void setFacebookAdListener(OnAdLoadListener listener){
        mFaceBookAdListener = listener;
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

    public AdView getAdmobAd(){
        return mAdmobAd;
    }

    public @AdHelper.AdState int getAdmobState(){
        return mAdmobAdState;
    }


    public NativeAd getFaceBookAd(){
        return mFacebookAd;
    }

    public @AdHelper.AdState int getFacebookAdState(){
        return mFacebookAdState;
    }

    public static final AdHelper getInstance(@NonNull Context context){
        if(mInstance == null){
            synchronized (AdHelper.class){
                if(mInstance == null){
                    mInstance = new AdHelper(context.getApplicationContext());
                }
            }
        }
        return mInstance;
    }

}
