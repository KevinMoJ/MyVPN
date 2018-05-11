package com.androapplite.shadowsocks.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.activity.WarnDialogActivity;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.vm.shadowsocks.core.LocalVpnService;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class WarnDialogShowService extends Service implements Handler.Callback {
    private static final String TAG = "WarnDialogShowService";

    private static final int MSG_WIFI_CONNECTED = 10;

    private Handler mHandler;
    private SharedPreferences mSharedPreference;
    private WarnDialogReceiver mWarnDialogReceiver;
    private String countryCode;

    private long startTime;

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

    private void monitorDevelopedCountryInactiveUserDialog() {
        long lastShowTime = mSharedPreference.getLong(SharedPreferenceKey.WARN_DIALOG_SHOW_DATE, 0);
        int showCount = mSharedPreference.getInt(SharedPreferenceKey.DEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_COUNT, 0);
        int count = (int) FirebaseRemoteConfig.getInstance().getLong("developed_country_inactive_user_dialog_show_count");
        //同一天一个弹窗弹一次，下午6点以后才可以弹
        if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= 18 && Calendar.getInstance().get(Calendar.HOUR_OF_DAY) <= 24
                && DateUtils.isToday(lastShowTime) && showCount < count && ((ShadowsocksApplication) this.getApplicationContext()).getOpenActivityNumber() <= 0) {
            showCount = showCount + 1;
            mSharedPreference.edit().putLong(SharedPreferenceKey.WARN_DIALOG_SHOW_DATE, System.currentTimeMillis()).apply();
            mSharedPreference.edit().putInt(SharedPreferenceKey.DEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_COUNT, showCount).apply();
            WarnDialogActivity.start(this, WarnDialogActivity.DEVELOPED_COUNTRY_INACTIVE_USER_DIALOG);
        } else if (!DateUtils.isToday(lastShowTime)) {
            mSharedPreference.edit().putLong(SharedPreferenceKey.WARN_DIALOG_SHOW_DATE, System.currentTimeMillis()).apply();
            mSharedPreference.edit().putInt(SharedPreferenceKey.DEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_COUNT, 0).apply();
        }
    }

    private void monitorWifiStateChangeDialog() {
        long lastShowTime = mSharedPreference.getLong(SharedPreferenceKey.WARN_DIALOG_SHOW_DATE, 0);
        int showCount = mSharedPreference.getInt(SharedPreferenceKey.WIFI_WARN_DIALOG_SHOW_COUNT, 0);
        int count = (int) FirebaseRemoteConfig.getInstance().getLong("wifi_dialog_show_count");
        //同一天一个弹窗最多弹两次 弹的次数可以云控控制
        if (DateUtils.isToday(lastShowTime) && showCount < count && ((ShadowsocksApplication) this.getApplicationContext()).getOpenActivityNumber() <= 0) {
            showCount = showCount + 1;
            mSharedPreference.edit().putInt(SharedPreferenceKey.WIFI_WARN_DIALOG_SHOW_COUNT, showCount).apply();
            mSharedPreference.edit().putLong(SharedPreferenceKey.WARN_DIALOG_SHOW_DATE, System.currentTimeMillis()).apply();
            WarnDialogActivity.start(this, WarnDialogActivity.CONNECT_PUBLIC_WIFI_DIALOG);
        } else if (!DateUtils.isToday(lastShowTime)) {
            mSharedPreference.edit().putInt(SharedPreferenceKey.WIFI_WARN_DIALOG_SHOW_COUNT, 0).apply();
            mSharedPreference.edit().putLong(SharedPreferenceKey.WARN_DIALOG_SHOW_DATE, System.currentTimeMillis()).apply();
        }
    }

    private void monitorUndevelopedCountryInactiveUserDialog() {
        long lastShowTime = mSharedPreference.getLong(SharedPreferenceKey.WARN_DIALOG_SHOW_DATE, 0);
        int showCount = mSharedPreference.getInt(SharedPreferenceKey.UNDEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_COUNT, 0);
        int count = (int) FirebaseRemoteConfig.getInstance().getLong("undeveloped_country_inactive_user_dialog_show_count");
        //同一天一个弹窗弹一次
        if (DateUtils.isToday(lastShowTime) && showCount < count && ((ShadowsocksApplication) this.getApplicationContext()).getOpenActivityNumber() <= 0) {
            showCount = showCount + 1;
            mSharedPreference.edit().putLong(SharedPreferenceKey.WARN_DIALOG_SHOW_DATE, System.currentTimeMillis()).apply();
            mSharedPreference.edit().putInt(SharedPreferenceKey.UNDEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_COUNT, showCount).apply();
            WarnDialogActivity.start(this, WarnDialogActivity.UNDEVELOPED_COUNTRY_INACTIVE_USER_DIALOG);
        } else if (!DateUtils.isToday(lastShowTime)) {
            mSharedPreference.edit().putLong(SharedPreferenceKey.WARN_DIALOG_SHOW_DATE, System.currentTimeMillis()).apply();
            mSharedPreference.edit().putInt(SharedPreferenceKey.UNDEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_COUNT, 0).apply();
        }
    }

    public static void start(Context context) {
        context.startService(new Intent(context, WarnDialogShowService.class));
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
                    }

                } else if (state == NetworkInfo.State.DISCONNECTED) {
                    Log.i(TAG, "onReceive:   " + "disconnected");
                }
            } else if (Intent.ACTION_TIME_TICK.equals(action)) {
                // 15分钟执行一次检查 云控控制开关并且针对发达国家（美国，德国，澳大利亚，英国）
                if (System.currentTimeMillis() - startTime >= TimeUnit.MINUTES.toMillis(15)) {
                    if (FirebaseRemoteConfig.getInstance().getBoolean("is_developed_country_inactive_user_dialog_show")
                            && (countryCode.equals("US") || countryCode.equals("DE") || countryCode.equals("GB") || countryCode.equals("AU"))) {
                        monitorDevelopedCountryInactiveUserDialog();
                    }
                    startTime = System.currentTimeMillis();
                }

                //针对不发达国家不活跃用户进行的弹窗
                if (FirebaseRemoteConfig.getInstance().getBoolean("is_undeveloped_country_inactive_user_dialog_show")
                        && !countryCode.equals("US") && !countryCode.equals("DE") && !countryCode.equals("GB") && !countryCode.equals("AU")) {
                    monitorUndevelopedCountryInactiveUserDialog();
                }
            }
        }
    }
}