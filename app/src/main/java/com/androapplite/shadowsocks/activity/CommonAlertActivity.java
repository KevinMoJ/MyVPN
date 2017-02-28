package com.androapplite.shadowsocks.activity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.androapplite.shadowsocks.CheckInAlarm;
import com.androapplite.shadowsocks.R;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.androapplite.shadowsocks.service.TimeCountDownService;

import java.util.concurrent.TimeUnit;

public class CommonAlertActivity extends AppCompatActivity {
    public static final int WIFI_DETECT = 1;
    public static final int CHECK_IN = 2;
    public static final int TIME_UP = 3;
    public static final int APP_PRIVACY = 4;
    public static final int EXENT_1_HOUR = 5;
    private static final String ALERT_TYPE = "ALERT_TYPE";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_common_alert);
        ImageView alertIcon = (ImageView)findViewById(R.id.alert_icon);
        TextView remainder = (TextView)findViewById(R.id.remainder);
        TextView alertTitle = (TextView)findViewById(R.id.alert_title);
        int type = getIntent().getIntExtra(ALERT_TYPE, 0);
        switch (type){
            case WIFI_DETECT:
                alertIcon.setImageResource(R.drawable.ic_perm_scan_wifi_black_24dp);
                alertTitle.setText("WIFI Detection");
                remainder.setText("Your WIFI connection may not be safe. Would you like to trun on VPN to protect your privacy?");
                break;
            case CHECK_IN:
                alertIcon.setImageResource(R.drawable.ic_date_range_black_24dp);
                alertTitle.setText("Check In");
                remainder.setText("You don't check in today. Would you like to check in?");
                break;
            case TIME_UP:
                alertIcon.setImageResource(R.drawable.ic_schedule_black_24dp);
                alertTitle.setText("Time Insufficient");
                remainder.setText("You are running out of VPN connection time. Would you like to earn connection time by watching video ads?");
                break;
            case APP_PRIVACY:
                alertIcon.setImageResource(R.drawable.ic_android_black_24dp);
                alertTitle.setText("App Detection");
                remainder.setText("Would you like to trun on VPN to unblock the app or protect your privacy?");
                break;
            case EXENT_1_HOUR:
                alertIcon.setImageResource(R.drawable.ic_schedule_black_24dp);
                alertTitle.setText("VPN will be stopped");
                remainder.setText("VPN connection only lasts 1 hour once. Would you like to extent 1 hour, if remaining time permits?");
                break;
        }
    }

    public void yes(View v){
        int type = getIntent().getIntExtra(ALERT_TYPE, 0);
        switch (type){
            case WIFI_DETECT:
                break;
            case CHECK_IN:
                break;
            case TIME_UP:
                break;
            case APP_PRIVACY:
                break;
            case EXENT_1_HOUR:
                SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
                sharedPreferences.edit().putBoolean(SharedPreferenceKey.EXTENT_1H_ALERT, true).commit();
                TimeCountDownService.start(this);
                break;
        }
        ShadowsocksApplication application = (ShadowsocksApplication)getApplication();
        if(application.getRunningActivityCount() < 2) {
            try {
//                Intent intent = new Intent(this, SplashActivity.class);
//                ActivityOptionsCompat options =
//                        ActivityOptionsCompat.makeCustomAnimation(this,
//                                R.anim.slide_bottom_in, R.anim.slide_bottom_out);
//                ActivityCompat.startActivity(this, intent, options.toBundle());
                startActivity(new Intent(this, SplashActivity.class));
//                overridePendingTransition(R.anim.slide_bottom_in, android.R.anim.fade_out);
            }catch (Exception e){
                ShadowsocksApplication.handleException(e);
            }
        }
        finish();
    }

    public void no(View v){
        finish();
    }

    public static void showAlert(Context context, int type){
        boolean shouldShowAlert = false;
        SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(context);
        long lastAlert;
        long current = System.currentTimeMillis();
        switch (type){
            case WIFI_DETECT:
                lastAlert = sharedPreferences.getLong(SharedPreferenceKey.LAST_WIFI_ALERT, 0);
                if(sharedPreferences.getBoolean("wifi_detect", true) &&
                        !isVPNConnected(context) && current - lastAlert > TimeUnit.HOURS.toMillis(1)){
                    shouldShowAlert = true;
                    sharedPreferences.edit().putLong(SharedPreferenceKey.LAST_WIFI_ALERT, current).commit();
                }
//                shouldShowAlert = true;
                break;
            case CHECK_IN:
                if(sharedPreferences.getBoolean("check_in", true) &&
                        !CheckInAlarm.alreadyCheckInToday(context)){
                    shouldShowAlert = true;
                }
                break;
            case TIME_UP:
                if(sharedPreferences.getBoolean("time_insufficient", true)) {
                    shouldShowAlert = true;
                }
                break;
            case APP_PRIVACY:
                lastAlert = sharedPreferences.getLong(SharedPreferenceKey.LAST_APP_ALERT, 0);
                if(sharedPreferences.getBoolean("app_detect", true) &&
                        !isVPNConnected(context) && current - lastAlert > TimeUnit.HOURS.toMillis(2)){
                    shouldShowAlert = true;
                    sharedPreferences.edit().putLong(SharedPreferenceKey.LAST_APP_ALERT, current).commit();
                }
                break;
            case EXENT_1_HOUR:
                shouldShowAlert = true;
                break;
        }

        if(shouldShowAlert) {
            Intent intent = new Intent(context, CommonAlertActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra(ALERT_TYPE, type);
            try {
                context.startActivity(intent);
            }catch (Exception e){
                ShadowsocksApplication.handleException(e);
            }
        }

    }

    private static boolean isVPNConnected(Context context){
        ActivityManager manager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (TimeCountDownService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
