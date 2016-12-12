package com.androapplite.shadowsocks.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.androapplite.shadowsocks.GAHelper;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;

import java.util.Calendar;

import yyf.shadowsocks.broadcast.*;
import yyf.shadowsocks.broadcast.Action;
import yyf.shadowsocks.preferences.SharedPreferenceKey;

public class ShadowsocksVPNServiceReceiver extends BroadcastReceiver {
    public ShadowsocksVPNServiceReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent != null) {
            String action = intent.getAction();
            long duration = intent.getLongExtra(SharedPreferenceKey.DURATION, 0);
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
                    GAHelper.sendEvent(context, "VPN状态", "连接成功");
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
                    GAHelper.sendEvent(context, "VPN状态", "错误");
                    if(duration > 0){
                        GAHelper.sendTimingEvent(context, "VPN计时", "错误", duration);
                    }
                    break;
            }
        }
    }
}
