package com.androapplite.shadowsocks.ad;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

/**
 * Created by jim on 16/8/4.
 */
public abstract  class  Banner<T extends  View> extends AdBase {
    private T mAdView;

    protected Banner(Context context, @AdPlatform int platform){
        super(context, platform, AD_BANNER);
        loadDisplayCount();
    }

    protected Banner(Context context, @AdPlatform int platform, String tag){
        super(context, platform, AD_BANNER, tag);
        loadDisplayCount();
    }

    public void addToViewGroup(ViewGroup container, ViewGroup.LayoutParams layoutParams){
        increaseAndSaveDisplayCound();
        removeFromParent();
        container.addView(mAdView, layoutParams);
    }

    public void addToViewGroup(ViewGroup container){
        increaseAndSaveDisplayCound();
        removeFromParent();
        container.addView(mAdView);
    }


    protected void increaseAndSaveDisplayCound(){
        increaseDisplayCount();
        saveDisplayCount();
    }

    private void removeFromParent(){
        final ViewParent parent = mAdView.getParent();
        if(parent != null){
            ((ViewGroup) parent).removeView(mAdView);
        }
    }

    public abstract void resume();

    public abstract void pause();

    public void destory(){
        removeFromParent();
    }

    protected T getAdView(){
        return mAdView;
    }

    protected void setAdView(T adView){
        mAdView = adView;
    }
}
