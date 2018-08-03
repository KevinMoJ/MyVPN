package com.androapplite.shadowsocks.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.connect.ConnectVpnHelper;
import com.androapplite.shadowsocks.model.ServerConfig;
import com.androapplite.vpn3.R;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;

import java.io.File;

public class ShareActivity extends AppCompatActivity {
    public static final int CLICK_MESSAGE_TEXT = 0;
    public static final int MAX_CLICK_TIME = 4;
    public static final int CLICK_DURING_TIME = 2000;

    private TextView mShareMessage;
    private int mCurClickTimes = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        final Drawable upArrow = getResources().getDrawable(R.drawable.ic_arrow_back_black_24dp);
        upArrow.setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_ATOP);
        actionBar.setHomeAsUpIndicator(upArrow);
        mShareMessage = (TextView) findViewById(R.id.share_message);
        mShareMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurClickTimes++;
                if (mCurClickTimes >= MAX_CLICK_TIME) {
                    mCurClickTimes = 0;
                    ServerConfig currentConfig = ConnectVpnHelper.getInstance(ShareActivity.this).getCurrentConfig();
                    if (currentConfig != null)
                        Toast.makeText(ShareActivity.this, "当前服务器: " + currentConfig.server + "   " + currentConfig.port + "    " + currentConfig.nation, Toast.LENGTH_SHORT).show();
                } else {
                    mClickTimeCountHandler.sendEmptyMessageDelayed(CLICK_MESSAGE_TEXT, CLICK_DURING_TIME);
                }
            }
        });
    }
    //https://play.google.com/store/apps/details?id=com.androapplite.vpn3&referrer=http%3A%2F%2Fa.com%3Futm_source%3Dclient%26utm_medium%3Dqrcode

    private Handler mClickTimeCountHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            mCurClickTimes = 0;
        }
    };

    public void shareByFacebook(View view) {
        ShareLinkContent content = new ShareLinkContent.Builder()
                .setContentUrl(Uri.parse("https://play.google.com/store/apps/details?id=com.androapplite.vpn3&referrer=http%3A%2F%2Fa.com%3Futm_source%3Dclient%26utm_medium%3Dfacebook"))
                .build();

        ShareDialog shareDialog = new ShareDialog(this);
        shareDialog.show(content);
    }

    public void shareByBluetooth(View view) {
        ApplicationInfo app = getApplication().getApplicationInfo();
        String filePath = app.sourceDir;

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("*/*");

        intent.setPackage("com.android.bluetooth");

        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filePath)));
        startActivity(Intent.createChooser(intent, "Share app"));
    }

        public void moreShare (View view){
            String url = "https://play.google.com/store/apps/details?id=com.androapplite.vpn3&referrer=http%3A%2F%2Fa.com%3Futm_source%3Dclient%26utm_medium%3Dcommon";
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT, url);
            shareIntent.setType("text/plain");
            try {
                startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.share)));
            } catch (ActivityNotFoundException e) {
                ShadowsocksApplication.handleException(e);
            }
        }

        @Override
        public boolean onOptionsItemSelected (MenuItem item){
            if (item.getItemId() == android.R.id.home) {
                finish();
            }
            return super.onOptionsItemSelected(item);
        }

        @Override
        public void onBackPressed () {
            super.onBackPressed();
        }
    }
