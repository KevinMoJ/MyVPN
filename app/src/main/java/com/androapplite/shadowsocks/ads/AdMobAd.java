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
//    private InterstitialAd mInterstitialAd;
    private String mBannerId;
    private String mNativeId;
    private String mNativeENId;
    private String mInterstitialId;

    private boolean enableBanner;
    private boolean enableNative;
    private boolean enableNativeEN;
//    private boolean enableInterstitial;

    private boolean bannerLoaded;
    private boolean nativeLoaded;
    private boolean nativeENLoaded;
//    private boolean interstitialLoaded;

    private boolean bannerRequest;
    private boolean nativeRequest;
    private boolean nativeENRequest;
//    private boolean interstitialRequest;
//    private long lastRequestInsterstialTime = 0;

    private AdStateListener mAdListener;

    private AdMobInterstitialAd[] fullAds;

    private long lastRequestNativeENTime = 0;

    private class AdMobInterstitialAd {
        public InterstitialAd ad;
        public String id;
        public boolean enabled;
        public boolean requested;
        public boolean loaded;
        public long lastRequestTime;
    }

    public AdMobAd(Context context, String bannerId, String nativeId, String interstitialId, String nativeENId) {
        this.mContext = context;
        this.mBannerId = bannerId;
        this.mNativeId = nativeId;
        this.mInterstitialId = interstitialId;
        if (!TextUtils.isEmpty(interstitialId)) {
            String[] ids = interstitialId.split(",");
            fullAds = new AdMobInterstitialAd[ids.length];
            for (int i = 0; i < ids.length; i++) {
                fullAds[i] = new AdMobInterstitialAd();
                fullAds[i].id = ids[i];
            }
        }
        this.mNativeENId = nativeENId;
    }

    public void resetId(String bannerId, String nativeId, String interstitialId, String nativeENId) {
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
                fullAds = new AdMobInterstitialAd[ids.length];
                for (int i = 0; i < ids.length; i++) {
                    fullAds[i] = new AdMobInterstitialAd();
                    fullAds[i].id = ids[i];
                }
            }
        }
        if (!mNativeENId.equals(nativeENId)) {
            mNativeENId = nativeENId;
            mNativeENAdView = null;
            nativeENLoaded = false;
            nativeENRequest = false;
        }
    }

    public void setAdListener(AdStateListener listener) {
        this.mAdListener = listener;
    }

    public void onResume() {
//        if (mBannerView != null) {
//            mBannerView.resume();
//        }
//        if (mNativeAdView != null) {
//            mNativeAdView.resume();
//        }
//        if (mNativeENAdView != null) {
//            mNativeENAdView.resume();
//        }
    }

    public void onPause() {
//        if (mBannerView != null) {
//            mBannerView.pause();
//        }
//        if (mNativeAdView != null) {
//            mNativeAdView.pause();
//        }
//        if (mNativeENAdView != null) {
//            mNativeENAdView.pause();
//        }
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
        for (int i = 0; i < fullAds.length; i++) {
            fullAds[i].enabled = flag;
        }
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
        for (int i = 0; i < fullAds.length; i++) {
            if (fullAds[i].loaded) {
                return true;
            }
        }
        return false;
    }

    public View getBanner() {
        return mBannerView;
    }

    public View getNative() {
        return mNativeAdView;
    }

    public View getNativeEN() {
        lastRequestNativeENTime = System.currentTimeMillis();
        mNativeENAdView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - lastRequestNativeENTime > 10000) {
                    nativeENLoaded = false;
                    nativeENRequest = false;
                    loadNewNativeENAd();
                }
            }
        }, 10000);
        return mNativeENAdView;
    }

    public void showInterstitial() {
        for (int i = 0; i < fullAds.length; i++) {
            if (fullAds[i].ad.isLoaded()) {
                try {
                    fullAds[i].loaded = false;
                    fullAds[i].ad.show();
                } catch (Exception ex) {
                }
                break;
            }
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
        for (int i = 0; i < fullAds.length; i++) {
            final AdMobInterstitialAd fullAd = fullAds[i];
            if (TextUtils.isEmpty(fullAd.id)) continue;
            if (fullAd.loaded) continue;
            if (fullAd.requested && (now - fullAd.lastRequestTime) < 1000 * 20) continue;
            if (!fullAd.enabled) continue;

            fullAd.requested = true;
            fullAd.lastRequestTime = now;

            if (fullAd.ad == null) {
                fullAd.ad = new InterstitialAd(mContext);
                fullAd.ad.setAdUnitId(fullAd.id);
                fullAd.ad.setAdListener(new AdListener() {
                    @Override
                    public void onAdClosed() {
                        fullAd.loaded = false;
                        fullAd.requested = false;
                        AdAppHelper.getInstance(mContext).loadNewInterstitial();
                    }

                    @Override
                    public void onAdLoaded() {
                        fullAd.loaded = true;
                        fullAd.requested = false;
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
                        fullAd.loaded = false;
                        fullAd.requested = false;
                    }
                });
            }
            AdRequest adRequest = new AdRequest.Builder().build();
            fullAd.ad.loadAd(adRequest);
        }
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
            mNativeENAdView.setAdSize(new AdSize(320, 180));

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
