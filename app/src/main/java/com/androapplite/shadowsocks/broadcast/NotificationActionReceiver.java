package com.androapplite.shadowsocks.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.GAHelper;
import com.androapplite.shadowsocks.activity.SplashActivity;

public class NotificationActionReceiver extends BroadcastReceiver {
    public NotificationActionReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()){
            case Action.NOTIFICATION_OPEN:
                Firebase.getInstance(context).logEvent("通知", "打开app");
                final Intent intent1 = new Intent(context, SplashActivity.class);
                intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent1);
                break;
            case Action.NOTIFICATION_DELETE:
                Log.d("NotificationAction", "delete");
                break;
        }
    }
}
