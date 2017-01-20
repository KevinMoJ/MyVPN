package com.androapplite.shadowsocks.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.androapplite.shadowsocks.R;

public class WatchVideoDialogActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_video_dialog);
//        setFinishOnTouchOutside(false);
    }

    public void watchVideo(View v){
        finish();
        //视频广告
        Toast.makeText(this, "视频广告", Toast.LENGTH_SHORT).show();
    }
}
