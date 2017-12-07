package com.vm.shadowsocks.core;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import com.androapplite.vpn3.R;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.activity.SplashActivity;

import java.util.concurrent.TimeUnit;


/**
 * Created by huangjian on 2017/10/19.
 */

public class VpnNotification implements LocalVpnService.onStatusChangedListener {
    private NotificationCompat.Builder mNormalNetworkStatusBuilder;
    private NotificationCompat.Builder mErrorNetworkStatusBuilder;
    private static final int VPN_NOTIFICATION_ID = 1;
    private Service mService;
    public static boolean gSupressNotification = false;


    public VpnNotification(Service service) {
        mService = service;
        Intent intent = new Intent(service, SplashActivity.class);
        intent.putExtra("source", "notificaiton");
        PendingIntent pendingIntent = PendingIntent.getActivity(service, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Bitmap largeIcon = BitmapFactory.decodeResource(service.getResources(), R.drawable.notification_icon_large);
        mNormalNetworkStatusBuilder = new NotificationCompat.Builder(service);
        mNormalNetworkStatusBuilder.setSmallIcon(R.drawable.notification_icon)
                .setLargeIcon(largeIcon)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .setOngoing(true)
                .setShowWhen(false)
                .setContentTitle(mService.getString(R.string.app_name));
        mErrorNetworkStatusBuilder = new NotificationCompat.Builder(service);
        mErrorNetworkStatusBuilder.setSmallIcon(R.drawable.notification_icon)
                .setLargeIcon(largeIcon)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .setOngoing(true)
                .setShowWhen(false)
                .setContentTitle(applyColorText(mService.getString(R.string.app_name), Color.RED))
                .setColor(Color.RED);
    }

    @Override
    public void onStatusChanged(String status, Boolean isRunning) {
        if (!isRunning) {
            showVpnStoppedNotificationGlobe(mService, !gSupressNotification);
            gSupressNotification = false;
        }
    }

    private  static SpannableString applyColorText(String text, int color) {
        SpannableString spannableString = new SpannableString(text);
        spannableString.setSpan(new ForegroundColorSpan(color), 0, text.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    @Override
    public void onLogReceived(String logString) {

    }

    @Override
    public void onTrafficUpdated(TcpTrafficMonitor tcpTrafficMonitor) {
        if (tcpTrafficMonitor != null) {
            try {
                Notification notification = null;
                String[] networkErrors = mService.getResources().getStringArray(R.array.network_errors);
                if (tcpTrafficMonitor.pPayloadReceivedSpeed <= 0 && tcpTrafficMonitor.pNetworkError >= 0
                        && tcpTrafficMonitor.pNetworkError < networkErrors.length) {
                    String error = networkErrors[tcpTrafficMonitor.pNetworkError];
                    mErrorNetworkStatusBuilder.setContentText(applyColorText(error, Color.RED));
                    notification = mErrorNetworkStatusBuilder.build();
                } else {
                    String text = mService.getString(R.string.notification_no_time, tcpTrafficMonitor.pPayloadReceivedSpeed, tcpTrafficMonitor.pPayloadSentSpeed);
                    mNormalNetworkStatusBuilder.setContentText(text);
                    notification = mNormalNetworkStatusBuilder.build();
                }
                mService.startForeground(1, notification);
            } catch (Exception e) {
                ShadowsocksApplication.handleException(e);
            }
        }
    }

//    public void dismissNotification(){
//        mService.stopForeground(true);
//    }

    public void showVpnStoppedNotification() {
        try {
            mErrorNetworkStatusBuilder
                    .setContentText(applyColorText(mService.getString(R.string.notification_vpn_stop), Color.RED));
            mService.startForeground(1, mErrorNetworkStatusBuilder.build());
        } catch (Exception e) {
            ShadowsocksApplication.handleException(e);
        }
    }

    public void showVpnStartedNotification() {
        try {
            String text = mService.getString(R.string.notification_no_time, 0, 0);
            mNormalNetworkStatusBuilder.setContentText(text);
            mService.startForeground(1, mNormalNetworkStatusBuilder.build());
        } catch (Exception e) {
            ShadowsocksApplication.handleException(e);
        }
    }

    public static void showVpnStoppedNotificationGlobe(Context context, boolean showFullScreenIntent){
        try {
            final Context applicationContext = context.getApplicationContext();
            Intent intent = new Intent(applicationContext, SplashActivity.class);
            intent.putExtra("source", "notificaiton");
            PendingIntent pendingIntent = PendingIntent.getActivity(applicationContext, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            Bitmap largeIcon = BitmapFactory.decodeResource(applicationContext.getResources(), R.drawable.notification_icon_large);
            NotificationCompat.Builder errorNetworkStatusBuilder = new NotificationCompat.Builder(applicationContext);
            errorNetworkStatusBuilder.setSmallIcon(R.drawable.notification_icon)
                    .setLargeIcon(largeIcon)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setShowWhen(false)
                    .setContentTitle(applyColorText(context.getString(R.string.app_name), Color.RED))
                    .setColor(Color.RED)
                    .setContentText(applyColorText(context.getString(R.string.notification_vpn_stop), Color.RED));
            if (showFullScreenIntent) {
                PendingIntent fullScreenIntent = PendingIntent.getActivity(applicationContext, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                errorNetworkStatusBuilder.setFullScreenIntent(fullScreenIntent, true);
                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showVpnStoppedNotificationGlobe(applicationContext, false);
                    }
                }, TimeUnit.SECONDS.toMillis(3));
            }
            NotificationManagerCompat.from(applicationContext).notify(VPN_NOTIFICATION_ID, errorNetworkStatusBuilder.build());
        } catch (Exception e) {
            ShadowsocksApplication.handleException(e);
        }
    }

}
