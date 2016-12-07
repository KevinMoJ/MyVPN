package com.androapplite.shadowsocks.fragment;


import android.app.Activity;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.v4.app.Fragment;
import android.widget.ListView;

import com.androapplite.shadowsocks.R;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener,
        Preference.OnPreferenceChangeListener
{
    private OnSettingsActionListener mListener;

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
        if(mListener != null){
            mListener.about();
        }
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if(key.equals(SharedPreferenceKey.NOTIFICATION)){
            if(mListener != null){
                mListener.enableNotification((Boolean)newValue);
            }
            return true;
        }
        return false;
    }

    public interface OnSettingsActionListener {
        void about();
        void enableNotification(boolean enable);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if(activity instanceof OnSettingsActionListener){
            mListener = (OnSettingsActionListener)activity;
        }else{
            throw new ClassCastException(activity.getLocalClassName() + " must implement " + OnSettingsActionListener.class.getSimpleName());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
