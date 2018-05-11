package com.androapplite.shadowsocks.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.HomeWatcher;
import com.androapplite.vpn3.R;
import com.bestgo.adsplugin.ads.AdAppHelper;
import com.bestgo.adsplugin.ads.AdType;
import com.bestgo.adsplugin.ads.listener.AdStateListener;

import java.lang.ref.WeakReference;

public class WarnDialogActivity extends AppCompatActivity {
    private static final String TAG = "WarnDialogActivity";

    public static final String SHOW_DIALOG_TYPE = "dialog_type";

    public static final int CONNECT_PUBLIC_WIFI_DIALOG = 1001;
    public static final int DEVELOPED_COUNTRY_INACTIVE_USER_DIALOG = 1002;
    public static final int UNDEVELOPED_COUNTRY_INACTIVE_USER_DIALOG = 1003;
    public static final int NET_SPEED_LOW_DIALOG = 1004;

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
    private HomeWatcher mHomeWatcher;

    private int type;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warn_dialog);
        initDate();
        initView();
        initUI();
    }


    private void initDate() {
        type = getIntent().getIntExtra(SHOW_DIALOG_TYPE, UNDEVELOPED_COUNTRY_INACTIVE_USER_DIALOG);
        mAdAppHelper = AdAppHelper.getInstance(this);
        mHomeWatcher = new HomeWatcher(this);
        mHomeWatcher.setOnHomePressedListener(mHomePressedListener);
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

        mWarnDialogClose.setOnClickListener(mOnClickListener);
        mWarnDialogBtn.setOnClickListener(mOnClickListener);
        mWarnDialogRoot.setOnClickListener(mOnClickListener);
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
        }
//        addBottomAd();
    }

    public static void start(Context context, int type) {
        Intent intent = new Intent(context, WarnDialogActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(WarnDialogActivity.SHOW_DIALOG_TYPE, type);
        context.startActivity(intent);
    }

    private HomeWatcher.OnHomePressedListener mHomePressedListener = new HomeWatcher.OnHomePressedListener() {
        @Override
        public void onHomePressed() {
            Toast.makeText(WarnDialogActivity.this, "点击了home", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onHomeLongPressed() {
            Toast.makeText(WarnDialogActivity.this, "长按了home", Toast.LENGTH_SHORT).show();
        }
    };

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.warn_dialog_close:
                    finish();
                    break;
                case R.id.warn_dialog_btn:
                    Toast.makeText(WarnDialogActivity.this, "点到了按钮", Toast.LENGTH_SHORT).show();
                    break;
                case R.id.warn_dialog_root:
                    Toast.makeText(WarnDialogActivity.this, "点到了空白处", Toast.LENGTH_SHORT).show();
                    break;

            }
        }
    };

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
        if (mAdStateListener != null) {
            mAdAppHelper.removeAdStateListener(mAdStateListener);
            mAdStateListener = null;
        }

        if (mOnClickListener != null)
            mOnClickListener = null;

        if (mHomePressedListener != null)
            mHomePressedListener = null;
    }

    @Override
    public void onBackPressed() {

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
                        activity.addBottomAd();
                        Log.i(TAG, "onAdLoaded: ");
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
                        Firebase.getInstance(mReference.get()).logEvent("主界面下方广告", "显示", "点击");
                        break;
                }
                AdAppHelper.getInstance(activity).removeAdStateListener(this);
            }
        }
    }
}
