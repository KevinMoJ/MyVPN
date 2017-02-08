package com.androapplite.shadowsocks.fragment;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.vpn3.R;

/**
 * Created by jim on 17/2/9.
 */

public class AlertSettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setSharedPreferencesName(DefaultSharedPrefeencesUtil.PREFERENCE_NAME);
        preferenceManager.setSharedPreferencesMode(Context.MODE_PRIVATE);
        addPreferencesFromResource(R.xml.alert_settings);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            getPreferenceScreen().removePreference(findPreference("app_detect"));
        }
    }
}
