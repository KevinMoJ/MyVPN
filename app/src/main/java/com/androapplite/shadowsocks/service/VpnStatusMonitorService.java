package com.androapplite.shadowsocks.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;

import java.util.UUID;

public class VpnStatusMonitorService extends Service {
    private String mUuid;
    public VpnStatusMonitorService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
        mUuid = sharedPreferences.getString(SharedPreferenceKey.UUID, null);
        if(mUuid == null){
            mUuid = UUID.randomUUID().toString();
            mUuid = mUuid.replace("-","");
            sharedPreferences.edit().putString(SharedPreferenceKey.UUID, mUuid).commit();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public static void startService(Context context){
        Intent intent = new Intent(context, VpnStatusMonitorService.class);
        context.startService(intent);
    }
}
