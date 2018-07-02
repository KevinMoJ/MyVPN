package com.androapplite.shadowsocks.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.androapplite.shadowsocks.utils.Utils;
import com.androapplite.vpn3.R;
import com.bestgo.adsplugin.ads.AdAppHelper;
import com.bestgo.adsplugin.ads.AdConfig;
import com.bestgo.adsplugin.ads.AdType;
import com.bestgo.adsplugin.ads.NativeAdGroup;
import com.bestgo.adsplugin.ads.listener.AdStateListener;
import com.bestgo.adsplugin.utils.GlobalInstance;
import com.facebook.ads.AdChoicesView;
import com.facebook.ads.MediaView;
import com.facebook.ads.NativeAd;
import com.google.android.gms.ads.VideoController;
import com.google.android.gms.ads.formats.NativeAppInstallAd;
import com.google.android.gms.ads.formats.NativeAppInstallAdView;
import com.google.android.gms.ads.formats.NativeContentAd;
import com.google.android.gms.ads.formats.NativeContentAdView;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.lang.ref.WeakReference;
import java.util.List;

import static com.bestgo.adsplugin.ads.AdType.ADMOB_FULL;

public class VPNConnectResultActivity extends AppCompatActivity {
    private static final String TAG = "VPNConnectResultActivit";

    public static final String VPV_RESULT_TYPE = "type";

    public static final int VPN_RESULT_CONNECT = 1;
    public static final int VPN_RESULT_DISCONNECT = 0;

    private TextView mDisconnectHour, mDisconnectMinute, mDisconnectSecond;
    private LinearLayout mResultConnectRoot, mResultDisconnectRoot;
    private FrameLayout mResultAdRoot;

