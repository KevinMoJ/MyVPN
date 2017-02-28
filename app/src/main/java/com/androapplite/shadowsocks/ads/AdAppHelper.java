package com.androapplite.shadowsocks.ads;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import com.androapplite.shadowsocks.GAHelper;
import com.umeng.analytics.game.UMGameAgent;
import com.umeng.analytics.onlineconfig.UmengOnlineConfigureListener;

import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Random;

public class AdAppHelper {
    private static AdAppHelper _this;
    private Context context;
    private String NAME = "AdAppHelper";
    private SharedPreferences mSP;

    private AdMobAd mAdMobAd;
    private FacebookAd mFacebookAd;

    private AdConfig mAdConfig;

    public static AdAppHelper getInstance(Context ctx) {
        if (_this == null) {
            _this = new AdAppHelper();
            _this.context = ctx;
        }
        return _this;
    }

    private void writeAssetsToSP() throws IOException {
        InputStream is = context.getAssets().open("config.xml");
        String packageName = context.getPackageName();
        String sharedSpFile = "/data/data/" + packageName + "/shared_prefs/ourdefault_game_config.xml";
        OutputStream os = new FileOutputStream(sharedSpFile);
        byte[] bytes = new byte[1024];
        int len = is.read(bytes);
        while (len > 0) {
            os.write(bytes, 0, len);
            len = is.read(bytes);
        }
        is.close();
        os.flush();
        os.close();
    }

