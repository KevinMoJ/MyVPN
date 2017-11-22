package com.androapplite.shadowsocks;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.format.DateUtils;

import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.vm.shadowsocks.core.TcpTrafficMonitor;

import java.util.Calendar;
import java.util.HashSet;

/**
 * Created by huangjian on 2017/11/20.
 */

public class PromotionTracking {
    private Context mContext;
    private static PromotionTracking mInstance;
    private Firebase mFirebase;
    private SharedPreferences mSharedPreference;
    private SharedPreferences.Editor mEditor;
    private HashSet<String> mPhoneModels;

    private void initPhoneModels() {
        String[] models = ("SM-J700F\n" +
                "GT-I9060I\n" +
                "SM-G935F\n" +
                "SM-J100H\n" +
                "SM-J710F\n" +
                "Redmi Note 4\n" +
                "Redmi 4X\n" +
                "SM-G570F\n" +
                "SM-G532F\n" +
                "SM-J200H\n" +
                "SM-J500H\n" +
                "GT-I9500\n" +
                "SM-N9005\n" +
                "SM-G925F\n" +
                "SM-J120F\n" +
                "SM-J320H\n" +
                "ALE-L21\n" +
                "SM-G530H\n" +
                "Redmi 4A\n" +
                "SM-G531H\n" +
                "SM-G955F\n" +
                "SM-N920C\n" +
                "SM-A310F\n" +
                "SM-N910C\n" +
                "SM-G900F\n" +
                "GT-I9300\n" +
                "SM-G950F\n" +
                "A1601\n" +
                "SM-G920F\n" +
                "SM-J510FN\n" +
                "SM-J320F\n" +
                "SM-J500F\n" +
                "SM-A510F\n" +
                "SM-J200F\n" +
                "SM-A520F\n" +
                "SM-G930F\n" +
                "GT-N7100\n" +
                "m3 note\n" +
                "A37f\n" +
                "SM-G532G\n" +
                "M3s\n" +
                "SM-A500F\n" +
                "GT-I9301I\n" +
                "Redmi Note 3\n" +
                "SM-G7102\n" +
                "LUA-U22\n" +
                "Redmi 3S\n" +
                "SM-J510F\n" +
                "CAM-L21\n" +
                "CUN-U29\n" +
                "SM-G361H\n" +
                "SM-A720F\n" +
                "SM-A710F\n" +
                "TIT-U02\n" +
                "SM-J700M\n" +
                "PRA-LA1\n" +
                "SM-J500M\n" +
                "SM-G355H\n" +
                "A6000\n" +
                "F670S\n" +
                "SM-N900\n" +
                "SM-G360H\n" +
                "A1000\n" +
                "SM-A320F\n" +
                "SM-G531F\n" +
                "SM-J730F\n" +
                "SM-J510H\n" +
                "J701F\n" +
                "SM-A500H\n" +
                "Boom J8\n" +
                "A2010-a\n" +
                "M5c\n" +
                "SM-J105H\n" +
                "D855\n" +
                "SM-J111F\n" +
                "M5 Note\n" +
                "C-8\n" +
                "M5S\n" +
                "SM-J200G\n" +
                "X557 Lite\n" +
                "SM-G530F\n" +
                "W3\n" +
                "SM-G900H\n" +
                "GT-S7262\n" +
                "A536\n" +
                "SM-J710GN\n" +
                "SM-E500H\n" +
                "SM-A300H\n" +
                "GT-S7582\n" +
                "GT-I9082\n" +
                "GT-I9300I\n" +
                "VNS-L21\n" +
                "A7000\n" +
                "A6010\n" +
                "CPH1609\n" +
                "A37fw\n" +
                "SM-J530F\n" +
                "Moto G (4)\n" +
                "SM-N950U").toLowerCase().split("\n");
        mPhoneModels = new HashSet<>(models.length);
        for (String model:models){
            mPhoneModels.add(model);
        }
    }

    private PromotionTracking(Context context) {
        mContext = context.getApplicationContext();
        mFirebase = Firebase.getInstance(mContext);
        mSharedPreference = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(mContext);
        mEditor = mSharedPreference.edit();
        initPhoneModels();

    }

    public static PromotionTracking getInstance(Context context) {
        if (mInstance == null) {
            synchronized (PromotionTracking.class) {
                if (mInstance == null) {
                    mInstance = new PromotionTracking(context);
                }
            }
        }
        return mInstance;
    }

    private boolean isNewDay(Calendar lastEventCalendar, Calendar current) {
        return lastEventCalendar.get(Calendar.DATE) != current.get(Calendar.DATE);
    }

