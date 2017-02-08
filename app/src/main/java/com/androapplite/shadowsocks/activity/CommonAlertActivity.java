package com.androapplite.shadowsocks.activity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.androapplite.shadowsocks.CheckInAlarm;
import com.androapplite.shadowsocks.R;
import com.androapplite.shadowsocks.service.TimeCountDownService;

public class CommonAlertActivity extends AppCompatActivity {
    public static final int WIFI_DETECT = 1;
    public static final int CHECK_IN = 2;
    public static final int TIME_UP = 3;
    public static final int APP_PRIVACY = 4;
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
        }
        finish();
        Toast.makeText(this, "yes", Toast.LENGTH_SHORT).show();
    }

    public void no(View v){
        finish();
        Toast.makeText(this, "no", Toast.LENGTH_SHORT).show();
    }

    public static void showAlert(Context context, int type){
        boolean shouldShowAlert = false;
        switch (type){
            case WIFI_DETECT:
                if(!isVPNConnected(context)){
                    shouldShowAlert = true;
                }
                break;
            case CHECK_IN:
                if(!CheckInAlarm.alreadyCheckInToday(context)){
                    shouldShowAlert = true;
                }
                break;
            case TIME_UP:
                shouldShowAlert = true;
                break;
            case APP_PRIVACY:
                if(!isVPNConnected(context)){
                    shouldShowAlert = true;
                }
                break;
        }

        if(shouldShowAlert) {
            Intent intent = new Intent(context, CommonAlertActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(ALERT_TYPE, type);
            context.startActivity(intent);
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
