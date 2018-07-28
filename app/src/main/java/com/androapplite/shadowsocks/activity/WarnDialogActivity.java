package com.androapplite.shadowsocks.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.ad.AdFullType;
import com.androapplite.shadowsocks.utils.RuntimeSettings;
import com.androapplite.vpn3.R;
import com.bestgo.adsplugin.ads.AdAppHelper;
import com.bestgo.adsplugin.ads.AdType;
import com.bestgo.adsplugin.ads.listener.AdStateListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.lang.ref.WeakReference;

public class WarnDialogActivity extends AppCompatActivity implements View.OnClickListener, Handler.Callback {
    private static final String TAG = "WarnDialogActivity";

    public static final String SHOW_DIALOG_TYPE = "dialog_type";

    private static final int MSG_DELAY_SHOW_INTERSTITIAL_AD = 100;

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

    private AdAppHelper mAdAppHelper;
    private BottomAdStateListener mAdStateListener;
    private Handler mHandler;
    private boolean isFullAdShow;
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
        mAdAppHelper = AdAppHelper.getInstance(this);
        mHandler = new Handler(this);
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

        if (FirebaseRemoteConfig.getInstance().getBoolean("is_warn_dialog_full_ad_show") && mAdAppHelper.isFullAdLoaded() && !RuntimeSettings.isVIP()) {
            mHandler.sendEmptyMessageDelayed(MSG_DELAY_SHOW_INTERSTITIAL_AD, 800);
            isFullAdShow = true;
        } else if (FirebaseRemoteConfig.getInstance().getBoolean("is_warn_dialog_native_ad_show") && mAdAppHelper.isNativeLoaded() && !RuntimeSettings.isVIP()) {
            addBottomAd();
            Firebase.getInstance(this).logEvent("大弹窗", "广告", "native加载");
            isFullAdShow = false;
        }
    }

    private void analysisDialogShow() {
        switch (type) {
            case CONNECT_PUBLIC_WIFI_DIALOG:
                Firebase.getInstance(this).logEvent("大弹窗", "显示", "链接WiFi");
                break;
            case UNDEVELOPED_COUNTRY_INACTIVE_USER_DIALOG:
                Firebase.getInstance(this).logEvent("大弹窗", "显示", "不发达国家不活跃用户");
                break;
            case DEVELOPED_COUNTRY_INACTIVE_USER_DIALOG:
                Firebase.getInstance(this).logEvent("大弹窗", "显示", "发达国家不活跃用户");
                break;
            case NET_SPEED_LOW_DIALOG:
                Firebase.getInstance(this).logEvent("大弹窗", "显示", "网速低");
                break;
            case LUCK_ROTATE_DIALOG:
                Firebase.getInstance(this).logEvent("大弹窗", "显示", "幸运转盘");
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
        Firebase firebase = Firebase.getInstance(this);
        switch (v.getId()) {
            case R.id.warn_dialog_close:
                firebase.logEvent("大弹窗", "取消");
                break;
            case R.id.warn_dialog_btn:
                if (type == NET_SPEED_LOW_DIALOG)
                    NetworkAccelerationActivity.start(this, true);
                else if (type == LUCK_ROTATE_DIALOG)
                    MainActivity.startLuckRotateActivity(this, true);
                else
                    startMainActivity();
                firebase.logEvent("大弹窗", "进入APP");
                break;
            case R.id.warn_dialog_root:
                FirebaseRemoteConfig firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
                double casualclickRate = firebaseRemoteConfig.getDouble("casual_click_rate");
                boolean result = Math.random() < casualclickRate;
                firebase.logEvent("大弹窗", "误点", String.valueOf(result));
                firebase.logEvent("大弹窗", "点击空白处");
                if (FirebaseRemoteConfig.getInstance().getBoolean("is_warn_dialog_back_use")) {
                    if (result) {
                        if (type == NET_SPEED_LOW_DIALOG)
                            NetworkAccelerationActivity.start(this, true);
                        else if (type == LUCK_ROTATE_DIALOG)
                            MainActivity.startLuckRotateActivity(this, true);
                        else
                            startMainActivity();
                        firebase.logEvent("大弹窗", "进入APP");
                    }
                }
                break;
            case R.id.warn_dialog_bg:
                if (type == NET_SPEED_LOW_DIALOG)
                    NetworkAccelerationActivity.start(this, true);
                else if (type == LUCK_ROTATE_DIALOG)
                    MainActivity.startLuckRotateActivity(this, true);
                else
                    startMainActivity();
                firebase.logEvent("大弹窗", "进入APP");
                break;
            case R.id.warn_dialog_cancel:
                firebase.logEvent("大弹窗", "取消");
                break;
        }
        finish();
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void addBottomAd() {
        if (mAdStateListener == null)
            mAdStateListener = new BottomAdStateListener(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER);
        try {
            mAdAppHelper.getNative(mAdContainer, params);
            mAdAppHelper.addAdStateListener(mAdStateListener);
            Firebase.getInstance(this).logEvent("警告弹窗底部广告", "显示", "成功");
        } catch (Exception ex) {
            Firebase.getInstance(this).logEvent("警告弹窗底部广告", "显示", "失败");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        activityIsShowing = false;
        if (mAdStateListener != null) {
            mAdAppHelper.removeAdStateListener(mAdStateListener);
            mAdStateListener = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (!FirebaseRemoteConfig.getInstance().getBoolean("is_warn_dialog_back_use"))
            super.onBackPressed();
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_DELAY_SHOW_INTERSTITIAL_AD:
                String adType = "";
                if (type == LUCK_ROTATE_DIALOG)
                    adType = AdFullType.CLOUD_LUCK_ROTATE_FULL_AD;
                else if (type == NET_SPEED_LOW_DIALOG)
                    adType = AdFullType.CLOUD_NET_SPEED_FULL_AD;
                else if (type == CONNECT_PUBLIC_WIFI_DIALOG)
                    adType = AdFullType.CLOUD_WIFI_DIALOG_FULL_AD;
                else if (type == DEVELOPED_COUNTRY_INACTIVE_USER_DIALOG)
                    adType = AdFullType.CLOUD_DEVELOPED_USER_FULL_AD;
                else if (type == UNDEVELOPED_COUNTRY_INACTIVE_USER_DIALOG)
                    adType = AdFullType.CLOUD_UNDEVELOPED_USER_FULL_AD;

                mAdAppHelper.showFullAd(adType);
                break;
        }
        return true;
    }

    private class BottomAdStateListener extends AdStateListener {
        private WeakReference<WarnDialogActivity> mReference;

        BottomAdStateListener(WarnDialogActivity activity) {
            mReference = new WeakReference<>(activity);
        }

        @Override
        public void onAdLoaded(AdType adType, int index) {
            WarnDialogActivity activity = mReference.get();
            if (activity != null) {
                switch (adType.getType()) {
                    case AdType.ADMOB_NATIVE:
                    case AdType.ADMOB_BANNER:
                    case AdType.FACEBOOK_BANNER:
                    case AdType.FACEBOOK_NATIVE:
                    case AdType.FACEBOOK_FBN_BANNER:
                    case AdType.ADMOB_NATIVE_AN:
                        if (FirebaseRemoteConfig.getInstance().getBoolean("is_warn_dialog_native_ad_show")) {
                            if (!activity.isFullAdShow && !RuntimeSettings.isVIP()) {
                                activity.addBottomAd();
                                Firebase.getInstance(activity).logEvent("大弹窗", "广告", "native加载");
                            }
                        }
                        break;
                }
                AdAppHelper.getInstance(activity).removeAdStateListener(this);
            }
        }

        @Override
        public void onAdClick(AdType adType, int index) {
            WarnDialogActivity activity = mReference.get();
            if (activity != null) {
                switch (adType.getType()) {
                    case AdType.ADMOB_NATIVE:
                    case AdType.ADMOB_BANNER:
                    case AdType.FACEBOOK_BANNER:
                    case AdType.FACEBOOK_NATIVE:
                    case AdType.FACEBOOK_FBN_BANNER:
                    case AdType.ADMOB_NATIVE_AN:
                        Firebase.getInstance(mReference.get()).logEvent("大弹窗native广告", "显示", "点击");
                        break;
                    case AdType.ADMOB_FULL:
                    case AdType.ADMOB_NATIVE_FULL:
                    case AdType.FACEBOOK_FULL:
                    case AdType.FACEBOOK_FBN:
                        Firebase.getInstance(mReference.get()).logEvent("大弹窗全屏广告", "显示", "点击");
                        break;
                }
                AdAppHelper.getInstance(activity).removeAdStateListener(this);
                finish();
            }
        }
    }
}
