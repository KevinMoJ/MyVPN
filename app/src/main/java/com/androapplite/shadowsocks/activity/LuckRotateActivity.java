package com.androapplite.shadowsocks.activity;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.luckPan.LuckPanLayout;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.androapplite.shadowsocks.utils.DialogUtils;
import com.androapplite.shadowsocks.luckPan.LuckRotateProgressBar;
import com.androapplite.vpn3.R;
import com.bestgo.adsplugin.ads.AdAppHelper;
import com.bestgo.adsplugin.ads.AdType;
import com.bestgo.adsplugin.ads.listener.AdStateListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.lang.ref.WeakReference;

public class LuckRotateActivity extends AppCompatActivity implements Handler.Callback {
    private static final String TAG = "LuckRotateActivity";
    public static final int SHOW_FULL_AD = 100;
    public static final String TYPE = "TYPE";
    public static final String START = "start";
    public static final String PREPAR = "preparing";

    private static int RESULT_TYPE_1 = 0;
    private static int RESULT_TYPE_2 = 2;
    private static int RESULT_TYPE_3 = 1;
    private static int RESULT_TYPE_5 = 6;
    private static int RESULT_TYPE_THANKS = 7;
    public static final int ERROR_MAX_COUNT = 10;

    private Button mStartRotateBt;
    private FrameLayout mAdContent;
    private LuckPanLayout mLuckPanLayout;
    private ActionBar mActionBar;
    private LuckRotateProgressBar mLuckPanBar;

    private SharedPreferences mSharedPreferences;
    private AdAppHelper mAdAppHelper;
    private InterstitialAdStateListener mStateListener;
    private Handler mHandler;
    private Dialog dialog;
    private Firebase mFirebase;

    private int rotatePos = -1;
    private int progressInt;
    private long startLuckPanTime;
    private long cloudGetLuckFreeDay;
    private boolean isLuckPanRunning;
    private boolean todayIsContinuePlay; // 能不能玩，不能玩就一直转到thanks，能玩的话就显示转到的时间,

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

    @Override
    protected void onResume() {
        super.onResume();
//        if (!VIPActivity.isVIPUser(this))
//            addBottomAd();
    }

    private void initView() {
        mLuckPanLayout = findViewById(R.id.luck_pan_layout);
        mAdContent = (FrameLayout) findViewById(R.id.luck_pan_ad);
        mLuckPanLayout.setAnimationEndListener(animationEndListener);
        mStartRotateBt = findViewById(R.id.btn_start_luck_pan);
        mLuckPanBar = (LuckRotateProgressBar) findViewById(R.id.luck_pan_bar);
        mStartRotateBt.setOnClickListener(clickListener);
    }

    private void initData() {
        mSharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
        mAdAppHelper = AdAppHelper.getInstance(this);
        mFirebase = Firebase.getInstance(this);
        mStateListener = new InterstitialAdStateListener(this);
        mAdAppHelper.addAdStateListener(mStateListener);
        mHandler = new Handler(this);
        cloudGetLuckFreeDay = FirebaseRemoteConfig.getInstance().getLong("luck_pan_get_day");
        mLuckPanBar.setMax((int) cloudGetLuckFreeDay);
        startLuckPanTime = mSharedPreferences.getLong(SharedPreferenceKey.LUCK_PAN_OPEN_START_TIME, 0);

        if (startLuckPanTime == 0) { // 新用户没数据
            startLuckPanTime = System.currentTimeMillis();
            mSharedPreferences.edit().putLong(SharedPreferenceKey.LUCK_PAN_OPEN_START_TIME, startLuckPanTime).apply();
            Log.i(TAG, "initData: 初始化数据，打开的日期是新用户" + startLuckPanTime);
        } else if (!DateUtils.isToday(startLuckPanTime)) { // 第二天打开的用户 保存的时间不是今天
            startLuckPanTime = System.currentTimeMillis();
            mSharedPreferences.edit().putLong(SharedPreferenceKey.LUCK_PAN_OPEN_START_TIME, startLuckPanTime).apply();
            Log.i(TAG, "initData: 初始化数据，打开的日期是第二天，重新保存了打开时间");
            // 不是今天的话，就把转盘得到的天数清零
            mSharedPreferences.edit().putLong(SharedPreferenceKey.LUCK_PAN_GET_FREE_DAY, 0).apply();
//            mSharedPreferences.edit().putLong(SharedPreferenceKey.LUCK_PAN_GET_DAY_TO_RECORD, 0).apply();
        }

        boolean showAd = getIntent().getBooleanExtra(TYPE, true);

        if (FirebaseRemoteConfig.getInstance().getBoolean("luck_pan_show_full_ad") && showAd && mAdAppHelper.isFullAdLoaded()) {
            mAdAppHelper.showFullAd();
            mFirebase.logEvent("幸运转盘", "全屏", "进入显示");
        }
    }

