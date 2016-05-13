package com.androapplite.shadowsocks.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.androapplite.shadowsocks.GAHelper;

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
            if(action.equals(Action.RESET_TOTAL)){
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                long txTotal = intent.getLongExtra(SharedPreferenceKey.TX_TOTAL, 0);
                long rxTotal = intent.getLongExtra(SharedPreferenceKey.RX_TOTAL, 0);
                long resetMonth = intent.getLongExtra(SharedPreferenceKey.LAST_RESET_MONTH, 0);
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(resetMonth);
                String monthString = String.valueOf(calendar.get(Calendar.MONTH));
                GAHelper.sendEvent(context, "流量", "发送", monthString, txTotal);
                GAHelper.sendEvent(context, "流量", "接收", monthString, rxTotal);
                GAHelper.sendEvent(context, "流量", "总共", monthString, txTotal + rxTotal);
            }
        }
    }
}