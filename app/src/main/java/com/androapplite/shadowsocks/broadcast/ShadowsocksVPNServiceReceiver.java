package com.androapplite.shadowsocks.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;

import yyf.shadowsocks.broadcast.Action;

public class ShadowsocksVPNServiceReceiver extends BroadcastReceiver {
    public ShadowsocksVPNServiceReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent != null) {
            String action = intent.getAction();
            long duration = intent.getLongExtra(yyf.shadowsocks.preferences.SharedPreferenceKey.DURATION, 0);
            SharedPreferences sharedPreferences;
            int errorCount;
            Firebase firebase = Firebase.getInstance(context);
            switch (action){
                case Action.INIT:
                    break;
                case Action.CONNECTING:
                    firebase.logEvent("VPN状态", "开始连接");
                    break;
                case Action.CONNECTED:
                    sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(context);
                    errorCount = sharedPreferences.getInt(SharedPreferenceKey.CONNECT_ERROR_COUNT, 0);
                    firebase.logEvent("VPN状态", "连接成功", "连续错误" + errorCount);

                    sharedPreferences.edit()
                            .putBoolean(SharedPreferenceKey.FIRST_CONNECT_SUCCESS, true)
                            .remove(SharedPreferenceKey.CONNECT_ERROR_COUNT)
                            .apply();
                    if(duration > 0){
                        firebase.logEvent("VPN计时", "连接", duration);
                    }
                    long successConnectCount = sharedPreferences.getLong(SharedPreferenceKey.SUCCESS_CONNECT_COUNT, 0) + 1;
                    sharedPreferences.edit().putLong(SharedPreferenceKey.SUCCESS_CONNECT_COUNT, successConnectCount).apply();
                    firebase.logEvent("累计连接成功失败次数", "成功", successConnectCount);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(com.androapplite.shadowsocks.broadcast.Action.CONNECT_COUNT_CHANGED));

                    break;
                case Action.STOPPING:
                    firebase.logEvent("VPN状态", "开始断开");
                    if(duration > 0){
                        firebase.logEvent("VPN计时", "使用", duration);
                    }
                    break;
                case Action.STOPPED:
                    firebase.logEvent("VPN状态", "断开成功");

                    if(duration > 0){
                        firebase.logEvent("VPN计时", "断开", duration);
                    }
                    break;
                case Action.ERROR:
                    sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(context);
                    errorCount = sharedPreferences.getInt(SharedPreferenceKey.CONNECT_ERROR_COUNT, 1);
                    firebase.logEvent("VPN状态", "错误", errorCount);

                    sharedPreferences.edit().putInt(SharedPreferenceKey.CONNECT_ERROR_COUNT, errorCount + 1).apply();
                    if(duration > 0){
                        firebase.logEvent("VPN计时", "错误", duration);
                    }
                    long failedConnectCount = sharedPreferences.getLong(SharedPreferenceKey.FAILED_CONNECT_COUNT, 0) + 1;
                    sharedPreferences.edit().putLong(SharedPreferenceKey.SUCCESS_CONNECT_COUNT, failedConnectCount).apply();
                    firebase.logEvent("累计连接成功失败次数", "失败", failedConnectCount);

                    LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(com.androapplite.shadowsocks.broadcast.Action.CONNECT_COUNT_CHANGED));
                    break;
            }
        }
    }
}
