package com.androapplite.shadowsocks.utils;

import android.content.Context;

import com.androapplite.shadowsocks.ShadowsocksApplication;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Created by Kiven.Mo on 2018/5/14.
 */

public class WarnDialogUtil {

    public static boolean isAdLoaded(Context context, boolean isLoadAd) {
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
