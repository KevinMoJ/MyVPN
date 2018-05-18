package com.androapplite.shadowsocks.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.utils.Utils;
import com.androapplite.vpn3.R;

import java.lang.ref.WeakReference;

public class RecommendActivity extends AppCompatActivity {
    private final String VPN6_URL = "market://details?id=com.androapplite.vpn6&referrer=http%3A%2F%2Fa.com%3Futm_source%3Dapp%26utm_medium%3Dapp%26utm_campaign%3Dapp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommend);
        findViewById(R.id.activity_recommend_close).setOnClickListener(mOnClickListener);
        findViewById(R.id.activity_recommend_imgv).setOnClickListener(mOnClickListener);
    }


    private View.OnClickListener mOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            WeakReference<RecommendActivity> reference = new WeakReference<>(RecommendActivity.this);
            RecommendActivity activity = reference.get();
            if (activity != null) {
                switch (v.getId()) {
                    case R.id.activity_recommend_close:
                        finish();
                        break;
                    case R.id.activity_recommend_imgv:
                        Utils.openGooglePlay(activity, VPN6_URL);
                        Firebase.getInstance(activity).logEvent("全屏广告", "点击", "主界面广告按钮本地互推全屏");
                        break;
                }
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOnClickListener != null) {
            mOnClickListener = null;
        }
    }
}
