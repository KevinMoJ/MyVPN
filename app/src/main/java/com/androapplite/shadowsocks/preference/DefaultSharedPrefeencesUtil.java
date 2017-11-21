package com.androapplite.shadowsocks.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

/**
 * Created by jim on 16/4/27.
 */
public class DefaultSharedPrefeencesUtil {
    public static final String PREFERENCE_NAME = "UI_PREFERENCE";
    public static final SharedPreferences getDefaultSharedPreferences(@NonNull Context context){
        return  context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
    }

    public static final SharedPreferences.Editor getDefaultSharedPreferencesEditor(Context context){
        return getDefaultSharedPreferences(context).edit();
    }
}
