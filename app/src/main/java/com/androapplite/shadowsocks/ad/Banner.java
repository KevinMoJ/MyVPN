package com.androapplite.shadowsocks.ad;

import android.content.Context;
import android.view.ViewGroup;

/**
 * Created by jim on 16/8/4.
 */
public abstract class Banner extends AdBase {

    protected Banner(Context context, @AdPlatform int platform){
        super(context, platform, AD_BANNER);
        loadDisplayCount();
    }

    protected void addToViewGroup(ViewGroup container, ViewGroup.LayoutParams layoutParams){
        addToViewGroup();
    }

    protected void addToViewGroup(ViewGroup container){
        addToViewGroup();
    }

    protected void addToViewGroup(){
        increaseDisplayCount();
        saveDisplayCount();
    }

//    protected Banner getBanner(){
//        increaseDisplayCount();
//        saveDisplayCount();
//        return this;
//    }

    public abstract void resume();

    public abstract void pause();

    public abstract void destory();
}
