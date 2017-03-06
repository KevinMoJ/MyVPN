package com.androapplite.shadowsocks.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
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
                finish();
                startActivity(new Intent(this, SplashActivity.class));
                break;
            case R.id.body:
                finish();
                startActivity(new Intent(this, SplashActivity.class));
                break;
        }
    }

    @Override
    protected void onDestroy() {
        mAutoDismissHandler.removeCallbacks(mAutoDismissRunable);
        super.onDestroy();
    }
}
