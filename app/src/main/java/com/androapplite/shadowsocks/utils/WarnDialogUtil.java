package com.androapplite.shadowsocks.utils;

import android.content.Context;

import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.bestgo.adsplugin.ads.AdAppHelper;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Created by Kiven.Mo on 2018/5/14.
 */

public class WarnDialogUtil {

    public static boolean isAdLoaded(Context context) {
        AdAppHelper adAppHelper = AdAppHelper.getInstance(context);
        boolean cloudAdShow = FirebaseRemoteConfig.getInstance().getBoolean("is_warn_dialog_ad_show");
        if (cloudAdShow)
            return adAppHelper.isFullAdLoaded() || adAppHelper.isNativeLoaded();
        else
            return true;
    }

    public static boolean isAppBackground() {
        return ((ShadowsocksApplication) ShadowsocksApplication.getGlobalContext().getApplicationContext()).getOpenActivityNumber() <= 0;
    }

    public static long getHourOrDay() {
        return Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    }

    //是否满足显示的时间间隔
    public static boolean isSpaceTimeShow(long lastShowTime, long spaceTime) {
        return System.currentTimeMillis() - lastShowTime >= TimeUnit.MINUTES.toMillis(spaceTime);
    }
}
