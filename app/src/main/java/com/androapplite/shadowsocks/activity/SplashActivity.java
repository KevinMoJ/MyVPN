package com.androapplite.shadowsocks.activity;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.ad.AdUtils;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.service.ServerListFetcherService;
import com.androapplite.shadowsocks.service.VpnManageService;
import com.androapplite.shadowsocks.service.WarnDialogShowService;
import com.androapplite.shadowsocks.util.IabHelper;
import com.androapplite.shadowsocks.util.IabResult;
import com.androapplite.shadowsocks.util.Inventory;
import com.androapplite.shadowsocks.util.Purchase;
import com.androapplite.shadowsocks.utils.DensityUtil;
import com.androapplite.shadowsocks.utils.InternetUtil;
import com.androapplite.shadowsocks.utils.RealTimeLogger;
import com.androapplite.shadowsocks.utils.RuntimeSettings;
import com.androapplite.shadowsocks.utils.Utils;
import com.androapplite.shadowsocks.view.RoundProgress;
import com.androapplite.vpn3.R;
import com.bestgo.adsplugin.ads.AdAppHelper;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.vm.shadowsocks.core.LocalVpnService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class SplashActivity extends AppCompatActivity implements Handler.Callback,
        Animator.AnimatorListener, RoundProgress.endAnimListener, View.OnClickListener {
    private static final int MSG_AD_LOADED_CHECK = 1;
    private AdAppHelper mAdAppHelper;
    private Handler mHandler;


    //带动画的闪屏页---------------开始
    private final long CHECK_INTERNAL = 200; //检查间隔

    private static final int MSG_CHECK_ENTER = 100;
    private static final int MSG_SKIP = 101;
    private static final int MSG_SPLASH_SHOW = 102;
    private static final int MSG_OPINION_INTERNET_TYPE = 103;
    private static final int MSG_CHECK_FREE_USE_TIME = 104;
    private static final int MSG_CHECK_VIP = 105;

    private boolean splashReady;
    boolean wholeProcess = false;

    private ImageView mIvBg;
    private ImageView mIvScan;
    private RoundProgress mRpClose;
    private ImageView mIvLogo;
    private TextView mTvTitle;
    private FrameLayout mFlAd;
    private TextView mSkipBtn;

    private ObjectAnimator alphaAnim1;
    private ObjectAnimator alphaAnim2;
    private Animation animationScan;
    private ObjectAnimator jumpAnim1;
    private ObjectAnimator jumpAnim2;
    private ObjectAnimator textUpAnim;
    private ObjectAnimator stayAnim;
    private AnimatorSet downAnimSet;
    private ObjectAnimator adAnim;
    //带动画的闪屏页---------------结束

    private IabHelper mIabHelper;
    /*标记hhelper 是否启动*/
    private boolean isStartHelper;
    private final List<String> skuList = new ArrayList<>();

    private long luckFreeDay; // 幸运转盘得到的天数
    private long freeUseTime; // 免费试用的时间


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdAppHelper = AdAppHelper.getInstance(this);
        mAdAppHelper.loadFullAd(AdUtils.FULL_AD_GOOD, 5);
        mAdAppHelper.loadFullAd(AdUtils.FULL_AD_BAD, 50);
        mAdAppHelper.loadNewNative();
        mAdAppHelper.loadNewSplashAd();
        mHandler = new Handler(this);
        mHandler.sendEmptyMessage(MSG_CHECK_VIP);
        mHandler.sendEmptyMessage(MSG_CHECK_FREE_USE_TIME);

        if (FirebaseRemoteConfig.getInstance().getBoolean("is_show_animation_ad_flash")) {
            showAnimationAdFlash();
        } else {
            showNormalAdFlash();
        }

        WarnDialogShowService.start(this);
        SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
        sharedPreferences.edit().putInt("SPLASH_OPEN_COUNT", sharedPreferences.getInt("SPLASH_OPEN_COUNT", 0) + 1).apply();
        RealTimeLogger.answerLogEvent("splash_open", "open", "open_count:" + sharedPreferences.getInt("SPLASH_OPEN_COUNT", 0));
        if (!LocalVpnService.IsRunning)
            ServerListFetcherService.fetchServerListAsync(this);
        VpnManageService.start(this);
        Firebase firebase = Firebase.getInstance(this);
        firebase.logEvent("屏幕", "闪屏屏幕");
        Intent intent = getIntent();
        if (intent != null) {
            String source = intent.getStringExtra("source");
            if (source != null) {
                firebase.logEvent("打开app来源", source);
            } else {
                firebase.logEvent("打开app来源", "图标");
            }
        }

        mHandler.sendEmptyMessage(MSG_OPINION_INTERNET_TYPE);
    }

    private void checkFreeUseTime() {
        //用活跃用户的打开APP的时间判断上次用户打开APP的时间
        long openAppTime = RuntimeSettings.getOpenAppToDecideInactiveTime();
        luckFreeDay = RuntimeSettings.getLuckPanGetRecord();
        freeUseTime = RuntimeSettings.getNewUserFreeUseTime();
        long newUserFreeTime = FirebaseRemoteConfig.getInstance().getLong("new_user_free_use_time");
        long differ = System.currentTimeMillis() - openAppTime;

        if (luckFreeDay <= 0) { //没有转转盘获取时间或者获取的时间到期
            RuntimeSettings.setLuckPanGetRecord(0);
            if (differ > 0) {
                long dif = differ / 1000; // 上次打开APP的时间到这次的时间间隔
                if (dif <= newUserFreeTime * 60) {// 20  15
                    RuntimeSettings.setNewUserFreeUseTime(freeUseTime - dif);
                    long newFreeUseTime = RuntimeSettings.getNewUserFreeUseTime();
                    if (newFreeUseTime < 0)
                        RuntimeSettings.setNewUserFreeUseTime(0);
                } else {
                    RuntimeSettings.setNewUserFreeUseTime(0);
                }
            }
        } else { // 有幸运转盘转到的时间，但是这次打开APP要更新一下
            if (differ > 0) {
                long overDay = differ / (1000 * 60 * 60 * 24); // 上次打开APP的时间到现在的过了多少天
                if (overDay <= luckFreeDay)
                    RuntimeSettings.setLuckPanGetRecord(luckFreeDay - overDay);
                else
                    RuntimeSettings.setLuckPanGetRecord(0);
                long newFreeUseDay = RuntimeSettings.getLuckPanGetRecord();
                if (newFreeUseDay < 0)
                    RuntimeSettings.setLuckPanGetRecord(0);

                RuntimeSettings.setNewUserFreeUseTime(0);
            }
        }
    }

    private void checkIsVIP() {
//        skuList.add(VIPActivity.PAY_ONE_MONTH);// 添加消费的SKU，此字段在Google后台有保存，用来区别当前用户是否支付，字段是商品ID
//        skuList.add("one_2");
//        skuList.add(VIPActivity.PAY_HALF_YEAR);
//        skuList.add("half_1");
//        skuList.add(VIPActivity.PAY_ONE_YEAR);
//        skuList.add(VIPActivity.PAY_SEVEN_FREE);

        String priorityList = FirebaseRemoteConfig.getInstance().getString("pay_id_list");
        try {
            JSONArray rootJsonArray = new JSONArray(priorityList);
            for (int i = 0; i < rootJsonArray.length(); i++) {
                JSONObject jsonObject = (JSONObject) rootJsonArray.get(i);
                String payId = jsonObject.getString("id");
                skuList.add(payId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        mIabHelper = new IabHelper(this, VIPActivity.PUBLIC_KEY.trim());
        mIabHelper.enableDebugLogging(true);

        mIabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    //helper设置失败是没法支付的，此处可以弹出提示框
                    isStartHelper = false;
                    Log.i("SplashActivity", "onIabSetupFinished: 初始化失败 ");
                    return;
                } else {
                    isStartHelper = true;
                    Log.i("SplashActivity", "onIabSetupFinished: 初始化成功 ");
                }

                if (mIabHelper == null) return;

                // IAB is fully set up. Now, let's get an inventory of stuff we own.
                try {
                    //查询库存并查询可售商品详细信息
                    mIabHelper.queryInventoryAsync(true, skuList, null, mGotInventoryListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                }
            }
        });
    }

    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        @Override
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            if (mIabHelper == null) return;

            if (result.isFailure()) {
                //在商品仓库中查询失败
                Firebase.getInstance(SplashActivity.this).logEvent("欢迎页检查VIP", "查询失败");
//                Log.i("SplashActivity", "onQueryInventoryFinished: 查询失败 ");
                return;
            }

            for (String s : skuList) {
                Purchase purchase = inventory.getPurchase(s);
                if (purchase != null) {
                    if (purchase.getSku().contains("one")) {
                        Log.i("SplashActivity", "onIabPurchaseFinishedMain: We have goods");
                        Firebase.getInstance(SplashActivity.this).logEvent("欢迎页检查VIP", "查询成功", "一个月");
                        RuntimeSettings.setVIP(true);
                        RuntimeSettings.setVIPPayOneMonth(true);
                        RuntimeSettings.setVIPPayHalfYear(false);
                        RuntimeSettings.setAutoRenewalVIP(purchase.isAutoRenewing());
                        RuntimeSettings.setVIPPayTime(purchase.getPurchaseTime());
                    } else if (purchase.getSku().contains("half")) {
                        Log.i("SplashActivity", "onIabPurchaseFinishedMain: We have goods");
                        Firebase.getInstance(SplashActivity.this).logEvent("欢迎页检查VIP", "查询成功", "半年");
                        RuntimeSettings.setVIP(true);
                        RuntimeSettings.setVIPPayOneMonth(false);
                        RuntimeSettings.setVIPPayHalfYear(true);
                        RuntimeSettings.setAutoRenewalVIP(purchase.isAutoRenewing());
                        RuntimeSettings.setVIPPayTime(purchase.getPurchaseTime());
                    } else if (purchase.getSku().contains("year")) {
                        Log.i("SplashActivity", "onIabPurchaseFinishedMain: We have goods");
                        Firebase.getInstance(SplashActivity.this).logEvent("欢迎页检查VIP", "查询成功", "一年");
                        RuntimeSettings.setVIP(true);
                        RuntimeSettings.setVIPPayOneMonth(false);
                        RuntimeSettings.setVIPPayHalfYear(false);
                        RuntimeSettings.setAutoRenewalVIP(purchase.isAutoRenewing());
                        RuntimeSettings.setVIPPayTime(purchase.getPurchaseTime());
                    } else if (purchase.getSku().contains("free")) {
                        Log.i("SplashActivity", "onIabPurchaseFinishedMain: We have goods");
                        Firebase.getInstance(SplashActivity.this).logEvent("欢迎页检查VIP", "查询成功", "试用7天");
                        RuntimeSettings.setVIP(true);
                        RuntimeSettings.setVIPPayOneMonth(true);
                        RuntimeSettings.setVIPPayHalfYear(false);
                        RuntimeSettings.setAutoRenewalVIP(purchase.isAutoRenewing());
                        RuntimeSettings.setVIPPayTime(purchase.getPurchaseTime());
                    } else {
                        Firebase.getInstance(SplashActivity.this).logEvent("欢迎页检查VIP", "没查询到");
                        RuntimeSettings.setVIP(false);
                    }
                }
            }
        }
    };


    private void showAnimationAdFlash() {
        if (mAdAppHelper.isSplashReady()) {
            setContentView(R.layout.flash_ad_animation_layout);
            splashReady = true;
        } else {
            setContentView(R.layout.flash_ad_normal_layout);
            splashReady = false;
            mIvBg = (ImageView) findViewById(R.id.iv_bg);
            mIvScan = (ImageView) findViewById(R.id.iv_scan);
        }
        initAnimationFlashView();
        mRpClose.setEndAnimListener(this);

        if (splashReady)
            startNativeAnim();
        else
            startSplashAnim();

        if (mSkipBtn != null) {
            Message message = Message.obtain();
            message.what = MSG_SKIP;
            message.arg1 = 5;
            mHandler.sendMessageDelayed(message, 1000);
            mSkipBtn.setText(String.format("Skip %ss", 5));
            mSkipBtn.setOnClickListener(this);
        }
    }

    private void initAnimationFlashView() {
        mIvBg = (ImageView) findViewById(R.id.iv_bg);
        mIvLogo = (ImageView) findViewById(R.id.iv_logo);
        mTvTitle = (TextView) findViewById(R.id.tv_title);
        mRpClose = (RoundProgress) findViewById(R.id.rp_close);
        mFlAd = (FrameLayout) findViewById(R.id.fl_ad);
        mIvScan = (ImageView) findViewById(R.id.iv_scan);
        mSkipBtn = (Button) findViewById(R.id.skip_btn);
    }

    private void showNormalAdFlash() {
        setContentView(R.layout.activity_splash);
        Message msg = Message.obtain();
        msg.what = MSG_AD_LOADED_CHECK;
        msg.arg1 = 1;

        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.splash_ad_ll);
        LinearLayout centerLogoLL = (LinearLayout) findViewById(R.id.center_logo_ll);
        LinearLayout bottomLogoLL = (LinearLayout) findViewById(R.id.bottom_logo_ll);
        if (mAdAppHelper.isSplashReady() && !RuntimeSettings.isVIP()) {
            View view = mAdAppHelper.getSplashAd();
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP);
            frameLayout.addView(view, layoutParams);

            centerLogoLL.setVisibility(View.GONE);
            frameLayout.setVisibility(View.VISIBLE);
            bottomLogoLL.setVisibility(View.VISIBLE);

            mHandler.sendMessageDelayed(msg, 3000);
        } else {
            centerLogoLL.setVisibility(View.VISIBLE);
            frameLayout.setVisibility(View.GONE);
            bottomLogoLL.setVisibility(View.GONE);

            mHandler.sendMessageDelayed(msg, 1000);
        }
    }

    //加载Splash广告动画1.5s，下面的进度条开始走5s
    private void startNativeAnim() {
        AdAppHelper adAppHelper = AdAppHelper.getInstance(this);
        if (AdAppHelper.getInstance(this).isSplashReady()) {
            String welcomeAd = adAppHelper.getCustomCtrlValue("welcome_ad", "1");
            float welcomeAdRate;
            try {
                welcomeAdRate = Float.parseFloat(welcomeAd);
            } catch (Exception e) {
                welcomeAdRate = 0;
            }
            if (Math.random() < welcomeAdRate && !RuntimeSettings.isVIP()) {
                View view = adAppHelper.getSplashAd();
                Firebase.getInstance(this).logEvent("闪屏广告", "准备好", "显示");
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP);
                mFlAd.addView(view, layoutParams);

                String welcomeAdMinS = adAppHelper.getCustomCtrlValue("welcome_ad_min", "5000");
                int welcomeAdMin;
                try {
                    welcomeAdMin = Integer.valueOf(welcomeAdMinS);
                } catch (Exception e) {
                    welcomeAdMin = 0;
                }
                String welcomeAdMaxS = adAppHelper.getCustomCtrlValue("welcome_ad_max", "0");
                int welcomeAdMax;
                try {
                    welcomeAdMax = Integer.valueOf(welcomeAdMaxS);
                } catch (Exception e) {
                    welcomeAdMax = 0;
                }
                mHandler.sendEmptyMessageDelayed(MSG_SPLASH_SHOW, (long) (Math.random() * welcomeAdMax + welcomeAdMin));

            } else {
                mFlAd.setVisibility(View.GONE);
                Firebase.getInstance(this).logEvent("闪屏广告", "准备好", "不显示");
            }
        } else {
            mFlAd.setVisibility(View.GONE);
            Firebase.getInstance(this).logEvent("闪屏广告", "没准备好", "不显示");
        }

        adAnim = ObjectAnimator.ofFloat(mFlAd, "translationY",
                -Utils.getScreenHeight(this), 0, 20, -20, 10, -10, 5, -5, 0).setDuration(1500);
        adAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        adAnim.start();
        mRpClose.setVisibility(View.VISIBLE);
        mRpClose.startAnim(5000);
        if (!splashReady) {
            mHandler.sendEmptyMessageDelayed(MSG_CHECK_ENTER, CHECK_INTERNAL);
        }
    }

    //透明动画 1s
    private void startSplashAnim() {
        alphaAnim1 = ObjectAnimator.ofFloat(mIvBg, "alpha", 0, 1).setDuration(1000);
        alphaAnim2 = ObjectAnimator.ofFloat(mIvLogo, "alpha", 0, 1).setDuration(1000);
        AnimatorSet set = new AnimatorSet();
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mIvBg.setVisibility(View.VISIBLE);
                mIvLogo.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (mIvScan != null) {
                    mIvScan.clearAnimation();
                    mIvScan.setVisibility(View.GONE);
                }
                startLogoAnim();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        set.playTogether(alphaAnim1, alphaAnim2);
        set.start();
    }

    //上下跳的平移动画,水波纹扩散动画 3s
    private void startLogoAnim() {
        animationScan = AnimationUtils.loadAnimation(this, R.anim.anim_tobig);
        float dy = DensityUtil.dip2px(this, 8);
        jumpAnim1 = ObjectAnimator.ofFloat(mIvLogo, "translationY",
                -dy, dy, -dy, dy, -dy, dy, 0);
        jumpAnim2 = ObjectAnimator.ofFloat(mIvBg, "translationY",
                -dy, dy, -dy, dy, -dy, dy, 0);
        AnimatorSet set = new AnimatorSet();
        set.setDuration(3000);
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mIvScan.startAnimation(animationScan);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mIvScan.clearAnimation();
                mIvScan.setVisibility(View.GONE);
                startTextUpAnim();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        set.playTogether(jumpAnim1, jumpAnim2);
        set.start();
    }

    //文字上升 平移动画 1s，全屏加载好直接跳
    private void startTextUpAnim() {
        textUpAnim = ObjectAnimator.ofFloat(mTvTitle, "translationY",
                Utils.getScreenHeight(this), 0).setDuration(1000);
        textUpAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        textUpAnim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mTvTitle.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (wholeProcess) {
                    startLockAnim();
                } else {
                    mTvTitle.setVisibility(View.VISIBLE);
                    if (AdAppHelper.getInstance(getApplicationContext()).isFullAdLoaded(AdUtils.FULL_AD_GOOD)) {
                        startMainActivity();
                    } else if (!Utils.isConnected(SplashActivity.this)) {
                        startMainActivity();
                    } else {
                        startLockAnim();
                    }
                }

            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        textUpAnim.start();
    }

    //原地停留1.5秒 透明动画，全屏加载好直接跳
    private void startLockAnim() {
        stayAnim = ObjectAnimator.ofFloat(mIvLogo, "alpha", 1.0f, 1.0f).setDuration(1500);
        stayAnim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (wholeProcess) {
                    startTextDownAnim();
                } else {
                    if (AdAppHelper.getInstance(getApplicationContext()).isFullAdLoaded(AdUtils.FULL_AD_GOOD)) {
                        startMainActivity();
                    } else {
                        startTextDownAnim();
                    }
                }

            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        stayAnim.start();
    }

    //文字、图标移下去动画,1s
    private void startTextDownAnim() {
        ObjectAnimator tranlationAnimX = ObjectAnimator.ofFloat(mIvBg, "translationX",
                0, (DensityUtil.dip2px(this, 150) - Utils.getScreenWidth(this) / 2.0f));
        ObjectAnimator tranlationAnimY = ObjectAnimator.ofFloat(mIvBg, "translationY",
                0, (mRpClose.getY() - mIvBg.getY() - mIvBg.getHeight() * 0.97f));

        ObjectAnimator tranlationAnimX2 = ObjectAnimator.ofFloat(mIvLogo, "translationX",
                0, (DensityUtil.dip2px(this, 150) - Utils.getScreenWidth(this) / 2.0f));
        ObjectAnimator tranlationAnimY2 = ObjectAnimator.ofFloat(mIvLogo, "translationY",
                0, (mRpClose.getY() - mIvLogo.getY() - mIvLogo.getHeight() * 1.5f));

        ObjectAnimator tranlationAnimX1 = ObjectAnimator.ofFloat(mTvTitle, "translationX",
                0, DensityUtil.dip2px(this, 50));
        ObjectAnimator tranlationAnimY1 = ObjectAnimator.ofFloat(mTvTitle, "translationY",
                0, (mRpClose.getY() - mTvTitle.getY() - mTvTitle.getHeight() * 4.0f));
        ObjectAnimator scaleAnimX = ObjectAnimator.ofFloat(mIvBg, "scaleX", 1f, 0.6f);
        ObjectAnimator scaleAnimY = ObjectAnimator.ofFloat(mIvBg, "scaleY", 1f, 0.6f);
        ObjectAnimator scaleAnimX1 = ObjectAnimator.ofFloat(mIvLogo, "scaleX", 1f, 0.5f);
        ObjectAnimator scaleAnimY1 = ObjectAnimator.ofFloat(mIvLogo, "scaleY", 1f, 0.5f);
        downAnimSet = new AnimatorSet();
        downAnimSet.setInterpolator(new AccelerateDecelerateInterpolator());
        downAnimSet.playTogether(tranlationAnimX, tranlationAnimX1, tranlationAnimX2, tranlationAnimY,
                tranlationAnimY1, tranlationAnimY2, scaleAnimX, scaleAnimY, scaleAnimX1, scaleAnimY1);
        downAnimSet.setDuration(1000);
        downAnimSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (wholeProcess) {
                    startNativeAnim();
                } else {
                    if (AdAppHelper.getInstance(getApplicationContext()).isFullAdLoaded(AdUtils.FULL_AD_GOOD)) {
                        startMainActivity();
                    } else {
                        startNativeAnim();
                    }
                }

            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        downAnimSet.start();
    }

    private void cancelAllAnim() {
        if (alphaAnim1 != null && alphaAnim1.isRunning()) {
            alphaAnim1.cancel();
        }
        if (alphaAnim2 != null && alphaAnim2.isRunning()) {
            alphaAnim2.cancel();
        }
        if (animationScan != null) {
            animationScan.cancel();
        }
        if (jumpAnim1 != null && jumpAnim1.isRunning()) {
            jumpAnim1.cancel();
        }
        if (jumpAnim2 != null && jumpAnim2.isRunning()) {
            jumpAnim2.cancel();
        }
        if (textUpAnim != null && textUpAnim.isRunning()) {
            textUpAnim.cancel();
        }
        if (stayAnim != null && stayAnim.isRunning()) {
            stayAnim.cancel();
        }
        if (downAnimSet != null && downAnimSet.isRunning()) {
            downAnimSet.cancel();
        }
        if (adAnim != null && adAnim.isRunning()) {
            adAnim.cancel();
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_AD_LOADED_CHECK:
                Log.d("SplanshActivity", "mAdAppHelper.isFullAdLoaded() " + mAdAppHelper.isFullAdLoaded(AdUtils.FULL_AD_GOOD));
                if (mAdAppHelper.isFullAdLoaded(AdUtils.FULL_AD_GOOD) || msg.arg1 > 4) {
                    startMainActivity();
                } else {
                    Message message = Message.obtain();
                    message.what = MSG_AD_LOADED_CHECK;
                    message.arg1 = 1 + msg.arg1;
                    mHandler.sendMessageDelayed(message, 1000);
                }
                break;

            case MSG_CHECK_ENTER:
                if (AdAppHelper.getInstance(getApplicationContext()).isFullAdLoaded(AdUtils.FULL_AD_GOOD)) {
                    mRpClose.cancelAnimator();
                    startMainActivity();
                } else
                    mHandler.sendEmptyMessageDelayed(MSG_CHECK_ENTER, CHECK_INTERNAL);
                break;

            case MSG_SKIP:
                int second = msg.arg1 - 1;
                if (second > 0) {
                    if (mSkipBtn != null) {
                        mSkipBtn.setText(String.format("Skip %ss", second));
                        Message message = Message.obtain();
                        message.what = MSG_SKIP;
                        message.arg1 = second;
                        mHandler.sendMessageDelayed(message, 1000);
                    }
                }
                break;

            case MSG_SPLASH_SHOW:
                if (mFlAd != null) {
                    mFlAd.setVisibility(View.GONE);
                }
                break;

            case MSG_OPINION_INTERNET_TYPE:
                Firebase.getInstance(this).logEvent("当前网络类型", "类型", InternetUtil.getNetworkState(SplashActivity.this));
                break;

            case MSG_CHECK_FREE_USE_TIME:
                checkFreeUseTime();
                break;
            case MSG_CHECK_VIP:
                try {
                    checkIsVIP();
                } catch (Exception e) {
                }
                break;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelAllAnim();
        if (mHandler != null)
            mHandler.removeCallbacksAndMessages(null);

        if (mRpClose != null)
            mRpClose.cancelAnimator();

        if (isStartHelper) {
            if (mIabHelper != null) {
                mIabHelper.disposeWhenFinished();
                mIabHelper = null;
            }
        }
    }

    @Override
    public void onAnimationStart(Animator animation) {

    }

    @Override
    public void onAnimationEnd(Animator animation) {
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
        finish();
    }

    @Override
    public void onAnimationCancel(Animator animation) {

    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }

    @Override
    public void endAnim() {
        mHandler.removeCallbacksAndMessages(null);
        if (mAdAppHelper.isFullAdLoaded(AdUtils.FULL_AD_GOOD)) {
            startMainActivity();
        } else {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startMainActivity();
                }
            }, 1000);
        }
    }

    private void startMainActivity() {
        Intent intent = null;
        if (RuntimeSettings.isVIP())
            intent = new Intent(this, MainActivity.class);
        else if (RuntimeSettings.getLuckPanGetRecord() > 0)
            intent = new Intent(this, MainActivity.class);
        else if (RuntimeSettings.getNewUserFreeUseTime() > 0)
            intent = new Intent(this, MainActivity.class);
        else
            intent = new Intent(this, RecommendVIPActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.skip_btn:
                startMainActivity();
                Firebase.getInstance(this).logEvent("广告界面点击跳过广告", "跳过");
                break;
        }
    }
}
