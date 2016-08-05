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

    public void addToViewGroup(ViewGroup container, ViewGroup.LayoutParams layoutParams){
        increaseDisplayCount();
        saveDisplayCount();
    }
//    protected Banner(@AdPlatform int platform, String tag){
//        super(platform, AD_BANNER, tag);
//    }


}
