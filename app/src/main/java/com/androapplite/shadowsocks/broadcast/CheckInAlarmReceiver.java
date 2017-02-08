package com.androapplite.shadowsocks.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CheckInAlarmReceiver extends BroadcastReceiver {
    public CheckInAlarmReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("CheckInAlarmReceiver", "启动时钟");
    }
}
