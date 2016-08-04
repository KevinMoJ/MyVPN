package com.androapplite.shadowsocks.ad;

/**
 * Created by jim on 16/8/4.
 */
public abstract class Banner extends AdBase {

    protected Banner(@AdPlatform int platform){
        super(platform, AD_BANNER);
    }


//    protected Banner(@AdPlatform int platform, String tag){
//        super(platform, AD_BANNER, tag);
//    }
}
