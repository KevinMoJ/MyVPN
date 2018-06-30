package com.androapplite.shadowsocks.utils;

import android.content.Context;
import android.net.TrafficStats;

import com.androapplite.shadowsocks.Firebase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.vm.shadowsocks.core.LocalVpnService;

import java.util.Timer;
import java.util.TimerTask;

public class NetWorkSpeedUtils {
    private Context context;
    private long lastTotalRxBytes = 0;
    private long lastTimeStamp = 0;
    private Timer mTimer;

    public NetWorkSpeedUtils(Context context) {
        this.context = context;
    }

    TimerTask task = new TimerTask() {
        @Override
        public void run() {
            showNetSpeed();
        }
    };

    public void startShowNetSpeed() {
        lastTotalRxBytes = getTotalRxBytes();
        lastTimeStamp = System.currentTimeMillis();
        if (mTimer == null) {
            mTimer = new Timer(); // 10s后启动任务，每5s执行一次
            mTimer.schedule(task, 10000, 5000);
        }
    }

    private long getTotalRxBytes() {
        return TrafficStats.getUidRxBytes(context.getApplicationInfo().uid) == TrafficStats.UNSUPPORTED ? 0 : (TrafficStats.getTotalRxBytes() / 1024);//转为KB
    }

    private void showNetSpeed() {
        long nowTotalRxBytes = getTotalRxBytes();
        long nowTimeStamp = System.currentTimeMillis();
        long speed = ((nowTotalRxBytes - lastTotalRxBytes) * 5000 / (nowTimeStamp - lastTimeStamp));//毫秒转换
        long speed2 = ((nowTotalRxBytes - lastTotalRxBytes) * 5000 % (nowTimeStamp - lastTimeStamp));//毫秒转换

        lastTimeStamp = nowTimeStamp;
        lastTotalRxBytes = nowTotalRxBytes;

        if (LocalVpnService.IsRunning && FirebaseRemoteConfig.getInstance().getBoolean("is_net_speed_pull")) {
            String s = String.valueOf(speed) + "." + String.valueOf(speed2) + " kb/s";
            Firebase.getInstance(context).logEvent("测试用户网速", s);
        }
    }

    public void release() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }
}
