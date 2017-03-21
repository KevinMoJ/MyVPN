package com.androapplite.shadowsocks.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import yyf.shadowsocks.IShadowsocksService;
import yyf.shadowsocks.IShadowsocksServiceCallback;


public class AutoRestartReceiver extends BroadcastReceiver {
    public AutoRestartReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        //什么都不做，在applictication里已经启动autoRestartService
//        if(intent != null) {
//            String action = intent.getAction();
//            if ("android.net.conn.CONNECTIVITY_CHANGE".equals(action) ||
//                    "android.intent.action.USER_PRESENT".equals(action)) {
//                if (isNetworkConnected(context)) {
//                }
//            }
//        }
    }

//    private boolean isNetworkConnected(Context context) {
//        try {
//            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
//            NetworkInfo[] networkInfo = connectivityManager.getAllNetworkInfo();
//            for (int i = 0; i < networkInfo.length; i++) {
//                if (networkInfo[i].getState() == NetworkInfo.State.CONNECTED){
//                    return true;
//                }
//            }
//        } catch (Exception ex) {
//        }
//        return false;
//    }
}
