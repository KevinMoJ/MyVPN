package com.androapplite.shadowsocks.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.androapplite.shadowsocks.GAHelper;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;

public class ReportUseTimeReceiver extends BroadcastReceiver {
    public ReportUseTimeReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(context);
        long lastUseTime = sharedPreferences.getLong(SharedPreferenceKey.LAST_USE_TIME, 0);
        long useTime = sharedPreferences.getLong(SharedPreferenceKey.USE_TIME, 0);
        if(lastUseTime <= 0 && useTime > 0){
            GAHelper.sendTimingEvent(context, "使用时间", "首次",useTime);
        }else if(useTime > lastUseTime){
            GAHelper.sendTimingEvent(context, "使用时间", "非首次",useTime-lastUseTime);
        }
        sharedPreferences.edit().putLong(SharedPreferenceKey.LAST_USE_TIME, useTime).commit();
    }
}