    public void init() {
        int pid = android.os.Process.myPid();
        String processName = "";
        ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo appProcess : mActivityManager.getRunningAppProcesses()) {
            if (appProcess.pid == pid) {
                processName = appProcess.processName;
                break;
            }
        }
        String packageName = context.getPackageName();
        if (processName.equals(packageName)) {
            mAdConfig = new AdConfig();
            mSP = context.getSharedPreferences("ad_app_helper", 0);
            initUmeng();
            initAd();
        }
    }

    public void onResume() {
        UMGameAgent.onResume(context);
        mAdMobAd.onResume();
    }

    public void onPause() {
        UMGameAgent.onPause(context);
        mAdMobAd.onPause();
    }

    private final static int INIT_AD = 1000;
    private final static int SHOW_FULL_AD = 1001;
    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == INIT_AD) {
                try {
                    initAd();
                } catch (Exception ex) {
                }
            } else if (msg.what == SHOW_FULL_AD) {
                showFullAdDelayed();
            }
        }
    };

    private void initUmeng() {
        UMGameAgent.init(context);
        UMGameAgent.setTraceSleepTime(false);
        UMGameAgent.setSessionContinueMillis(60000L);

        String adIds = UMGameAgent.getConfigParams(context, "ad_ids");
        if (TextUtils.isEmpty(adIds)) {
            try {
                writeAssetsToSP();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        readUmeng();
        UMGameAgent.updateOnlineConfig(context);
        UMGameAgent.setOnlineConfigureListener(new UmengOnlineConfigureListener() {
            public void onDataReceived(JSONObject data) {
                try {
                    readUmeng();
                    handler.sendEmptyMessage(INIT_AD);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void readUmeng() {
        HashMap<String, String> kv = AdConfig.getValues(context, "ad_ids");
        mAdConfig.ad_ids.admob = AdConfig.getString(kv, "admob", "");
        mAdConfig.ad_ids.admob_full = AdConfig.getString(kv, "admob_full", "");
        mAdConfig.ad_ids.admob_n = AdConfig.getString(kv, "admob_n", "");
        mAdConfig.ad_ids.fb = AdConfig.getString(kv, "fb", "");
        mAdConfig.ad_ids.fb_f = AdConfig.getString(kv, "fb_f", "");
        mAdConfig.ad_ids.fb_n = AdConfig.getString(kv, "fb_n", "");
        String v = AdConfig.getString(kv, "fbns", "");
        try {
            mAdConfig.ad_ids.fbns_banner = v.split(",")[0];
            mAdConfig.ad_ids.fbns_full = v.split(",")[1];
        } catch (Exception ex) {
        }

        kv = AdConfig.getValues(context, "ngs_ctrl");
        mAdConfig.ngs_ctrl.exe = AdConfig.getInt(kv, "exe", 1);
        mAdConfig.ngs_ctrl.admob = AdConfig.getInt(kv, "admob", 100);
        mAdConfig.ngs_ctrl.facebook = AdConfig.getInt(kv, "facebook", 100);
        mAdConfig.ngs_ctrl.fbn = AdConfig.getInt(kv, "fbn", 100);

        kv= AdConfig.getValues(context, "banner_ctrl");
        mAdConfig.banner_ctrl.exe = AdConfig.getInt(kv, "exe", 1);
        mAdConfig.banner_ctrl.admob = AdConfig.getInt(kv, "admob", 100);
        mAdConfig.banner_ctrl.facebook = AdConfig.getInt(kv, "facebook", 100);
        mAdConfig.banner_ctrl.fbn = AdConfig.getInt(kv, "fbn", 100);

        kv = AdConfig.getValues(context, "native_ctrl");
        mAdConfig.native_ctrl.exe = AdConfig.getInt(kv, "exe", 1);
        mAdConfig.native_ctrl.admob = AdConfig.getInt(kv, "admob", 100);
        mAdConfig.native_ctrl.facebook = AdConfig.getInt(kv, "facebook", 100);

        kv = AdConfig.getValues(context, "ad_count_ctrl");
        mAdConfig.ad_count_ctrl.exe = AdConfig.getInt(kv, "exe", 1);
        mAdConfig.ad_count_ctrl.full_interval = AdConfig.getInt(kv, "full_interval", 0);
        mAdConfig.ad_count_ctrl.full_count = AdConfig.getInt(kv, "full_count", 1000);
        mAdConfig.ad_count_ctrl.banner_interval = AdConfig.getInt(kv, "banner_interval", 0);
        mAdConfig.ad_count_ctrl.banner_count = AdConfig.getInt(kv, "banner_count", 1000);
        mAdConfig.ad_count_ctrl.native_interval = AdConfig.getInt(kv, "native_interval", 0);
        mAdConfig.ad_count_ctrl.native_count = AdConfig.getInt(kv, "native_count", 1000);

        kv = AdConfig.getValues(context, "ad_ctrl");
        mAdConfig.ad_ctrl.ad_delay = AdConfig.getInt(kv, "ad_delay", 0);
        String value = AdConfig.getString(kv, "ngsorder", "");
        parseNgsOrder(mAdConfig.ad_ctrl.ngsorder, value);
        value = AdConfig.getString(kv, "ngsorder:admob", "");
        parseNgsOrder(mAdConfig.ad_ctrl.ngsorder_admob, value);

        mAdConfig.ad_count_ctrl.last_day = mSP.getInt("last_day", 0);
        mAdConfig.ad_count_ctrl.last_full_show_time = mSP.getLong("last_full_show_time", 0);
        mAdConfig.ad_count_ctrl.last_full_show_count = mSP.getInt("last_full_show_count", 0);
        mAdConfig.ad_count_ctrl.last_banner_show_time = mSP.getLong("last_banner_show_time", 0);
        mAdConfig.ad_count_ctrl.last_banner_show_count = mSP.getInt("last_banner_show_count", 0);
        mAdConfig.ad_count_ctrl.last_native_show_time = mSP.getLong("last_native_show_time", 0);
        mAdConfig.ad_count_ctrl.last_native_show_count = mSP.getInt("last_native_show_count", 0);
        if (mAdConfig.ad_count_ctrl.last_day != Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) {
            SharedPreferences.Editor editor = mSP.edit();
            editor.putInt("last_day", Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
            mAdConfig.ad_count_ctrl.last_full_show_count = 0;
            mAdConfig.ad_count_ctrl.last_banner_show_count = 0;
            mAdConfig.ad_count_ctrl.last_native_show_count = 0;
            editor.putInt("last_full_show_count", 0);
            editor.putInt("last_banner_show_count", 0);
            editor.putInt("last_native_show_count", 0);
            editor.commit();
        }
    }

    private void parseNgsOrder(AdConfig.AdCtrl.NgsOrder ngsOrder, String value) {
        String[] ngsorder = value.split("\\|");
        for (int i = 0; i < ngsorder.length; i++) {
            String one = ngsorder[i];
            if (one.contains("before:")) {
                String vv = one.replace("before:", "");
                try {
                    ngsOrder.before = Integer.parseInt(vv);
                } catch (Exception ex) {
                }
            } else if (one.contains("adt:")) {
                String vv = one.replace("adt:", "");
                try {
                    ngsOrder.adt = Integer.parseInt(vv);
                } catch (Exception ex) {
                }
            } else if (one.contains("adtype=")) {
                String vv = one.replace("adtype=", "");
                try {
                    ngsOrder.adt_type = Integer.parseInt(vv);
                } catch (Exception ex) {
                }
            }
        }
    }

    private void initAd() {
        if (mAdMobAd == null) {
            mAdMobAd = new AdMobAd(context, mAdConfig.ad_ids.admob, mAdConfig.ad_ids.admob_n, mAdConfig.ad_ids.admob_full);
            mAdMobAd.setBannerEnabled(mAdConfig.banner_ctrl.exe == 1 && mAdConfig.banner_ctrl.admob > 0);
            mAdMobAd.setNativeEnabled(mAdConfig.native_ctrl.exe == 1 && mAdConfig.native_ctrl.admob > 0);
            mAdMobAd.setInterstitialEnabled(mAdConfig.ngs_ctrl.exe == 1 && mAdConfig.ngs_ctrl.admob > 0);
        } else {
            mAdMobAd.setBannerEnabled(mAdConfig.banner_ctrl.exe == 1 && mAdConfig.banner_ctrl.admob > 0);
            mAdMobAd.setNativeEnabled(mAdConfig.native_ctrl.exe == 1 && mAdConfig.native_ctrl.admob > 0);
            mAdMobAd.setInterstitialEnabled(mAdConfig.ngs_ctrl.exe == 1 && mAdConfig.ngs_ctrl.admob > 0);
            mAdMobAd.resetId(mAdConfig.ad_ids.admob, mAdConfig.ad_ids.admob_n, mAdConfig.ad_ids.admob_full);
        }

        if (mFacebookAd == null) {
            mFacebookAd = new FacebookAd(context, mAdConfig.ad_ids.fb, mAdConfig.ad_ids.fb_n, mAdConfig.ad_ids.fb_f, mAdConfig.ad_ids.fbns_full);
            mFacebookAd.setBannerEnabled(mAdConfig.banner_ctrl.exe == 1 && mAdConfig.banner_ctrl.facebook > 0);
            mFacebookAd.setNativeEnabled(mAdConfig.native_ctrl.exe == 1 && mAdConfig.native_ctrl.facebook > 0);
            mFacebookAd.setInterstitialEnabled(mAdConfig.ngs_ctrl.exe == 1 && mAdConfig.ngs_ctrl.facebook > 0);
            mFacebookAd.setFBNEnabled(mAdConfig.ngs_ctrl.exe == 1 && mAdConfig.ngs_ctrl.fbn > 0);
        } else {
            mFacebookAd.setBannerEnabled(mAdConfig.banner_ctrl.exe == 1 && mAdConfig.banner_ctrl.facebook > 0);
            mFacebookAd.setNativeEnabled(mAdConfig.native_ctrl.exe == 1 && mAdConfig.native_ctrl.facebook > 0);
            mFacebookAd.setInterstitialEnabled(mAdConfig.ngs_ctrl.exe == 1 && mAdConfig.ngs_ctrl.facebook > 0);
            mFacebookAd.setFBNEnabled(mAdConfig.ngs_ctrl.exe == 1 && mAdConfig.ngs_ctrl.fbn > 0);
            mFacebookAd.resetId(mAdConfig.ad_ids.fb, mAdConfig.ad_ids.fb_n, mAdConfig.ad_ids.fb_f, mAdConfig.ad_ids.fbns_full);
        }
        mAdMobAd.setAdListener(listener);
        mFacebookAd.setAdListener(listener);
    }

    private AdListener listener = new AdListener() {
        @Override
        public void onAdLoaded(AdType adType) {
            switch (adType.getType()) {
                case AdType.ADMOB_BANNER:
                    break;
                case AdType.ADMOB_NATIVE:
                    break;
                case AdType.ADMOB_FULL:
                    UMGameAgent.onEvent(context, "jzcg_admob");
                    GAHelper.sendEvent(context, "广告", "加载成功", "Admob全屏");
                    break;
                case AdType.FACEBOOK_BANNER:
                    break;
                case AdType.FACEBOOK_NATIVE:
                    break;
                case AdType.FACEBOOK_FULL:
                    UMGameAgent.onEvent(context, "jzcg_facebook");
                    GAHelper.sendEvent(context, "广告", "加载成功", "Facebook全屏");
                    break;
            }
        }

        @Override
        public void onAdOpen(AdType adType) {
            switch (adType.getType()) {
                case AdType.ADMOB_BANNER:
                    break;
                case AdType.ADMOB_NATIVE:
                    break;
                case AdType.ADMOB_FULL:
                    UMGameAgent.onEvent(context, "cgzs_admob");
                    GAHelper.sendEvent(context, "广告", "成功展示", "Admob全屏");
                    break;
                case AdType.FACEBOOK_BANNER:
                    break;
                case AdType.FACEBOOK_NATIVE:
                    break;
                case AdType.FACEBOOK_FULL:
                    UMGameAgent.onEvent(context, "cgzs_facebook");
                    GAHelper.sendEvent(context, "广告", "成功展示", "Facebook全屏");
                    break;
            }
        }
    };

    public View getBanner() {
        if (mAdConfig.ad_count_ctrl.exe == 1) {
            long now = System.currentTimeMillis();
            if (now - mAdConfig.ad_count_ctrl.last_banner_show_time < mAdConfig.ad_count_ctrl.banner_interval
                    || mAdConfig.ad_count_ctrl.last_banner_show_count >= mAdConfig.ad_count_ctrl.banner_count) {
                return null;
            }
            SharedPreferences.Editor editor = mSP.edit();
            mAdConfig.ad_count_ctrl.last_banner_show_time = now;
            mAdConfig.ad_count_ctrl.last_banner_show_count++;
            editor.putLong("last_banner_show_time", mAdConfig.ad_count_ctrl.last_banner_show_time);
            editor.putInt("last_banner_show_count", mAdConfig.ad_count_ctrl.last_banner_show_count);
            editor.commit();
        }

        if (mAdConfig.banner_ctrl.facebook > mAdConfig.banner_ctrl.admob && mFacebookAd.isBannerLoaded()) {
            View banner = mFacebookAd.getBanner();
            if (banner != null) {
                ViewGroup parent = (ViewGroup) banner.getParent();
                if (parent != null) {
                    parent.removeAllViews();
                }
                banner.setVisibility(View.VISIBLE);
            }
            return banner;
        } else {
            View banner = mAdMobAd.getBanner();
            if (banner != null) {
                ViewGroup parent = (ViewGroup) banner.getParent();
                if (parent != null) {
                    parent.removeAllViews();
                }
                banner.setVisibility(View.VISIBLE);
            }
            return banner;
        }
    }

    public View getNative() {
        if (mAdConfig.ad_count_ctrl.exe == 1) {
            long now = System.currentTimeMillis();
            if (now - mAdConfig.ad_count_ctrl.last_native_show_time < mAdConfig.ad_count_ctrl.native_interval
                    || mAdConfig.ad_count_ctrl.last_native_show_count >= mAdConfig.ad_count_ctrl.native_count) {
                return null;
            }
            SharedPreferences.Editor editor = mSP.edit();
            mAdConfig.ad_count_ctrl.last_native_show_time = now;
            mAdConfig.ad_count_ctrl.last_native_show_count++;
            editor.putLong("last_native_show_time", mAdConfig.ad_count_ctrl.last_native_show_time);
            editor.putInt("last_native_show_count", mAdConfig.ad_count_ctrl.last_native_show_count);
            editor.commit();
        }

        if (mAdConfig.native_ctrl.facebook > mAdConfig.native_ctrl.admob && mFacebookAd.isNativeLoaded()) {
            View view = mFacebookAd.getNative();
            if (view != null) {
                ViewGroup parent = (ViewGroup) view.getParent();
                if (parent != null) {
                    parent.removeAllViews();
                }
            }
            return view;
        } else {
            View view = mAdMobAd.getNative();
            if (view != null) {
                ViewGroup parent = (ViewGroup) view.getParent();
                if (parent != null) {
                    parent.removeAllViews();
                }
            }
            return view;
        }
    }

    public boolean isNativeLoaded() {
        return mFacebookAd.isNativeLoaded() || mAdMobAd.isNativeLoaded();
    }

    public boolean isFullAdLoaded() {
        return mFacebookAd.isInterstitialLoaded() || mAdMobAd.isInterstitialLoaded() || mFacebookAd.isFBNLoaded();
    }

    public void showFullAd() {
        if (mAdConfig.ad_count_ctrl.exe == 1) {
            long now = System.currentTimeMillis();
            if (now - mAdConfig.ad_count_ctrl.last_full_show_time < mAdConfig.ad_count_ctrl.full_interval
                    || mAdConfig.ad_count_ctrl.last_full_show_count >= mAdConfig.ad_count_ctrl.full_count) {
                return;
            }
            SharedPreferences.Editor editor = mSP.edit();
            mAdConfig.ad_count_ctrl.last_full_show_time = now;
            mAdConfig.ad_count_ctrl.last_full_show_count++;
            editor.putLong("last_full_show_time", mAdConfig.ad_count_ctrl.last_full_show_time);
            editor.putInt("last_full_show_count", mAdConfig.ad_count_ctrl.last_full_show_count);
            editor.commit();
        }
        Random random = new Random();
        if (mAdConfig.ad_ctrl.ngsorder.adt_type == 1 && (mFacebookAd.isFBNLoaded() || mFacebookAd.isInterstitialLoaded())) {
            int fbn_show_count = mSP.getInt("fbn_show_count", 0);
            int facebook_show_count = mSP.getInt("facebook_show_count", 0);
            if (mAdConfig.ad_ctrl.ngsorder.before >= (fbn_show_count + facebook_show_count)) {
                handler.sendEmptyMessageDelayed(SHOW_FULL_AD, 0);
            } else {
                int r = mAdConfig.ad_ctrl.ngsorder.adt > 0 ? random.nextInt(mAdConfig.ad_ctrl.ngsorder.adt) : 0;
                handler.sendEmptyMessageDelayed(SHOW_FULL_AD, r);
            }
        } else if (mAdConfig.ad_ctrl.ngsorder_admob.adt_type == 1 && mAdMobAd.isInterstitialLoaded()) {
            int admob_show_count = mSP.getInt("admob_show_count", 0);
            if (mAdConfig.ad_ctrl.ngsorder_admob.before >= admob_show_count) {
                handler.sendEmptyMessageDelayed(SHOW_FULL_AD, 0);
            } else {
                int r = mAdConfig.ad_ctrl.ngsorder_admob.adt > 0 ? random.nextInt(mAdConfig.ad_ctrl.ngsorder_admob.adt) : 0;
                handler.sendEmptyMessageDelayed(SHOW_FULL_AD, r);
            }
        } else {
            int r = mAdConfig.ad_ctrl.ad_delay > 0 ? random.nextInt(mAdConfig.ad_ctrl.ad_delay) : 0;
            handler.sendEmptyMessageDelayed(SHOW_FULL_AD, r);
        }
    }

    private void showFullAdDelayed() {
        int max = mAdConfig.ngs_ctrl.facebook + mAdConfig.ngs_ctrl.admob + mAdConfig.ngs_ctrl.fbn;
        if (max < 100) {
            max = 100;
        }
        Random random = new Random();
        int r = random.nextInt(max);
        if (r >= (mAdConfig.ngs_ctrl.facebook + mAdConfig.ngs_ctrl.admob) && mFacebookAd.isFBNLoaded()) {
            UMGameAgent.onEvent(context, "dk_fbn");
            GAHelper.sendEvent(context, "广告", "打开", "FacebookFBN");
            mFacebookAd.showFBNAd();
            int fbn_show_count = mSP.getInt("fbn_show_count", 0);
            SharedPreferences.Editor editor = mSP.edit();
            editor.putInt("fbn_show_count", ++fbn_show_count).apply();
        } else if (r > mAdConfig.ngs_ctrl.admob && r <= (max - mAdConfig.ngs_ctrl.admob - mAdConfig.ngs_ctrl.fbn)
                && mFacebookAd.isInterstitialLoaded()) {
            UMGameAgent.onEvent(context, "dk_facebook");
            GAHelper.sendEvent(context, "广告", "打开", "Facebook全屏");
            mFacebookAd.showInterstitial();
            int facebook_show_count = mSP.getInt("facebook_show_count", 0);
            SharedPreferences.Editor editor = mSP.edit();
            editor.putInt("facebook_show_count", ++facebook_show_count).apply();
        } else if (mAdMobAd.isInterstitialLoaded()) {
            UMGameAgent.onEvent(context, "dk_admob");
            GAHelper.sendEvent(context, "广告", "打开", "Admob全屏");
            mAdMobAd.showInterstitial();
            int admob_show_count = mSP.getInt("admob_show_count", 0);
            SharedPreferences.Editor editor = mSP.edit();
            editor.putInt("admob_show_count", ++admob_show_count).apply();
        } else if (mFacebookAd.isInterstitialLoaded()) {
            UMGameAgent.onEvent(context, "dk_facebook");
            GAHelper.sendEvent(context, "广告", "打开", "Facebook全屏");
            mFacebookAd.showInterstitial();
            int facebook_show_count = mSP.getInt("facebook_show_count", 0);
            SharedPreferences.Editor editor = mSP.edit();
            editor.putInt("facebook_show_count", ++facebook_show_count).apply();
        }  else if (mFacebookAd.isFBNLoaded()) {
            UMGameAgent.onEvent(context, "dk_fbn");
            GAHelper.sendEvent(context, "广告", "打开", "FacebookFBN");
            mFacebookAd.showFBNAd();
            int fbn_show_count = mSP.getInt("fbn_show_count", 0);
            SharedPreferences.Editor editor = mSP.edit();
            editor.putInt("fbn_show_count", ++fbn_show_count).apply();
        } else {
            UMGameAgent.onEvent(context, "ad_not_ready");
            GAHelper.sendEvent(context, "广告", "广告没有准备好");
            loadNewInterstitial();
        }
    }

    public void loadNewBanner() {
        mAdMobAd.loadNewBanner();
        mFacebookAd.loadNewBanner();
    }

    public void loadNewNative() {
        mAdMobAd.loadNewNativeAd();
        mFacebookAd.loadNewNativeAd();
    }

    public void loadNewInterstitial() {
        mAdMobAd.loadNewInterstitial();
        mFacebookAd.loadNewInterstitial();
        mFacebookAd.loadNewFBNAd();
    }
}
