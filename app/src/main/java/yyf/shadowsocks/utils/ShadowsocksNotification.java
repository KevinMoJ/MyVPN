package yyf.shadowsocks.utils;

import android.app.KeyguardManager;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.TimeUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.vpn3.R;
import com.androapplite.shadowsocks.activity.ConnectivityActivity;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;

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
    private final PendingIntent mPendingIntent;

    public ShadowsocksNotification(BaseService baseService, String profileName){
        mService = baseService;
        mKeyguardManager = (KeyguardManager)mService.getSystemService(Context.KEYGUARD_SERVICE);
        mNotificationManager = (NotificationManager)mService.getSystemService(Context.NOTIFICATION_SERVICE);
        mProfileName = profileName;
        mIsVisible = true;

        Intent intent = new Intent(Action.NOTIFICATION_OPEN);
        mPendingIntent = PendingIntent.getBroadcast(mService, 0, intent, FLAG_UPDATE_CURRENT);
        final Bitmap largeIcon = BitmapFactory.decodeResource(mService.getResources(), R.drawable.notification_icon_large);
        mBuilder = new NotificationCompat.Builder(mService)
                .setSmallIcon(R.drawable.notification_icon)
                .setLargeIcon(largeIcon)
                .setColor(getColor(R.color.colorPrimary))
                .setContentTitle(mService.getString(R.string.app_name))
                .setContentIntent(mPendingIntent)
                .setAutoCancel(false)
                .setOngoing(true)
                .setShowWhen(false);

        mCallback = new IShadowsocksServiceCallback.Stub(){
            @Override
            public void stateChanged(int state, String msg) throws RemoteException {

            }

            @Override
            public void trafficUpdated(long txRate, long rxRate, long txTotal, long rxTotal) throws RemoteException {
                String txr = TrafficMonitor.formatTrafficRate(mService, txRate);
                String rxr = TrafficMonitor.formatTrafficRate(mService, rxRate);
                mBuilder.setContentText(String.format(mService.getString(R.string.notification_no_time), rxr, txr))
                        .setColor(getColor(R.color.notification_small_icon_bg_connect));
                final Notification notification = mBuilder.build();
                RemoteViews remoteViews = notification.contentView;
                View v = LayoutInflater.from(mService).inflate(remoteViews.getLayoutId(), null);
                remoteViews.setInt(v.getId(), "setBackgroundResource", R.color.notification_bg_connect);
                applyTextColorToRemoteViews(remoteViews, v, Color.WHITE);
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

    private @ColorInt int getColor(@ColorRes int id){
        return mService.getResources().getColor(id);
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


    private static void applyTextColorToRemoteViews(RemoteViews remoteViews, View view, int color) {
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0, count = vg.getChildCount(); i < count; i++) {
                applyTextColorToRemoteViews(remoteViews, vg.getChildAt(i), color);
            }
        } else if (view instanceof TextView) {
            remoteViews.setTextColor(view.getId(), color);
        }
    }

    public void notifyStopConnection(){
        mBuilder.setContentText(mService.getString(R.string.notification_vpn_stop))
                .setColor(getColor(R.color.notification_small_icon_bg_disconnect));
        final Notification notification = mBuilder.build();
        RemoteViews remoteViews = notification.contentView;
        View v = LayoutInflater.from(mService).inflate(remoteViews.getLayoutId(), null);
        remoteViews.setInt(v.getId(), "setBackgroundResource", R.color.notification_bg_disconnect);
        applyTextColorToRemoteViews(remoteViews, v, Color.WHITE);
        mService.startForeground(1, notification);


//        final Bitmap largeIcon = BitmapFactory.decodeResource(mService.getResources(), R.drawable.notification_icon_large);
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(mService)
//                .setSmallIcon(R.drawable.notification_icon)
//                .setLargeIcon(largeIcon)
//                .setColor(getColor(R.color.colorPrimary))
//                .setContentTitle(mService.getString(R.string.app_name))
//                .setContentIntent(mPendingIntent)
//                .setFullScreenIntent(mPendingIntent, true);
//
//        mNotificationManager.notify(2, builder.build());
    }


}
