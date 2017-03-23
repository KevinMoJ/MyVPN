package com.androapplite.shadowsocks.broadcast;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.service.AutoRestartService;

import java.util.List;

import yyf.shadowsocks.IShadowsocksService;
import yyf.shadowsocks.IShadowsocksServiceCallback;
import yyf.shadowsocks.service.ShadowsocksVpnService;


public class AutoRestartReceiver extends BroadcastReceiver {
    public AutoRestartReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(!isVPNServiceRunning(context)){
            AutoRestartService.recoverVpnServiceAfterKill(context);
        }
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

    private boolean isVPNServiceRunning(Context context) {
        boolean isWork = false;
        ActivityManager myAM = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> myList = myAM.getRunningServices(Integer.MAX_VALUE);
        if (myList.size() <= 0) {
            return false;
        }
        for (int i = 0; i < myList.size(); i++) {
            String mName = myList.get(i).service.getClassName().toString();
            Log.d("自动重启", mName);
            if (mName.equals(ShadowsocksVpnService.class.getCanonicalName())) {
                isWork = true;
                break;
            }
        }
        return isWork;
    }
}
