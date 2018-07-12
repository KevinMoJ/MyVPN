package com.androapplite.shadowsocks.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.androapplite.shadowsocks.luckPan.LuckPanLayout;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.androapplite.vpn3.R;
import com.bestgo.adsplugin.ads.AdAppHelper;
import com.bestgo.adsplugin.ads.AdType;
import com.bestgo.adsplugin.ads.listener.AdStateListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.lang.ref.WeakReference;

public class LuckRotateActivity extends AppCompatActivity {
    private static final String TAG = "LuckRotateActivity";
    private static int RESULT_TYPE_1 = 0;
    private static int RESULT_TYPE_2 = 2;
    private static int RESULT_TYPE_3 = 1;
    private static int RESULT_TYPE_5 = 6;
    private static int RESULT_TYPE_THANKS = 7;
    public static final int ERROR_MAX_COUNT = 5;

    private Button btnStartLuck;
    private FrameLayout mAdContent;
    private LuckPanLayout luckPanLayout;
    private int rotatePos = -1;
    private ActionBar mActionBar;
    private long startLuckPanTime;
    private SharedPreferences mSharedPreferences;
    private AdAppHelper mAdAppHelper;
    private InterstitialAdStateListener mStateListener;
    private boolean isLuckPanRunning;

    private int[] LuckNumbers = {RESULT_TYPE_1, RESULT_TYPE_2, RESULT_TYPE_3, RESULT_TYPE_5, RESULT_TYPE_THANKS};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_luck_rotate);

        mActionBar = getSupportActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setDisplayShowHomeEnabled(true);
        mActionBar.setElevation(0);
        final Drawable upArrow = getResources().getDrawable(R.drawable.ic_arrow_back_black_24dp);
        upArrow.setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_ATOP);
        mActionBar.setHomeAsUpIndicator(upArrow);

        initView();
        initData();
        initUI();
    }

    private void initView() {
        luckPanLayout = findViewById(R.id.luck_pan_layout);
        mAdContent = (FrameLayout) findViewById(R.id.luck_pan_ad);
        luckPanLayout.setAnimationEndListener(animationEndListener);
        btnStartLuck = findViewById(R.id.btn_start_luck_pan);
        btnStartLuck.setOnClickListener(clickListener);
//        btnEnableClick(true);
    }

    private void initUI() {
        mActionBar.setTitle("Luck Game");
        if (!VIPActivity.isVIPUser(this))
            addBottomAd();
    }

    private void initData() {
        mSharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
        mAdAppHelper = AdAppHelper.getInstance(this);
        mStateListener = new InterstitialAdStateListener(this);
        startLuckPanTime = mSharedPreferences.getLong(SharedPreferenceKey.LUCK_PAN_OPEN_START_TIME, 0);

        if (startLuckPanTime == 0) { // 新用户没数据
            startLuckPanTime = System.currentTimeMillis();
            mSharedPreferences.edit().putLong(SharedPreferenceKey.LUCK_PAN_OPEN_START_TIME, startLuckPanTime).apply();
            Log.i(TAG, "initData: 初始化数据，打开的日期是新用户" + startLuckPanTime);
        } else if (!DateUtils.isToday(startLuckPanTime)) { // 第二天打开的用户 保存的时间不是今天
            startLuckPanTime = System.currentTimeMillis();
            mSharedPreferences.edit().putLong(SharedPreferenceKey.LUCK_PAN_OPEN_START_TIME, startLuckPanTime).apply();
            Log.i(TAG, "initData: 初始化数据，打开的日期是第二天，重新保存了打开时间");
            // 不是今天的话，就把转盘得到的时间清零
            mSharedPreferences.edit().putLong(SharedPreferenceKey.LUCK_PAN_GET_FREE_TIME, 0).apply();
        }

        Log.i(TAG, "initData: 初始化" + startLuckPanTime);
    }

    private void addBottomAd() {
        try {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            mAdAppHelper.getNative(2, mAdContent, params);
//            mAdAppHelper.addAdStateListener(mStateListener);
        } catch (Exception e) {
        }
    }

    private void btnEnableClick(boolean enableClick) {
        if (enableClick) {
            btnStartLuck.setClickable(true);
            btnStartLuck.setText("start");
            btnStartLuck.setBackground(getResources().getDrawable(R.drawable.luck_pan_bt_bg));
        } else {
            btnStartLuck.setClickable(false);
            btnStartLuck.setText("preparing");
            btnStartLuck.setBackground(getResources().getDrawable(R.drawable.luck_pan_bt_pressed));
        }
        isLuckPanRunning = !enableClick;
    }

    void startRotate() {
        long freeTime = mSharedPreferences.getLong(SharedPreferenceKey.LUCK_PAN_GET_FREE_TIME, 0);
        long cloudFreeTimeMax = FirebaseRemoteConfig.getInstance().getLong("luck_pan_get_free_time_max");

        rotatePos = LuckNumbers[(int) (Math.random() * LuckNumbers.length)];
        String rotateString = getRotateString(rotatePos); // 得到转盘的结果

        Log.i(TAG, "startRotate: 得到的结果 " + rotateString);

        if (!rotateString.equals("thanks") && !rotateString.equals("")) {
            long rotateLong = Long.parseLong(rotateString);

            if (DateUtils.isToday(startLuckPanTime)) {
                //如果当天玩的结果时间之和大于约定的20分钟，就让他的结果一直为thanks
                if (freeTime + rotateLong >= cloudFreeTimeMax) {
                    luckPanLayout.rotate(RESULT_TYPE_THANKS, 100);
                    Log.i(TAG, "startRotate: 达到了20分钟，一直thanks " + (freeTime + rotateLong));
                    return;
                } else {
                    mSharedPreferences.edit().putLong(SharedPreferenceKey.LUCK_PAN_GET_FREE_TIME, rotateLong + freeTime).apply();
                    Log.i(TAG, "startRotate: 没有达到20分钟，随机显示" + (freeTime + rotateLong));
                    luckPanLayout.rotate(rotatePos, 100);
                }
            }
        } else {
            luckPanLayout.rotate(rotatePos, 100);
        }
    }

    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_start_luck_pan:
                    startRotate();
                    break;
            }
        }
    };

    private String getRotateString(int pos) {
        String s = "";
        if (pos == 0)
            s = "1";
        else if (pos == 2)
            s = "2";
        else if (pos == 1)
            s = "3";
        else if (pos == 6)
            s = "5";
        else if (pos == 7)
            s = "thanks";
        return s;
    }

    LuckPanLayout.AnimationEndListener animationEndListener = new LuckPanLayout.AnimationEndListener() {
        @Override
        public void endAnimation(int position) {
            btnEnableClick(true);
            //network error 连续超过5次 不能再继续玩
            /*动画结束以后再次请求数据*/
//            getLuckPanData();
        }

        @Override
        public void startAnimation(int position) {
            btnEnableClick(false);
        }
    };


    public static void startLuckActivity(Context context) {
        Intent intent = new Intent(context, LuckRotateActivity.class);
        context.startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            if (!isLuckPanRunning)
                finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (!isLuckPanRunning)
            super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAdAppHelper.removeAdStateListener(mStateListener);
        mAdAppHelper = null;
    }

    private static class InterstitialAdStateListener extends AdStateListener {
        private WeakReference<LuckRotateActivity> mActivityReference;

        InterstitialAdStateListener(LuckRotateActivity activity) {
            mActivityReference = new WeakReference<>(activity);
        }

        @Override
        public void onAdLoaded(AdType adType, int index) {
            LuckRotateActivity activity = mActivityReference.get();
            if (activity != null) {
                activity.addBottomAd();
            }
        }
    }
}
