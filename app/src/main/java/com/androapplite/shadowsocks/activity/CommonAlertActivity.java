package com.androapplite.shadowsocks.activity;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.androapplite.shadowsocks.R;

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
        int type = getIntent().getIntExtra(ALERT_TYPE, 0);
        switch (type){
            case WIFI_DETECT:
                alertIcon.setImageResource(R.drawable.ic_perm_scan_wifi_black_24dp);
                break;
            case CHECK_IN:
                alertIcon.setImageResource(R.drawable.ic_date_range_black_24dp);
                break;
            case TIME_UP:
                alertIcon.setImageResource(R.drawable.ic_schedule_black_24dp);
                break;
            case APP_PRIVACY:
                alertIcon.setImageResource(R.drawable.ic_android_black_24dp);
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
        Intent intent = new Intent(context, CommonAlertActivity.class);
        intent.putExtra(ALERT_TYPE, type);
        context.startActivity(intent);
    }
}
