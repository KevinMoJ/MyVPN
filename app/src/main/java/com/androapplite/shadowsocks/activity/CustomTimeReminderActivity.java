package com.androapplite.shadowsocks.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.androapplite.shadowsocks.service.TimeCountDownService;
import com.androapplite.vpn3.R;

public class CustomTimeReminderActivity extends AppCompatActivity implements View.OnClickListener{
    private Handler mAutoDismissHandler;
    private Runnable mAutoDismissRunable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_time_reminder);

        View titleView = findViewById(R.id.title);
        titleView.setAlpha(0.94f);
        titleView.setOnClickListener(this);
        TextView bodyTextView = (TextView) findViewById(R.id.body);
        bodyTextView.setAlpha(0.81f);
        bodyTextView.setOnClickListener(this);
        String remindText = "VPN connection will be stop after %d minute(s). Touch to expand!";
        SharedPreferences sp = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
        int minute= sp.getInt("customize_time_reminder", 0);
        bodyTextView.setText(String.format(remindText, minute));
        findViewById(R.id.close).setOnClickListener(this);

        mAutoDismissRunable = new Runnable() {
            @Override
            public void run() {
                finish();
            }
        };

        mAutoDismissHandler = new Handler();
        mAutoDismissHandler.postDelayed(mAutoDismissRunable, 10 * 1000);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.close:
                finish();
                break;
            case R.id.title:
                extentTimeSpan();
                break;
            case R.id.body:
                extentTimeSpan();
                break;
        }
    }

    private void extentTimeSpan() {
        finish();
        ShadowsocksApplication application = (ShadowsocksApplication)getApplication();
        if(application.getRunningActivityCount() < 2) {
            try {
                startActivity(new Intent(this, SplashActivity.class));
            }catch (Exception e){
                ShadowsocksApplication.handleException(e);
            }
        }
        SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
        sharedPreferences.edit().putBoolean(SharedPreferenceKey.EXTENT_1H_ALERT, true).commit();
        TimeCountDownService.start(this);
    }

    @Override
    protected void onDestroy() {
        mAutoDismissHandler.removeCallbacks(mAutoDismissRunable);
        super.onDestroy();
    }

    public static void startCustomTimeReminderActivity(Context context){
        Intent intent = new Intent(context, CustomTimeReminderActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try {
            context.startActivity(intent);
        }catch (Exception e){
            ShadowsocksApplication.handleException(e);
        }
    }
}
