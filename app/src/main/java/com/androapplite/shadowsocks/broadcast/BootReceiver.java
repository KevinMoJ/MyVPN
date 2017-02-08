package com.androapplite.shadowsocks.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.androapplite.shadowsocks.CheckInAlarm;

public class BootReceiver extends BroadcastReceiver {
    public BootReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            CheckInAlarm.startCheckInAlarm(context);
        }
    }
}
