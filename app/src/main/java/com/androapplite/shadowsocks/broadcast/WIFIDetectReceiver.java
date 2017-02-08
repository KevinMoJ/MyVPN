package com.androapplite.shadowsocks.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class WIFIDetectReceiver extends BroadcastReceiver {
    public WIFIDetectReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
//        if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
//            NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
//            if (info.getState().equals(NetworkInfo.State.CONNECTED)) {
//
//                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
//                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
//
//                // 当前WIFI名称
//                Log.d("WIFIDetectReceiver", "连接到WIFI " + wifiInfo.getSSID());
//            }
//        }
        if (intent.getAction().equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION))
        {
            SupplicantState state = (SupplicantState) intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
            if(state == SupplicantState.COMPLETED){
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();

                // 当前WIFI名称
                Log.d("WIFIDetectReceiver", "连接到WIFI " + wifiInfo.getSSID());
            }
        }
    }
}
