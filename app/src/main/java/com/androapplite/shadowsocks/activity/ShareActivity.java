package com.androapplite.shadowsocks.activity;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.androapplite.shadowsocks.R;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;

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
}
