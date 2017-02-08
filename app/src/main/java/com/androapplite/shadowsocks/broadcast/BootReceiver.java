package com.androapplite.shadowsocks.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.androapplite.shadowsocks.CheckInAlarm;
import com.androapplite.shadowsocks.service.AppCheckService;

public class BootReceiver extends BroadcastReceiver {
    public BootReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            CheckInAlarm.startCheckInAlarm(context);
            AppCheckService.startAppCheckService(context);
        }
    }
}
