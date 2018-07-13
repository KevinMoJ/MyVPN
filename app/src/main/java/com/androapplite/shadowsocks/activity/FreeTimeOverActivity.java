package com.androapplite.shadowsocks.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.vpn3.R;

public class FreeTimeOverActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_free_time_over);
        final Firebase firebase = Firebase.getInstance(this);
        Button vipDialogBt = (Button) findViewById(R.id.free_time_over_join_vip);
        Button cancelDialogBt = (Button) findViewById(R.id.free_time_over_cancel);

        vipDialogBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VIPActivity.startVIPActivity(FreeTimeOverActivity.this, VIPActivity.TYPE_FREE_TIME_OVER);
                firebase.logEvent("免费使用弹窗", "点击跳转vip购买界面");
                finish();
            }
        });
        cancelDialogBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LuckRotateActivity.startLuckActivity(FreeTimeOverActivity.this);
                firebase.logEvent("免费使用弹窗", "跳转到转盘界面");
                finish();
            }
        });
    }
}