    private SharedPreferences mSharedPreference;
    private AdAppHelper mAdAppHelper;
    private InterstitialADLoaded mADLoaded;

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
        mAdAppHelper = AdAppHelper.getInstance(this);
        mADLoaded = new InterstitialADLoaded(this);
        mAdAppHelper.setAdStateListener(mADLoaded);
        mSharedPreference = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);

    }

    private void initView() {
        mDisconnectHour = (TextView) findViewById(R.id.activity_disconnect_hour);
        mDisconnectMinute = (TextView) findViewById(R.id.activity_disconnect_minute);
        mDisconnectSecond = (TextView) findViewById(R.id.activity_disconnect_second);

        mResultConnectRoot = (LinearLayout) findViewById(R.id.activity_result_connect_root);
        mResultDisconnectRoot = (LinearLayout) findViewById(R.id.activity_result_disconnect_root);
        mResultAdRoot = (FrameLayout) findViewById(R.id.activity_result_ad_root);
    }

    private void initUI() {
        long startConnectDuration = mSharedPreference.getLong(SharedPreferenceKey.CONNECTING_START_TIME, 0);
        int type = mGetIntent.getIntExtra(VPV_RESULT_TYPE, VPN_RESULT_CONNECT);

        mResultConnectRoot.setVisibility(type == VPN_RESULT_CONNECT ? View.VISIBLE : View.GONE);
        mResultDisconnectRoot.setVisibility(type == VPN_RESULT_DISCONNECT ? View.VISIBLE : View.GONE);
        //链接成功显示全屏广告，有云控
        if (type == VPN_RESULT_CONNECT && FirebaseRemoteConfig.getInstance().getBoolean("result_show_full_ad")) {
            mAdAppHelper.showFullAd();
            Firebase.getInstance(this).logEvent("连接结果页全屏", "显示", "true");
        }

        updateDuration(System.currentTimeMillis() - startConnectDuration);
        if (type == VPN_RESULT_DISCONNECT)
            mActionBar.setTitle(R.string.connect_result_disconnect_title);
        FillAdContent();
    }

    private void FillAdContent() {
        NativeAdGroup availableNativeAd = mAdAppHelper.getAvailableNativeAd();
        NativeAd facebookAd = availableNativeAd.facebookAd;
        NativeAppInstallAd admobAppInstallAd = availableNativeAd.admobAppInstallAd;
        NativeContentAd admobContentAd = availableNativeAd.admobContentAd;
        AdConfig.RecommendAdItem item = mAdAppHelper.getRecommendItem();

        View view = null;
        if (facebookAd != null) {
            view = populateFaceBookAdView(facebookAd);
            Firebase.getInstance(this).logEvent("链接结果页", "广告", "线上广告显示成功");
        } else if (admobAppInstallAd != null) {
            view = populateAppInstallAdView(admobAppInstallAd);
            Firebase.getInstance(this).logEvent("链接结果页", "广告", "线上广告显示成功");
        } else if (admobContentAd != null) {
            view = populateContentAdView(admobContentAd);
            Firebase.getInstance(this).logEvent("链接结果页", "广告", "线上广告显示成功");
        } else if (item != null)
            view = populateRecommendAdView(item);

        if (view != null) {
            mResultAdRoot.removeAllViews();
            mResultAdRoot.addView(view);
            Firebase.getInstance(this).logEvent("链接结果页", "广告", "显示成功");
        } else {
            Firebase.getInstance(this).logEvent("链接结果页", "广告", "显示失败");
        }
    }

    private View populateFaceBookAdView(NativeAd facebookAd) {
        View view = initRootView(R.layout.vpnconnect_result_fb);

        MediaView mediaView = (MediaView) view.findViewById(R.id.activity_result_ad_fb_poster);
        ImageView icon = (ImageView) view.findViewById(R.id.activity_result_ad_icon);
        TextView title = (TextView) view.findViewById(R.id.activity_result_ad_title);
        TextView message = (TextView) view.findViewById(R.id.activity_result_ad_message);
        TextView action = (TextView) view.findViewById(R.id.activity_result_ad_action);
        LinearLayout adChoice = (LinearLayout) view.findViewById(R.id.activity_result_ad_fb_choice);
        Button button = (Button) view.findViewById(R.id.activity_result_ad_bt);

        mediaView.setNativeAd(facebookAd);

        NativeAd.Image adIcon = facebookAd.getAdIcon();
        if (adIcon != null)
            GlobalInstance.getImageLoader(this).displayImage(adIcon.getUrl(), icon);
        else
            icon.setImageResource(R.drawable.vpn_result_default_icon);

        title.setText(facebookAd.getAdTitle());

        String body = facebookAd.getAdBody();
        if (body == null || "null".equals(body))
            body = facebookAd.getAdSubtitle();
        if (body != null && !body.equals("null"))
            message.setText(body);

        String callToAction = facebookAd.getAdCallToAction();
        if (callToAction == null || "null".equals(callToAction)) {
            callToAction = getString(android.R.string.ok);
        }
        action.setText(callToAction);

        AdChoicesView adChoicesView = new AdChoicesView(getApplicationContext(), facebookAd, true);
        adChoice.addView(adChoicesView);

        facebookAd.registerViewForInteraction(view);
        return view;
    }

    private View populateAppInstallAdView(NativeAppInstallAd nativeAppInstallAd) {
        NativeAppInstallAdView adView = new NativeAppInstallAdView(this);
        adView.setPadding(0, getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin), 0, 0);
        adView.addView(initRootView(R.layout.vpnconnect_result_admob_appinstall));

        VideoController vc = nativeAppInstallAd.getVideoController();
        vc.setVideoLifecycleCallbacks(new VideoController.VideoLifecycleCallbacks() {
            public void onVideoEnd() {
                // Publishers should allow native ads to complete video playback before refreshing
                // or replacing them with another ad in the same UI location.
                super.onVideoEnd();
            }
        });

        ImageView poster = (ImageView) adView.findViewById(R.id.activity_result_ad_admob_poster);
        ImageView icon = (ImageView) adView.findViewById(R.id.activity_result_ad_icon);
        com.google.android.gms.ads.formats.MediaView mediaView = (com.google.android.gms.ads.formats.MediaView) adView.findViewById(R.id.activity_result_ad_admob_media);
        TextView title = (TextView) adView.findViewById(R.id.activity_result_ad_title);
        TextView message = (TextView) adView.findViewById(R.id.activity_result_ad_message);
        TextView action = (TextView) adView.findViewById(R.id.activity_result_ad_action);
        Button button = (Button) adView.findViewById(R.id.activity_result_ad_bt);

        adView.setHeadlineView(title);
        adView.setBodyView(message);
        adView.setImageView(poster);
        adView.setMediaView(mediaView);
//        adView.setIconView(icon);
        adView.setCallToActionView(action);
        adView.setCallToActionView(button);

        title.setText(nativeAppInstallAd.getHeadline());
        message.setText(nativeAppInstallAd.getBody());
        action.setText(nativeAppInstallAd.getCallToAction());

        if (vc.hasVideoContent()) {
            adView.setMediaView(mediaView);
            poster.setVisibility(View.GONE);
        } else {
            mediaView.setVisibility(View.GONE);

            // At least one image is guaranteed.
            List<com.google.android.gms.ads.formats.NativeAd.Image> images = nativeAppInstallAd.getImages();
            if (images.size() > 0)
                poster.setImageDrawable(images.get(0).getDrawable());
            else
                poster.setImageResource(R.drawable.vpn_result_default_power);
        }
        com.google.android.gms.ads.formats.NativeAd.Image logoImage = nativeAppInstallAd.getIcon();

        if (logoImage == null)
            icon.setImageResource(R.drawable.vpn_result_default_icon);
        else
            icon.setImageDrawable(logoImage.getDrawable());

        adView.setNativeAd(nativeAppInstallAd);

        return adView;
    }

    private View populateContentAdView(NativeContentAd contentAd) {
        NativeContentAdView adView = new NativeContentAdView(this);
        adView.setPadding(0, getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin), 0, 0);
        adView.addView(initRootView(R.layout.vpnconnect_result_admob));

        ImageView poster = (ImageView) adView.findViewById(R.id.activity_result_ad_admob_poster);
        ImageView icon = (ImageView) adView.findViewById(R.id.activity_result_ad_icon);
        TextView title = (TextView) adView.findViewById(R.id.activity_result_ad_title);
        TextView message = (TextView) adView.findViewById(R.id.activity_result_ad_message);
        TextView action = (TextView) adView.findViewById(R.id.activity_result_ad_action);
        Button button = (Button) adView.findViewById(R.id.activity_result_ad_bt);

        adView.setHeadlineView(title);
        adView.setBodyView(message);
        adView.setImageView(poster);
