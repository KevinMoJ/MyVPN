package com.androapplite.shadowsocks.ads;


import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.NativeExpressAdView;

public class AdMobAd {
    private Context mContext;
    private AdView mNativeAdView;
    public NativeExpressAdView mNativeENAdView;
    private AdView mBannerView;
    private InterstitialAd mInterstitialAd;
    private String mBannerId;
    private String mNativeId;
    private String mNativeENId;
    private String mInterstitialId;

    private boolean enableBanner;
    private boolean enableNative;
    private boolean enableNativeEN;
    private boolean enableInterstitial;

    private boolean bannerLoaded;
    private boolean nativeLoaded;
    private boolean nativeENLoaded;
    private boolean interstitialLoaded;

    private boolean bannerRequest;
    private boolean nativeRequest;
    private boolean nativeENRequest;
    private boolean interstitialRequest;
    private long lastRequestInsterstialTime = 0;

    private com.androapplite.shadowsocks.ads.AdListener mAdListener;

    public static int NATIVE_WIDTH = 320;
    public static int NATIVE_HEIGHT = 320;

    public AdMobAd(Context context, String bannerId, String nativeId, String interstitialId, String nativeENId) {
        this.mContext = context;
        this.mBannerId = bannerId;
        this.mNativeId = nativeId;
        this.mInterstitialId = interstitialId;
        this.mNativeENId = nativeENId;
    }

    public void resetId(String bannerId, String nativeId, String interstitialId, String nativeENId) {
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
        if (!mNativeENId.equals(nativeENId)) {
            mNativeENId = nativeENId;
            mNativeENAdView = null;
            nativeENLoaded = false;
            nativeENRequest = false;
            loadNewNativeENAd();
        }
    }

    public void setAdListener(com.androapplite.shadowsocks.ads.AdListener listener) {
        this.mAdListener = listener;
    }

    public void onResume() {
        if (mBannerView != null) {
            mBannerView.resume();
        }
        if (mNativeAdView != null) {
            mNativeAdView.resume();
        }
        if (mNativeENAdView != null) {
            mNativeENAdView.resume();
        }
    }

    public void onPause() {
        if (mBannerView != null) {
            mBannerView.pause();
        }
        if (mNativeAdView != null) {
            mNativeAdView.pause();
        }
        if (mNativeENAdView != null) {
            mNativeENAdView.pause();
        }
    }

    public void setBannerEnabled(boolean flag) {
        enableBanner = flag;
    }

    public void setNativeEnabled(boolean flag) {
        enableNative = flag;
    }

    public void setNativeENEnabled(boolean flag) {
        enableNativeEN = flag;
    }

    public void setInterstitialEnabled(boolean flag) {
        enableInterstitial = flag;
    }

    public boolean isBannerLoaded() {
        return bannerLoaded;
    }

    public boolean isNativeLoaded() {
        return nativeLoaded;
    }

    public boolean isNativeENLoaded() {
        return nativeENLoaded;
    }

    public boolean isInterstitialLoaded() {
        return interstitialLoaded;
    }

    public View getBanner() {
        return mBannerView;
    }

    public View getNative() {
        return mNativeAdView;
    }

    public View getNativeEN() {
        return mNativeENAdView;
    }

    public void showInterstitial() {
        interstitialLoaded = false;
        if (mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        }
    }

    public void loadNewBanner() {
        if (TextUtils.isEmpty(mBannerId)) return;
        if (bannerLoaded) return;
        if (bannerRequest) return;
        if (!enableBanner) return;

        bannerRequest = true;

        if (mBannerView == null) {
            mBannerView = new AdView(mContext);
            mBannerView.setAdUnitId(mBannerId);
            mBannerView.setAdSize(AdSize.BANNER);

            mBannerView.setAdListener(new AdListener() {
                @Override
                public void onAdFailedToLoad(int i) {
                    bannerRequest = false;
                    bannerLoaded = false;
                }

                @Override
                public void onAdLoaded() {
                    bannerLoaded = true;
                    bannerRequest = false;
                }
            });
        }

        AdRequest adRequest = new AdRequest.Builder().build();
        mBannerView.loadAd(adRequest);
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
            mInterstitialAd = new InterstitialAd(mContext);
            mInterstitialAd.setAdUnitId(mInterstitialId);
            mInterstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdClosed() {
                    interstitialRequest = false;
                    interstitialLoaded = false;
                    AdAppHelper.getInstance(mContext).loadNewInterstitial();
                }

                @Override
                public void onAdLoaded() {
                    interstitialLoaded = true;
                    interstitialRequest = false;
                    if (mAdListener != null) {
                        mAdListener.onAdLoaded(new AdType(AdType.ADMOB_FULL));
                    }
                }

                @Override
                public void onAdOpened() {
                    if (mAdListener != null) {
                        mAdListener.onAdOpen(new AdType(AdType.ADMOB_FULL));
                    }
                }

                @Override
                public void onAdFailedToLoad(int i) {
                    interstitialLoaded = false;
                    interstitialRequest = false;
                }
            });
        }
        AdRequest adRequest = new AdRequest.Builder().build();
        mInterstitialAd.loadAd(adRequest);
    }

    public void loadNewNativeAd() {
        if (TextUtils.isEmpty(mNativeId)) return;
        if (nativeLoaded) return;
        if (nativeRequest) return;
        if (!enableNative) return;

        nativeRequest = true;

        if (mNativeAdView == null) {
            mNativeAdView = new AdView(mContext);
            mNativeAdView.setAdUnitId(mNativeId);
            mNativeAdView.setAdSize(AdSize.MEDIUM_RECTANGLE);

            mNativeAdView.setAdListener(new AdListener() {
                @Override
                public void onAdFailedToLoad(int i) {
                    nativeRequest = false;
                    nativeLoaded = false;
                }

                @Override
                public void onAdLoaded() {
                    nativeLoaded = true;
                    nativeRequest = false;
                }
            });
        }

        AdRequest adRequest = new AdRequest.Builder().build();
        mNativeAdView.loadAd(adRequest);
    }

    public void loadNewNativeENAd() {
        if (TextUtils.isEmpty(mNativeENId)) return;
        if (nativeENLoaded) return;
        if (nativeENRequest) return;
        if (!enableNativeEN) return;

        nativeENRequest = true;

        if (mNativeENAdView == null) {
            mNativeENAdView = new NativeExpressAdView(mContext);
            mNativeENAdView.setAdUnitId(mNativeENId);
            mNativeENAdView.setAdSize(new AdSize(320, 250));

            mNativeENAdView.setAdListener(new AdListener() {
                @Override
                public void onAdFailedToLoad(int i) {
                    nativeENRequest = false;
                    nativeENLoaded = false;
                }

                @Override
                public void onAdLoaded() {
                    nativeENLoaded = true;
                    nativeENRequest = false;
                }
            });
        }

        AdRequest adRequest = new AdRequest.Builder().build();
        mNativeENAdView.loadAd(adRequest);
    }

}
