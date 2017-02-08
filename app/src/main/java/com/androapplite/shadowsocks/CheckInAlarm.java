package com.androapplite.shadowsocks;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.format.DateUtils;
import android.util.TimeUtils;

import com.androapplite.shadowsocks.broadcast.CheckInAlarmReceiver;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Created by jim on 17/2/8.
 */

public final class CheckInAlarm {

    public static void startCheckInAlarm(Context context){
        Intent intent = new Intent(context, CheckInAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 24);
        calendar.set(Calendar.HOUR_OF_DAY, 20);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        alarmManager.setRepeating(AlarmManager.RTC, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
    }

    public static void checkIn(Context context){
        SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(context);
        long lastGrantTime = sharedPreferences.getLong(SharedPreferenceKey.LAST_GRANT_TIME, 0);

        if(DateUtils.isToday(lastGrantTime)) return;

        Calendar today0 = Calendar.getInstance();
        today0.set(Calendar.HOUR_OF_DAY, 0);
        today0.set(Calendar.MINUTE, 0);
        today0.set(Calendar.SECOND, 0);

        Calendar yesterday0 = Calendar.getInstance();
        yesterday0.setTimeInMillis(today0.getTimeInMillis() - DateUtils.DAY_IN_MILLIS);

        Calendar lastGrant = Calendar.getInstance();
        if(lastGrantTime > 0) {
            lastGrant.setTimeInMillis(lastGrantTime);
        }else{
            lastGrant = yesterday0;
        }

        int continousCheckIn = sharedPreferences.getInt(SharedPreferenceKey.CONITNUOUSE_CHECK_IN, 0);
        if(lastGrant.before(today0)){
            if(lastGrant.before(yesterday0)){
                continousCheckIn = 1;
            }else{
                continousCheckIn += 1;
            }

            int countDown = sharedPreferences.getInt(SharedPreferenceKey.TIME_COUNT_DOWN, 0);

            if(continousCheckIn == 3 || continousCheckIn == 6) {
                countDown += 7200;
            }else if(continousCheckIn >= 7){
                countDown += 7200 + 3600;
            }else{
                countDown += 3600;
            }

            if(continousCheckIn >= 7){
                continousCheckIn = 0;
            }

            sharedPreferences.edit().putLong(SharedPreferenceKey.LAST_GRANT_TIME, System.currentTimeMillis())
                    .putInt(SharedPreferenceKey.TIME_COUNT_DOWN, countDown)
                    .putInt(SharedPreferenceKey.CONITNUOUSE_CHECK_IN, continousCheckIn)
                    .commit();
        }
    }

    public static boolean alreadyCheckInToday(Context context){
        SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(context);
        long lastGrantTime = sharedPreferences.getLong(SharedPreferenceKey.LAST_GRANT_TIME, 0);
        return DateUtils.isToday(lastGrantTime);
    }
}