    private void initUI() {
        mActionBar.setTitle("Lucky Game");
        btnEnableClick(mAdAppHelper.isFullAdLoaded(), true);
        long getLuckFreeDays = mSharedPreferences.getLong(SharedPreferenceKey.LUCK_PAN_GET_FREE_DAY, 0);
        mLuckPanBar.setProgress((int) getLuckFreeDays);

        if (mLuckPanBar.getProgress() >= mLuckPanBar.getMax())
            mLuckPanBar.setPicture(R.drawable.progress_bar_tip_full);
        else
            mLuckPanBar.setPicture(R.drawable.progress_bar_tip_normal);
    }

    private void addBottomAd() {
        try {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            mAdAppHelper.getNative(2, mAdContent, params);
        } catch (Exception e) {
        }
    }

    private void btnEnableClick(boolean enableClick, boolean isInitBt) {
        if (isInitBt)
            mFirebase.logEvent("幸运转盘", "进入按钮准备情况", String.valueOf(enableClick));

        if (enableClick) {
//            mStartRotateBt.setClickable(true);
            mStartRotateBt.setText(START);
            mStartRotateBt.setBackground(getResources().getDrawable(R.drawable.luck_pan_bt_bg));
        } else {
//            mStartRotateBt.setClickable(false);
            mStartRotateBt.setText(PREPAR);
            mStartRotateBt.setBackground(getResources().getDrawable(R.drawable.luck_pan_start_bt_gray_bg));
        }
    }

    private void startRotate() {
        long freeDaysToShow = mSharedPreferences.getLong(SharedPreferenceKey.LUCK_PAN_GET_DAY_TO_RECORD, 0);
        long getLuckFreeDays = mSharedPreferences.getLong(SharedPreferenceKey.LUCK_PAN_GET_FREE_DAY, 0);

        rotatePos = getRotatePos();
        String rewardString = getRotateString(rotatePos); // 得到转盘的结果

        Log.i(TAG, "startRotate: 得到的结果 " + rewardString);
        if (!rewardString.equals("thanks") && !rewardString.equals("")) {
            long rewardLong = Long.parseLong(rewardString);
            //如果当天得到的天数大于的一天最大得到的天数，就让他的结果一直为thanks
            if (getLuckFreeDays + rewardLong > cloudGetLuckFreeDay) { // 5 + 2 > 7
                todayIsContinuePlay = false;
                mLuckPanLayout.rotate(RESULT_TYPE_THANKS, 100);
                mFirebase.logEvent("开始游戏", "转盘得到的天数", "一直thanks");
                Log.i(TAG, "startRotate: 得到的天数达到次数，一直thanks ");
            } else {
                todayIsContinuePlay = true;
                mSharedPreferences.edit().putLong(SharedPreferenceKey.LUCK_PAN_GET_FREE_DAY, rewardLong + getLuckFreeDays).apply();
                mSharedPreferences.edit().putLong(SharedPreferenceKey.LUCK_PAN_GET_DAY_TO_RECORD, rewardLong + freeDaysToShow).apply(); // 用来给dialog显示的
                mFirebase.logEvent("开始游戏", "转盘得到的天数", String.valueOf(rewardLong));
                progressInt = (int) mSharedPreferences.getLong(SharedPreferenceKey.LUCK_PAN_GET_FREE_DAY, 0);
                Log.i(TAG, "startRotate: 没有达到得到的天数次数，随机显示" + (rewardLong + freeDaysToShow));
                mLuckPanLayout.rotate(rotatePos, 100);
            }
        } else {
            mFirebase.logEvent("开始游戏", "转盘得到的天数", "thanks");
            mLuckPanLayout.rotate(rotatePos, 100);
        }
    }

