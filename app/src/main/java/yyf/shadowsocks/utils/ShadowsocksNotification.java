package yyf.shadowsocks.utils;

import android.app.KeyguardManager;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.vpn3.R;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;

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
    private View mRootView;

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

        showDisconnectStatus();

        mCallback = new TrafficUpdator(this);

        mLockReceiver = new LockReceiver(this);

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
        showDisconnectStatus();
    }

    public void disableNotification(){
        unregisterCallback();
        mService.stopForeground(true);
        mNotificationManager.cancel(1);
        mRootView = null;
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
        int remain = mService.getRemain();
        Log.d("notification", "remain " + remain);
        if(remain <= 1){
            mBuilder.setContentText(mService.getString(R.string.notification_vpn_network_error1))
                    .setSubText(mService.getString(R.string.notification_vpn_network_error2))
                    .setColor(getColor(R.color.notification_small_icon_bg_disconnect))
                    .setSmallIcon(R.drawable.notification_explain_marker)
                    ;
            final Notification notification = mBuilder.build();
            RemoteViews remoteViews = notification.contentView;
            if(mRootView == null) {
                mRootView = LayoutInflater.from(mService).inflate(remoteViews.getLayoutId(), null);
            }
            remoteViews.setInt(mRootView.getId(), "setBackgroundResource", R.color.notification_bg_disconnect);
            applyTextColorToRemoteViews(remoteViews, mRootView, getColor(R.color.notification_text_about_disconnect));
            mService.startForeground(1, notification);

            final Notification notification2 = mBuilder.build();
            mNotificationManager.notify(2, notification2);
            mNotificationManager.cancel(2);

            Firebase.getInstance(mService).logEvent("VPN断开","到时间");
        }else {
            showDisconnectStatus();
            Firebase.getInstance(mService).logEvent("VPN断开","没到时间", remain);
        }
    }

    private void showDisconnectStatus() {
        mBuilder.setContentText(mService.getString(R.string.notification_vpn_stop))
                .setColor(getColor(R.color.notification_small_icon_bg_disconnect));
        final Notification notification = mBuilder.build();
        RemoteViews remoteViews = notification.contentView;
        if(mRootView == null) {
            mRootView = LayoutInflater.from(mService).inflate(remoteViews.getLayoutId(), null);
        }
        remoteViews.setInt(mRootView.getId(), "setBackgroundResource", R.color.notification_bg_disconnect);
        applyTextColorToRemoteViews(remoteViews, mRootView, Color.WHITE);
        mService.startForeground(1, notification);
    }


    private static class TrafficUpdator extends IShadowsocksServiceCallback.Stub{
        private WeakReference<ShadowsocksNotification> mNotificationReference;
        TrafficUpdator(ShadowsocksNotification notification){
            mNotificationReference = new WeakReference<ShadowsocksNotification>(notification);
        }

        @Override
        public void stateChanged(int state, String msg) throws RemoteException {

        }

        @Override
        public void trafficUpdated(long txRate, long rxRate, long txTotal, long rxTotal) throws RemoteException {
            ShadowsocksNotification sn = mNotificationReference.get();
            if(sn != null){
                if(sn.mService.getRemain() > 0) {
                    String txr = TrafficMonitor.formatTrafficRate(sn.mService, txRate);
                    String rxr = TrafficMonitor.formatTrafficRate(sn.mService, rxRate);
                    sn.mBuilder.setContentText(String.format(sn.mService.getString(R.string.notification_no_time), rxr, txr))
                            .setColor(sn.mService.getResources().getColor(R.color.notification_small_icon_bg_connect))
                            .setOngoing(true)
                            .setAutoCancel(false)
                            .setFullScreenIntent(null, false)
                            .setSubText(null)
                            .setSmallIcon(R.drawable.notification_icon)
                    ;
                    try {
                        final Notification notification = sn.mBuilder.build();
                        RemoteViews remoteViews = notification.contentView;
                        if (sn.mRootView == null) {
                            sn.mRootView = LayoutInflater.from(sn.mService).inflate(remoteViews.getLayoutId(), null);
                        }
                        remoteViews.setInt(sn.mRootView.getId(), "setBackgroundResource", R.color.notification_bg_connect);
                        applyTextColorToRemoteViews(remoteViews, sn.mRootView, Color.WHITE);
                        sn.mService.startForeground(1, notification);
                        sn.mNotificationManager.cancel(2);
                    }catch (Exception e){
                        ShadowsocksApplication.handleException(e);
                    }
                }
            }
        }
    }

    private static class LockReceiver extends BroadcastReceiver{
        private WeakReference<ShadowsocksNotification> mNotificationReference;
        LockReceiver(ShadowsocksNotification notification){
            mNotificationReference = new WeakReference<ShadowsocksNotification>(notification);
        }
        @Override
        public void onReceive(Context context, Intent intent) {
            ShadowsocksNotification sn = mNotificationReference.get();
            if(sn != null){
                if(intent != null){
                    sn.update(intent.getAction());
                }
            }
        }
    }
}
