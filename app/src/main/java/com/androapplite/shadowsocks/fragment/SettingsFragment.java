package com.androapplite.shadowsocks.fragment;


import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.androapplite.shadowsocks.R;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;

import static java.security.AccessController.getContext;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener,
        Preference.OnPreferenceChangeListener
{


    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesName(DefaultSharedPrefeencesUtil.PREFERENCE_NAME);
        addPreferencesFromResource(R.xml.settings);

        Preference aboutPreference = findPreference("about");
        aboutPreference.setOnPreferenceClickListener(this);

        SwitchPreference notificationPreference = (SwitchPreference)findPreference(SharedPreferenceKey.NOTIFICATION);
        notificationPreference.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ListView listView = (ListView)getView().findViewById(android.R.id.list);
        listView.setDivider(null);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        about();
        return true;
    }

    private void about(){
        PackageManager packageManager = getActivity().getPackageManager();
        String packageName = getActivity().getPackageName();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            String version = packageInfo.versionName;

            String appName = getResources().getString(R.string.app_name);

            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.about)
                    .setMessage(appName + " (" + version + ")")
                    .show();
        } catch (PackageManager.NameNotFoundException e) {
            ShadowsocksApplication.handleException(e);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if(key.equals(SharedPreferenceKey.NOTIFICATION)){
            Toast.makeText(getActivity(), "notification " + newValue, Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }
}
