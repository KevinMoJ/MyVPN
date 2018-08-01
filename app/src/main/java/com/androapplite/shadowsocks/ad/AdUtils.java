package com.androapplite.shadowsocks.ad;

import android.content.Context;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.utils.Rotate3dAnimation;
import com.androapplite.vpn3.R;
import com.bestgo.adsplugin.ads.AdAppHelper;
import com.bestgo.adsplugin.ads.AdType;
import com.bestgo.adsplugin.ads.listener.AdAutoShowListener;
import com.bestgo.adsplugin.ads.listener.AdReadyListener;
import com.bestgo.adsplugin.ads.listener.AdStateListener;
import com.crashlytics.android.Crashlytics;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.google.firebase.FirebaseApp;

import io.fabric.sdk.android.Fabric;

/**
 * 作者：shaobo on 2018/3/16 19:13
 * 邮箱：zhanshaobo1992@gmail.com
 * 描述：
 */

public class AdUtils {
    private static final String TAG = "AdUtils";
    public static final int FULL_AD_GOOD = 0;
    public static final int FULL_AD_BAD = 1;
    private static AdAppHelper adAppHelper;
    public static boolean isGoodFullAdReady;
    public static boolean isBadFullAdReady;

    public static void initAdHelper(final Context context) {
        //初始化facebook sdk
        FacebookSdk.sdkInitialize(context);
        //开启facebook应用分析
        AppEventsLogger.activateApp(context);
        Fabric.with(context, new Crashlytics());
        FirebaseApp.initializeApp(context);
        adAppHelper = AdAppHelper.getInstance(context);
//        AdAppHelper.FACEBOOK = FacebookAnalytics.getInstance(context);
        try {
            initAd(context);
            adAppHelper.init("");

            adAppHelper.setAdAutoShowListener(new AdAutoShowListener() {
                @Override
                public void onFullAdReady() {
                    Log.i(TAG, "onFullAdReady:  广告准备好 ");
                }
            });
            //添加监听广告成功回调
            adAppHelper.addAdStateListener(new AdStateListener() {

                @Override
                public void onAdOpen(AdType adType, int index) {
                    super.onAdOpen(adType, index);
                    Log.i(TAG, "onAdOpen: 广告打开  ");
                }

                @Override
                public void onAdOpen(AdType adType, int index, String fullAdName) {
                    super.onAdOpen(adType, index, fullAdName);
                    fullAdName = !TextUtils.isEmpty(fullAdName) ? fullAdName : "";
                    switch (fullAdName) {
                        case AdFullType.MAIN_ENTER_FULL_AD:
                            Firebase.getInstance(context).logEvent("全屏广告", "主页进入", "广告显示");
                            isGoodFullAdReady = false;
                            break;
                        case AdFullType.MAIN_EXIT_FULL_AD:
                            Firebase.getInstance(context).logEvent("全屏广告", "主页退出", "广告显示");
                            isGoodFullAdReady = false;
                            break;
                        case AdFullType.CONNECT_SUCCESS_FULL_AD:
                            Firebase.getInstance(context).logEvent("全屏广告", "VPN连接成功", "广告显示");
                            isGoodFullAdReady = false;
                            break;
                        case AdFullType.LUCK_ROTATE_ENTER_FULL_AD:
                            Firebase.getInstance(context).logEvent("全屏广告", "转盘进入", "广告显示");
                            isBadFullAdReady = false;
                            break;
                        case AdFullType.LUCK_ROTATE_PLAY_FULL_AD:
                            Firebase.getInstance(context).logEvent("全屏广告", "玩转盘", "广告显示");
                            isGoodFullAdReady = false;
                            break;
                        case AdFullType.ROCKET_SPEED_ENTER_FULL_AD:
                            Firebase.getInstance(context).logEvent("全屏广告", "小火箭进入", "广告显示");
                            isBadFullAdReady = false;
                            break;
                        case AdFullType.ROCKET_SPEED_FINISH_FULL_AD:
                            Firebase.getInstance(context).logEvent("全屏广告", "小火箭加速完成", "广告显示");
                            isBadFullAdReady = false;
                            break;
                        case AdFullType.SERVER_LIST_ENTER_FULL_AD:
                            Firebase.getInstance(context).logEvent("全屏广告", "服务器列表进入", "广告显示");
                            isBadFullAdReady = false;
                            break;
                        case AdFullType.CLOUD_WIFI_DIALOG_FULL_AD:
                            Firebase.getInstance(context).logEvent("全屏广告", "WiFi云弹窗", "广告显示");
                            isBadFullAdReady = false;
                            break;
                        case AdFullType.CLOUD_NET_SPEED_FULL_AD:
                            Firebase.getInstance(context).logEvent("全屏广告", "网速低云弹窗", "广告显示");
                            isBadFullAdReady = false;
                            break;
                        case AdFullType.CLOUD_DEVELOPED_USER_FULL_AD:
                            Firebase.getInstance(context).logEvent("全屏广告", "发达国家不活跃云弹窗", "广告显示");
                            isBadFullAdReady = false;
                            break;
                        case AdFullType.CLOUD_UNDEVELOPED_USER_FULL_AD:
                            Firebase.getInstance(context).logEvent("全屏广告", "非发达国家不活跃云弹窗", "广告显示");
                            isBadFullAdReady = false;
                            break;
                        case AdFullType.CLOUD_LUCK_ROTATE_FULL_AD:
                            Firebase.getInstance(context).logEvent("全屏广告", "转盘云弹窗", "广告显示");
                            isBadFullAdReady = false;
                            break;
                        case AdFullType.CANCEL_FREE_VIP_FULL_AD:
                            Firebase.getInstance(context).logEvent("全屏广告", "免费会员取消", "广告显示");
                            isGoodFullAdReady = false;
                            break;
                        case AdFullType.CONNECT_RESULT_ACTIVITY_FULL_AD:
                            Firebase.getInstance(context).logEvent("全屏广告", "连接结果页全屏", "广告显示");
                            isGoodFullAdReady = false;
                            break;
                        case AdFullType.DISCONNECT_RESULT_ACTIVITY_FULL_AD:
                            Firebase.getInstance(context).logEvent("全屏广告", "断开结果页全屏", "广告显示");
                            isGoodFullAdReady = false;
                            break;
                        case AdFullType.SERVER_LIST_SWITCH_SUCCESS:
                            Firebase.getInstance(context).logEvent("全屏广告", "服务器列表切换", "广告显示");
                            isBadFullAdReady = false;
                            break;
                    }
                }

                @Override
                public void onAdLoadFailed(AdType adType, int index, String reason) {
                    super.onAdLoadFailed(adType, index, reason);
                    Log.i(TAG, "onAdLoadFailed:  广告加载失败 " + index + "     " + reason);
                }

                @Override
                public void onAdClick(AdType adType, int index) {
                    super.onAdClick(adType, index);

                }
            });
            adAppHelper.setAdReadyListener(new AdReadyListener() {
                @Override
                public void onFullAdReady(int index) {
                    Log.i(TAG, "onFullAdReady:   全屏广告准备好");
                    if (index == AdUtils.FULL_AD_GOOD)
                        isGoodFullAdReady = true;
                    else if (index == AdUtils.FULL_AD_BAD)
                        isBadFullAdReady = true;

                }

                @Override
                public void onNativeAdReady(int index) {
                    Log.i(TAG, "onFullAdReady:   native广告准备好");
                }

                @Override
                public void onBannerAdReady() {
                    Log.i(TAG, "onFullAdReady:   banner广告准备好");
                }

                @Override
                public void onSplashAdReady(int index) {
                    Log.i(TAG, "onFullAdReady:   闪屏广告准备好");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static AdAppHelper getAdAppHelper() {
        if (adAppHelper == null) {
            return AdAppHelper.getInstance(ShadowsocksApplication.getGlobalContext());
        } else
            return adAppHelper;
    }

    /**
     * 跳转到自动弹窗界面
     *
     * @param context
     */
    private static void jump2AutoShowActivity(Context context) {

    }

    /**
     * 根据不同的种类获取不同的数据
     *
     * @param type
     * @return 返回-1表示不管如何继续展示广告 =-2则不展示广告了
     */
    public static int getAvailaableCount(String type) {
        return 0;
    }

    /**
     * 初始化native宽高
     *
     * @param context
     */
    private static void initAd(Context context) {
        AdAppHelper.GA_RESOURCE_ID = R.xml.ga_tracker;
        AdAppHelper.FIREBASE = Firebase.getInstance(context);

        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        if (displayMetrics.density * 320 >= displayMetrics.widthPixels) {
            AdAppHelper.NATIVE_ADMOB_WIDTH_LIST = new int[1];
            AdAppHelper.NATIVE_ADMOB_WIDTH_LIST[0] = 280;
        }
    }

    /**
     * Native 动画
     *
     * @param frameLayout
     */
    public static void showNativeAnimation(final FrameLayout frameLayout) {
        frameLayout.post(new Runnable() {
            @Override
            public void run() {
                if (frameLayout != null) {
                    float centerX = frameLayout.getWidth() / 2.0f;
                    float centerY = frameLayout.getHeight() / 2.0f;
                    Rotate3dAnimation rotate3dAnimation = new Rotate3dAnimation(ShadowsocksApplication.getGlobalContext(), 180, 360, centerX, centerY, 0f, false);
                    rotate3dAnimation.setDuration(1000);
                    rotate3dAnimation.setFillAfter(false);
                    rotate3dAnimation.setInterpolator(new LinearInterpolator());
                    frameLayout.startAnimation(rotate3dAnimation);
                }
            }
        });

    }
}
