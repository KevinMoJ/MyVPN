package com.androapplite.shadowsocks.activity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.androapplite.shadowsocks.service.ServerListFetcherService;
import com.androapplite.shadowsocks.service.VpnManageService;
import com.androapplite.shadowsocks.service.WarnDialogShowService;
import com.androapplite.shadowsocks.util.IabBroadcastReceiver;
import com.androapplite.shadowsocks.util.IabHelper;
import com.androapplite.shadowsocks.util.IabResult;
import com.androapplite.shadowsocks.util.Inventory;
import com.androapplite.shadowsocks.util.Purchase;
import com.androapplite.shadowsocks.utils.InternetUtil;
import com.androapplite.vpn3.R;
import com.bestgo.adsplugin.ads.AdAppHelper;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.vm.shadowsocks.core.LocalVpnService;

import java.util.ArrayList;
import java.util.List;

public class RecommendVIPActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "RecommendVIPActivity";

    public static final int RECOMMEND_ICON_COUNT = 3;

    private ScrollView mScrollView;
    private ImageView mRecommendVipClose;
    private ViewPager mRecommendVipPager;
    private TextView mRecommendVipSevenDayBt;
    private TextView mRecommendVipFreeTimeBt;
    private LinearLayout mRecommendVipWelcomeRoot;
    private TextView mRecommendVipTopSevenBt;
    private TextView mRecommendVipTopFreeBt;
    private TextView mRecommendVipBottomSevenBt;
    private TextView mRecommendVipBottomFreeBt;
    private ImageView mRecommendVipBottomClose;

    private MyViewPageAdapter myAdapter;
    private List<ItemBean> mItemBeans;

    private IabHelper mIabHelper;
    /*标记IaHelper 是否启动*/
    private boolean isStartHelper;
    private IabBroadcastReceiver mBroadcastReceiver;
    private List<String> skuList = new ArrayList<>();
    private SharedPreferences mSharedPreferences;

    private AdAppHelper mAdAppHelper;

    private int[] bannerImages;
    private int[] bannerFlags;
    private String[] bannerTitles;
    private String[] bannerMessage;
    private long luckFreeDay;
    private long freeUseTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
        checkFreeUseTime();

        luckFreeDay = mSharedPreferences.getLong(SharedPreferenceKey.LUCK_PAN_GET_FREE_DAY, 0);
        freeUseTime = mSharedPreferences.getLong(SharedPreferenceKey.NEW_USER_FREE_USER_TIME, 0);
        boolean isVIP = mSharedPreferences.getBoolean(SharedPreferenceKey.VIP, false);

        if (isVIP) {
            startActivity(new Intent(this, SplashActivity.class));
            finish();
        } else {
            if (luckFreeDay > 0) {
                startActivity(new Intent(this, SplashActivity.class));
                finish();
            } else if (luckFreeDay <= 0 && freeUseTime > 0) {
                startActivity(new Intent(this, SplashActivity.class));
                finish();
            }
        }
        setContentView(R.layout.activity_recommend_vip);
        initView();
        initData();
    }

    private void checkFreeUseTime() {
        //用活跃用户的打开APP的时间判断上次用户打开APP的时间
        long openAppTime = mSharedPreferences.getLong(SharedPreferenceKey.OPEN_APP_TIME_TO_DECIDE_INACTIVE_USER, 0);
        luckFreeDay = mSharedPreferences.getLong(SharedPreferenceKey.LUCK_PAN_GET_FREE_DAY, 0);
        freeUseTime = mSharedPreferences.getLong(SharedPreferenceKey.NEW_USER_FREE_USER_TIME, 0);
        long newUserFreeTime = FirebaseRemoteConfig.getInstance().getLong("new_user_free_use_time");
        long differ = System.currentTimeMillis() - openAppTime;

        if (luckFreeDay <= 0) { //没有转转盘获取时间或者获取的时间到期
            mSharedPreferences.edit().putLong(SharedPreferenceKey.LUCK_PAN_GET_FREE_DAY, 0).apply();
            if (differ > 0) {
                long dif = differ / 1000; // 上次打开APP的时间到这次的时间间隔
                if (dif <= newUserFreeTime * 60) {// 20  15
                    mSharedPreferences.edit().putLong(SharedPreferenceKey.NEW_USER_FREE_USER_TIME, freeUseTime - dif).apply();
                    long newFreeUseTime = mSharedPreferences.getLong(SharedPreferenceKey.NEW_USER_FREE_USER_TIME, 0);
                    if (newFreeUseTime < 0)
                        mSharedPreferences.edit().putLong(SharedPreferenceKey.NEW_USER_FREE_USER_TIME, 0).apply();
                } else {
                    mSharedPreferences.edit().putLong(SharedPreferenceKey.NEW_USER_FREE_USER_TIME, 0).apply();
                }
            }
        } else { // 有幸运转盘转到的时间，但是这次打开APP要更新一下
            if (differ > 0) {
                long overDay = differ / (1000 * 60 * 60 * 24); // 上次打开APP的时间到现在的过了多少天
                if (overDay <= luckFreeDay)
                    mSharedPreferences.edit().putLong(SharedPreferenceKey.LUCK_PAN_GET_FREE_DAY, luckFreeDay - overDay).apply();
                else
                    mSharedPreferences.edit().putLong(SharedPreferenceKey.LUCK_PAN_GET_FREE_DAY, 0).apply();
                long newFreeUseDay = mSharedPreferences.getLong(SharedPreferenceKey.LUCK_PAN_GET_FREE_DAY, 0);
                if (newFreeUseDay < 0)
                    mSharedPreferences.edit().putLong(SharedPreferenceKey.LUCK_PAN_GET_FREE_DAY, 0).apply();

                mSharedPreferences.edit().putLong(SharedPreferenceKey.NEW_USER_FREE_USER_TIME, 0).apply();
            }
        }
    }

    private void initView() {
        mRecommendVipWelcomeRoot = (LinearLayout) findViewById(R.id.recommend_vip_welcome_root);
        mScrollView = (ScrollView) findViewById(R.id.recommend_vip_scroll);
        mScrollView.setVisibility(View.GONE);

        mRecommendVipClose = (ImageView) findViewById(R.id.recommend_vip_close);
        mRecommendVipPager = (ViewPager) findViewById(R.id.recommend_vip_pager);
        mRecommendVipSevenDayBt = (TextView) findViewById(R.id.recommend_vip_seven_day_bt);
        mRecommendVipFreeTimeBt = (TextView) findViewById(R.id.recommend_vip_free_time_bt);
        mRecommendVipTopSevenBt = (TextView) findViewById(R.id.recommend_vip_top_seven_bt);
        mRecommendVipTopFreeBt = (TextView) findViewById(R.id.recommend_vip_top_free_bt);
        mRecommendVipBottomSevenBt = (TextView) findViewById(R.id.recommend_vip_bottom_seven_bt);
        mRecommendVipBottomFreeBt = (TextView) findViewById(R.id.recommend_vip_bottom_free_bt);
        mRecommendVipBottomClose = (ImageView) findViewById(R.id.recommend_vip_bottom_close);

        mRecommendVipSevenDayBt.setOnClickListener(this);
        mRecommendVipTopSevenBt.setOnClickListener(this);
        mRecommendVipBottomSevenBt.setOnClickListener(this);

        mRecommendVipFreeTimeBt.setOnClickListener(this);
        mRecommendVipTopFreeBt.setOnClickListener(this);
        mRecommendVipBottomFreeBt.setOnClickListener(this);

        mRecommendVipClose.setOnClickListener(this);
        mRecommendVipBottomClose.setOnClickListener(this);
    }

    private void initData() {
        bannerImages = new int[]{R.drawable.recommend_rocket_icon, R.drawable.recommend_vpn_icon, R.drawable.recommend_lock_icon};
        bannerTitles = new String[]{getResources().getString(R.string.banner_1_title),
                getResources().getString(R.string.banner_2_title),
                getResources().getString(R.string.banner_3_title)};
        bannerMessage = new String[]{getResources().getString(R.string.banner_1_message),
                getResources().getString(R.string.banner_2_message),
                getResources().getString(R.string.banner_3_message)};
        bannerFlags = new int[]{R.drawable.banner_1, R.drawable.banner_2, R.drawable.banner_3};

        mItemBeans = new ArrayList<>();
        for (int i = 0; i < RECOMMEND_ICON_COUNT; i++) {
            mItemBeans.add(new ItemBean(bannerImages[i], bannerFlags[i], bannerTitles[i], bannerMessage[i]));
        }

        myAdapter = new MyViewPageAdapter(this, mItemBeans);

        mRecommendVipPager.setAdapter(myAdapter);
        mRecommendVipPager.setCurrentItem(0);
        mRecommendVipPager.setOffscreenPageLimit(mItemBeans.size() - 1);

        mAdAppHelper = AdAppHelper.getInstance(this);
        mAdAppHelper.loadNewInterstitial();
        mAdAppHelper.loadNewNative();
        mAdAppHelper.loadNewSplashAd();
        try {
            checkIsVIP();
        } catch (Exception e) {
        }
        WarnDialogShowService.start(this);
        if (!LocalVpnService.IsRunning)
            ServerListFetcherService.fetchServerListAsync(this);
        VpnManageService.start(this);
        Firebase.getInstance(this).logEvent("当前网络类型", "类型", InternetUtil.getNetworkState(RecommendVIPActivity.this));
    }

    private void checkIsVIP() {
        skuList.add(VIPActivity.PAY_ONE_MONTH);
        skuList.add(VIPActivity.PAY_HALF_YEAR);
        skuList.add(VIPActivity.PAY_ONE_YEAR);
        skuList.add(VIPActivity.PAY_SEVEN_FREE); // 添加消费的SKU，此字段在Google后台有保存，用来区别当前用户是否支付，字段是商品ID

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

                mBroadcastReceiver = new IabBroadcastReceiver(new IabBroadcastReceiver.IabBroadcastListener() {
                    @Override
                    public void receivedBroadcast() {
                        try {
                            mIabHelper.queryInventoryAsync(mGotInventoryListener);
                        } catch (IabHelper.IabAsyncInProgressException e) {
                        }
                    }
                });
                IntentFilter broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
                registerReceiver(mBroadcastReceiver, broadcastFilter);

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
                Firebase.getInstance(RecommendVIPActivity.this).logEvent("欢迎页检查VIP", "查询失败");
//                Log.i("SplashActivity", "onQueryInventoryFinished: 查询失败 ");
                return;
            }

            SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(RecommendVIPActivity.this);
            Purchase oneMonthPurchase = inventory.getPurchase(VIPActivity.PAY_ONE_MONTH);
            Purchase halfYearPurchase = inventory.getPurchase(VIPActivity.PAY_HALF_YEAR);
            Purchase oneYearPurchase = inventory.getPurchase(VIPActivity.PAY_ONE_YEAR);
            Purchase sevenFreePurchase = inventory.getPurchase(VIPActivity.PAY_SEVEN_FREE);
            if (oneMonthPurchase != null) {
                Log.i("SplashActivity", "onIabPurchaseFinishedMain: We have goods");
                Firebase.getInstance(RecommendVIPActivity.this).logEvent("欢迎页检查VIP", "查询成功", "一个月");
                sharedPreferences.edit().putBoolean(SharedPreferenceKey.VIP, true).apply();
                sharedPreferences.edit().putBoolean(SharedPreferenceKey.IS_VIP_PAY_ONE_MONTH, true).apply();
                sharedPreferences.edit().putBoolean(SharedPreferenceKey.IS_AUTOMATIC_RENEWAL_VIP, oneMonthPurchase.isAutoRenewing()).apply();
                sharedPreferences.edit().putLong(SharedPreferenceKey.VIP_PAY_TIME, oneMonthPurchase.getPurchaseTime()).apply();
                return;
            } else if (halfYearPurchase != null) {
                Log.i("SplashActivity", "onIabPurchaseFinishedMain: We have goods");
                Firebase.getInstance(RecommendVIPActivity.this).logEvent("欢迎页检查VIP", "查询成功", "半年");
                sharedPreferences.edit().putBoolean(SharedPreferenceKey.VIP, true).apply();
                sharedPreferences.edit().putBoolean(SharedPreferenceKey.IS_VIP_PAY_ONE_MONTH, false).apply();
                sharedPreferences.edit().putBoolean(SharedPreferenceKey.IS_AUTOMATIC_RENEWAL_VIP, halfYearPurchase.isAutoRenewing()).apply();
                sharedPreferences.edit().putLong(SharedPreferenceKey.VIP_PAY_TIME, halfYearPurchase.getPurchaseTime()).apply();
                return;
            } else if (oneYearPurchase != null) {
                Log.i("SplashActivity", "onIabPurchaseFinishedMain: We have goods");
                Firebase.getInstance(RecommendVIPActivity.this).logEvent("欢迎页检查VIP", "查询成功", "一年");
                sharedPreferences.edit().putBoolean(SharedPreferenceKey.VIP, true).apply();
                sharedPreferences.edit().putBoolean(SharedPreferenceKey.IS_VIP_PAY_ONE_MONTH, false).apply();
                sharedPreferences.edit().putBoolean(SharedPreferenceKey.IS_AUTOMATIC_RENEWAL_VIP, oneYearPurchase.isAutoRenewing()).apply();
                sharedPreferences.edit().putLong(SharedPreferenceKey.VIP_PAY_TIME, oneYearPurchase.getPurchaseTime()).apply();
            } else if (sevenFreePurchase != null) {
                Log.i("SplashActivity", "onIabPurchaseFinishedMain: We have goods");
                Firebase.getInstance(RecommendVIPActivity.this).logEvent("欢迎页检查VIP", "查询成功", "免费试用");
                sharedPreferences.edit().putBoolean(SharedPreferenceKey.VIP, true).apply();
                sharedPreferences.edit().putBoolean(SharedPreferenceKey.IS_VIP_PAY_ONE_MONTH, false).apply();
                sharedPreferences.edit().putBoolean(SharedPreferenceKey.IS_AUTOMATIC_RENEWAL_VIP, sevenFreePurchase.isAutoRenewing()).apply();
                sharedPreferences.edit().putLong(SharedPreferenceKey.VIP_PAY_TIME, sevenFreePurchase.getPurchaseTime()).apply();
            } else {
                Firebase.getInstance(RecommendVIPActivity.this).logEvent("欢迎页检查VIP", "没查询到");
                sharedPreferences.edit().putBoolean(SharedPreferenceKey.VIP, false).apply();
            }
        }
    };

    @Override
    public void onBackPressed() {
        if (!FirebaseRemoteConfig.getInstance().getBoolean("recommend_vip_hide_back")) {
            if (mRecommendVipWelcomeRoot.getVisibility() == View.VISIBLE && mScrollView.getVisibility() == View.GONE) {
                showScrollView();
            } else if (mRecommendVipWelcomeRoot.getVisibility() == View.GONE && mScrollView.getVisibility() == View.VISIBLE) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.recommend_vip_close:
                showScrollView();
                break;

            case R.id.recommend_vip_seven_day_bt:
            case R.id.recommend_vip_top_seven_bt:
            case R.id.recommend_vip_bottom_seven_bt:
                payFreeSeven();
                break;

            case R.id.recommend_vip_free_time_bt:
            case R.id.recommend_vip_top_free_bt:
            case R.id.recommend_vip_bottom_free_bt:
                MainActivity.startLuckRotateActivity(this, false);
                finish();
                break;
            case R.id.recommend_vip_bottom_close:
                startActivity(new Intent(this, MainActivity.class));
                finish();
                break;
        }
    }

    private void payFreeSeven() {
        if (!mIabHelper.subscriptionsSupported()) {
            return;
        }

        mIabHelper.flagEndAsync();
        try {
            mIabHelper.launchSubscriptionPurchaseFlow(this, VIPActivity.PAY_SEVEN_FREE, 10001, new IabHelper.OnIabPurchaseFinishedListener() {
                @Override
                public void onIabPurchaseFinished(IabResult result, Purchase info) {
                    if (result.isFailure()) {
                        if (result.getResponse() == IabHelper.IABHELPER_USER_CANCELLED) {
                            // 交易取消
                            Firebase.getInstance(RecommendVIPActivity.this).logEvent("VIP交易", "交易取消", "免费试用");
                            Log.i(TAG, "onIabPurchaseFinished: 交易取消");
                        } else {
                            // 交易失败
                            Firebase.getInstance(RecommendVIPActivity.this).logEvent("VIP交易", "交易失败", "免费试用");
                            Log.i(TAG, "onIabPurchaseFinished: 交易失败");
                        }
                    } else {
                        Firebase.getInstance(RecommendVIPActivity.this).logEvent("VIP交易", "交易成功", "免费试用");
                        Log.i(TAG, "onIabPurchaseFinished: 交易成功的finish 半年");
                        //存个字段，说明是VIP用户
                        SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(RecommendVIPActivity.this);
                        sharedPreferences.edit().putBoolean(SharedPreferenceKey.VIP, true).apply();
                        sharedPreferences.edit().putBoolean(SharedPreferenceKey.IS_VIP_PAY_ONE_MONTH, false).apply();
                        sharedPreferences.edit().putBoolean(SharedPreferenceKey.IS_AUTOMATIC_RENEWAL_VIP, info.isAutoRenewing()).apply();
                        sharedPreferences.edit().putLong(SharedPreferenceKey.VIP_PAY_TIME, info.getPurchaseTime()).apply();
                        VIPFinishActivity.startVIPFinishActivity(RecommendVIPActivity.this, true);
                        finish();
                    }
                }
            }, "jjjj");
        } catch (IabHelper.IabAsyncInProgressException e) {
        }

        try {
            mIabHelper.queryInventoryAsync(false, skuList, null, mGotInventoryListener);
        } catch (IabHelper.IabAsyncInProgressException e) {
            e.printStackTrace();
        }
    }

    private void showScrollView() {
        mScrollView.scrollTo(0, 0);
        mRecommendVipWelcomeRoot.setVisibility(View.GONE);
        mScrollView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isStartHelper) {
            if (mBroadcastReceiver != null)
                unregisterReceiver(mBroadcastReceiver);
            if (mIabHelper != null) {
                mIabHelper.disposeWhenFinished();
                mIabHelper = null;
            }
        }
    }

    private class MyViewPageAdapter extends PagerAdapter {
        private List<ItemBean> mList;
        private Context mContext;

        public MyViewPageAdapter(Context context, List<ItemBean> list) {
            mContext = context;
            mList = list;
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        //判断是否是否为同一张图片，这里返回方法中的两个参数做比较就可以
        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        //设置viewpage内部东西的方法，如果viewpage内没有子空间滑动产生不了动画效果
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.recommend_vip_pager_image_item, container, false);
            ImageView icon = (ImageView) view.findViewById(R.id.pager_icon);
            ImageView flag = (ImageView) view.findViewById(R.id.pager_flag);
            TextView title = (TextView) view.findViewById(R.id.pager_title);
            TextView message = (TextView) view.findViewById(R.id.pager_message);

            icon.setImageResource(mList.get(position).mImageIcon);
            title.setText(mList.get(position).mItemTitle);
            message.setText(mList.get(position).mItemContent);
            flag.setImageResource(mList.get(position).mImageFlag);
            container.addView(view);
            return view;
        }
    }

    private class ItemBean {
        public int mImageIcon;
        public int mImageFlag;
        public String mItemTitle;
        public String mItemContent;

        public ItemBean(int imageIcon, int imageFlag, String title, String content) {
            mImageIcon = imageIcon;
            mImageFlag = imageFlag;
            mItemTitle = title;
            mItemContent = content;
        }
    }
}
