package com.androapplite.shadowsocks.activity;

import android.app.Dialog;
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
import android.widget.Toast;

import com.androapplite.shadowsocks.luckpan.LuckPanLayout;
import com.androapplite.shadowsocks.luckpan.LuckRotateProgressBar;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.utils.DialogUtils;
import com.androapplite.shadowsocks.utils.RuntimeSettings;
import com.androapplite.vpn3.R;

public class LuckRotateActivity extends AppCompatActivity {
    private static final String TAG = "LuckRotateActivity";
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
    private Dialog dialog;

    private int progressInt;
    private long startLuckPanTime;
    private long cloudGetLuckFreeDay;
    private boolean isLuckPanRunning;
    private boolean todayIsContinuePlay; // 能不能玩，不能玩就一直转到thanks，能玩的话就显示转到的时间,
    private boolean isUpdateAngle; // 是否能更新角度，保证能更新一次，防止点击的次数多次被设置，在转盘转动更新的动画中进行判断

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
        cloudGetLuckFreeDay = 7;
        mLuckPanBar.setMax((int) cloudGetLuckFreeDay);
        startLuckPanTime = RuntimeSettings.getLuckPanOpenStartTime();

        if (startLuckPanTime == 0) { // 新用户没数据
            startLuckPanTime = System.currentTimeMillis();
            RuntimeSettings.setLuckPanOpenStartTime(startLuckPanTime);
            Log.i(TAG, "initData: 初始化数据，打开的日期是新用户" + startLuckPanTime);
        } else if (!DateUtils.isToday(startLuckPanTime)) { // 第二天打开的用户 保存的时间不是今天
            startLuckPanTime = System.currentTimeMillis();
            RuntimeSettings.setLuckPanOpenStartTime(startLuckPanTime);
            Log.i(TAG, "initData: 初始化数据，打开的日期是第二天，重新保存了打开时间");
            // 不是今天的话，就把转盘得到的天数清零
            RuntimeSettings.setLuckPanFreeDay(0);
            RuntimeSettings.setClickRotateStartCount(0);//点击转盘按钮的次数，不是同一天清零
        }
    }

    private void initUI() {
        mActionBar.setTitle("Lucky Game");
        btnEnableClick(true, true);
        long getLuckFreeDays = RuntimeSettings.getLuckPanFreeDay();
        mLuckPanBar.setProgress((int) getLuckFreeDays);

        if (mLuckPanBar.getProgress() >= mLuckPanBar.getMax())
            mLuckPanBar.setPicture(R.drawable.progress_bar_tip_full);
        else
            mLuckPanBar.setPicture(R.drawable.progress_bar_tip_normal);
    }

    private void btnEnableClick(boolean enableClick, boolean isInitBt) {
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
        isUpdateAngle = true;
        mLuckPanLayout.setOffsetAngle(0); // 偏移到指定位置的数值侄零，等转盘转动过程中如果有广告加载成功就设置指定位置
        mLuckPanLayout.rotate(RESULT_TYPE_THANKS, 100);
    }

    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_start_luck_pan:
                    if (mStartRotateBt.getText().equals(START)) {
                        startRotate();
                    } else {
                        Toast.makeText(LuckRotateActivity.this, getResources().getString(R.string.please_try_it_later), Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    private int getPlayRotatePosition() { // 设置转盘转到指定的位置，转之前确定
        RuntimeSettings.setClickRotateStartCount(RuntimeSettings.getClickRotateStartCount() + 1);
        int todayGetCount = RuntimeSettings.getClickRotateStartCount();

        if (todayGetCount <= 3)
            return RESULT_TYPE_THANKS;
        else if (todayGetCount == 4)
            return RESULT_TYPE_1;
        else if (todayGetCount <= 6)
            return RESULT_TYPE_THANKS;
        else if (todayGetCount == 7)
            return RESULT_TYPE_1;
        else if (todayGetCount == 8)
            return RESULT_TYPE_THANKS;
        else if (todayGetCount == 9)
            return RESULT_TYPE_2;
        else if (todayGetCount <= 12)
            return RESULT_TYPE_THANKS;
        else if (todayGetCount == 13)
            return RESULT_TYPE_3;
        else
            return RESULT_TYPE_THANKS;
    }

    private String getRotateString(int pos) {
        String s = "";
        if (pos == 0 || pos == 3 || pos == 5)
            s = "1";
        else if (pos == 2 || pos == 4)
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
            long freeDaysToShow = RuntimeSettings.getLuckPanGetRecord();
            long getLuckFreeDays = RuntimeSettings.getLuckPanFreeDay();

            String rewardString = getRotateString(mLuckPanLayout.getRotatePos()); // 得到转盘的结果

            Log.i(TAG, "endAnimation:  得到转盘的结果  " + rewardString + "   转盘位置  " + mLuckPanLayout.getRotatePos()
                    + "   转盘偏移量    " + mLuckPanLayout.getOffsetAngle() + "  点击次数  " + RuntimeSettings.getClickRotateStartCount());

            if (!rewardString.equals("thanks") && !rewardString.equals("")) {
                long rewardLong = Long.parseLong(rewardString);
                //如果当天得到的天数大于的一天最大得到的天数，就让他的结果一直为thanks
                if (getLuckFreeDays + rewardLong > cloudGetLuckFreeDay) { // 5 + 2 > 7
                    todayIsContinuePlay = false;
                    Log.i(TAG, "startRotate: 得到的天数达到次数，一直thanks ");
                } else {
                    todayIsContinuePlay = true;
                    RuntimeSettings.setLuckPanFreeDay(rewardLong + getLuckFreeDays);
                    RuntimeSettings.setLuckPanGetRecord(rewardLong + freeDaysToShow); // 用来给dialog显示的
                    Log.i(TAG, "startRotate: 没有达到得到的天数次数，随机显示" + (rewardLong + freeDaysToShow));
                }
            }

            if (!todayIsContinuePlay)
                rewardString = "thanks";

            progressInt = (int) RuntimeSettings.getLuckPanFreeDay();

            if (!rewardString.equals("thanks")) {
                mLuckPanBar.setProgress(progressInt);
                if (mLuckPanBar.getProgress() >= mLuckPanBar.getMax())
                    mLuckPanBar.setPicture(R.drawable.progress_bar_tip_full);
            }

            if (!isFinishing()) {
                dialog = DialogUtils.showGameGetTimeDialog(LuckRotateActivity.this, rewardString, null);
            }
            btnEnableClick(true, true);
        }

        @Override
        public void startAnimation(int position) {
            isLuckPanRunning = true;
            btnEnableClick(false, false);
        }

        @Override
        public void animationUpdate() {
            if (isUpdateAngle) {
                isUpdateAngle = false;
                int offsetAngle = getOffsetAngle(getPlayRotatePosition());
                mLuckPanLayout.setOffsetAngle(offsetAngle);
                Log.i(TAG, "animationUpdate:   设置偏移量   " + offsetAngle);
            }
        }
    };

    //5 - 45
    //1 - 90
    //2 - 135
    //3 - 270
    //thanks - 360
    private int getOffsetAngle(int num) { // 转盘设置偏移量，转到指定的位置，转盘转之前设置指定位置，过程中改变最终位置
        int offsetAngle = 0;
        if (num == RESULT_TYPE_1)
            offsetAngle = 90;
        else if (num == RESULT_TYPE_2)
            offsetAngle = 135;
        else if (num == RESULT_TYPE_3)
            offsetAngle = 270;
        else if (num == RESULT_TYPE_5)
            offsetAngle = 45;
        else if (num == RESULT_TYPE_THANKS)
            offsetAngle = 360;

        return offsetAngle;
    }

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
    }
}
