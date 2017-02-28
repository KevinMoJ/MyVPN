package com.androapplite.shadowsocks.ads;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.ads.Ad;
import com.facebook.ads.AdChoicesView;
import com.facebook.ads.AdError;
import com.facebook.ads.MediaView;
import com.facebook.ads.NativeAd;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.NativeExpressAdView;
import com.lisasa.applock.R;
import com.umeng.analytics.game.UMGameAgent;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class AdHelper {
    private class Config {
        boolean lockShow = false;
        boolean installShow = false;
        boolean prelockShow = false;
        boolean afterLaunchShow = false;
        int lockShowInterval = 0;
        int installShowInterval = 0;//毫秒
        int prelockShowInterval = 0;
        int afterLaunchShowInterval = 0;
        int lockShowCount = 0;
        int installShowCount = 0;
        int prelockShowCount = 0;
        int afterLaunchShowCount = 0;
        int lastlockShowCount = 0;
        int lastInstallShowCount = 0;
        int lastPreLockShowCount = 0;
        int lastAfterLaunchShowCount = 0;
        long lastLockShowTime = 0;
        long lastInstallShowTime = 0;
        long lastPreLockShowTime = 0;
        long lastAfterLaunchShowTime = 0;

        String admob = "";
        String fb = "";
        boolean selfQuitAd =false;
        double weight = -1;
        int lastDay = 0;
    }

    private final static String NAME = "AdHelper";
    private Config mConfig;
    private Context mContext;
    private static AdHelper _this;
    private long mLastLoadConfigTime;

    private NativeAd mNativeAd;
    private NativeExpressAdView mBannerView;
    private LinearLayout mAdLayout;
    private boolean mFBLoaded = false;
    private boolean mFBRequested = false;
    private boolean mFBUsed = false;
    private boolean mAdmobLoaded = false;
    private boolean mAdmobRequested = false;
    private boolean mAdmobUsed = false;
    private long lastLoadErrorTime = 0;

    protected AdHelper(Context context) {
        mContext = context;

        init();
    }

    public static AdHelper getInstance(Context context) {
        if (_this == null) {
            _this = new AdHelper(context);
        }
        return _this;
    }

    public boolean enableLockViewAd() {
        reloadConfig();
        long now = System.currentTimeMillis();
        if (now - mConfig.lastLockShowTime < mConfig.lockShowInterval) {
            return false;
        }
        return mConfig.lockShow && mConfig.lastlockShowCount <= mConfig.lockShowCount;
    }

    public void loadLockViewAd(int width, int height) {
        if (enableLockViewAd()) {
            AdAppHelper.getInstance(mContext).loadNewNative();
        }
    }

    public View showLockViewAd() {
        if (enableLockViewAd() && AdAppHelper.getInstance(mContext).isNativeLoaded()) {
            mConfig.lastlockShowCount++;
            mConfig.lastLockShowTime = System.currentTimeMillis();
            SharedPreferences.Editor editor = mContext.getSharedPreferences("ad_helper", Context.MODE_PRIVATE).edit();
            editor.putLong("lastLockShowTime", mConfig.lastLockShowTime);
            editor.putInt("lastlockShowCount", mConfig.lastlockShowCount);
            editor.commit();
            return AdAppHelper.getInstance(mContext).getNative();
        } else {
            return null;
        }
    }

    public void showFullAd() {
        AdAppHelper.getInstance(mContext).showFullAd();
    }

    public void loadNgsAd() {
        if (!mConfig.prelockShow && !mConfig.installShow && !mConfig.afterLaunchShow) return;
//        if (mConfig.lastPreLockShowCount >= mConfig.prelockShowCount ||
//                mConfig.lastInstallShowCount >= mConfig.installShowCount ||
//                mConfig.lastAfterLaunchShowCount >= mConfig.afterLaunchShowCount) {
//            return;
//        }

        try {
            AdAppHelper.getInstance(mContext).loadNewInterstitial();
        } catch (Exception ex) {
        }
    }

    public boolean enableShowPreLockAd() {
        long now = System.currentTimeMillis();
        if (now - mConfig.lastPreLockShowTime < mConfig.prelockShowInterval) {
            return false;
        }
        return AdAppHelper.getInstance(mContext).isFullAdLoaded()
                && mConfig.prelockShow && mConfig.lastPreLockShowCount <= mConfig.prelockShowCount;
    }

    public void showPreLockAd() {
        if (enableShowPreLockAd()) {
            mConfig.lastPreLockShowCount++;
            mConfig.lastPreLockShowTime = System.currentTimeMillis();
            SharedPreferences.Editor editor = mContext.getSharedPreferences("ad_helper", Context.MODE_PRIVATE).edit();
            editor.putLong("lastPreLockShowTime", mConfig.lastPreLockShowTime);
            editor.putInt("lastPreLockShowCount", mConfig.lastPreLockShowCount);
            editor.commit();

            showFullAd();
        }
    }

    public boolean enableInstallAd() {
        long now = System.currentTimeMillis();
        if (now - mConfig.lastInstallShowTime < mConfig.installShowInterval) {
            return false;
        }
        return AdAppHelper.getInstance(mContext).isFullAdLoaded()
                && mConfig.installShow && mConfig.lastInstallShowCount <= mConfig.installShowCount;
    }
    public void showInstallLockAd() {
        if (enableInstallAd()) {
            mConfig.lastInstallShowTime = System.currentTimeMillis();
            mConfig.lastInstallShowCount++;
            SharedPreferences.Editor editor = mContext.getSharedPreferences("ad_helper", Context.MODE_PRIVATE).edit();
            editor.putLong("lastInstallShowTime", mConfig.lastInstallShowTime);
            editor.putInt("lastInstallShowCount", mConfig.lastInstallShowCount);
            editor.commit();
            showFullAd();
        }
    }
    public boolean enableAfterLaunchAd() {
        long now = System.currentTimeMillis();
        if (now - mConfig.lastAfterLaunchShowTime < mConfig.afterLaunchShowInterval) {
            return false;
        }
        return (AdAppHelper.getInstance(mContext).isFullAdLoaded() || mConfig.selfQuitAd && isAdLoaded())
                && mConfig.afterLaunchShow && mConfig.lastAfterLaunchShowCount < mConfig.afterLaunchShowCount;
    }
    public void showAfterLaunchAd() {
        if (enableAfterLaunchAd()) {
            mConfig.lastAfterLaunchShowTime = System.currentTimeMillis();
            mConfig.lastAfterLaunchShowCount++;
            SharedPreferences.Editor editor = mContext.getSharedPreferences("ad_helper", Context.MODE_PRIVATE).edit();
            editor.putLong("lastAfterLaunchShowTime", mConfig.lastAfterLaunchShowTime);
            editor.putInt("lastAfterLaunchShowCount", mConfig.lastAfterLaunchShowCount);
            editor.commit();

            if (mConfig.selfQuitAd && isAdLoaded()) {
                Intent intent = new Intent(mContext, AdActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
            } else {
                showFullAd();
            }
        }
    }

    private void deleteAd() {
    }

    private void init() {
        mAdLayout = new LinearLayout(mContext);
        mBannerView = new NativeExpressAdView(mContext);

        loadConfig();
        Timer timer = new Timer("loadConfig");
        timer.scheduleAtFixedRate(loadAdTask, 0, 1000 * 10);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                loadConfig();
            }
        }).start();
    }

    private void reloadConfig() {
        long now = System.currentTimeMillis();
        if (now - mLastLoadConfigTime > 1800 * 1000) {
            UMGameAgent.updateOnlineConfig(mContext);
            loadConfig();
            deleteAd();
        }
    }

    private void loadConfig() {
        mLastLoadConfigTime = System.currentTimeMillis();
        SharedPreferences sp = mContext.getSharedPreferences("ad_helper", Context.MODE_PRIVATE);
        mConfig = new Config();
        mConfig.lastLockShowTime = sp.getLong("lastLockShowTime", 0);
        mConfig.lastInstallShowTime = sp.getLong("lastInstallShowTime", 0);
        mConfig.lastPreLockShowTime = sp.getLong("lastPreLockShowTime", 0);
        mConfig.lastAfterLaunchShowTime = sp.getLong("lastAfterLaunchShowTime", 0);
        mConfig.lastInstallShowCount = sp.getInt("lastInstallShowCount", 0);
        mConfig.lastlockShowCount = sp.getInt("lastlockShowCount", 0);
        mConfig.lastPreLockShowCount = sp.getInt("lastPreLockShowCount", 0);
        mConfig.lastAfterLaunchShowCount = sp.getInt("lastAfterLaunchShowCount", 0);
        mConfig.lastDay = sp.getInt("lastDay", 0);
        if (mConfig.lastDay != new Date().getDate()) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putInt("lastDay", new Date().getDate());
            editor.commit();
            mConfig.lastInstallShowCount = 0;
            mConfig.lastlockShowCount = 0;
            mConfig.lastPreLockShowCount = 0;
            mConfig.lastAfterLaunchShowCount = 0;
        }
        Map<String, String> map = new HashMap<>();
        String config = UMGameAgent.getConfigParams(mContext, "ourgame_config");
        if (!TextUtils.isEmpty(config)) {
            config = config.trim();
            int start = config.indexOf("{");
            int end = config.indexOf("}");
            if (start != -1 && end != -1) {
                config = config.substring(start + 1, end);
                config = config.trim();
                String[] pairs = config.split(",");
                for (int i = 0; i < pairs.length; i++) {
                    String one = pairs[i];
                    String[] kv = one.split("=");
                    if (kv.length == 2) {
                        String key = kv[0].trim();
                        String value = kv[1].trim();
                        map.put(key, value);
                    }
                }
            }
        }
        if (map.containsKey("lock_show")) {
            String value = map.get("lock_show");
            mConfig.lockShow = ("1".equals(value) || "true".equalsIgnoreCase(value));
        }
        if (map.containsKey("install_show")) {
            String value = map.get("install_show");
            mConfig.installShow = ("1".equals(value) || "true".equalsIgnoreCase(value));
        }
        if (map.containsKey("prelock_show")) {
            String value = map.get("prelock_show");
            mConfig.prelockShow = ("1".equals(value) || "true".equalsIgnoreCase(value));
        }
        if (map.containsKey("after_launch_show")) {
            String value = map.get("after_launch_show");
            mConfig.afterLaunchShow = ("1".equals(value) || "true".equalsIgnoreCase(value));
        }
        if (map.containsKey("lock_show_interval")) {
            String value = map.get("lock_show_interval");
            try {
                mConfig.lockShowInterval = Integer.parseInt(value) * 1000;
            } catch (Exception ex) {
                mConfig.lockShowInterval = Integer.MAX_VALUE;
            }
        }
        if (map.containsKey("install_show_interval")) {
            String value = map.get("install_show_interval");
            try {
                mConfig.installShowInterval = Integer.parseInt(value) * 1000;
            } catch (Exception ex) {
                mConfig.installShowInterval = Integer.MAX_VALUE;
            }
        }
        if (map.containsKey("prelock_show_interval")) {
            String value = map.get("prelock_show_interval");
            try {
                mConfig.prelockShowInterval = Integer.parseInt(value) * 1000;
            } catch (Exception ex) {
                mConfig.prelockShowInterval = Integer.MAX_VALUE;
            }
        }
        if (map.containsKey("after_launch_show_interval")) {
            String value = map.get("after_launch_show_interval");
            try {
                mConfig.afterLaunchShowInterval = Integer.parseInt(value) * 1000;
            } catch (Exception ex) {
                mConfig.afterLaunchShowInterval = Integer.MAX_VALUE;
            }
        }
        if (map.containsKey("lock_show_count")) {
            String value = map.get("lock_show_count");
            try {
                mConfig.lockShowCount = Integer.parseInt(value);
            } catch (Exception ex) {
                mConfig.lockShowCount = 0;
            }
        }
        if (map.containsKey("install_show_count")) {
            String value = map.get("install_show_count");
            try {
                mConfig.installShowCount = Integer.parseInt(value);
            } catch (Exception ex) {
                mConfig.installShowCount = 0;
            }
        }
        if (map.containsKey("prelock_show_count")) {
            String value = map.get("prelock_show_count");
            try {
                mConfig.prelockShowCount = Integer.parseInt(value);
            } catch (Exception ex) {
                mConfig.prelockShowCount = 0;
            }
        }
        if (map.containsKey("after_launch_show_count")) {
            String value = map.get("after_launch_show_count");
            try {
                mConfig.afterLaunchShowCount = Integer.parseInt(value);
            } catch (Exception ex) {
                mConfig.afterLaunchShowCount = 0;
            }
        }
        if (map.containsKey("self_quit_ad")) {
            String value = map.get("self_quit_ad");
            if (value != null && value.equals("1")) {
                mConfig.selfQuitAd = true;
            } else {
                mConfig.selfQuitAd = false;
            }
        }
        if (map.containsKey("admob")) {
            String value = map.get("admob");
            mConfig.admob = value;
        }
        if (map.containsKey("fb")) {
            String value = map.get("fb");
            mConfig.fb = value;
        }
        if (map.containsKey("weight")) {
            String value = map.get("weight");
            try {
                mConfig.weight = Float.parseFloat(value);
            } catch (Exception ex) {
            }
        }
    }

    public boolean isAdLoaded() {
        return mFBLoaded || mAdmobLoaded;
    }

    public void loadNewAd() {
        if (mFBUsed) {
            mFBLoaded = false;
            mFBUsed = false;
        }
        if (mAdmobUsed) {
            mAdmobLoaded = false;
            mAdmobUsed = false;
        }
        handler.sendEmptyMessage(1000);
        handler.sendEmptyMessage(1001);
    }

    public View getAdView() {
        Random random = new Random();
        float r = (1 + random.nextInt(100)) / 100f;
        if (r < mConfig.weight && mFBLoaded) {
            ViewGroup viewGroup = (ViewGroup)mAdLayout.getParent();
            if (viewGroup != null) {
                viewGroup.removeView(mAdLayout);
            }
            mFBUsed = true;
            return mAdLayout;
        } else {
            ViewGroup viewGroup = (ViewGroup)mBannerView.getParent();
            if (viewGroup != null) {
                viewGroup.removeView(mBannerView);
            }
            mAdmobUsed = true;
            return mBannerView;
        }
    }

    private TimerTask loadAdTask = new TimerTask() {
        @Override
        public void run() {
            if (!TextUtils.isEmpty(mConfig.admob) && mConfig.weight < 1) {
                handler.sendEmptyMessageDelayed(1001, 0);
            }
            if (!TextUtils.isEmpty(mConfig.fb) && mConfig.weight > 0) {
                handler.sendEmptyMessageDelayed(1000, 0);
            }
        }
    };

    Handler handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1000) {
                loadFacebookAd();
            } else if (msg.what == 1001) {
                loadAdmobAd();
            }
        }
    };

    private void loadFacebookAd() {
        if (mFBLoaded) return;
        if (mFBRequested) return;

        if (System.currentTimeMillis() - lastLoadErrorTime < 1000 * 1800) {
            return;
        }

        mFBRequested = true;
        if (mNativeAd != null) {
            mNativeAd.destroy();
        }
//        AdSettings.addTestDevice("5aa7589ebe0d4ca38185e0d2378e8aad");
        mNativeAd = new NativeAd(mContext, mConfig.fb);
        mNativeAd.setAdListener(new com.facebook.ads.AdListener() {

            @Override
            public void onError(Ad ad, AdError error) {
                if (error.getErrorCode() == 1002) {
                    lastLoadErrorTime = System.currentTimeMillis();
                }
                mFBRequested = false;
                mFBLoaded = false;
            }

            @Override
            public void onAdLoaded(Ad ad) {
                // Ad loaded callback
                mFBLoaded = true;
                mFBRequested = false;
                if (mNativeAd != null) {
                    mNativeAd.unregisterView();
                }

                mAdLayout.removeAllViews();
                // Add the Ad view into the ad container.
                LayoutInflater inflater = LayoutInflater.from(mContext);
                // Inflate the Ad view.  The layout referenced should be the one you created in the last step.
                View adView = inflater.inflate(R.layout.native_ad_layout, mAdLayout, false);
                mAdLayout.addView(adView);
                mNativeAd.registerViewForInteraction(mAdLayout);

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

    private boolean _admobSet = false;
    private void loadAdmobAd() {
        if (mAdmobLoaded) return;
        if (mAdmobRequested) return;
        if (!_admobSet) {
            _admobSet = true;
            mBannerView.setAdUnitId(mConfig.admob);
            mBannerView.setAdSize(new AdSize(280, 320));
//            mBannerView.setAdSize(AdSize.MEDIUM_RECTANGLE);
        }
        mBannerView.setAdListener(new com.google.android.gms.ads.AdListener() {
            @Override
            public void onAdLoaded() {
                mAdmobRequested = false;
                mAdmobLoaded = true;
            }

            @Override
            public void onAdOpened() {

            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                mAdmobLoaded = false;
                mAdmobRequested = false;
            }

            @Override
            public void onAdClosed() {
            }
        });
        AdRequest adRequest = new AdRequest.Builder()
//                .addTestDevice("919D974C26F3EC724E027BE77DF536ED")
                .build();
        mBannerView.loadAd(adRequest);
    }
}
