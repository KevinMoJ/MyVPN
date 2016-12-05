package com.androapplite.shadowsocks.activity;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.androapplite.shadowsocks.R;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;

import java.io.File;

public class ShareActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
    }

    public void shareByFacebook(View view){
        ShareLinkContent content = new ShareLinkContent.Builder()
                .setContentUrl(Uri.parse("https://play.google.com/store/apps/details?id=com.androapplite.shadowsocks&referrer=utm_source%3Dclient%26utm_medium%3Dfacebook\n"))
                .build();

        ShareDialog shareDialog = new ShareDialog(this);
        shareDialog.show(content);
    }

    public void shareByBluetooth(View view){
        ApplicationInfo app = getApplication().getApplicationInfo();
        String filePath = app.sourceDir;

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("*/*");

        intent.setPackage("com.android.bluetooth");

        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filePath)));
        startActivity(Intent.createChooser(intent, "Share app"));
    }
}
