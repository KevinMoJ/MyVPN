package com.androapplite.shadowsocks.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;

public class ReportUseTimeReceiver extends BroadcastReceiver {
    public ReportUseTimeReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(context);

    }
}
