package com.androapplite.shadowsocks.service;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.activity.WarnDialogActivity;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.androapplite.shadowsocks.utils.WarnDialogUtil;
import com.bestgo.adsplugin.ads.AdAppHelper;
import com.bestgo.adsplugin.ads.AdType;
import com.bestgo.adsplugin.ads.listener.AdStateListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.vm.shadowsocks.core.LocalVpnService;

import java.util.concurrent.TimeUnit;

public class WarnDialogShowService extends Service implements Handler.Callback {
    private static final String TAG = "WarnDialogShowService";

    private static final int MSG_WIFI_CONNECTED = 10;

    private Handler mHandler;
    private SharedPreferences mSharedPreference;
    private WarnDialogReceiver mWarnDialogReceiver;
    private WarnDialogAdStateListener mAdStateListener;
    private String countryCode;

    private long startTime;
    private boolean isWifiConnected;

    public WarnDialogShowService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        mSharedPreference = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
        countryCode = mSharedPreference.getString(SharedPreferenceKey.COUNTRY_CODE, "未知");
        startTime = System.currentTimeMillis();
        mHandler = new Handler(this);
        registerWarnDialogReceiver();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1001, new Notification());
        return super.onStartCommand(intent, flags, startId);
    }

    private void registerWarnDialogReceiver() {
        mWarnDialogReceiver = new WarnDialogReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(mWarnDialogReceiver, filter);
    }

    private void unregisterWarnDialogReceiver() {
        if (mWarnDialogReceiver != null) {
            unregisterReceiver(mWarnDialogReceiver);
            mWarnDialogReceiver = null;
        }
    }


    private void monitorWifiStateChangeDialog() {
        long date = mSharedPreference.getLong(SharedPreferenceKey.WIFI_WARN_DIALOG_SHOW_TIME, 0);
        long lastShowTime = mSharedPreference.getLong(SharedPreferenceKey.WIFI_WARN_DIALOG_SHOW_TIME, 0);
        int showCount = mSharedPreference.getInt(SharedPreferenceKey.WIFI_WARN_DIALOG_SHOW_COUNT, 0);
        int count = (int) FirebaseRemoteConfig.getInstance().getLong("wifi_dialog_show_count");
        int spaceTime = (int) FirebaseRemoteConfig.getInstance().getLong("wifi_dialog_show_space_minutes");
        long hour_of_day = WarnDialogUtil.getHourOrDay();
        boolean isInactiveUser = !DateUtils.isToday(mSharedPreference.getLong(SharedPreferenceKey.OPEN_APP_TIME_TO_DECIDE_INACTIVE_USER, 0));
        if (mAdStateListener == null)
            mAdStateListener = new WarnDialogAdStateListener();

        if (!WarnDialogUtil.isAdLoaded(this, true))
            AdAppHelper.getInstance(this).setAdStateListener(mAdStateListener);

        //同一天一个弹窗最多弹两次 弹的次数可以云控控制   默认2小时冷却,间隔可以配置   23:00 - 9:00 的时间段禁止弹
        if (WarnDialogUtil.isAdLoaded(this, true) && WarnDialogUtil.isSpaceTimeShow(lastShowTime, spaceTime) && isInactiveUser
                && hour_of_day > 9 && hour_of_day < 23) {
            //新用户第一次没有数据的时候弹窗
            if (date == 0 && WarnDialogUtil.isAppBackground() && showCount < count) {
                showCount = showCount + 1;
                mSharedPreference.edit().putInt(SharedPreferenceKey.WIFI_WARN_DIALOG_SHOW_COUNT, showCount).apply();
                mSharedPreference.edit().putLong(SharedPreferenceKey.WIFI_WARN_DIALOG_SHOW_TIME, System.currentTimeMillis()).apply();
                Firebase.getInstance(this).logEvent("大弹窗", "开始跳转", "链接WiFi");
                WarnDialogActivity.start(this, WarnDialogActivity.CONNECT_PUBLIC_WIFI_DIALOG);
            } else if (DateUtils.isToday(date) && showCount < count && WarnDialogUtil.isAppBackground()) {
                showCount = showCount + 1;
                mSharedPreference.edit().putInt(SharedPreferenceKey.WIFI_WARN_DIALOG_SHOW_COUNT, showCount).apply();
                mSharedPreference.edit().putLong(SharedPreferenceKey.WIFI_WARN_DIALOG_SHOW_TIME, System.currentTimeMillis()).apply();
                Firebase.getInstance(this).logEvent("大弹窗", "开始跳转", "链接WiFi");
                WarnDialogActivity.start(this, WarnDialogActivity.CONNECT_PUBLIC_WIFI_DIALOG);
            } else if (!DateUtils.isToday(date) && WarnDialogUtil.isAppBackground()) {
                mSharedPreference.edit().putInt(SharedPreferenceKey.WIFI_WARN_DIALOG_SHOW_COUNT, 1).apply();
                mSharedPreference.edit().putLong(SharedPreferenceKey.WIFI_WARN_DIALOG_SHOW_TIME, System.currentTimeMillis()).apply();
                Firebase.getInstance(this).logEvent("大弹窗", "开始跳转", "链接WiFi");
                WarnDialogActivity.start(this, WarnDialogActivity.CONNECT_PUBLIC_WIFI_DIALOG);
            }
        }
    }

    private void monitorDevelopedCountryInactiveUserDialog() {
        long date = mSharedPreference.getLong(SharedPreferenceKey.DEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_TIME, 0);
        long lastShowTime = mSharedPreference.getLong(SharedPreferenceKey.DEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_TIME, 0);
        int showCount = mSharedPreference.getInt(SharedPreferenceKey.DEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_COUNT, 0);
        int count = (int) FirebaseRemoteConfig.getInstance().getLong("developed_country_inactive_user_dialog_show_count");
        int spaceTime = (int) FirebaseRemoteConfig.getInstance().getLong("developed_country_inactive_user_dialog_space_minutes");
        long hour_of_day = WarnDialogUtil.getHourOrDay();
        boolean isInactiveUser = !DateUtils.isToday(mSharedPreference.getLong(SharedPreferenceKey.OPEN_APP_TIME_TO_DECIDE_INACTIVE_USER, 0));

        //默认2小时冷却,间隔可以配置,并且判断为不活跃用户 开始跳转时间为当地6:00 - 23:00
        if (WarnDialogUtil.isAdLoaded(this, false) && WarnDialogUtil.isSpaceTimeShow(lastShowTime, spaceTime)
                && isInactiveUser && hour_of_day >= 18 && hour_of_day <= 23) {
            if (date == 0 && WarnDialogUtil.isAppBackground() && showCount < count) {
                showCount = showCount + 1;
                mSharedPreference.edit().putLong(SharedPreferenceKey.DEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_TIME, System.currentTimeMillis()).apply();
                mSharedPreference.edit().putInt(SharedPreferenceKey.DEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_COUNT, showCount).apply();
                Firebase.getInstance(this).logEvent("大弹窗", "开始跳转", "发达国家不活跃用户");
                WarnDialogActivity.start(this, WarnDialogActivity.DEVELOPED_COUNTRY_INACTIVE_USER_DIALOG);
            } else if (DateUtils.isToday(date) && showCount < count && WarnDialogUtil.isAppBackground()) {
                showCount = showCount + 1;
                mSharedPreference.edit().putLong(SharedPreferenceKey.DEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_TIME, System.currentTimeMillis()).apply();
                mSharedPreference.edit().putInt(SharedPreferenceKey.DEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_COUNT, showCount).apply();
                Firebase.getInstance(this).logEvent("大弹窗", "开始跳转", "发达国家不活跃用户");
                WarnDialogActivity.start(this, WarnDialogActivity.DEVELOPED_COUNTRY_INACTIVE_USER_DIALOG);
            } else if (!DateUtils.isToday(date) && WarnDialogUtil.isAppBackground()) {
                mSharedPreference.edit().putLong(SharedPreferenceKey.DEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_TIME, System.currentTimeMillis()).apply();
                mSharedPreference.edit().putInt(SharedPreferenceKey.DEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_COUNT, 1).apply();
                Firebase.getInstance(this).logEvent("大弹窗", "开始跳转", "发达国家不活跃用户");
                WarnDialogActivity.start(this, WarnDialogActivity.DEVELOPED_COUNTRY_INACTIVE_USER_DIALOG);
            }
        }
    }

    private void monitorUndevelopedCountryInactiveUserDialog() {
        long date = mSharedPreference.getLong(SharedPreferenceKey.UNDEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_TIME, 0);
        long lastShowTime = mSharedPreference.getLong(SharedPreferenceKey.UNDEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_TIME, 0);
        int showCount = mSharedPreference.getInt(SharedPreferenceKey.UNDEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_COUNT, 0);
        int count = (int) FirebaseRemoteConfig.getInstance().getLong("undeveloped_country_inactive_user_dialog_show_count");
        int spaceTime = (int) FirebaseRemoteConfig.getInstance().getLong("is_undeveloped_country_inactive_user_dialog_space_minutes");
        long hour_of_day = WarnDialogUtil.getHourOrDay();
        boolean isInactiveUser = !DateUtils.isToday(mSharedPreference.getLong(SharedPreferenceKey.OPEN_APP_TIME_TO_DECIDE_INACTIVE_USER, 0));

        //默认2小时冷却,间隔可以配置,并且判断为不活跃用户 开始跳转时间为当地6:00 - 23:00
        if (WarnDialogUtil.isAdLoaded(this, false) && WarnDialogUtil.isSpaceTimeShow(lastShowTime, spaceTime) && isInactiveUser && hour_of_day >= 18 && hour_of_day <= 23) {
            if (date == 0 && WarnDialogUtil.isAppBackground() && showCount < count) {
                showCount = showCount + 1;
                mSharedPreference.edit().putLong(SharedPreferenceKey.UNDEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_TIME, System.currentTimeMillis()).apply();
                mSharedPreference.edit().putInt(SharedPreferenceKey.UNDEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_COUNT, showCount).apply();
                Firebase.getInstance(this).logEvent("大弹窗", "开始跳转", "不发达国家不活跃用户");
                WarnDialogActivity.start(this, WarnDialogActivity.UNDEVELOPED_COUNTRY_INACTIVE_USER_DIALOG);
            } else if (DateUtils.isToday(date) && showCount < count && WarnDialogUtil.isAppBackground()) {
                showCount = showCount + 1;
                mSharedPreference.edit().putLong(SharedPreferenceKey.UNDEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_TIME, System.currentTimeMillis()).apply();
                mSharedPreference.edit().putInt(SharedPreferenceKey.UNDEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_COUNT, showCount).apply();
                Firebase.getInstance(this).logEvent("大弹窗", "开始跳转", "不发达国家不活跃用户");
                WarnDialogActivity.start(this, WarnDialogActivity.UNDEVELOPED_COUNTRY_INACTIVE_USER_DIALOG);
            } else if (!DateUtils.isToday(date) && WarnDialogUtil.isAppBackground()) {
                mSharedPreference.edit().putLong(SharedPreferenceKey.UNDEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_TIME, System.currentTimeMillis()).apply();
                mSharedPreference.edit().putInt(SharedPreferenceKey.UNDEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_COUNT, 1).apply();
                Firebase.getInstance(this).logEvent("大弹窗", "开始跳转", "不发达国家不活跃用户");
                WarnDialogActivity.start(this, WarnDialogActivity.UNDEVELOPED_COUNTRY_INACTIVE_USER_DIALOG);
            }
        }
    }

    private class WarnDialogAdStateListener extends AdStateListener {
        @Override
        public void onAdLoaded(AdType adType, int index) {
            if (isWifiConnected && !LocalVpnService.IsRunning) {
                monitorWifiStateChangeDialog();
                AdAppHelper.getInstance(WarnDialogShowService.this).removeAdStateListener(mAdStateListener);
            }
        }
    }

    public static void start(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(new Intent(context, WarnDialogShowService.class));
        } else {
            context.startService(new Intent(context, WarnDialogShowService.class));
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterWarnDialogReceiver();
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_WIFI_CONNECTED:
                mHandler.removeMessages(MSG_WIFI_CONNECTED);
                monitorWifiStateChangeDialog();
                break;
        }
        return true;
    }

    private class WarnDialogReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (networkInfo == null)
                    return;

                NetworkInfo.State state = networkInfo.getState();
                if (state == NetworkInfo.State.CONNECTED) {
                    WifiInfo wifiInfo = null;
                    try {
                        wifiInfo = ((WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE)).getConnectionInfo();
                    } catch (Exception e) {
                    }

                    if (wifiInfo == null) {
                        Log.i(TAG, "getSystemService wifiInfo is null");
                        return;
                    }
                    //wifi链接一天弹两次
                    if (FirebaseRemoteConfig.getInstance().getBoolean("is_wifi_dialog_show")
                            && !TextUtils.isEmpty(wifiInfo.getSSID())
                            && !LocalVpnService.IsRunning) {
                        Log.i(TAG, "onReceive:   " + wifiInfo.getSSID());
                        //这个状态会执行两次，没有发现解决的好办法，为了只起一个界面延迟做一下跳转，在handler里面做removeMessages
                        mHandler.sendEmptyMessageDelayed(MSG_WIFI_CONNECTED, 2000);
                        AdAppHelper.getInstance(WarnDialogShowService.this).loadNewNative();
                        isWifiConnected = true;
                    }

                } else if (state == NetworkInfo.State.DISCONNECTED) {
                    Log.i(TAG, "onReceive:   " + "disconnected");
                    isWifiConnected = false;
                }
            } else if (Intent.ACTION_TIME_TICK.equals(action)) {
                // 15分钟执行一次检查
                if (System.currentTimeMillis() - startTime >= TimeUnit.MINUTES.toMillis(15)) {
                    if (FirebaseRemoteConfig.getInstance().getBoolean("is_developed_country_inactive_user_dialog_show")
                            && (countryCode.equals("US") || countryCode.equals("DE") || countryCode.equals("GB") || countryCode.equals("AU"))
                            && !LocalVpnService.IsRunning) {
                        //云控控制开关并且针对发达国家（美国，德国，澳大利亚，英国）
                        monitorDevelopedCountryInactiveUserDialog();
                    } else if (FirebaseRemoteConfig.getInstance().getBoolean("is_undeveloped_country_inactive_user_dialog_show")
                            && !countryCode.equals("US") && !countryCode.equals("DE") && !countryCode.equals("GB") && !countryCode.equals("AU")
                            && !LocalVpnService.IsRunning) {
                        //针对不发达国家不活跃用户进行的弹窗
                        monitorUndevelopedCountryInactiveUserDialog();
                    }
                    startTime = System.currentTimeMillis();
                }
            }
        }
    }
}