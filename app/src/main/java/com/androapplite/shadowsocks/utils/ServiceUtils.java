package com.androapplite.shadowsocks.utils;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ServiceUtils {
    private final static int FORGROUND_BG_NOTIFICATION_ID = 12034;

    public static void startService(Context context, Intent intent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Exception ex) {
        }
    }

    public static void startForgound(Service service) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                service.startForeground(FORGROUND_BG_NOTIFICATION_ID, new Notification());
            }
        } catch (Exception ex) {
        }
    }
}