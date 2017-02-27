package com.androapplite.shadowsocks.ads;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.androapplite.shadowsocks.GAHelper;
import com.androapplite.shadowsocks.R;
import com.facebook.ads.AdChoicesView;
import com.facebook.ads.MediaView;
import com.facebook.ads.NativeAd;
import com.umeng.analytics.game.UMGameAgent;

import java.util.ArrayList;
import java.util.List;

public class AdActivity extends Activity {
    public static NativeAd mNativeAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        UMGameAgent.onEvent(getApplicationContext(), "cgzs_fbn");
        GAHelper.sendEvent(getApplicationContext(), "广告", "成功展示", "FacebookFBN");
        try {
            initView();
        } catch (Exception ex) {
            finish();
        }
    }

    private void initView() {
        setContentView(R.layout.native_full_ad_layout);
        View adView = findViewById(R.id.adView);
        mNativeAd.registerViewForInteraction(adView);

        findViewById(R.id.native_ad_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Create native UI using the ad metadata.
        ImageView nativeAdIcon = (ImageView) adView.findViewById(R.id.native_ad_icon);
        TextView nativeAdTitle = (TextView) adView.findViewById(R.id.native_ad_title);
        MediaView nativeAdMedia = (MediaView) adView.findViewById(R.id.native_ad_media);
        TextView nativeAdBody = (TextView) adView.findViewById(R.id.native_ad_body);
        TextView nativeAdCallToAction = (TextView) adView.findViewById(R.id.native_ad_call_to_action);

        // Set the Text.
        nativeAdTitle.setText(mNativeAd.getAdTitle());
        nativeAdBody.setText(mNativeAd.getAdBody());
        nativeAdCallToAction.setText(mNativeAd.getAdCallToAction() + "?");

        // Download and display the ad icon.
        NativeAd.Image adIcon = mNativeAd.getAdIcon();
        NativeAd.downloadAndDisplayImage(adIcon, nativeAdIcon);

        // Download and display the cover image.
        nativeAdMedia.setNativeAd(mNativeAd);

        // Add the AdChoices icon
        LinearLayout adChoicesContainer = (LinearLayout) adView.findViewById(R.id.ad_choices_container);
        AdChoicesView adChoicesView = new AdChoicesView(getApplicationContext(), mNativeAd, true);
        adChoicesContainer.addView(adChoicesView);

        // Register the Title and CTA button to listen for clicks.
        List<View> clickableViews = new ArrayList<>();
        clickableViews.add(nativeAdTitle);
        clickableViews.add(nativeAdMedia);
    }
}
