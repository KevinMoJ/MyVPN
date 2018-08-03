package com.androapplite.shadowsocks.activity;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.androapplite.shadowsocks.utils.RuntimeSettings;
import com.androapplite.vpn3.R;

public class VPNConnectResultActivity extends AppCompatActivity {
    private static final String TAG = "VPNConnectResultActivit";

    public static final String VPV_RESULT_TYPE = "type";

    public static final int VPN_RESULT_CONNECT = 1;
    public static final int VPN_RESULT_DISCONNECT = 0;

    private TextView mDisconnectHour, mDisconnectMinute, mDisconnectSecond, mVIPResultText;
    private LinearLayout mResultConnectRoot, mResultDisconnectRoot;
    private ImageView mVIPResultImage;

    private ActionBar mActionBar;
    private Intent mGetIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vpnconnect_result);
        mActionBar = getSupportActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setDisplayShowHomeEnabled(true);
        mActionBar.setElevation(0);
        final Drawable upArrow = getResources().getDrawable(R.drawable.ic_arrow_back_black_24dp);
        upArrow.setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_ATOP);
        mActionBar.setHomeAsUpIndicator(upArrow);

        initData();
        initView();
        initUI();
    }

    private void initData() {
        mGetIntent = getIntent();
    }

    private void initView() {
        mDisconnectHour = (TextView) findViewById(R.id.activity_disconnect_hour);
        mDisconnectMinute = (TextView) findViewById(R.id.activity_disconnect_minute);
        mDisconnectSecond = (TextView) findViewById(R.id.activity_disconnect_second);
        mVIPResultText = (TextView) findViewById(R.id.activity_result_vip_text);

        mResultConnectRoot = (LinearLayout) findViewById(R.id.activity_result_connect_root);
        mResultDisconnectRoot = (LinearLayout) findViewById(R.id.activity_result_disconnect_root);

        mVIPResultImage = (ImageView) findViewById(R.id.activity_result_vip_icon);
    }

    private void initUI() {
        long startConnectDuration = RuntimeSettings.getVPNStartTime();
        int type = mGetIntent.getIntExtra(VPV_RESULT_TYPE, VPN_RESULT_CONNECT);

        mResultConnectRoot.setVisibility(type == VPN_RESULT_CONNECT ? View.VISIBLE : View.GONE);
        mResultDisconnectRoot.setVisibility(type == VPN_RESULT_DISCONNECT ? View.VISIBLE : View.GONE);

        updateDuration(System.currentTimeMillis() - startConnectDuration);
        if (type == VPN_RESULT_DISCONNECT)
            mActionBar.setTitle(R.string.connect_result_disconnect_title);
        else if (type == VPN_RESULT_CONNECT)
            mActionBar.setTitle("");

        if (type == VPN_RESULT_CONNECT) {
            mVIPResultImage.setImageResource(R.drawable.vip_connect_success_icon);
            mVIPResultText.setVisibility(View.GONE);
        } else if (type == VPN_RESULT_DISCONNECT) {
            mVIPResultImage.setImageResource(R.drawable.vip_disconnect_success_icon);
            mVIPResultText.setVisibility(View.VISIBLE);
        }
        mVIPResultImage.setVisibility(View.VISIBLE);
    }

    private void updateDuration(long duration) {
        duration = duration / 1000;
        int hour = (int) (duration / 3600);
        int minute = (int) ((duration - hour * 3600) / 60);
        int second = (int) ((duration - hour * 3600 - minute * 60));

        mDisconnectHour.setText(hour < 10 ? (hour == 0 ? String.valueOf(0 + "" + 0) : String.valueOf(0 + "" + hour)) : String.valueOf(hour));
        mDisconnectMinute.setText(minute < 10 ? (minute == 0 ? String.valueOf(0 + "" + 0) : String.valueOf(0 + "" + minute)) : String.valueOf(minute));
        mDisconnectSecond.setText(second < 10 ? (second == 0 ? String.valueOf(0 + "" + 0) : String.valueOf(0 + "" + second)) : String.valueOf(second));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
