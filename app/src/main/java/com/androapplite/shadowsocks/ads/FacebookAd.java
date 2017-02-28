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

import com.androapplite.lisasa.applock.newapplock.utils.Analytics;
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
import com.lisasa.applock.R;
import com.umeng.analytics.game.UMGameAgent;

import java.util.ArrayList;
import java.util.List;

public class FacebookAd {
    private Context mContext;
    private LinearLayout mNativeAdView;
    private NativeAd mNativeAd;
    private NativeAd mFBNAd;
    private AdView mBannerView;
    private InterstitialAd mInterstitialAd;
    private String mBannerId;
    private String mNativeId;
    private String mInterstitialId;
    private String mFBNId;

    private boolean enableBanner;
    private boolean enableNative;
    private boolean enableInterstitial;
    private boolean enableFBN;

    private boolean bannerLoaded;
    private boolean nativeLoaded;
    private boolean interstitialLoaded;
    private boolean fbnLoaded;

    private boolean bannerRequest;
    private boolean nativeRequest;
    private boolean interstitialRequest;
    private boolean fbnRequest;
    private long lastRequestInsterstialTime = 0;

    public static int NATIVE_WIDTH = 320;
    public static int NATIVE_HEIGHT = 320;

    private com.androapplite.lisasa.applock.newapplock.ads.AdListener mAdListener;

    public FacebookAd(Context context, String bannerId, String nativeId, String interstitialId, String fbnFull) {
        this.mContext = context;
        this.mBannerId = bannerId;
        this.mNativeId = nativeId;
        this.mInterstitialId = interstitialId;
        this.mFBNId = fbnFull;
        this.mNativeAdView = new LinearLayout(mContext);
    }

    public void resetId(String bannerId, String nativeId, String interstitialId, String fbnFull) {
        if (!mBannerId.equals(bannerId)) {
            mBannerId = bannerId;
            mBannerView = null;
            bannerLoaded = false;
            bannerRequest = false;
            loadNewBanner();
        }
        if (!mNativeId.equals(nativeId)) {
            mNativeId = nativeId;
            mNativeAdView = null;
            nativeLoaded = false;
            nativeRequest = false;
            loadNewNativeAd();
        }
        if (!mInterstitialId.equals(interstitialId)) {
            mInterstitialId = interstitialId;
            mInterstitialAd = null;
            interstitialLoaded = false;
            interstitialRequest = false;
            loadNewInterstitial();
        }
        if (!mFBNId.equals(fbnFull)) {
            mFBNId = fbnFull;
            mFBNAd = null;
            fbnLoaded = false;
            fbnRequest = false;
            loadNewFBNAd();
        }
    }

    public void setAdListener(com.androapplite.lisasa.applock.newapplock.ads.AdListener listener) {
        this.mAdListener = listener;
    }

    public void setBannerEnabled(boolean flag) {
        enableBanner = flag;
    }

    public void setNativeEnabled(boolean flag) {
        enableNative = flag;
    }

    public void setInterstitialEnabled(boolean flag) {
        enableInterstitial = flag;
    }

    public void setFBNEnabled(boolean flag) {
        enableFBN = flag;
    }

    public boolean isBannerLoaded() {
        return bannerLoaded;
    }

    public boolean isNativeLoaded() {
        return nativeLoaded;
    }

    public boolean isInterstitialLoaded() {
        return interstitialLoaded;
    }

    public boolean isFBNLoaded() {
        return fbnLoaded;
    }

    public View getBanner() {
        return mBannerView;
    }

    public View getNative() {
        nativeLoaded = false;
        return mNativeAdView;
    }

    public void showInterstitial() {
        interstitialLoaded = false;
        if (mInterstitialAd.isAdLoaded()) {
            try {
                mInterstitialAd.show();
            } catch (Exception ex) {
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
        if (TextUtils.isEmpty(mInterstitialId)) return;
        if (interstitialLoaded) return;
        if (interstitialRequest && (now - lastRequestInsterstialTime) < 1000 * 20) return;
        if (!enableInterstitial) return;

        interstitialRequest = true;
        lastRequestInsterstialTime = now;

        if (mInterstitialAd == null) {
            mInterstitialAd = new InterstitialAd(mContext, mInterstitialId);
            mInterstitialAd.setAdListener(new InterstitialAdListener() {
                @Override
                public void onInterstitialDisplayed(Ad ad) {
                    if (mAdListener != null) {
                        mAdListener.onAdOpen(new AdType(AdType.FACEBOOK_FULL));
                    }
                }

                @Override
                public void onInterstitialDismissed(Ad ad) {
                    interstitialLoaded = false;
                    interstitialRequest = false;
                    AdAppHelper.getInstance(mContext).loadNewInterstitial();
                }

                @Override
                public void onError(Ad ad, AdError adError) {
                    interstitialLoaded = false;
                    interstitialRequest = false;
                }

                @Override
                public void onAdLoaded(Ad ad) {
                    interstitialLoaded = true;
                    interstitialRequest = false;
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
            mInterstitialAd.loadAd();
        } catch (Exception ex) {
            mInterstitialAd = null;
            interstitialRequest = false;
            interstitialLoaded = false;
            loadNewInterstitial();
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
                mNativeAd.registerViewForInteraction(mNativeAdView);

                // Create native UI using the ad metadata.
                ImageView nativeAdIcon = (ImageView) adView.findViewById(R.id.native_ad_icon);
                TextView nativeAdTitle = (TextView) adView.findViewById(R.id.native_ad_title);
                MediaView nativeAdMedia = (MediaView) adView.findViewById(R.id.native_ad_media);
                TextView nativeAdSocialContext = (TextView) adView.findViewById(R.id.native_ad_social_context);
                TextView nativeAdBody = (TextView) adView.findViewById(R.id.native_ad_body);
                Button nativeAdCallToAction = (Button) adView.findViewById(R.id.native_ad_call_to_action);

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

                // Register the Title and CTA button to listen for clicks.
                List<View> clickableViews = new ArrayList<>();
                clickableViews.add(nativeAdTitle);
                clickableViews.add(nativeAdMedia);
            }

            @Override
            public void onAdClicked(Ad ad) {
                // Ad clicked callback
            }
        });

        mNativeAd.loadAd();
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
                Analytics.getInstance(mContext)._sendEvent("广告", "加载成功", "FacebookBN");
            }

            @Override
            public void onAdClicked(Ad ad) {
                // Ad clicked callback
            }
        });

        mFBNAd.loadAd();
    }
}
