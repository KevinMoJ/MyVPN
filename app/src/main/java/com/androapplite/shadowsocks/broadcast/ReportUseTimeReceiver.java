package com.androapplite.shadowsocks.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.androapplite.shadowsocks.Firebase;
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
            Firebase.getInstance(context).logEvent("使用时间", "首次",useTime);
        }else if(useTime > lastUseTime){
            Firebase.getInstance(context).logEvent("使用时间", "非首次",useTime-lastUseTime);
        }
        sharedPreferences.edit().putLong(SharedPreferenceKey.LAST_USE_TIME, useTime).commit();
    }
}
