package com.androapplite.shadowsocks.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.androapplite.shadowsocks.GAHelper;
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
            switch (action){
                case Action.INIT:
                    break;
                case Action.CONNECTING:
                    GAHelper.sendEvent(context, "VPN状态", "开始连接");
                    if(duration > 0){
                        GAHelper.sendTimingEvent(context, "VPN计时", "使用", duration);
                    }
                    break;
                case Action.CONNECTED:
                    sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(context);
                    errorCount = sharedPreferences.getInt(SharedPreferenceKey.CONNECT_ERROR_COUNT, 0);
                    GAHelper.sendEvent(context, "VPN状态", "连接成功", "连续错误" + errorCount);
                    sharedPreferences.edit()
                            .putBoolean(SharedPreferenceKey.FIRST_CONNECT_SUCCESS, true)
                            .remove(SharedPreferenceKey.CONNECT_ERROR_COUNT)
                            .commit();
                    if(duration > 0){
                        GAHelper.sendTimingEvent(context, "VPN计时", "连接", duration);
                    }
                    break;
                case Action.STOPPING:
                    GAHelper.sendEvent(context, "VPN状态", "开始断开");
                    if(duration > 0){
                        GAHelper.sendTimingEvent(context, "VPN计时", "使用", duration);
                    }
                    break;
                case Action.STOPPED:
                    GAHelper.sendEvent(context, "VPN状态", "断开成功");
                    if(duration > 0){
                        GAHelper.sendTimingEvent(context, "VPN计时", "断开", duration);
                    }
                    break;
                case Action.ERROR:
                    sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(context);
                    errorCount = sharedPreferences.getInt(SharedPreferenceKey.CONNECT_ERROR_COUNT, 1);
                    GAHelper.sendEvent(context, "VPN状态", "错误", String.valueOf(errorCount));
                    sharedPreferences.edit().putInt(SharedPreferenceKey.CONNECT_ERROR_COUNT, errorCount + 1).commit();
                    if(duration > 0){
                        GAHelper.sendTimingEvent(context, "VPN计时", "错误", duration);
                    }
                    break;
            }
        }
    }
}
