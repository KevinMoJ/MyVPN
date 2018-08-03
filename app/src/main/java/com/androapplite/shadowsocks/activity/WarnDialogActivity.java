package com.androapplite.shadowsocks.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.androapplite.vpn3.R;

public class WarnDialogActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "WarnDialogActivity";

    public static final String SHOW_DIALOG_TYPE = "dialog_type";

    public static final int CONNECT_PUBLIC_WIFI_DIALOG = 1001;
    public static final int DEVELOPED_COUNTRY_INACTIVE_USER_DIALOG = 1002;
    public static final int UNDEVELOPED_COUNTRY_INACTIVE_USER_DIALOG = 1003;
    public static final int NET_SPEED_LOW_DIALOG = 1004;
    public static final int LUCK_ROTATE_DIALOG = 1005;

    private RelativeLayout mWarnDialogRoot;
    private ImageView mWarnDialogBg;
    private LinearLayout mWarnDialogTitle;
    private ImageView mWarnDialogClose;
    private TextView mWarnDialogMessage;
    private Button mWarnDialogBtn;
    private TextView mWarnDialogCancel;
    private FrameLayout mAdContainer;

    public static boolean activityIsShowing;

    private int type;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warn_dialog);
        initDate();
        initView();
        initUI();
        analysisDialogShow();
    }

    private void initDate() {
        activityIsShowing = true;
        type = getIntent().getIntExtra(SHOW_DIALOG_TYPE, UNDEVELOPED_COUNTRY_INACTIVE_USER_DIALOG);
    }


    private void initView() {
        mWarnDialogRoot = (RelativeLayout) findViewById(R.id.warn_dialog_root);
        mWarnDialogBg = (ImageView) findViewById(R.id.warn_dialog_bg);
        mWarnDialogTitle = (LinearLayout) findViewById(R.id.warn_dialog_title);
        mWarnDialogClose = (ImageView) findViewById(R.id.warn_dialog_close);
        mWarnDialogMessage = (TextView) findViewById(R.id.warn_dialog_message);
        mWarnDialogBtn = (Button) findViewById(R.id.warn_dialog_btn);
        mWarnDialogCancel = (TextView) findViewById(R.id.warn_dialog_cancel);
        mAdContainer = (FrameLayout) findViewById(R.id.ad_container);

        mWarnDialogClose.setOnClickListener(this);
        mWarnDialogBtn.setOnClickListener(this);
        mWarnDialogRoot.setOnClickListener(this);
        mWarnDialogBg.setOnClickListener(this);
        mWarnDialogCancel.setOnClickListener(this);
    }

    private void initUI() {
        if (type == CONNECT_PUBLIC_WIFI_DIALOG) {
            mWarnDialogMessage.setText(R.string.warn_dialog_wifi_connected_message);
            mWarnDialogBtn.setText(R.string.immediate_protection);
            mWarnDialogCancel.setText(R.string.indifferent);
            mWarnDialogBg.setImageResource(R.drawable.public_wifi_dialog_bg);
        } else if (type == UNDEVELOPED_COUNTRY_INACTIVE_USER_DIALOG) {
            mWarnDialogMessage.setText(R.string.warn_dialog_undeveloped_country_inactive_user_message);
            mWarnDialogBtn.setText(R.string.experience_now);
            mWarnDialogCancel.setText(R.string.ignore);
            mWarnDialogBg.setImageResource(R.drawable.underdeveloped_inactive_user_dialog_bg);
        } else if (type == DEVELOPED_COUNTRY_INACTIVE_USER_DIALOG) {
            mWarnDialogMessage.setText(R.string.warn_dialog_developed_country_inactive_user_message);
            mWarnDialogBtn.setText(R.string.immediate_protection);
            mWarnDialogCancel.setText(R.string.indifferent);
            mWarnDialogBg.setImageResource(R.drawable.secrete_dialog_bg);
        } else if (type == NET_SPEED_LOW_DIALOG) {
            mWarnDialogMessage.setText(R.string.warn_dialog_net_speed_low_message);
            mWarnDialogBtn.setText(R.string.vpn_acceleration);
            mWarnDialogCancel.setText(R.string.indifferent);
            mWarnDialogBg.setImageResource(R.drawable.inactive_user_bg);
        } else if (type == LUCK_ROTATE_DIALOG) {
            mWarnDialogMessage.setText(R.string.luck_rotate_dialog_message);
            mWarnDialogBtn.setText(R.string.luck_rotate_dialog_bt_text);
            mWarnDialogCancel.setText(R.string.luck_rotate_dialog_cancel_text);
            mWarnDialogBg.setImageResource(R.drawable.luck_rotate_dialog_bg);
        }
    }

    private void analysisDialogShow() {
        switch (type) {
            case CONNECT_PUBLIC_WIFI_DIALOG:
                break;
            case UNDEVELOPED_COUNTRY_INACTIVE_USER_DIALOG:
                break;
            case DEVELOPED_COUNTRY_INACTIVE_USER_DIALOG:
                break;
            case NET_SPEED_LOW_DIALOG:
                break;
            case LUCK_ROTATE_DIALOG:
                break;
        }
    }

    public static void start(Context context, int type) {
        Intent intent = new Intent(context, WarnDialogActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(WarnDialogActivity.SHOW_DIALOG_TYPE, type);
        context.startActivity(intent);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.warn_dialog_close:
                break;
            case R.id.warn_dialog_btn:
                if (type == NET_SPEED_LOW_DIALOG)
                    NetworkAccelerationActivity.start(this, true);
                else if (type == LUCK_ROTATE_DIALOG)
                    MainActivity.startLuckRotateActivity(this, true);
                else
                    startMainActivity();
                break;
            case R.id.warn_dialog_root:
                break;
            case R.id.warn_dialog_bg:
                if (type == NET_SPEED_LOW_DIALOG)
                    NetworkAccelerationActivity.start(this, true);
                else if (type == LUCK_ROTATE_DIALOG)
                    MainActivity.startLuckRotateActivity(this, true);
                else
                    startMainActivity();
                break;
            case R.id.warn_dialog_cancel:
                break;
        }
        finish();
    }

    private String getType(int type) {
        String stringType = "";
        if (type == CONNECT_PUBLIC_WIFI_DIALOG)
            stringType = "WiFi弹窗";
        else if (type == LUCK_ROTATE_DIALOG)
            stringType = "转盘弹窗";
        else if (type == DEVELOPED_COUNTRY_INACTIVE_USER_DIALOG)
            stringType = "发达国家弹窗";
        else if (type == UNDEVELOPED_COUNTRY_INACTIVE_USER_DIALOG)
            stringType = "不发达国家弹窗";
        else if (type == NET_SPEED_LOW_DIALOG)
            stringType = "网速慢弹窗";
        return stringType;
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        activityIsShowing = false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
