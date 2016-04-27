package com.androapplite.shadowsocks.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.androapplite.shadowsocks.R;
import com.androapplite.shadowsocks.broadcast.Action;

import java.util.concurrent.TimeUnit;

public class SplashActivity extends BaseShadowsocksActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initBackgroundReceiver();
        initIntentFilter();

        getWindow().getDecorView().postDelayed(new Runnable() {
            @Override
            public void run() {
//                startActivity(new Intent(SplashActivity.this, ConnectionActivity.class));
                startActivity(new Intent(SplashActivity.this, NewUserGuideActivity.class));

            }
        }, TimeUnit.SECONDS.toMillis(2));
    }

    private void initBackgroundReceiver(){
        mBackgroundReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(Action.CONNECTION_ACTIVITY_SHOW)){
                    finish();
                }
            }
        };
    }

    private void initIntentFilter(){
        mBackgroundReceiverIntentFilter = new IntentFilter();
        mBackgroundReceiverIntentFilter.addAction(Action.CONNECTION_ACTIVITY_SHOW);
    }
}
