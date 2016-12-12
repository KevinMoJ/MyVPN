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
            if(action.equals(Action.CONNECTED)){
            }else if(action.equals(Action.STOPPED)){
                long duration = intent.getLongExtra(SharedPreferenceKey.DURATION, 0);
                if(duration != 0){
                    GAHelper.sendTimingEvent(context, "VPN计时", "使用", duration);
                }
            }else if(action.equals(Action.ERROR)){
                long duration = intent.getLongExtra(SharedPreferenceKey.DURATION, 0);
                if(duration != 0){
                    GAHelper.sendTimingEvent(context, "VPN计时","错误", duration);
                }else{
                    GAHelper.sendEvent(context, "VPN状态", "错误");
                }
            }
        }
    }
}
