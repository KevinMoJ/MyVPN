package com.androapplite.shadowsocks.utils;

import android.content.Context;

import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.ad.AdUtils;
import com.bestgo.adsplugin.ads.AdAppHelper;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Created by Kiven.Mo on 2018/5/14.
 */

public class WarnDialogUtil {

    public static boolean isAdLoaded(Context context, boolean isLoadAd) {
        AdAppHelper adAppHelper = AdAppHelper.getInstance(context);
        boolean cloudNativeAdShow = FirebaseRemoteConfig.getInstance().getBoolean("is_warn_dialog_native_ad_show");
        boolean cloudFullAdShow = FirebaseRemoteConfig.getInstance().getBoolean("is_warn_dialog_full_ad_show");
        if (cloudNativeAdShow) {
            if (isLoadAd) {
                if (adAppHelper.isNativeLoaded())
                    return true;
                else {
                    adAppHelper.loadNewNative();
                    return false;
                }
            } else
                return adAppHelper.isNativeLoaded();

        } else if (cloudFullAdShow) {
            if (isLoadAd) {
                if (adAppHelper.isFullAdLoaded(AdUtils.FULL_AD_BAD))
                    return true;
                else {
                    adAppHelper.loadFullAd(AdUtils.FULL_AD_BAD, 0);
                    return false;
                }
            } else
                return adAppHelper.isFullAdLoaded(AdUtils.FULL_AD_BAD);
        } else
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

    public static String getDateTime() {
        Calendar calendar = Calendar.getInstance();
        //获取系统的日期
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        return String.valueOf(year + "年" + month + "月" + day + "日" + hour + ":" + minute);
    }
}
