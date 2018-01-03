package com.androapplite.shadowsocks;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.format.DateUtils;

import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.facebook.appevents.AppEventsLogger;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
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
    private AppEventsLogger mFBLogger;

    private int mByte1M;
    private int mByte10M;
    private int mByte100M;

    private void initPhoneModels() {
        String modelsString = "SM-J700F\\nGT-I9060I\\nSM-G935F\\nSM-J100H\\nSM-J710F\\nRedmi Note 4\\nRedmi 4X\\nSM-G570F\\nSM-G532F\\nSM-J200H\\nSM-J500H\\nGT-I9500\\nSM-N9005\\nSM-G925F\\nSM-J120F\\nSM-J320H\\nALE-L21\\nSM-G530H\\nRedmi 4A\\nSM-G531H\\nSM-G955F\\nSM-N920C\\nSM-A310F\\nSM-N910C\\nSM-G900F\\nGT-I9300\\nSM-G950F\\nA1601\\nSM-G920F\\nSM-J510FN\\nSM-J320F\\nSM-J500F\\nSM-A510F\\nSM-J200F\\nSM-A520F\\nSM-G930F\\nGT-N7100\\nm3 note\\nA37f\\nSM-G532G\\nM3s\\nSM-A500F\\nGT-I9301I\\nRedmi Note 3\\nSM-G7102\\nLUA-U22\\nRedmi 3S\\nSM-J510F\\nCAM-L21\\nCUN-U29\\nSM-G361H\\nSM-A720F\\nSM-A710F\\nTIT-U02\\nSM-J700M\\nPRA-LA1\\nSM-J500M\\nSM-G355H\\nA6000\\nF670S\\nSM-N900\\nSM-G360H\\nA1000\\nSM-A320F\\nSM-G531F\\nSM-J730F\\nSM-J510H\\nJ701F\\nSM-A500H\\nBoom J8\\nA2010-a\\nM5c\\nSM-J105H\\nD855\\nSM-J111F\\nM5 Note\\nC-8\\nM5S\\nSM-J200G\\nX557 Lite\\nSM-G530F\\nW3\\nSM-G900H\\nGT-S7262\\nA536\\nSM-J710GN\\nSM-E500H\\nSM-A300H\\nGT-S7582\\nGT-I9082\\nGT-I9300I\\nVNS-L21\\nA7000\\nA6010\\nCPH1609\\nA37fw\\nSM-J530F\\nMoto G (4)\\nvivo 1606\\nSM-N950U";
        initPhoneModels(modelsString);

    }

    private PromotionTracking(Context context) {
        mContext = context.getApplicationContext();
        mFirebase = Firebase.getInstance(mContext);
        mSharedPreference = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(mContext);
        mEditor = mSharedPreference.edit();
        initPhoneModels();
        mByte1M = 1024 * 1024;
        mByte10M = 10 * mByte1M;
        mByte100M = 10 * mByte10M;
        mFBLogger = AppEventsLogger.newLogger(context);
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
            mFBLogger.logEvent("openMainPage_2_Times_EveryDay");
        } else if (count == 3){
            mFirebase.logEvent("每天3次打开主页的用户", "广告投放统计");
            mFBLogger.logEvent("openMainPage_3_TimesEveryDay");
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
                mFBLogger.logEvent("openApp_" + count + "_DaysContinuously");
            } else if (count >=8 && count < 14){
                mFirebase.logEvent("连续打开7天", "广告投放统计");
                mFBLogger.logEvent("openApp_7_DaysContinuously");
            } else if (count >=14 && count < 30) {
                mFirebase.logEvent("连续打开14天", "广告投放统计");
                mFBLogger.logEvent("openApp_14_DaysContinuously");
            } else if (count >= 30) {
                mFirebase.logEvent("连续打开30天", "广告投放统计");
                mFBLogger.logEvent("openApp_30_DaysContinuously");
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
                    mFirebase.logEvent("安装" + uninstallDay + "天未删除", "广告投放统计");
                    mFBLogger.logEvent("uninstall_" + uninstallDay + "_Days");
                } else if (uninstallDay >=8 && uninstallDay <14){
                    mFirebase.logEvent("安装7天未删除", "广告投放统计");
                    mFBLogger.logEvent("uninstall_7_Days");
                } else if (uninstallDay >=14 && uninstallDay < 30) {
                    mFirebase.logEvent("安装14天未删除", "广告投放统计");
                    mFBLogger.logEvent("uninstall_14_Days");
                } else if (uninstallDay >= 30) {
                    mFirebase.logEvent("安装30天未删除", "广告投放统计");
                    mFBLogger.logEvent("uninstall_30_Days");
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
        mFBLogger.logEvent("switchCountry");
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
        if (payload >= mByte1M && payload < mByte10M) {
            boolean isReport = mSharedPreference.getBoolean(SharedPreferenceKey.PAYLOAD_1M, false);
            if (!isReport) {
                mFirebase.logEvent("每天流量1M", "广告投放统计");
                mFBLogger.logEvent("usage_1M_EveryDay");
                mEditor.putBoolean(SharedPreferenceKey.PAYLOAD_1M, true);
            }
        }else if (payload >= mByte10M && payload < mByte100M) {
            boolean isReport = mSharedPreference.getBoolean(SharedPreferenceKey.PAYLOAD_10M, false);
            if (!isReport) {
                mFirebase.logEvent("每天流量10M", "广告投放统计");
                mFBLogger.logEvent("usage_10M_EveryDay");
                mEditor.putBoolean(SharedPreferenceKey.PAYLOAD_10M, true);
            }
        } else if (payload >= mByte100M){
            boolean isReport = mSharedPreference.getBoolean(SharedPreferenceKey.PAYLOAD_100M, false);
            if (!isReport) {
                mFirebase.logEvent("每天流量100M", "广告投放统计");
                mFBLogger.logEvent("usage_100M_EveryDay");
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
                mFBLogger.logEvent("install_Facebook");
            } catch (Exception e) {
            }
            try{
                packageManager.getPackageInfo("com.skype.raider", 0);
                mFirebase.logEvent("装过Skype", "广告投放统计");
                mFBLogger.logEvent("install_Skype");
            } catch (Exception e) {
            }
            try{
                packageManager.getPackageInfo("com.twitter.android", 0);
                mFirebase.logEvent("装过Twitter", "广告投放统计");
                mFBLogger.logEvent("install_Twitter");
            } catch (Exception e) {
            }
            try{
                packageManager.getPackageInfo("com.whatsapp", 0);
                mFirebase.logEvent("装过Whatsapp", "广告投放统计");
                mFBLogger.logEvent("install_Whatsapp");
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
            FirebaseRemoteConfig firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
            String phoneModelsString = firebaseRemoteConfig.getString("track_phone_models");
            initPhoneModels(phoneModelsString);
            if (mPhoneModels.contains(model)) {
                mFirebase.logEvent("手机型号" + model, "广告投放统计");
                mFBLogger.logEvent("phoneModel_" + model);

            }
            mFirebase.logEvent("手机版本" + Build.VERSION.RELEASE, "广告投放统计");
            mFBLogger.logEvent("phoneOSVersion_" + Build.VERSION.RELEASE);
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
            mFBLogger.logEvent("clickConnectButton3Times");
        }
        mEditor.putLong(SharedPreferenceKey.CLICK_CONNECT_BUTTON_TIME, lastCalendar.getTimeInMillis())
                .putInt(SharedPreferenceKey.CLICK_CONNECT_BUTTON_COUNT, count)
                .apply();
    }

    private void initPhoneModels(String phoneModelsString) {
        if(phoneModelsString != null && !phoneModelsString.isEmpty()) {
            String[] models = phoneModelsString.toLowerCase().split("\\\\n");
            mPhoneModels = new HashSet<>(models.length);
            for (String model : models) {
                mPhoneModels.add(model);
            }
        }
    }
}
