package com.androapplite.shadowsocks.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;

public class WatchVideoADCallbackReceiver extends BroadcastReceiver {
    public WatchVideoADCallbackReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent != null && intent.getAction().equals(Action.VIDEO_AD_FINISH)){
            SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(context);
            int countDown = sharedPreferences.getInt(SharedPreferenceKey.TIME_COUNT_DOWN, 0);
            countDown += 3600;
            sharedPreferences.edit().putInt(SharedPreferenceKey.TIME_COUNT_DOWN, countDown).commit();
        }
    }
}
