package com.androapplite.shadowsocks.ad;

import android.content.Context;
import android.os.Build;

import com.androapplite.shadowsocks.R;
import com.androapplite.shadowsocks.ad.admob.AdMobInterstitial;
import com.google.android.gms.ads.MobileAds;

import java.util.ArrayList;

/**
 * Created by jim on 16/8/4.
 */
public class AdHelper {
    private static AdHelper mInstance;
    private ArrayList<AdBase> mAds;

    private static final ArrayList<String> NO_AD_PHONES = new ArrayList<String>();
    static{
        NO_AD_PHONES.add("Xiaomi/cm_pisces/pisces:5.1.1/LMY48W/534f9b320e:userdebug/test-keys");
        NO_AD_PHONES.add("google/occam/mako:5.1.1/LMY48M/2167285:user/release-keys");
    }

    private AdHelper(Context context){
        MobileAds.initialize(context, context.getString(R.string.admob_application_id));
        initAds(context);
    }


    public AdHelper getmInstance(Context context){
        if(mInstance == null){
            synchronized (AdHelper.class){
                if(mInstance == null) {
                    mInstance = new AdHelper(context);
                }
            }
        }
        return  mInstance;
    }

    public static boolean isAdNeedToShow(){
        return !isNoAdPhone();
    }

    public static boolean isNoAdPhone(){
        String buildDisplay = Build.FINGERPRINT;
        return NO_AD_PHONES.contains(buildDisplay);
    }

    private void initAds(Context context){
        mAds.add(new AdMobInterstitial(context, context.getString(R.string.admob_interstitial_id),
                context.getString(R.string.tag_connect)));
    }

    public void loadAll(){
        if(isNoAdPhone()) return;

        for(AdBase adBase:filter()){
            adBase.load();
        }
    }

    private ArrayList<AdBase> filter(){
        return mAds;
    }

//    public void addAd(AdBase adBase){
//        mAds.add(adBase);
//    }
//
//
//    public void loadByTag(String tag){
//        if(isNoAdPhone()) return;
//
//    }
//
//    public void showByTag(String tag){
//        if(isNoAdPhone()) return;
//    }




}
