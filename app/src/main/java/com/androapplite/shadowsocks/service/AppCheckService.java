package com.androapplite.shadowsocks.service;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.activity.CommonAlertActivity;
import com.androapplite.vpn3.R;

import java.util.Timer;

public class AppCheckService extends Service {
    private MonitorThread mMonitorThread;
    public AppCheckService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(mMonitorThread == null){
            mMonitorThread = new MonitorThread();
            mMonitorThread.start();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private class MonitorThread extends Thread{
        private String mCurrentApp;
        @Override
        public void run() {
            while (!interrupted()){
                try{
                    Thread.sleep(100);
                    ActivityManager am = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
                    // The first in the list of RunningTasks is always the foreground task.
                    ActivityManager.RunningTaskInfo foregroundTaskInfo = am.getRunningTasks(1).get(0);
                    ComponentName topActivity = foregroundTaskInfo.topActivity;
                    String currentApp = topActivity.getPackageName();
                    if(!currentApp.equals(mCurrentApp)){
                        String privateApp = getString(R.string.private_app);
//                        String privateApp = BuildConfig.APPLICATION_ID;
                        if(privateApp.contains(currentApp)){
                            CommonAlertActivity.showAlert(AppCheckService.this, CommonAlertActivity.APP_PRIVACY);
                        }
                        mCurrentApp = currentApp;
                    }

                }catch (Exception e){
                    ShadowsocksApplication.handleException(e);
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMonitorThread.interrupt();
    }

    public static void startAppCheckService(Context context){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            context.startService(new Intent(context, AppCheckService.class));
        }
//        context.startService(new Intent(context, AppCheckService.class));

    }
}
