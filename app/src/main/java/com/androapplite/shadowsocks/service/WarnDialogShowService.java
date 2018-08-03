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

import com.androapplite.shadowsocks.activity.LoadingDialogActivity;
import com.androapplite.shadowsocks.activity.WarnDialogActivity;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.androapplite.shadowsocks.utils.RuntimeSettings;
import com.androapplite.shadowsocks.utils.ServiceUtils;
import com.androapplite.shadowsocks.utils.Utils;
import com.androapplite.shadowsocks.utils.WarnDialogUtil;
import com.vm.shadowsocks.core.LocalVpnService;

import java.util.concurrent.TimeUnit;

public class WarnDialogShowService extends Service implements Handler.Callback {
    private static final String TAG = "WarnDialogShowService";

    private static final int MSG_WIFI_CONNECTED = 10;

    private Handler mHandler;
    private SharedPreferences mSharedPreference;
    private WarnDialogReceiver mWarnDialogReceiver;
    private String countryCode;

    private long startTime;
    private boolean isWifiConnected;
    private long previousShowTime;//幸运转盘弹窗上一次出现的时间

    public WarnDialogShowService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        mSharedPreference = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
        countryCode = mSharedPreference.getString(SharedPreferenceKey.COUNTRY_CODE, "未知");
        startTime = System.currentTimeMillis();
        long openLuckPanTime = RuntimeSettings.getLuckPanOpenStartTime();
        if (!DateUtils.isToday(openLuckPanTime))
            RuntimeSettings.setLuckPanOpenStartTime(0);
        mHandler = new Handler(this);
        registerWarnDialogReceiver();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ServiceUtils.startForgound(this);
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
        long date = RuntimeSettings.getWifiDialogShowTime();
        long lastShowTime = RuntimeSettings.getWifiDialogShowTime();
        int showCount = RuntimeSettings.getWifiDialogShowCount();
        int count = 2;
        int spaceTime = 120;
        long hour_of_day = WarnDialogUtil.getHourOrDay();
        boolean isInactiveUser = !DateUtils.isToday(RuntimeSettings.getOpenAppToDecideInactiveTime());
        //同一天一个弹窗最多弹两次 弹的次数可以云控控制   默认2小时冷却,间隔可以配置   23:00 - 9:00 的时间段禁止弹
        if (WarnDialogUtil.isAdLoaded(this, true) && WarnDialogUtil.isSpaceTimeShow(lastShowTime, spaceTime) && isInactiveUser
                && hour_of_day > 9 && hour_of_day < 23) {
            //新用户第一次没有数据的时候弹窗
            if (date == 0 && WarnDialogUtil.isAppBackground() && showCount < count) {
                showCount = showCount + 1;
                RuntimeSettings.setWifiDialogShowCount(showCount);
                RuntimeSettings.setWifiDialogShowTime(System.currentTimeMillis());
                LoadingDialogActivity.start(this, WarnDialogActivity.CONNECT_PUBLIC_WIFI_DIALOG);
            } else if (DateUtils.isToday(date) && showCount < count && WarnDialogUtil.isAppBackground()) {
                showCount = showCount + 1;
                RuntimeSettings.setWifiDialogShowCount(showCount);
                RuntimeSettings.setWifiDialogShowTime(System.currentTimeMillis());
                LoadingDialogActivity.start(this, WarnDialogActivity.CONNECT_PUBLIC_WIFI_DIALOG);
            } else if (!DateUtils.isToday(date) && WarnDialogUtil.isAppBackground()) {
                RuntimeSettings.setWifiDialogShowCount(1);
                RuntimeSettings.setWifiDialogShowTime(System.currentTimeMillis());
                LoadingDialogActivity.start(this, WarnDialogActivity.CONNECT_PUBLIC_WIFI_DIALOG);
            }
        }
    }

    private void monitorDevelopedCountryInactiveUserDialog() {
        long date = RuntimeSettings.getDevelopedCountryInactiveUserDialogShowTime();
        long lastShowTime = RuntimeSettings.getDevelopedCountryInactiveUserDialogShowTime();
        int showCount = RuntimeSettings.getDevelopedCountryInactiveUserDialogShowCount();
        int count = 2;
        int spaceTime = 120;
        long hour_of_day = WarnDialogUtil.getHourOrDay();
        boolean isInactiveUser = !DateUtils.isToday(RuntimeSettings.getOpenAppToDecideInactiveTime());

        //默认2小时冷却,间隔可以配置,并且判断为不活跃用户 开始跳转时间为当地6:00 - 23:00
        if (WarnDialogUtil.isAdLoaded(this, false) && WarnDialogUtil.isSpaceTimeShow(lastShowTime, spaceTime)
                && isInactiveUser && hour_of_day >= 18 && hour_of_day <= 23) {
            if (date == 0 && WarnDialogUtil.isAppBackground() && showCount < count) {
                showCount = showCount + 1;
                RuntimeSettings.setDevelopedCountryInactiveUserDialogShowCount(showCount);
                RuntimeSettings.setDevelopedCountryInactiveUserDialogShowTime(System.currentTimeMillis());
                LoadingDialogActivity.start(this, WarnDialogActivity.DEVELOPED_COUNTRY_INACTIVE_USER_DIALOG);
            } else if (DateUtils.isToday(date) && showCount < count && WarnDialogUtil.isAppBackground()) {
                showCount = showCount + 1;
                RuntimeSettings.setDevelopedCountryInactiveUserDialogShowTime(System.currentTimeMillis());
                RuntimeSettings.setDevelopedCountryInactiveUserDialogShowCount(showCount);
                LoadingDialogActivity.start(this, WarnDialogActivity.DEVELOPED_COUNTRY_INACTIVE_USER_DIALOG);
            } else if (!DateUtils.isToday(date) && WarnDialogUtil.isAppBackground()) {
                RuntimeSettings.setDevelopedCountryInactiveUserDialogShowTime(System.currentTimeMillis());
                RuntimeSettings.setDevelopedCountryInactiveUserDialogShowCount(showCount);
                LoadingDialogActivity.start(this, WarnDialogActivity.DEVELOPED_COUNTRY_INACTIVE_USER_DIALOG);
            }
        }
    }

    private void monitorUndevelopedCountryInactiveUserDialog() {
        long date = RuntimeSettings.getUndevelopedCountryInactiveUserDialogShowTime();
        long lastShowTime = RuntimeSettings.getUndevelopedCountryInactiveUserDialogShowTime();
        int showCount = RuntimeSettings.getUndevelopedCountryInactiveUserDialogShowCount();
        int count = 2;
        int spaceTime = 120;
        long hour_of_day = WarnDialogUtil.getHourOrDay();
        boolean isInactiveUser = !DateUtils.isToday(RuntimeSettings.getOpenAppToDecideInactiveTime());

        //默认2小时冷却,间隔可以配置,并且判断为不活跃用户 开始跳转时间为当地6:00 - 23:00
        if (WarnDialogUtil.isAdLoaded(this, false) && WarnDialogUtil.isSpaceTimeShow(lastShowTime, spaceTime) && isInactiveUser && hour_of_day >= 18 && hour_of_day <= 23) {
            if (date == 0 && WarnDialogUtil.isAppBackground() && showCount < count) {
                showCount = showCount + 1;
                RuntimeSettings.setUndevelopedCountryInactiveUserDialogShowCount(showCount);
                RuntimeSettings.setUndevelopedCountryInactiveUserDialogShowTime(System.currentTimeMillis());
                LoadingDialogActivity.start(this, WarnDialogActivity.UNDEVELOPED_COUNTRY_INACTIVE_USER_DIALOG);
            } else if (DateUtils.isToday(date) && showCount < count && WarnDialogUtil.isAppBackground()) {
                showCount = showCount + 1;
                RuntimeSettings.setUndevelopedCountryInactiveUserDialogShowCount(showCount);
                RuntimeSettings.setUndevelopedCountryInactiveUserDialogShowTime(System.currentTimeMillis());
                LoadingDialogActivity.start(this, WarnDialogActivity.UNDEVELOPED_COUNTRY_INACTIVE_USER_DIALOG);
            } else if (!DateUtils.isToday(date) && WarnDialogUtil.isAppBackground()) {
                RuntimeSettings.setUndevelopedCountryInactiveUserDialogShowCount(1);
                RuntimeSettings.setUndevelopedCountryInactiveUserDialogShowTime(System.currentTimeMillis());
                LoadingDialogActivity.start(this, WarnDialogActivity.UNDEVELOPED_COUNTRY_INACTIVE_USER_DIALOG);
            }
        }
    }

    public static void start(Context context) {
        ServiceUtils.startService(context, new Intent(context, WarnDialogShowService.class));
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
                    if (!TextUtils.isEmpty(wifiInfo.getSSID())
                            && !LocalVpnService.IsRunning
                            && !RuntimeSettings.isVIP()) {
                        Log.i(TAG, "onReceive:   " + wifiInfo.getSSID());
                        //这个状态会执行两次，没有发现解决的好办法，为了只起一个界面延迟做一下跳转，在handler里面做removeMessages
                        mHandler.sendEmptyMessageDelayed(MSG_WIFI_CONNECTED, 2000);
                        isWifiConnected = true;
                    }

                } else if (state == NetworkInfo.State.DISCONNECTED) {
                    Log.i(TAG, "onReceive:   " + "disconnected");
                    isWifiConnected = false;
                }
            } else if (Intent.ACTION_TIME_TICK.equals(action)) {
                // 15分钟执行一次检查
                if ((System.currentTimeMillis() - startTime >= TimeUnit.MINUTES.toMillis(15)) && !RuntimeSettings.isVIP()) {
                    if ((countryCode.equals("US") || countryCode.equals("DE") || countryCode.equals("GB") || countryCode.equals("AU"))
                            && !LocalVpnService.IsRunning) {
                        //云控控制开关并且针对发达国家（美国，德国，澳大利亚，英国）
                        monitorDevelopedCountryInactiveUserDialog();
                    } else if (!countryCode.equals("US") && !countryCode.equals("DE") && !countryCode.equals("GB") && !countryCode.equals("AU")
                            && !LocalVpnService.IsRunning) {
                        //针对不发达国家不活跃用户进行的弹窗
                        monitorUndevelopedCountryInactiveUserDialog();
                    }
                    startTime = System.currentTimeMillis();
                }

                // 当当天的转转盘天数没有达到7天的时候，每隔一个小时弹一次弹窗，弹出的弹窗要带出全屏，如果没有全屏的话就下一分钟进行弹，直到弹出弹窗为止，弹出的时间
                //开始计时，当弹出的
                long todayGetFreeDay = RuntimeSettings.getLuckPanFreeDay();
                long cloudGetLuckFreeDay = 7;
                long cloudLuckPanShowSpaceTime = 120;
                long hour_of_day = WarnDialogUtil.getHourOrDay();
                long cloudShowCountDay = 3;
                int showCount = RuntimeSettings.getLuckPanDialogShowCount();

                if ((todayGetFreeDay < cloudGetLuckFreeDay) && Utils.isScreenOn(WarnDialogShowService.this)
                        && WarnDialogUtil.isSpaceTimeShow(previousShowTime, cloudLuckPanShowSpaceTime) && !WarnDialogActivity.activityIsShowing
                        && WarnDialogUtil.isAppBackground() && WarnDialogUtil.isAdLoaded(WarnDialogShowService.this, true)
                        && hour_of_day >= 9 && hour_of_day <= 24 && showCount <= cloudShowCountDay) {
                    previousShowTime = System.currentTimeMillis();
                    LoadingDialogActivity.start(WarnDialogShowService.this, WarnDialogActivity.LUCK_ROTATE_DIALOG);
                    RuntimeSettings.setLuckPanDialogShowCount(RuntimeSettings.getLuckPanDialogShowCount() + 1);
                }

                if (!DateUtils.isToday(previousShowTime))
                    RuntimeSettings.setLuckPanDialogShowCount(0);

            }
        }
    }
}