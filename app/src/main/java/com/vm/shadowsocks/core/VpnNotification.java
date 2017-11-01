package com.vm.shadowsocks.core;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import com.androapplite.powervpn.R;
import com.androapplite.shadowsocks.activity.MainActivity;
import com.androapplite.shadowsocks.activity.SplashActivity;


/**
 * Created by huangjian on 2017/10/19.
 */

public class VpnNotification implements LocalVpnService.onStatusChangedListener {
    private NotificationCompat.Builder mBuilder;
    private Service mService;


    public VpnNotification(Service service) {
        Intent intent = new Intent(service, SplashActivity.class);
        intent.putExtra("source", "notificaiton");
        PendingIntent pendingIntent = PendingIntent.getActivity(service, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Bitmap largeIcon = BitmapFactory.decodeResource(service.getResources(), R.drawable.notification_icon_large);
        mBuilder = new NotificationCompat.Builder(service);
        mBuilder.setSmallIcon(R.drawable.notification_icon)
                .setLargeIcon(largeIcon)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .setOngoing(true)
                .setShowWhen(false);
        mService = service;

    }

    @Override
    public void onStatusChanged(String status, Boolean isRunning) {
        if (!isRunning) {
            showVpnStoppedNotification();
        }
    }

    private SpannableString applyColorText(String text, int color) {
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
            String text = mService.getString(R.string.notification_no_time, tcpTrafficMonitor.pPayloadReceivedSpeed, tcpTrafficMonitor.pPayloadSentSpeed);
            SpannableString spannableString = new SpannableString(text);
            spannableString.setSpan(new ForegroundColorSpan(Color.BLACK), 0, text.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
            mBuilder.setContentTitle(applyColorText(mService.getString(R.string.app_name), Color.BLACK))
                    .setContentText(applyColorText(text, Color.DKGRAY))
                    .setColor(Color.BLACK);
            mService.startForeground(1, mBuilder.build());
        }
    }

//    public void dismissNotification(){
//        mService.stopForeground(true);
//    }

    public void showVpnStoppedNotification() {
        mBuilder.setContentTitle(applyColorText(mService.getString(R.string.app_name), Color.RED))
                .setContentText(applyColorText("VPN is stopped. Touch to open", Color.RED))
                .setColor(Color.RED);
        mService.startForeground(1, mBuilder.build());
    }

    public void showVpnStartedNotification() {
        String text = "send: 0 B/s, receive: 0 B/s";
        SpannableString spannableString = new SpannableString(text);
        spannableString.setSpan(new ForegroundColorSpan(Color.BLACK), 0, text.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        mBuilder.setContentTitle(applyColorText(mService.getString(R.string.app_name), Color.BLACK))
                .setContentText(applyColorText(text, Color.DKGRAY))
                .setColor(Color.BLACK);
        mService.startForeground(1, mBuilder.build());
    }

}
