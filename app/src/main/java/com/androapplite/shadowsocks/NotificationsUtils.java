package com.androapplite.shadowsocks;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by jim on 17/3/24.
 */

public class NotificationsUtils {

    public static boolean isNotificationEnabled(Context context) {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    public static void goToSet(Context context){
        Intent intent = new Intent();
        intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
        intent.putExtra("app_package", context.getPackageName());
        intent.putExtra("app_uid", context.getApplicationInfo().uid);
        try {
            context.startActivity(intent);
        }catch (Exception e){
            intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            try {
                context.startActivity(intent);
            }catch (Exception e1){
                intent = new Intent(Settings.ACTION_SETTINGS);
                try {
                    context.startActivity(intent);
                }catch (Exception e2){
                    ShadowsocksApplication.handleException(e);
                }
            }
        }
    }

    public static int getNotificationImportance(Context context) {
        return NotificationManagerCompat.from(context).getImportance();
    }
}