//        adView.setLogoView(icon);
        adView.setCallToActionView(action);
        adView.setCallToActionView(button);

        title.setText(contentAd.getHeadline());
        message.setText(contentAd.getBody());
        action.setText(contentAd.getCallToAction());

        List<com.google.android.gms.ads.formats.NativeAd.Image> images = contentAd.getImages();
        if (images.size() > 0)
            poster.setImageDrawable(images.get(0).getDrawable());
        else
            poster.setImageResource(R.drawable.vpn_result_default_power);

        com.google.android.gms.ads.formats.NativeAd.Image logoImage = contentAd.getLogo();

        if (logoImage == null)
            icon.setImageResource(R.drawable.vpn_result_default_icon);
        else
            icon.setImageDrawable(logoImage.getDrawable());

        adView.setNativeAd(contentAd);

        return adView;
    }

    private View populateRecommendAdView(AdConfig.RecommendAdItem item) {
        View adView = initRootView(R.layout.vpnconnect_result_recommend);

        ImageView poster = (ImageView) adView.findViewById(R.id.activity_result_ad_recommend_poster);
        ImageView icon = (ImageView) adView.findViewById(R.id.activity_result_ad_icon);
        TextView title = (TextView) adView.findViewById(R.id.activity_result_ad_title);
        TextView message = (TextView) adView.findViewById(R.id.activity_result_ad_message);
        TextView action = (TextView) adView.findViewById(R.id.activity_result_ad_action);
        Button button = (Button) adView.findViewById(R.id.activity_result_ad_bt);

        if (item.image_url != null)
            GlobalInstance.getImageLoader(this).displayImage(item.image_url, poster);
        else
            poster.setImageResource(R.drawable.vpn_result_default_power);

        if (item.icon_url != null)
            GlobalInstance.getImageLoader(getApplicationContext()).displayImage(item.icon_url, icon);
        else
            poster.setImageResource(R.drawable.vpn_result_default_icon);

        if (item.sub_title != null)
            title.setText(item.sub_title);

        if (item.title != null)
            message.setText(item.title);

        if (item.action_title != null)
            action.setText(item.action_title);

        adView.setOnClickListener(mRecommendAdClickLis);
        button.setOnClickListener(mRecommendAdClickLis);

        adView.setTag(item);
        button.setTag(item);

        return adView;
    }

    private View initRootView(int layoutResource) {
        return LayoutInflater.from(this).inflate(layoutResource, null);
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

    private class InterstitialADLoaded extends AdStateListener {
        private WeakReference<VPNConnectResultActivity> mReference;

        InterstitialADLoaded(VPNConnectResultActivity activity) {
            mReference = new WeakReference<>(activity);
        }

        @Override
        public void onAdLoaded(AdType adType, int index) {
            Log.i(TAG, "onAdLoaded: ");
            switch (adType.getType()) {
                case AdType.FACEBOOK_NATIVE:
                case AdType.ADMOB_NATIVE_AN:
                case AdType.ADMOB_NATIVE:
                    FillAdContent();
                    break;
            }
        }

        @Override
        public void onAdClick(AdType adType, int index) {
            VPNConnectResultActivity activity = mReference.get();
            if (activity != null) {
                switch (adType.getType()) {
                    case AdType.FACEBOOK_NATIVE:
                    case AdType.ADMOB_NATIVE_AN:
                    case AdType.ADMOB_NATIVE:
                        Firebase.getInstance(activity).logEvent("链接结果页", "广告", "点击");
                        break;
                }
            }
        }

        @Override
        public void onAdLoadFailed(AdType adType, int index, String reason) {
            VPNConnectResultActivity activity = mReference.get();
            if (activity != null) {
                switch (adType.getType()) {
                    case ADMOB_FULL:
                    case AdType.FACEBOOK_FBN:
                    case AdType.FACEBOOK_FULL:
                        Firebase.getInstance(activity).logEvent("连接结果页全屏", "连接", "false");
                        break;
                }
            }
        }
    }

    private View.OnClickListener mRecommendAdClickLis = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            WeakReference<VPNConnectResultActivity> reference = new WeakReference<>(VPNConnectResultActivity.this);
            VPNConnectResultActivity activity = reference.get();
            AdConfig.RecommendAdItem item = (AdConfig.RecommendAdItem) v.getTag();
            if (activity != null && item != null) {
                Utils.openGooglePlay(activity, item.link_url);
                Firebase.getInstance(activity).logEvent("链接结果页", "广告", "互推点击");
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mADLoaded != null)
            mADLoaded = null;
        if (mRecommendAdClickLis != null)
            mRecommendAdClickLis = null;
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