    public void reportOpenMainPageCount() {
        Calendar currentCalendar = Calendar.getInstance();
        long last = mSharedPreference.getLong(SharedPreferenceKey.OPEN_MAIN_PAGE_TIME, currentCalendar.getTimeInMillis());
        Calendar lastCalendar = Calendar.getInstance();
        lastCalendar.setTimeInMillis(last);
        int count = mSharedPreference.getInt(SharedPreferenceKey.OPEN_MAIN_PAGE_COUNT, 0);
        if (isNewDay(lastCalendar, currentCalendar)) {
            lastCalendar = currentCalendar;
            count = 0;
        }
        count++;
        if (count == 2) {
            mFirebase.logEvent("每天2次打开主页的用户", "广告投放统计");
        } else if (count == 3){
            mFirebase.logEvent("每天3次打开主页的用户", "广告投放统计");
        }
        mEditor.putLong(SharedPreferenceKey.OPEN_MAIN_PAGE_TIME, lastCalendar.getTimeInMillis())
                .putInt(SharedPreferenceKey.OPEN_MAIN_PAGE_COUNT, count)
                .apply();
    }

    private boolean isContinousDay(Calendar lastEventCalendar, Calendar current) {
        return current.get(Calendar.DAY_OF_YEAR) - lastEventCalendar.get(Calendar.DAY_OF_YEAR) == 1
                || (lastEventCalendar.get(Calendar.MONTH) == Calendar.DECEMBER
                && lastEventCalendar.get(Calendar.DATE) == 31
                && current.get(Calendar.MONTH) == Calendar.JANUARY
                && current.get(Calendar.DATE) == 1);
    }

    public void reportContinuousDayCount() {
        Calendar currentCalendar = Calendar.getInstance();
        long last = mSharedPreference.getLong(SharedPreferenceKey.CONTINOUS_DAY_TIME, currentCalendar.getTimeInMillis() - DateUtils.DAY_IN_MILLIS);
        Calendar lastCalendar = Calendar.getInstance();
        lastCalendar.setTimeInMillis(last);
        int count = mSharedPreference.getInt(SharedPreferenceKey.CONTINOUS_DAY_COUNT, 0);
        if (isNewDay(lastCalendar, currentCalendar)) {
            if (isContinousDay(lastCalendar, currentCalendar)) {
                count++;
            } else {
                count = 0;
            }
            lastCalendar = currentCalendar;
            if (count >= 2 && count < 8) {
                mFirebase.logEvent("连续打开" + count + "天", "广告投放统计");
            } else if (count >=14 && count < 30) {
                mFirebase.logEvent("连续打开14天", "广告投放统计");
            } else if (count >= 30) {
                mFirebase.logEvent("连续打开30天", "广告投放统计");
            }
            mEditor.putLong(SharedPreferenceKey.CONTINOUS_DAY_TIME, lastCalendar.getTimeInMillis())
                    .putInt(SharedPreferenceKey.CONTINOUS_DAY_COUNT, count)
                    .apply();
        }
    }

