package com.androapplite.shadowsocks.activity;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.androapplite.vpn3.R;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.service.TimeCountDownService;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class WatchVideoADDialogActivity extends AppCompatActivity {
    private static final String TIME_USE_UP = "TIME_USE_UP";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_video_ad_dialog);
        Intent intent = getIntent();
        if(intent != null && intent.getBooleanExtra(TIME_USE_UP, true)){
            TextView remainder = (TextView)findViewById(R.id.remainder);
            remainder.setText(R.string.dialog_time_use_up);
        }
//        setFinishOnTouchOutside(false);
    }

    public void watchVideo(View v){
        finish();
        //视频广告
        Toast.makeText(this, "视频广告", Toast.LENGTH_SHORT).show();
        //视频广告回调
//        Intent intent = new Intent(Action.VIDEO_AD_FINISH);
//        sendBroadcast(intent);
    }

    public static void showTimeWillBeUsedUpDialog(Context context){
        Intent intent = new Intent(context, WatchVideoADDialogActivity.class);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    public static void showTimeUsedUpDialog(Context context){
        Intent intent = new Intent(context, WatchVideoADDialogActivity.class);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(TIME_USE_UP, true);
        context.startActivity(intent);
    }

}