    private int getRotatePos() {
        int rotate = (int) (Math.random() * 100);
        long cloudThanksProb = FirebaseRemoteConfig.getInstance().getLong("luck_rotate_thanks"); // 66
        long cloudOneProb = FirebaseRemoteConfig.getInstance().getLong("luck_rotate_one"); // 20
        long cloudTwoProb = FirebaseRemoteConfig.getInstance().getLong("luck_rotate_two"); // 10
        long cloudThreeProb = FirebaseRemoteConfig.getInstance().getLong("luck_rotate_three"); // 3
        long cloudFiveProb = FirebaseRemoteConfig.getInstance().getLong("luck_rotate_five"); // 1

        if (rotate >= 0 && rotate <= cloudThanksProb - 1)
            rotate = RESULT_TYPE_THANKS;
        else if (rotate >= cloudThanksProb && rotate <= cloudThanksProb + cloudOneProb - 1) // 85
            rotate = RESULT_TYPE_1;
        else if (rotate >= cloudThanksProb + cloudOneProb && rotate <= cloudThanksProb + cloudOneProb + cloudTwoProb - 1)
            rotate = RESULT_TYPE_2;
        else if (rotate >= cloudThanksProb + cloudOneProb + cloudTwoProb && rotate <= cloudThanksProb + cloudOneProb + cloudTwoProb + cloudThreeProb - 1)
            rotate = RESULT_TYPE_3;
        else if (rotate >= cloudThanksProb + cloudOneProb + cloudTwoProb + cloudThreeProb + cloudFiveProb - 1)
            rotate = RESULT_TYPE_5;

        return rotate;
    }

    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_start_luck_pan:
                    if (mStartRotateBt.getText().equals(START)) {
                        mFirebase.logEvent("开始游戏", "转盘按钮", "点击");
                        startRotate();
                    } else {
                        mFirebase.logEvent("开始游戏", "转盘按钮准备", "点击");
                        Toast.makeText(LuckRotateActivity.this, getResources().getString(R.string.please_try_it_later), Toast.LENGTH_SHORT).show();
                    }
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
            isLuckPanRunning = false;
            btnEnableClick(false, false);
            String rotateString = getRotateString(rotatePos);

            if (!todayIsContinuePlay)
                rotateString = "thanks";

            if (!rotateString.equals("thanks")) {
                mLuckPanBar.setProgress(progressInt);
                if (mLuckPanBar.getProgress() >= mLuckPanBar.getMax())
                    mLuckPanBar.setPicture(R.drawable.progress_bar_tip_full);
            }

            if (!isFinishing()) {
                dialog = DialogUtils.showGameGetTimeDialog(LuckRotateActivity.this, rotateString, null);
                if (FirebaseRemoteConfig.getInstance().getBoolean("luck_pan_show_full_ad")) {
                    mHandler.sendEmptyMessageDelayed(SHOW_FULL_AD, 5);
                }
            }
        }

        @Override
        public void startAnimation(int position) {
            isLuckPanRunning = true;
            btnEnableClick(false, false);
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

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case SHOW_FULL_AD:
                mAdAppHelper.showFullAd();
                mFirebase.logEvent("幸运转盘", "结果全屏", "显示");
                return true;
        }
        return false;
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
//                activity.addBottomAd();
                if (!activity.isLuckPanRunning) {
                    activity.btnEnableClick(true, false);
                }
                activity.mFirebase.logEvent("幸运转盘", "全屏", "加载");
            }
        }

        @Override
        public void onAdClick(AdType adType, int index) {
            LuckRotateActivity activity = mActivityReference.get();
            if (activity != null) {
                switch (adType.getType()) {
                    case AdType.ADMOB_FULL:
                    case AdType.FACEBOOK_FBN:
                    case AdType.FACEBOOK_FULL:
                    case AdType.RECOMMEND_AD:
                        activity.mFirebase.logEvent("幸运转盘", "全屏", "点击");
                        break;
                }
            }
        }
    }
}