    public void reportUninstallDayCount() {
        Calendar currentCalendar = Calendar.getInstance();
        long last = mSharedPreference.getLong(SharedPreferenceKey.UNINSTALL_DAY_TIME, currentCalendar.getTimeInMillis() - DateUtils.DAY_IN_MILLIS);
        Calendar lastCalendar = Calendar.getInstance();
        lastCalendar.setTimeInMillis(last);
        if (isNewDay(lastCalendar, currentCalendar)) {
            try {
                PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
                long installTime = packageInfo.firstInstallTime;
                int day = (int) ((currentCalendar.getTimeInMillis() - installTime) / DateUtils.DAY_IN_MILLIS);
                int uninstallDay = day - 1;
                if (uninstallDay >= 2 && uninstallDay < 8) {
                    mFirebase.logEvent("安装" + day + "天未删除", "广告投放统计");
                } else if (uninstallDay >=14 && uninstallDay < 30) {
                    mFirebase.logEvent("安装14天未删除", "广告投放统计");
                } else if (uninstallDay >= 30) {
                    mFirebase.logEvent("安装30天未删除", "广告投放统计");
                }
                lastCalendar = currentCalendar;
                mEditor.putLong(SharedPreferenceKey.UNINSTALL_DAY_TIME, lastCalendar.getTimeInMillis())
                        .apply();
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

        }
    }

    public void reportSwitchCountry() {
        mFirebase.logEvent("切换国家", "广告投放统计");
    }

    public void reportUsageByte(TcpTrafficMonitor tcpTrafficMonitor) {
        Calendar currentCalendar = Calendar.getInstance();
        long last = mSharedPreference.getLong(SharedPreferenceKey.PAYLOAD_TIME, currentCalendar.getTimeInMillis());
        Calendar lastCalendar = Calendar.getInstance();
        lastCalendar.setTimeInMillis(last);
        long payload = mSharedPreference.getLong(SharedPreferenceKey.PAYLOAD_BYTE, 0);
        if (isNewDay(lastCalendar, currentCalendar)) {
            lastCalendar = currentCalendar;
            payload = 0;
            mEditor.putBoolean(SharedPreferenceKey.PAYLOAD_10M, false)
                    .putBoolean(SharedPreferenceKey.PAYLOAD_100M, false)
                    .apply();

        }
        payload += tcpTrafficMonitor.pProxyPayloadReceivedByteCount + tcpTrafficMonitor.pProxyPayloadSentByteCount;
        if (payload >= 1024*1024 && payload < 1024 * 1024 * 100) {
            boolean isReport = mSharedPreference.getBoolean(SharedPreferenceKey.PAYLOAD_10M, false);
            if (!isReport) {
                mFirebase.logEvent("每天流量10M", "广告投放统计");
                mEditor.putBoolean(SharedPreferenceKey.PAYLOAD_10M, true);
            }
        } else if (payload >= 1024 * 1024 * 100){
            boolean isReport = mSharedPreference.getBoolean(SharedPreferenceKey.PAYLOAD_100M, false);
            if (!isReport) {
                mFirebase.logEvent("每天流量100M", "广告投放统计");
                mEditor.putBoolean(SharedPreferenceKey.PAYLOAD_100M, true);
            }
        }
        mEditor.putLong(SharedPreferenceKey.PAYLOAD_TIME, lastCalendar.getTimeInMillis())
                .putLong(SharedPreferenceKey.PAYLOAD_BYTE, payload)
                .apply();
    }

    public void reportAppInstall() {
        Calendar currentCalendar = Calendar.getInstance();
        long last = mSharedPreference.getLong(SharedPreferenceKey.INSTALL_APP_TIME, currentCalendar.getTimeInMillis() - DateUtils.DAY_IN_MILLIS);
        Calendar lastCalendar = Calendar.getInstance();
        lastCalendar.setTimeInMillis(last);
        if (isNewDay(lastCalendar, currentCalendar)) {
            lastCalendar = currentCalendar;
            PackageManager packageManager = mContext.getPackageManager();
            try{
                packageManager.getPackageInfo("com.facebook.katana", 0);
                mFirebase.logEvent("装过Facebook", "广告投放统计");
            } catch (Exception e) {
            }
            try{
                packageManager.getPackageInfo("com.skype.raider", 0);
                mFirebase.logEvent("装过Skype", "广告投放统计");
            } catch (Exception e) {
            }
            try{
                packageManager.getPackageInfo("com.twitter.android", 0);
                mFirebase.logEvent("装过Twitter", "广告投放统计");
            } catch (Exception e) {
            }
            try{
                packageManager.getPackageInfo("com.whatsapp", 0);
                mFirebase.logEvent("装过Whatsapp", "广告投放统计");
            } catch (Exception e) {
            }
            mEditor.putLong(SharedPreferenceKey.INSTALL_APP_TIME, lastCalendar.getTimeInMillis()).apply();
        }
    }

    public void reportPhoneModelAndAndroidOS() {
        Calendar currentCalendar = Calendar.getInstance();
        long last = mSharedPreference.getLong(SharedPreferenceKey.PHONE_MODEL_OS_TIME, currentCalendar.getTimeInMillis() - DateUtils.DAY_IN_MILLIS);
        Calendar lastCalendar = Calendar.getInstance();
        lastCalendar.setTimeInMillis(last);
        if (isNewDay(lastCalendar, currentCalendar)) {
            lastCalendar = currentCalendar;
            String model = Build.MODEL.toLowerCase();
            if (mPhoneModels.contains(model)) {
                mFirebase.logEvent("手机型号" + model, "广告投放统计");
            }
            mFirebase.logEvent("手机版本" + Build.VERSION.RELEASE, "广告投放统计");
            mEditor.putLong(SharedPreferenceKey.INSTALL_APP_TIME, lastCalendar.getTimeInMillis()).apply();
        }
    }

    public void reportClickConnectButtonCount() {
        Calendar currentCalendar = Calendar.getInstance();
        long last = mSharedPreference.getLong(SharedPreferenceKey.CLICK_CONNECT_BUTTON_TIME, currentCalendar.getTimeInMillis());
        Calendar lastCalendar = Calendar.getInstance();
        lastCalendar.setTimeInMillis(last);
        int count = mSharedPreference.getInt(SharedPreferenceKey.CLICK_CONNECT_BUTTON_COUNT, 0);
        if (isNewDay(lastCalendar, currentCalendar)) {
            lastCalendar = currentCalendar;
            count = 0;
        }
        count++;
        if (count == 3){
            mFirebase.logEvent("点击连接按钮超过3次", "广告投放统计");
        }
        mEditor.putLong(SharedPreferenceKey.CLICK_CONNECT_BUTTON_TIME, lastCalendar.getTimeInMillis())
                .putInt(SharedPreferenceKey.CLICK_CONNECT_BUTTON_COUNT, count)
                .apply();
    }
}
