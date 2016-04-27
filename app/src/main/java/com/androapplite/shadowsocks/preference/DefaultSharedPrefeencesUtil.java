package com.androapplite.shadowsocks.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by jim on 16/4/27.
 */
public class DefaultSharedPrefeencesUtil {
    public static final SharedPreferences getDefaultSharedPreferences(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static final SharedPreferences.Editor getDefaultSharedPreferencesEditor(Context context){
        return getDefaultSharedPreferences(context).edit();
    }

    public static final boolean isNewUser(Context context){
        SharedPreferences sharedPreferences = getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(SharedPreferenceKey.IS_NEW_USER, true);
    }

    public static final void markAsOldUser(Context context){
        getDefaultSharedPreferencesEditor(context).putBoolean(SharedPreferenceKey.IS_NEW_USER, false).apply();
    }
}
