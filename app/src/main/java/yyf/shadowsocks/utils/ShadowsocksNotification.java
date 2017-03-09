package yyf.shadowsocks.utils;

import android.app.KeyguardManager;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

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
    private final RemoteViews mConnectremoteViews;
    private final PendingIntent mPendingIntent;

    public ShadowsocksNotification(BaseService baseService, String profileName){
        mService = baseService;
        mKeyguardManager = (KeyguardManager)mService.getSystemService(Context.KEYGUARD_SERVICE);
        mNotificationManager = (NotificationManager)mService.getSystemService(Context.NOTIFICATION_SERVICE);
        mProfileName = profileName;
        mIsVisible = true;

        mConnectremoteViews = new RemoteViews(mService.getPackageName(), R.layout.notification_connect_view);

        Intent intent = new Intent(mService, ConnectivityActivity.class);
        mPendingIntent = PendingIntent.getActivity(mService, 0, intent, FLAG_UPDATE_CURRENT);
        mBuilder = new NotificationCompat.Builder(mService)
                .setWhen(0)
                .setSmallIcon(R.drawable.notification_icon)
                .setContent(mConnectremoteViews)
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
                mConnectremoteViews.setTextViewText(R.id.content, String.format(mService.getString(R.string.notification_no_time), rxr, txr));
                mBuilder.setContent(mConnectremoteViews);
                final Notification notification = mBuilder.build();
                mService.startForeground(1, notification);

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
        PowerManager powerManager = (PowerManager) mService.getSystemService(Context.POWER_SERVICE);
        update(powerManager.isScreenOn() ? Intent.ACTION_SCREEN_ON : Intent.ACTION_SCREEN_OFF);

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
                    registerCallback();
                    break;
            }
        }
    }

    private void registerCallback() {
        mService.registerCallback(mCallback);
        mCallbackRegistered = true;
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

        disableNotification();
    }

    public void enableNotification(){
        registerCallback();
    }

    public void disableNotification(){
        unregisterCallback();
        mService.stopForeground(true);
        mNotificationManager.cancel(1);
    }

    public void notifyStopConnection(){
        int remain = mService.getRemain();
        if(remain <= 1){
            RemoteViews remoteViews = new RemoteViews(mService.getPackageName(), R.layout.notification_network_error_view);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(mService)
                    .setSmallIcon(R.drawable.notification_icon)
                    .setContentIntent(mPendingIntent)
                    .setContent(remoteViews)
                    .setFullScreenIntent(mPendingIntent, true)
                    .setShowWhen(false)
                    .setAutoCancel(true)
                    ;
            Notification notification2 = builder.build();
            mNotificationManager.notify(2, notification2);

            mBuilder.setContent(remoteViews);
            final Notification notification = mBuilder.build();
            mService.startForeground(1, notification);

        }else {
            RemoteViews remoteViews = new RemoteViews(mService.getPackageName(), R.layout.notification_disconnect_view);
            mBuilder.setContent(remoteViews);
            final Notification notification = mBuilder.build();
            mService.startForeground(1, notification);
        }
    }


}
