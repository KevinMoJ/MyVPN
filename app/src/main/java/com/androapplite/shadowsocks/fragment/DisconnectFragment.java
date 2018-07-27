package com.androapplite.shadowsocks.fragment;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.utils.RuntimeSettings;
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

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class DisconnectFragment extends DialogFragment implements View.OnClickListener {
    private OnDisconnectActionListener mListener;
    private FrameLayout mAdLayout;
    private LinearLayout mDisconnectRoot;
    private AdAppHelper mAdAppHelper;

    public DisconnectFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_disconnect, container, false);
        mDisconnectRoot = (LinearLayout) v.findViewById(R.id.disconnect_bt_root);

        v.findViewById(R.id.cancel_disconnect).setOnClickListener(this);
        v.findViewById(R.id.disconnect).setOnClickListener(this);
        mAdLayout = (FrameLayout) v.findViewById(R.id.adContainer);
        mAdAppHelper = AdAppHelper.getInstance(getContext());
        mAdAppHelper.setAdStateListener(new InterstitialADLoaded());
        if (!RuntimeSettings.isVIP())
            fillAdContent();
        return v;
    }

    private void fillAdContent() {
        NativeAdGroup availableNativeAd = mAdAppHelper.getAvailableNativeAd();
        NativeAd facebookAd = availableNativeAd.facebookAd;
        NativeAppInstallAd admobAppInstallAd = availableNativeAd.admobAppInstallAd;
        NativeContentAd admobContentAd = availableNativeAd.admobContentAd;
        AdConfig.RecommendAdItem item = mAdAppHelper.getRecommendItem();

        View view = null;
        if (facebookAd != null)
            view = populateFaceBookAdView(facebookAd);

        else if (admobAppInstallAd != null)
            view = populateAppInstallAdView(admobAppInstallAd);

        else if (admobContentAd != null)
            view = populateContentAdView(admobContentAd);

        else if (item != null)
            view = populateRecommendAdView(item);

        mAdLayout.removeAllViews();

        if (view != null) {
            mAdLayout.addView(view);
            Firebase.getInstance(getContext()).logEvent("断开弹窗", "广告", "加载成功");
        } else {
            try {
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                mAdAppHelper.getNative(mAdLayout, params);
                Firebase.getInstance(getContext()).logEvent("断开弹窗", "广告", "加载成功");
            } catch (Exception e) {
                Firebase.getInstance(getContext()).logEvent("断开弹窗", "广告", "加载失败");
            }
        }
    }

    private View populateFaceBookAdView(NativeAd facebookAd) {
        View adView = initRootView(R.layout.disconnect_ad_fb);

        MediaView mediaView = (MediaView) adView.findViewById(R.id.disconnect_ad_fb_poster);
        ImageView icon = (ImageView) adView.findViewById(R.id.disconnect_ad_fb_icon);
        TextView title = (TextView) adView.findViewById(R.id.disconnect_ad_fb_title);
        TextView message = (TextView) adView.findViewById(R.id.disconnect_ad_fb_message);
        LinearLayout adChoice = (LinearLayout) adView.findViewById(R.id.disconnect_ad_fb_choice);
        Button disconnectBt = (Button) adView.findViewById(R.id.disconnect_ad_fb_disconnect);
        Button cancelBt = (Button) adView.findViewById(R.id.disconnect_ad_fb_cancel);

        mediaView.setNativeAd(facebookAd);

        NativeAd.Image adIcon = facebookAd.getAdIcon();
        if (adIcon != null)
            GlobalInstance.getImageLoader(getContext()).displayImage(adIcon.getUrl(), icon);
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
        cancelBt.setText(callToAction);

        AdChoicesView adChoicesView = new AdChoicesView(getContext(), facebookAd, true);
        adChoice.addView(adChoicesView);

        List<View> viewList = new ArrayList<>();
        viewList.add(mediaView);
        viewList.add(icon);
        viewList.add(title);
        viewList.add(message);
        viewList.add(cancelBt);

        facebookAd.registerViewForInteraction(adView, viewList);
        disconnectBt.setOnClickListener(mFacebookAdClickLis);
        mDisconnectRoot.setVisibility(View.GONE);
        return adView;
    }

    private View populateAppInstallAdView(NativeAppInstallAd nativeAppInstallAd) {
        NativeAppInstallAdView adView = new NativeAppInstallAdView(getContext());
        adView.setPadding(0, getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin), 0, 0);
        adView.addView(initRootView(R.layout.disconnect_ad_admob_appinstall));

        VideoController vc = nativeAppInstallAd.getVideoController();
        vc.setVideoLifecycleCallbacks(new VideoController.VideoLifecycleCallbacks() {
            public void onVideoEnd() {
                // Publishers should allow native ads to complete video playback before refreshing
                // or replacing them with another ad in the same UI location.
                super.onVideoEnd();
            }
        });

        ImageView poster = (ImageView) adView.findViewById(R.id.disconnect_ad_admob_appinstall_poster);
        ImageView icon = (ImageView) adView.findViewById(R.id.disconnect_ad_admob_appinstall_icon);
        com.google.android.gms.ads.formats.MediaView mediaView = (com.google.android.gms.ads.formats.MediaView) adView.findViewById(R.id.disconnect_ad_admob_appinstall_media);
        TextView title = (TextView) adView.findViewById(R.id.disconnect_ad_admob_appinstall_title);
        TextView message = (TextView) adView.findViewById(R.id.disconnect_ad_admob_appinstall_message);
        Button disconnectBt = (Button) adView.findViewById(R.id.disconnect_ad_admob_appinstall_disconnect);
        Button cancelBt = (Button) adView.findViewById(R.id.disconnect_ad_admob_appinstall_cancel);

        adView.setHeadlineView(title);
        adView.setBodyView(message);
        adView.setImageView(poster);
        adView.setMediaView(mediaView);
        adView.setIconView(icon);
        adView.setCallToActionView(cancelBt);

        title.setText(nativeAppInstallAd.getHeadline());
        message.setText(nativeAppInstallAd.getBody());
        cancelBt.setText(nativeAppInstallAd.getCallToAction());

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
        disconnectBt.setOnClickListener(mAdmobAdClickLis);
        mDisconnectRoot.setVisibility(View.GONE);
        return adView;
    }

    private View populateContentAdView(NativeContentAd contentAd) {
        NativeContentAdView adView = new NativeContentAdView(getContext());
        adView.setPadding(0, getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin), 0, 0);
        adView.addView(initRootView(R.layout.disconnect_ad_admob));

        ImageView poster = (ImageView) adView.findViewById(R.id.disconnect_ad_admob_poser);
        ImageView icon = (ImageView) adView.findViewById(R.id.disconnect_ad_admob_icon);
        TextView title = (TextView) adView.findViewById(R.id.disconnect_ad_admob_title);
        TextView message = (TextView) adView.findViewById(R.id.disconnect_ad_admob_message);
        Button disconnectBt = (Button) adView.findViewById(R.id.disconnect_ad_admob_disconnect);
        Button cancelBt = (Button) adView.findViewById(R.id.disconnect_ad_admob_cancel);

        adView.setHeadlineView(title);
        adView.setBodyView(message);
        adView.setImageView(poster);
        adView.setLogoView(icon);
        adView.setCallToActionView(cancelBt);

        title.setText(contentAd.getHeadline());
        message.setText(contentAd.getBody());
        cancelBt.setText(contentAd.getCallToAction());

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
        disconnectBt.setOnClickListener(mAdmobAdClickLis);
        mDisconnectRoot.setVisibility(View.GONE);
        return adView;
    }

    private View populateRecommendAdView(AdConfig.RecommendAdItem item) {
        View adView = initRootView(R.layout.disconnect_ad_recommend);

        ImageView poster = (ImageView) adView.findViewById(R.id.disconnect_ad_recommend_poser);
        ImageView icon = (ImageView) adView.findViewById(R.id.disconnect_ad_recommend_icon);
        TextView title = (TextView) adView.findViewById(R.id.disconnect_ad_recommend_title);
        TextView message = (TextView) adView.findViewById(R.id.disconnect_ad_recommend_message);
        Button disconnectBt = (Button) adView.findViewById(R.id.disconnect_ad_recommend_disconnect);
        Button cancelBt = (Button) adView.findViewById(R.id.disconnect_ad_recommend_cancel);

        if (item.image_url != null)
            GlobalInstance.getImageLoader(getContext()).displayImage(item.image_url, poster);
        else
            poster.setImageResource(R.drawable.vpn_result_default_power);

        if (item.icon_url != null)
            GlobalInstance.getImageLoader(getContext()).displayImage(item.icon_url, icon);
        else
            poster.setImageResource(R.drawable.vpn_result_default_icon);

        if (item.sub_title != null)
            title.setText(item.sub_title);

        if (item.title != null)
            message.setText(item.title);

        if (item.action_title != null)
            cancelBt.setText(item.action_title);

        poster.setOnClickListener(mRecommendAdClickLis);
        icon.setOnClickListener(mRecommendAdClickLis);
        title.setOnClickListener(mRecommendAdClickLis);
        message.setOnClickListener(mRecommendAdClickLis);
        disconnectBt.setOnClickListener(mRecommendAdClickLis);
        cancelBt.setOnClickListener(mRecommendAdClickLis);

        poster.setTag(item);
        icon.setTag(item);
        title.setTag(item);
        message.setTag(item);
        cancelBt.setTag(item);

        mDisconnectRoot.setVisibility(View.GONE);
        return adView;
    }

    private View initRootView(int layoutResource) {
        return LayoutInflater.from(getContext()).inflate(layoutResource, null);
    }

    private View.OnClickListener mRecommendAdClickLis = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.disconnect_ad_recommend_disconnect && mListener != null) {
                mListener.onDisconnect(DisconnectFragment.this);
                dismissAllowingStateLoss();
            } else {
                AdConfig.RecommendAdItem item = (AdConfig.RecommendAdItem) v.getTag();
                if (item != null) {
                    Utils.openGooglePlay(getContext(), item.link_url);
                    Firebase.getInstance(getContext()).logEvent("连接VPN", "断开", "广告点击");
                }
            }
        }
    };

    private View.OnClickListener mFacebookAdClickLis = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mListener != null) {
                mListener.onDisconnect(DisconnectFragment.this);
                dismissAllowingStateLoss();
            }
        }
    };

    private View.OnClickListener mAdmobAdClickLis = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mListener != null) {
                mListener.onDisconnect(DisconnectFragment.this);
                dismissAllowingStateLoss();
            }
        }
    };

    public interface OnDisconnectActionListener {
        void onDisconnect(DisconnectFragment disconnectFragment);

        void onDismiss(DisconnectFragment disconnectFragment);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnDisconnectActionListener) {
            mListener = (OnDisconnectActionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnDisconnectActionListener");
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnDisconnectActionListener) {
            mListener = (OnDisconnectActionListener) activity;
        } else {
            throw new RuntimeException(activity.toString()
                    + " must implement OnDisconnectActionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View v) {
        if (mListener != null) {
            dismissAllowingStateLoss();
            switch (v.getId()) {
                case R.id.cancel_disconnect:
                    Firebase.getInstance(getContext()).logEvent("连接VPN", "断开", "取消断开");
                    break;
                case R.id.disconnect:
                    mListener.onDisconnect(this);
                    break;
            }
        }
    }

    private class InterstitialADLoaded extends AdStateListener {

        @Override
        public void onAdClick(AdType adType, int index) {
            switch (adType.getType()) {
                case AdType.FACEBOOK_NATIVE:
                case AdType.ADMOB_NATIVE_AN:
                case AdType.ADMOB_NATIVE:
                    Firebase.getInstance(getContext()).logEvent("连接VPN", "断开", "广告点击");
                    break;
            }
        }

        @Override
        public void onAdLoaded(AdType adType, int index) {
            switch (adType.getType()) {
                case AdType.FACEBOOK_NATIVE:
                case AdType.ADMOB_NATIVE_AN:
                case AdType.ADMOB_NATIVE:
                    if (getContext() != null) {
                        if (isAdded()) {
                            fillAdContent();
                        }
                    }
                    break;
            }
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(STYLE_NO_TITLE);
        return dialog;
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        try {
            manager.beginTransaction().add(this, tag).commitAllowingStateLoss();
        } catch (Exception e) {
            ShadowsocksApplication.handleException(e);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (mListener != null) {
            mListener.onDismiss(this);
        }
        super.onDismiss(dialog);
    }
}
