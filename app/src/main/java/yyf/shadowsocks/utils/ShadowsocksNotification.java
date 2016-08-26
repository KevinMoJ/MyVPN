package yyf.shadowsocks.utils;

import android.app.KeyguardManager;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;

import com.androapplite.shadowsocks.R;
import com.androapplite.shadowsocks.activity.ConnectivityActivity;

import yyf.shadowsocks.IShadowsocksServiceCallback;
import yyf.shadowsocks.service.BaseService;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

/**
 * Created by jim on 16/8/26.
 */

public class ShadowsocksNotification {
    private final KeyguardManager mKeyguardManager;
    private final NotificationManager mNotificationManager;
    private final BaseService mService;
    private String mProfileName;
    private boolean mIsVisible;
    private final IShadowsocksServiceCallback.Stub mCallback;
    private final NotificationCompat.Builder mBuilder;
    private BroadcastReceiver mLockReceiver;
    private boolean mCallbackRegistered;

    public ShadowsocksNotification(BaseService baseService, String profileName){
        mService = baseService;
        mKeyguardManager = (KeyguardManager)mService.getSystemService(Context.KEYGUARD_SERVICE);
        mNotificationManager = (NotificationManager)mService.getSystemService(Context.NOTIFICATION_SERVICE);
        mProfileName = profileName;
        mIsVisible = true;
        /*
                Intent intent = new Intent(this, ConnectivityActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, FLAG_UPDATE_CURRENT);

        String txRate = TrafficMonitor.formatTrafficRate(context, mTrafficMonitor.txRate);
        String rxRate = TrafficMonitor.formatTrafficRate(context, mTrafficMonitor.rxRate);
        String txTotal = TrafficMonitor.formatTraffic(context, mTrafficMonitor.txTotal);
        String rxTotal = TrafficMonitor.formatTraffic(context, mTrafficMonitor.rxTotal);
        NotificationCompat.Builder mBuilder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.notification_icon)
                        .setContentIntent(pendingIntent)
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(String.format("↑%s, %s", txRate, txTotal))
                        .setSubText(String.format("↓%s, %s", rxRate, rxTotal))
                        .setAutoCancel(false)
                        .setOngoing(true);

        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(NOTIFICATION_ID, mBuilder.build());
         */
        Intent intent = new Intent(mService, ConnectivityActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(mService, 0, intent, FLAG_UPDATE_CURRENT);
        mBuilder = new NotificationCompat.Builder(mService)
                .setWhen(0)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(mService.getString(R.string.app_name))
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .setOngoing(true);

        mCallback = new IShadowsocksServiceCallback.Stub(){
            @Override
            public void stateChanged(int state, String msg) throws RemoteException {

            }

            @Override
            public void trafficUpdated(long txRate, long rxRate, long txTotal, long rxTotal) throws RemoteException {
                String txr = TrafficMonitor.formatTrafficRate(mService, txRate);
                String rxr = TrafficMonitor.formatTrafficRate(mService, rxRate);
                String txt = TrafficMonitor.formatTraffic(mService, txTotal);
                String rxt = TrafficMonitor.formatTraffic(mService, rxTotal);
                mBuilder.setContentText(String.format("↑%s, %s", txr, txt))
                        .setSubText(String.format("↓%s, %s", rxr, rxt));
                mService.startForeground(1, mBuilder.build());

            }
        };

        mLockReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent != null){
                    update(intent.getAction());
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mService.registerReceiver(mLockReceiver, intentFilter);

    }


    private void update(String action){
        if(mService.getState() == Constants.State.CONNECTED){
            switch (action){
                case Intent.ACTION_SCREEN_OFF:
                    setVisible(false);
                    unregisterCallback();
                    break;
                case Intent.ACTION_SCREEN_ON:
                    setVisible(mKeyguardManager.inKeyguardRestrictedInputMode());
                    mService.registerCallback(mCallback);
                    mCallbackRegistered = true;
                    break;
            }
        }
    }

    private void setVisible(boolean visible){
        if(mIsVisible != visible) {
            mIsVisible = visible;
            mBuilder.setPriority(mIsVisible ? NotificationCompat.PRIORITY_LOW : NotificationCompat.PRIORITY_MIN);
            mService.startForeground(1, mBuilder.build());
        }
    }

    private void unregisterCallback(){
        if(mCallbackRegistered){
            mService.unregisterCallback(mCallback);
            mCallbackRegistered = false;
        }
    }

    public void destroy(){
        if(mLockReceiver != null){
            mService.unregisterReceiver(mLockReceiver);
            mLockReceiver = null;
        }

        unregisterCallback();
        mService.stopForeground(true);
        mNotificationManager.cancel(1);
    }

}
