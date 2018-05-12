package com.vm.shadowsocks.core;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;

import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.activity.SplashActivity;
import com.androapplite.shadowsocks.activity.WarnDialogActivity;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.androapplite.vpn3.R;
import com.bestgo.adsplugin.ads.AdAppHelper;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

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
                    try {
                        showNetSpeedLowWarnDialog(tcpTrafficMonitor.pPayloadReceivedSpeed, tcpTrafficMonitor.pPayloadSentSpeed);
                    } catch (Exception e) {}
                    mNormalNetworkStatusBuilder.setContentText(text);
                    notification = mNormalNetworkStatusBuilder.build();
                }
                mService.startForeground(1, notification);
            } catch (Exception e) {
                ShadowsocksApplication.handleException(e);
            }
        }
    }

    private void showNetSpeedLowWarnDialog(long receivedSpeed, long sendSpeed) {
        AdAppHelper adAppHelper = AdAppHelper.getInstance(ShadowsocksApplication.getGlobalContext());
        SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(mService);
        long lastShowTime = sharedPreferences.getLong(SharedPreferenceKey.WARN_DIALOG_SHOW_DATE, 0);
        //针对用户网速低于一定值的时候弹窗(暂时写上低于100的时候)，一天弹一次，有云控控制弹不弹，弹的次数也可以云控控制
        int count = (int) FirebaseRemoteConfig.getInstance().getLong("net_speed_low_dialog_show_count");
        int showCount = sharedPreferences.getInt(SharedPreferenceKey.NET_SPEED_LOW_WARN_DIALOG_SHOW_COUNT, 0);
        long cloudUpload = FirebaseRemoteConfig.getInstance().getLong("net_speed_low_upload");
        long cloudDownload = FirebaseRemoteConfig.getInstance().getLong("net_speed_low_download");

        if (adAppHelper.isFullAdLoaded() || adAppHelper.isNativeLoaded()) {
            if (FirebaseRemoteConfig.getInstance().getBoolean("is_net_speed_low_dialog_show") && (receivedSpeed <= cloudDownload || sendSpeed <= cloudUpload)) {
                if (DateUtils.isToday(lastShowTime) && showCount < count
                        && ((ShadowsocksApplication) ShadowsocksApplication.getGlobalContext().getApplicationContext()).getOpenActivityNumber() <= 0) {
                    showCount = showCount + 1;
                    sharedPreferences.edit().putInt(SharedPreferenceKey.NET_SPEED_LOW_WARN_DIALOG_SHOW_COUNT, showCount).apply();
                    sharedPreferences.edit().putLong(SharedPreferenceKey.WARN_DIALOG_SHOW_DATE, System.currentTimeMillis()).apply();
                    WarnDialogActivity.start(ShadowsocksApplication.getGlobalContext(), WarnDialogActivity.NET_SPEED_LOW_DIALOG);
                } else if (!DateUtils.isToday(lastShowTime)) {
                    sharedPreferences.edit().putInt(SharedPreferenceKey.NET_SPEED_LOW_WARN_DIALOG_SHOW_COUNT, 0).apply();
                    sharedPreferences.edit().putLong(SharedPreferenceKey.WARN_DIALOG_SHOW_DATE, System.currentTimeMillis()).apply();
                }
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
