package com.androapplite.shadowsocks.activity;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by jim on 16/4/26.
 */
public class BaseShadowsocksActivity extends AppCompatActivity {
    protected BroadcastReceiver mBackgroundReceiver;
    protected IntentFilter mBackgroundReceiverIntentFilter;


    protected void registerBackgroundReceiver(){
        if(mBackgroundReceiver != null && mBackgroundReceiverIntentFilter != null){
            LocalBroadcastManager.getInstance(this).registerReceiver(mBackgroundReceiver, mBackgroundReceiverIntentFilter);
        }
    }

    protected void unregisterBackgroundReceiver(){
        if(mBackgroundReceiver != null){
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBackgroundReceiver);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        unregisterBackgroundReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        registerBackgroundReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterBackgroundReceiver();
    }
}
