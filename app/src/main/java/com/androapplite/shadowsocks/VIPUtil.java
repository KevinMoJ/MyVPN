package com.androapplite.shadowsocks;

import android.content.Context;
import android.content.SharedPreferences;

import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;

/**
 * Created by jim on 17/1/18.
 */

public final class VIPUtil {
    public static final boolean isVIP(Context context){
        SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(context);
        return isVIP(sharedPreferences);
    }

    public static final boolean isVIP(SharedPreferences sharedPreferences){
        long exipreDate = sharedPreferences.getLong(SharedPreferenceKey.EXPIRED_DATE, 0);
        return exipreDate > System.currentTimeMillis();
    }
}
