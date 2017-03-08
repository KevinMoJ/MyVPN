package com.androapplite.shadowsocks.ads;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.androapplite.shadowsocks.GAHelper;
import com.androapplite.shadowsocks.R;
import com.facebook.ads.Ad;
import com.facebook.ads.AdChoicesView;
import com.facebook.ads.AdError;
import com.facebook.ads.AdListener;
import com.facebook.ads.AdSize;
import com.facebook.ads.AdView;
import com.facebook.ads.InterstitialAd;
import com.facebook.ads.InterstitialAdListener;
import com.facebook.ads.MediaView;
import com.facebook.ads.NativeAd;
import com.umeng.analytics.game.UMGameAgent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FacebookAd {
    private Context mContext;
    private LinearLayout mNativeAdView;
    private NativeAd mNativeAd;
    private NativeAd mFBNAd;
    private AdView mBannerView;
    private LinearLayout mFBNBannerAdView;
    private NativeAd mFBNBannerAd;
//    private InterstitialAd mInterstitialAd;
    private String mBannerId;
    private String mNativeId;
    private String mInterstitialId;
    private String mFBNId;
    private String mFBNBannerId;

    private boolean enableBanner;
    private boolean enableNative;
//    private boolean enableInterstitial;
    private boolean enableFBN;
    private boolean enableFBNBanner;

    private boolean bannerLoaded;
    private boolean nativeLoaded;
//    private boolean interstitialLoaded;
    private boolean fbnLoaded;
    private boolean fbnBannerLoaded;

    private boolean bannerRequest;
    private boolean nativeRequest;
//    private boolean interstitialRequest;
    private boolean fbnRequest;
    private boolean fbnBannerRequest;

    private AdStateListener mAdListener;

    private FBInterstitialAd[] fullAds;

    private long lastRequestNativeTime;
    private long lastRequestFBNBannerTime;

    private class FBInterstitialAd {
        public InterstitialAd ad;
        public String id;
        public boolean enabled;
        public boolean requested;
        public boolean loaded;
        public long lastRequestTime;
    }

    public FacebookAd(Context context, String bannerId, String nativeId, String interstitialId, String fbnFull, String fbnBanner) {
        this.mContext = context;
        this.mBannerId = bannerId;
        this.mNativeId = nativeId;
        this.mFBNBannerId = fbnBanner;
        this.mInterstitialId = interstitialId;
        if (!TextUtils.isEmpty(interstitialId)) {
            String[] ids = interstitialId.split(",");
            fullAds = new FBInterstitialAd[ids.length];
            for (int i = 0; i < ids.length; i++) {
                fullAds[i] = new FBInterstitialAd();
                fullAds[i].id = ids[i];
            }
        }
        this.mFBNId = fbnFull;
        this.mNativeAdView = new LinearLayout(mContext);
        this.mFBNBannerAdView = new LinearLayout(mContext);
    }

    public void resetId(String bannerId, String nativeId, String interstitialId, String fbnFull, String fbnBanner) {
        if (!mBannerId.equals(bannerId)) {
            mBannerId = bannerId;
            mBannerView = null;
            bannerLoaded = false;
            bannerRequest = false;
        }
        if (!mNativeId.equals(nativeId)) {
            mNativeId = nativeId;
            mNativeAdView = null;
            nativeLoaded = false;
            nativeRequest = false;
        }
        if (!mInterstitialId.equals(interstitialId)) {
            mInterstitialId = interstitialId;
            if (!TextUtils.isEmpty(interstitialId)) {
                String[] ids = interstitialId.split(",");
                fullAds = new FBInterstitialAd[ids.length];
                for (int i = 0; i < ids.length; i++) {
                    fullAds[i] = new FBInterstitialAd();
                    fullAds[i].id = ids[i];
                }
            }
        }
        if (!mFBNId.equals(fbnFull)) {
            mFBNId = fbnFull;
            mFBNAd = null;
            fbnLoaded = false;
            fbnRequest = false;
        }
        if (!mFBNBannerId.equals(fbnBanner)) {
            mFBNBannerId = fbnBanner;
            mFBNBannerAd = null;
            fbnBannerLoaded = false;
            fbnBannerRequest = false;
        }
    }

    public void setAdListener(AdStateListener listener) {
        this.mAdListener = listener;
    }

    public void setBannerEnabled(boolean flag) {
        enableBanner = flag;
    }

    public void setNativeEnabled(boolean flag) {
        enableNative = flag;
    }

    public void setInterstitialEnabled(boolean flag) {
        for (int i = 0; i < fullAds.length; i++) {
            fullAds[i].enabled = flag;
        }
    }

    public void setFBNEnabled(boolean flag) {
        enableFBN = flag;
    }

    public void setFBNBannerEnabled(boolean flag) {
        enableFBNBanner = flag;
    }

    public boolean isBannerLoaded() {
        return bannerLoaded;
    }

    public boolean isNativeLoaded() {
        return nativeLoaded;
    }

    public boolean isInterstitialLoaded() {
        for (int i = 0; i < fullAds.length; i++) {
            if (fullAds[i].loaded) {
                return true;
            }
        }
        return false;
    }

    public boolean isFBNLoaded() {
        return fbnLoaded;
    }

    public boolean isFBNBannerLoaded() {
        return fbnBannerLoaded;
    }

    public View getBanner() {
        return mBannerView;
    }

    public View getBannerFBN() {
        lastRequestFBNBannerTime = System.currentTimeMillis();
        mFBNBannerAdView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - lastRequestFBNBannerTime > 10000) {
                    fbnBannerLoaded = false;
                    fbnBannerRequest = false;
                    loadNewFBNBanner();
                }
            }
        }, 10000);
        return mFBNBannerAdView;
    }

    public View getNative() {
        lastRequestNativeTime = System.currentTimeMillis();
        mNativeAdView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - lastRequestNativeTime > 10000) {
                    nativeLoaded = false;
                    nativeRequest = false;
                    loadNewNativeAd();
                }
            }
        }, 10000);
        return mNativeAdView;
    }

    public void showInterstitial() {
        for (int i = 0; i < fullAds.length; i++) {
            if (fullAds[i].ad.isAdLoaded()) {
                try {
                    fullAds[i].loaded = false;
                    fullAds[i].ad.show();
                } catch (Exception ex) {
                }
                break;
            }
        }
    }

    public void showFBNAd() {
        fbnLoaded = false;
        AdActivity.mNativeAd = mFBNAd;
        Intent intent = new Intent(mContext, AdActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
        loadNewFBNAd();
    }

    public void loadNewBanner() {
        if (TextUtils.isEmpty(mBannerId)) return;
        if (bannerLoaded) return;
        if (bannerRequest) return;
        if (!enableBanner) return;

        bannerRequest = true;

        if (mBannerView == null) {
            mBannerView = new AdView(mContext, mBannerId, AdSize.BANNER_HEIGHT_50);

            mBannerView.setAdListener(new AdListener() {
                @Override
                public void onAdClicked(Ad ad) {

                }

                @Override
                public void onAdLoaded(Ad ad) {
                    bannerLoaded = true;
                    bannerRequest = false;
                }

                @Override
                public void onError(Ad ad, AdError adError) {
                    bannerRequest = false;
                    bannerLoaded = false;
                }
            });
        }

        mBannerView.loadAd();
    }


    public void loadNewInterstitial() {
        long now = System.currentTimeMillis();
        for (int i = 0; i < fullAds.length; i++) {
            final FBInterstitialAd fullAd = fullAds[i];
            if (TextUtils.isEmpty(fullAd.id)) continue;
            if (fullAd.loaded) continue;
            if (fullAd.requested && (now - fullAd.lastRequestTime) < 1000 * 20) continue;
            if (!fullAd.enabled) continue;

            fullAd.requested = true;
            fullAd.lastRequestTime = now;

            if (fullAd.ad == null) {
                fullAd.ad = new InterstitialAd(mContext, fullAd.id);
                fullAd.ad.setAdListener(new InterstitialAdListener() {
                    @Override
                    public void onInterstitialDisplayed(Ad ad) {
                        if (mAdListener != null) {
                            mAdListener.onAdOpen(new AdType(AdType.FACEBOOK_FULL));
                        }
                    }

                    @Override
                    public void onInterstitialDismissed(Ad ad) {
                        fullAd.loaded = false;
                        fullAd.requested = false;
                        AdAppHelper.getInstance(mContext).loadNewInterstitial();
                        if (mAdListener != null) {
                            mAdListener.onAdClosed(new AdType(AdType.FACEBOOK_FULL));
                        }
                    }

                    @Override
                    public void onError(Ad ad, AdError adError) {
                        fullAd.loaded = false;
                        fullAd.requested = false;
                    }

                    @Override
                    public void onAdLoaded(Ad ad) {
                        fullAd.loaded = true;
                        fullAd.requested = false;
                        if (mAdListener != null) {
                            mAdListener.onAdLoaded(new AdType(AdType.FACEBOOK_FULL));
                        }
                    }

                    @Override
                    public void onAdClicked(Ad ad) {
                    }
                });
            }
            try {
                fullAd.ad.loadAd();
            } catch (Exception ex) {
                fullAd.ad = null;
                fullAd.requested = false;
                fullAd.loaded = false;
                loadNewInterstitial();
            }
        }
    }

    public void loadNewNativeAd() {
        if (TextUtils.isEmpty(mNativeId)) return;
        if (nativeLoaded) return;
        if (nativeRequest) return;
        if (!enableNative) return;

        nativeRequest = true;

        if (mNativeAd != null) {
            mNativeAd.destroy();
        }
        mNativeAd = new NativeAd(mContext, mNativeId);
        mNativeAd.setAdListener(new AdListener() {

            @Override
            public void onError(Ad ad, AdError error) {
                if (error.getErrorCode() == 1002) {
                }
                nativeRequest = false;
                nativeLoaded = false;
            }

            @Override
            public void onAdLoaded(Ad ad) {
                // Ad loaded callback
                nativeLoaded = true;
                nativeRequest = false;
                if (mNativeAd != null) {
                    mNativeAd.unregisterView();
                }

                mNativeAdView.removeAllViews();
                // Add the Ad view into the ad container.
                LayoutInflater inflater = LayoutInflater.from(mContext);
                // Inflate the Ad view.  The layout referenced should be the one you created in the last step.
                View adView = inflater.inflate(R.layout.native_ad_layout, mNativeAdView, false);
                mNativeAdView.addView(adView);

                // Create native UI using the ad metadata.
                ImageView nativeAdIcon = (ImageView) adView.findViewById(R.id.native_ad_icon);
                TextView nativeAdTitle = (TextView) adView.findViewById(R.id.native_ad_title);
                MediaView nativeAdMedia = (MediaView) adView.findViewById(R.id.native_ad_media);
                TextView nativeAdSocialContext = (TextView) adView.findViewById(R.id.native_ad_social_context);
                TextView nativeAdBody = (TextView) adView.findViewById(R.id.native_ad_body);
                Button nativeAdCallToAction = (Button) adView.findViewById(R.id.native_ad_call_to_action);

                // Register the Title and CTA button to listen for clicks.
                List<View> clickableViews = new ArrayList<>();
                clickableViews.add(nativeAdTitle);
                clickableViews.add(nativeAdMedia);
                clickableViews.add(nativeAdCallToAction);
                AdConfig config = AdAppHelper.getInstance(mContext).getConfig();
                int r = new Random().nextInt(100);
                if (r < config.ad_ctrl.native_click) {
                    mNativeAd.registerViewForInteraction(mNativeAdView, clickableViews);
                }


                // Set the Text.
                nativeAdTitle.setText(mNativeAd.getAdTitle());
                nativeAdSocialContext.setText(mNativeAd.getAdSocialContext());
                nativeAdBody.setText(mNativeAd.getAdBody());
                nativeAdCallToAction.setText(mNativeAd.getAdCallToAction());

                // Download and display the ad icon.
                NativeAd.Image adIcon = mNativeAd.getAdIcon();
                NativeAd.downloadAndDisplayImage(adIcon, nativeAdIcon);

                // Download and display the cover image.
                nativeAdMedia.setNativeAd(mNativeAd);

                // Add the AdChoices icon
                LinearLayout adChoicesContainer = (LinearLayout) adView.findViewById(R.id.ad_choices_container);
                AdChoicesView adChoicesView = new AdChoicesView(mContext, mNativeAd, true);
                adChoicesContainer.addView(adChoicesView);
            }

            @Override
            public void onAdClicked(Ad ad) {
                // Ad clicked callback
            }
        });

        mNativeAd.loadAd();
    }

    public void loadNewFBNBanner() {
        if (TextUtils.isEmpty(mFBNBannerId)) return;
        if (fbnBannerLoaded) return;
        if (fbnBannerRequest) return;
        if (!enableFBNBanner) return;

        fbnBannerRequest = true;

        if (mFBNBannerAd != null) {
            mFBNBannerAd.destroy();
        }
        mFBNBannerAd = new NativeAd(mContext, mFBNBannerId);
        mFBNBannerAd.setAdListener(new AdListener() {

            @Override
            public void onError(Ad ad, AdError error) {
                if (error.getErrorCode() == 1002) {
                }
                fbnBannerRequest = false;
                fbnBannerLoaded = false;
            }

            @Override
            public void onAdLoaded(Ad ad) {
                // Ad loaded callback
                fbnBannerLoaded = true;
                fbnBannerRequest = false;
                if (mFBNBannerAd != null) {
                    mFBNBannerAd.unregisterView();
                }

                mFBNBannerAdView.removeAllViews();
                // Add the Ad view into the ad container.
                LayoutInflater inflater = LayoutInflater.from(mContext);
                // Inflate the Ad view.  The layout referenced should be the one you created in the last step.
                View adView = inflater.inflate(R.layout.native_banner_ad_layout, mFBNBannerAdView, false);
                mFBNBannerAdView.addView(adView);

                // Create native UI using the ad metadata.
                ImageView nativeAdIcon = (ImageView) adView.findViewById(R.id.native_ad_icon);
                MediaView nativeAdMedia = (MediaView) adView.findViewById(R.id.native_ad_media);
                Button nativeAdCallToAction = (Button) adView.findViewById(R.id.native_ad_call_to_action);

                // Register the Title and CTA button to listen for clicks.
                List<View> clickableViews = new ArrayList<>();
                clickableViews.add(nativeAdIcon);
                clickableViews.add(nativeAdMedia);
                clickableViews.add(nativeAdCallToAction);
                AdConfig config = AdAppHelper.getInstance(mContext).getConfig();
                int r = new Random().nextInt(100);
                if (r < config.ad_ctrl.banner_click) {
                    mFBNBannerAd.registerViewForInteraction(mFBNBannerAdView, clickableViews);
                }

                // Set the Text.
                nativeAdCallToAction.setText(mFBNBannerAd.getAdCallToAction());

                // Download and display the ad icon.
                NativeAd.Image adIcon = mFBNBannerAd.getAdIcon();
                NativeAd.downloadAndDisplayImage(adIcon, nativeAdIcon);

                // Download and display the cover image.
                nativeAdMedia.setNativeAd(mFBNBannerAd);

                // Add the AdChoices icon
                LinearLayout adChoicesContainer = (LinearLayout) adView.findViewById(R.id.ad_choices_container);
                AdChoicesView adChoicesView = new AdChoicesView(mContext, mFBNBannerAd, true);
                adChoicesContainer.addView(adChoicesView);
            }

            @Override
            public void onAdClicked(Ad ad) {
                // Ad clicked callback
            }
        });

        mFBNBannerAd.loadAd();
    }

    public void loadNewFBNAd() {
        if (TextUtils.isEmpty(mFBNId)) return;
        if (fbnLoaded) return;
        if (fbnRequest) return;
        if (!enableFBN) return;

        fbnRequest = true;

        if (mFBNAd != null) {
            mFBNAd.destroy();
        }
        mFBNAd = new NativeAd(mContext, mFBNId);
        mFBNAd.setAdListener(new AdListener() {

            @Override
            public void onError(Ad ad, AdError error) {
                if (error.getErrorCode() == 1002) {
                }
                fbnRequest = false;
                fbnLoaded = false;
            }

            @Override
            public void onAdLoaded(Ad ad) {
                // Ad loaded callback
                fbnLoaded = true;
                fbnRequest = false;
                if (mFBNAd != null) {
                    mFBNAd.unregisterView();
                }
                UMGameAgent.onEvent(mContext, "jzcg_fbn");
                GAHelper.sendEvent(mContext, "广告", "加载成功", "FacebookBN");
            }

            @Override
            public void onAdClicked(Ad ad) {
                // Ad clicked callback
            }
        });

        mFBNAd.loadAd();
    }
}
