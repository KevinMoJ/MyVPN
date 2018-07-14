package com.androapplite.shadowsocks.activity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.androapplite.shadowsocks.util.IabBroadcastReceiver;
import com.androapplite.shadowsocks.util.IabBroadcastReceiver.IabBroadcastListener;
import com.androapplite.shadowsocks.util.IabHelper;
import com.androapplite.shadowsocks.util.IabHelper.IabAsyncInProgressException;
import com.androapplite.shadowsocks.util.IabResult;
import com.androapplite.shadowsocks.util.Inventory;
import com.androapplite.shadowsocks.util.Purchase;
import com.androapplite.vpn3.R;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.vm.shadowsocks.core.LocalVpnService;

import java.util.ArrayList;
import java.util.List;

public class VIPActivity extends AppCompatActivity implements IabBroadcastListener, View.OnClickListener {
    private static final String TAG = "VIPActivity";
    public static final String PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhHtkULOFh9/C7v27NwL4/RSke/jvbzM8w42+Rul4s59fVPz3QYRop7VA5m6ds37nHAOHgZs4Uc+CbXo4hgoQwZKFc+yIMyImKA3KPhWWh/yUQQuCdYWM3G1QfS5KdZfWULFXKyEZo6HfRbT8+Nu0tNaPbTqVzaDGsPeUMv+8G72cY8BnsJUv6PwyM/lGtHDQMjy8+dawVghj23OxmRZCTkAG9oM9ksOKN9vw5ToZ1wQOyhe78iePzOnLGIpjgV4G68C4wnoFyESf06dAsJSN//gAajZZv1SJVA8yB7o+RtIAEluZFr11XRqProziWE7buwjErWpQoAvZw27HxXNgCwIDAQAB";

    public static final String TYPE = "TYPE";
    public static final int TYPE_SERVER_LIST = 101;
    public static final int TYPE_MAIN_PAO = 102;
    public static final int TYPE_NAV = 103;
    public static final int TYPE_NET_SPEED_FINISH = 104;
    public static final int TYPE_FREE_TIME_OVER = 105;

    private static final int RC_REQUEST = 10001;
    public static String PAY_ONE_MONTH = "one_3";
    public static String PAY_HALF_YEAR = "half_2";
    public static String PAY_ONE_YEAR = "year_1";
    public static String PAY_SEVEN_FREE = "free_1";

    private IabBroadcastReceiver mBroadcastReceiver;
    private IabHelper mHelper;
    private ImageView mVipOneMonthBt;
    private TextView mVipOneMonthMoneyText;
    private ImageView mVipHalfYearBt;
    private TextView mVipHalfYearMoneyText;
    private ImageView mVipOneYearBt;
    private TextView mVipOneYearMoneyText;
    private TextView mServerMessageText;
    private ActionBar mActionBar;
    /*标记IaHelper 是否启动*/
    private boolean isStartHelper;

    List<String> skuList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay_vip);

        mActionBar = getSupportActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setDisplayShowHomeEnabled(true);
        mActionBar.setElevation(0);
        final Drawable upArrow = getResources().getDrawable(R.drawable.ic_arrow_back_black_24dp);
        upArrow.setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_ATOP);
        mActionBar.setHomeAsUpIndicator(upArrow);

        initView();
        initUI();
        initGooglePayHelper();
        analysisSource();
        if (LocalVpnService.Instance != null) {
            LocalVpnService.Instance.isPayLoadInterface = true;
        }
    }

    private void analysisSource() {
        int type = getIntent().getIntExtra(TYPE, TYPE_MAIN_PAO);
        if (type == TYPE_SERVER_LIST)
            Firebase.getInstance(this).logEvent("进入vip界面来源", "服务器列表点击");
        else if (type == TYPE_MAIN_PAO)
            Firebase.getInstance(this).logEvent("进入vip界面来源", "主界面小泡泡");
        else if (type == TYPE_NAV)
            Firebase.getInstance(this).logEvent("进入vip界面来源", "侧边栏");
        else if (type == TYPE_NET_SPEED_FINISH)
            Firebase.getInstance(this).logEvent("进入vip界面来源", "加速成功结果页");
        else if (type == TYPE_FREE_TIME_OVER)
            Firebase.getInstance(this).logEvent("进入vip界面来源", "面试试用结束");
    }

    private void initGooglePayHelper() {
        skuList.add(PAY_ONE_MONTH);
        skuList.add(PAY_HALF_YEAR);
        skuList.add(PAY_ONE_YEAR);
        skuList.add(PAY_SEVEN_FREE); // 添加消费的SKU，此字段在Google后台有保存，用来区别当前用户是否支付，字段是商品ID

        mHelper = new IabHelper(this, PUBLIC_KEY.trim());
        mHelper.enableDebugLogging(true);

        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d(TAG, "Setup finished.");

                if (!result.isSuccess()) {
                    //helper设置失败是没法支付的，此处可以弹出提示框
                    isStartHelper = false;
                    Log.i(TAG, "onIabSetupFinished: 初始化失败");
                    return;
                } else {
                    isStartHelper = true;
                    Log.i(TAG, "onIabSetupFinished: 初始化成功");
                }

                if (mHelper == null) return;

                mBroadcastReceiver = new IabBroadcastReceiver(VIPActivity.this);
                IntentFilter broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
                registerReceiver(mBroadcastReceiver, broadcastFilter);

                // IAB is fully set up. Now, let's get an inventory of stuff we own.
                Log.d(TAG, "Setup successful. Querying inventory.");
                try {
                    //查询库存并查询可售商品详细信息
                    mHelper.queryInventoryAsync(true, skuList, null, mGotInventoryListener);
                } catch (IabAsyncInProgressException e) {
                }
            }
        });

    }

    private void initView() {
        mVipOneMonthBt = (ImageView) findViewById(R.id.vip_one_month_bt);
        mVipOneMonthMoneyText = (TextView) findViewById(R.id.vip_one_month_money_text);
        mVipHalfYearBt = (ImageView) findViewById(R.id.vip_half_year_bt);
        mVipHalfYearMoneyText = (TextView) findViewById(R.id.vip_half_year_money_text);
        mVipOneYearBt = (ImageView) findViewById(R.id.vip_one_year_bt);
        mVipOneYearMoneyText = (TextView) findViewById(R.id.vip_one_year_money_text);
        mServerMessageText = (TextView) findViewById(R.id.server_message_text);

        mVipOneMonthBt.setOnClickListener(this);
        mVipHalfYearBt.setOnClickListener(this);
        mVipOneYearBt.setOnClickListener(this);

        mActionBar.setTitle("VIP");
    }

    private void initUI() {
        String oneMonthMoney = FirebaseRemoteConfig.getInstance().getString("pay_one_month_money");
        String halfYearMoney = FirebaseRemoteConfig.getInstance().getString("pay_half_year_money");
        String oneYearMoney = FirebaseRemoteConfig.getInstance().getString("pay_one_year_money");

        mServerMessageText.setText(getResources().getString(R.string.more_than_countries, String.valueOf(3)));
        mVipOneMonthMoneyText.setText(getResources().getString(R.string.pay_one_month_bt_text, String.valueOf(oneMonthMoney)));
        mVipHalfYearMoneyText.setText(getResources().getString(R.string.pay_one_month_bt_text, String.valueOf(halfYearMoney)));
        mVipOneYearMoneyText.setText(getResources().getString(R.string.pay_one_month_bt_text, String.valueOf(oneYearMoney)));
    }

    public static void startVIPActivity(Context context, int type) {
        Intent intent = new Intent(context, VIPActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(TYPE, type);
        context.startActivity(intent);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.vip_one_month_bt:
                payOneMonth();
                break;

            case R.id.vip_half_year_bt:
                payHalfYear();
                break;

            case R.id.vip_one_year_bt:
                payOneYear();
                break;

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        }

        if (itemId == R.id.vip_luck_pan) {
            LuckRotateActivity.startLuckActivity(this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);

        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            Log.d(TAG, "onActivityResult handled by IABUtil.");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.vip_right_menu, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isStartHelper) {
            if (mBroadcastReceiver != null) {
                unregisterReceiver(mBroadcastReceiver);
            }

            if (mHelper != null) {
                mHelper.disposeWhenFinished();
                mHelper = null;
            }
        }

        if (LocalVpnService.Instance != null) {
            LocalVpnService.Instance.isPayLoadInterface = false;
        }
    }

    private IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        @Override
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            if (mHelper == null) return;

            if (result.isFailure()) {
                //在商品仓库中查询失败
                Log.i(TAG, "onIabPurchaseFinished: 查询失败");
                Firebase.getInstance(VIPActivity.this).logEvent("VIP交易", "查询失败");
                return;
            }
            SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(VIPActivity.this);
            Purchase oneMonthPurchase = inventory.getPurchase(PAY_ONE_MONTH);
            Purchase halfYearPurchase = inventory.getPurchase(PAY_HALF_YEAR);
            Purchase oneYearPurchase = inventory.getPurchase(PAY_ONE_YEAR);
            Purchase sevenFreePurchase = inventory.getPurchase(PAY_SEVEN_FREE);
//            Log.i(TAG, "onQueryInventoryFinished:  " + inventory.hasPurchase(PAY_ONE_MONTH) + "    " + inventory.hasPurchase(PAY_HALF_YEAR));

            if (oneMonthPurchase != null) {
                Log.i(TAG, "onQueryInventoryFinished: 查询成功的finish 一个月 ");
                Firebase.getInstance(VIPActivity.this).logEvent("VIP交易", "查询成功", "一个月");
                sharedPreferences.edit().putBoolean(SharedPreferenceKey.VIP, true).apply();
                sharedPreferences.edit().putBoolean(SharedPreferenceKey.IS_VIP_PAY_ONE_MONTH, true).apply();
                sharedPreferences.edit().putBoolean(SharedPreferenceKey.IS_AUTOMATIC_RENEWAL_VIP, oneMonthPurchase.isAutoRenewing()).apply();
                sharedPreferences.edit().putLong(SharedPreferenceKey.VIP_PAY_TIME, oneMonthPurchase.getPurchaseTime()).apply();
                finish();
                return;
            } else if (halfYearPurchase != null) {
                Log.i(TAG, "onQueryInventoryFinished: 查询成功的finish 半年 ");
                Firebase.getInstance(VIPActivity.this).logEvent("VIP交易", "查询成功", "半年");
                sharedPreferences.edit().putBoolean(SharedPreferenceKey.VIP, true).apply();
                sharedPreferences.edit().putBoolean(SharedPreferenceKey.IS_VIP_PAY_ONE_MONTH, false).apply();
                sharedPreferences.edit().putBoolean(SharedPreferenceKey.IS_AUTOMATIC_RENEWAL_VIP, halfYearPurchase.isAutoRenewing()).apply();
                sharedPreferences.edit().putLong(SharedPreferenceKey.VIP_PAY_TIME, halfYearPurchase.getPurchaseTime()).apply();
                finish();
                return;
            } else if (oneYearPurchase != null) {
                Log.i(TAG, "onQueryInventoryFinished: 查询成功的finish 一年 ");
                Firebase.getInstance(VIPActivity.this).logEvent("VIP交易", "查询成功", "一年");
                sharedPreferences.edit().putBoolean(SharedPreferenceKey.VIP, true).apply();
                sharedPreferences.edit().putBoolean(SharedPreferenceKey.IS_VIP_PAY_ONE_MONTH, false).apply();
                sharedPreferences.edit().putBoolean(SharedPreferenceKey.IS_AUTOMATIC_RENEWAL_VIP, oneYearPurchase.isAutoRenewing()).apply();
                sharedPreferences.edit().putLong(SharedPreferenceKey.VIP_PAY_TIME, oneYearPurchase.getPurchaseTime()).apply();
                finish();
            } else if (sevenFreePurchase != null) {
                Log.i(TAG, "onQueryInventoryFinished: 查询成功的finish 一年 ");
                Firebase.getInstance(VIPActivity.this).logEvent("VIP交易", "查询成功", "免费试用");
                sharedPreferences.edit().putBoolean(SharedPreferenceKey.VIP, true).apply();
                sharedPreferences.edit().putBoolean(SharedPreferenceKey.IS_VIP_PAY_ONE_MONTH, false).apply();
                sharedPreferences.edit().putBoolean(SharedPreferenceKey.IS_AUTOMATIC_RENEWAL_VIP, sevenFreePurchase.isAutoRenewing()).apply();
                sharedPreferences.edit().putLong(SharedPreferenceKey.VIP_PAY_TIME, sevenFreePurchase.getPurchaseTime()).apply();
                finish();
            } else {
                Firebase.getInstance(VIPActivity.this).logEvent("VIP交易", "没查询到");
                Log.i(TAG, "onQueryInventoryFinished:没查询到 ");
                sharedPreferences.edit().putBoolean(SharedPreferenceKey.VIP, false).apply();
            }
        }
    };

    //消费完成的回调
    private IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            if (mHelper == null)
                return;

            Log.d(TAG, "Consumption finished.Purchase: " + purchase + ", result: " + result);
            if (result.isSuccess()) {
                //消费成功
                Log.d(TAG, "Consumptionsuccessful. Provisioning.");
            } else {
                //消费失败
            }
        }
    };

    @Override
    public void receivedBroadcast() {
        try {
            mHelper.queryInventoryAsync(mGotInventoryListener);
        } catch (IabHelper.IabAsyncInProgressException e) {
        }
    }

    private void payOneMonth() {
        if (!mHelper.subscriptionsSupported()) {
            return;
        }

        mHelper.flagEndAsync();
        try {
            mHelper.launchSubscriptionPurchaseFlow(this, PAY_ONE_MONTH, RC_REQUEST, new IabHelper.OnIabPurchaseFinishedListener() {
                @Override
                public void onIabPurchaseFinished(IabResult result, Purchase info) {
                    if (result.isFailure()) {
                        if (result.getResponse() == IabHelper.IABHELPER_USER_CANCELLED) {
                            // 交易取消
                            Firebase.getInstance(VIPActivity.this).logEvent("VIP交易", "交易取消", "一个月");
                            Log.i(TAG, "onIabPurchaseFinished: 交易取消");
                        } else {
                            // 交易失败
                            Firebase.getInstance(VIPActivity.this).logEvent("VIP交易", "交易失败", "一个月");
                            Log.i(TAG, "onIabPurchaseFinished: 交易失败");
                        }
                    } else {
                        Firebase.getInstance(VIPActivity.this).logEvent("VIP交易", "交易成功", "一个月");
                        Log.i(TAG, "onIabPurchaseFinished: 交易成功 一个月");
                        //存个字段，说明是VIP用户
                        SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(VIPActivity.this);
                        sharedPreferences.edit().putBoolean(SharedPreferenceKey.VIP, true).apply();
                        sharedPreferences.edit().putBoolean(SharedPreferenceKey.IS_VIP_PAY_ONE_MONTH, true).apply();
                        sharedPreferences.edit().putBoolean(SharedPreferenceKey.IS_AUTOMATIC_RENEWAL_VIP, info.isAutoRenewing()).apply();
                        sharedPreferences.edit().putLong(SharedPreferenceKey.VIP_PAY_TIME, info.getPurchaseTime()).apply();
                        VIPFinishActivity.startVIPFinishActivity(VIPActivity.this, true);
                        finish();
                    }
                }
            }, "dddd");
        } catch (IabAsyncInProgressException e) {
        }

        try {
            mHelper.queryInventoryAsync(false, skuList, null, mGotInventoryListener);
        } catch (IabAsyncInProgressException e) {
            e.printStackTrace();
        }
    }

    private void payHalfYear() {
        if (!mHelper.subscriptionsSupported()) {
            return;
        }

        mHelper.flagEndAsync();
        try {
            mHelper.launchSubscriptionPurchaseFlow(this, PAY_HALF_YEAR, RC_REQUEST, new IabHelper.OnIabPurchaseFinishedListener() {
                @Override
                public void onIabPurchaseFinished(IabResult result, Purchase info) {
                    if (result.isFailure()) {
                        if (result.getResponse() == IabHelper.IABHELPER_USER_CANCELLED) {
                            // 交易取消
                            Firebase.getInstance(VIPActivity.this).logEvent("VIP交易", "交易取消", "半年");
                            Log.i(TAG, "onIabPurchaseFinished: 交易取消");
                        } else {
                            // 交易失败
                            Firebase.getInstance(VIPActivity.this).logEvent("VIP交易", "交易失败", "半年");
                            Log.i(TAG, "onIabPurchaseFinished: 交易失败");
                        }
                    } else {
                        Firebase.getInstance(VIPActivity.this).logEvent("VIP交易", "交易成功", "半年");
                        Log.i(TAG, "onIabPurchaseFinished: 交易成功的finish 半年");
                        //存个字段，说明是VIP用户
                        SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(VIPActivity.this);
                        sharedPreferences.edit().putBoolean(SharedPreferenceKey.VIP, true).apply();
                        sharedPreferences.edit().putBoolean(SharedPreferenceKey.IS_VIP_PAY_ONE_MONTH, false).apply();
                        sharedPreferences.edit().putBoolean(SharedPreferenceKey.IS_AUTOMATIC_RENEWAL_VIP, info.isAutoRenewing()).apply();
                        sharedPreferences.edit().putLong(SharedPreferenceKey.VIP_PAY_TIME, info.getPurchaseTime()).apply();
                        VIPFinishActivity.startVIPFinishActivity(VIPActivity.this, true);
                        finish();
                    }
                }
            }, "ffff");
        } catch (IabAsyncInProgressException e) {
        }

        try {
            mHelper.queryInventoryAsync(false, skuList, null, mGotInventoryListener);
        } catch (IabAsyncInProgressException e) {
            e.printStackTrace();
        }
    }


    private void payOneYear() {
        if (!mHelper.subscriptionsSupported()) {
            return;
        }

        mHelper.flagEndAsync();
        try {
            mHelper.launchSubscriptionPurchaseFlow(this, PAY_ONE_YEAR, RC_REQUEST, new IabHelper.OnIabPurchaseFinishedListener() {
                @Override
                public void onIabPurchaseFinished(IabResult result, Purchase info) {
                    if (result.isFailure()) {
                        if (result.getResponse() == IabHelper.IABHELPER_USER_CANCELLED) {
                            // 交易取消
                            Firebase.getInstance(VIPActivity.this).logEvent("VIP交易", "交易取消", "一年");
                            Log.i(TAG, "onIabPurchaseFinished: 交易取消");
                        } else {
                            // 交易失败
                            Firebase.getInstance(VIPActivity.this).logEvent("VIP交易", "交易失败", "一年");
                            Log.i(TAG, "onIabPurchaseFinished: 交易失败");
                        }
                    } else {
                        Firebase.getInstance(VIPActivity.this).logEvent("VIP交易", "交易成功", "一年");
                        Log.i(TAG, "onIabPurchaseFinished: 交易成功的finish 一年");
                        //存个字段，说明是VIP用户
                        SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(VIPActivity.this);
                        sharedPreferences.edit().putBoolean(SharedPreferenceKey.VIP, true).apply();
                        sharedPreferences.edit().putBoolean(SharedPreferenceKey.IS_VIP_PAY_ONE_MONTH, false).apply();
                        sharedPreferences.edit().putBoolean(SharedPreferenceKey.IS_AUTOMATIC_RENEWAL_VIP, info.isAutoRenewing()).apply();
                        sharedPreferences.edit().putLong(SharedPreferenceKey.VIP_PAY_TIME, info.getPurchaseTime()).apply();
                        VIPFinishActivity.startVIPFinishActivity(VIPActivity.this, true);
                        finish();
                    }
                }
            }, "aaaa");
        } catch (IabAsyncInProgressException e) {
        }

        try {
            mHelper.queryInventoryAsync(false, skuList, null, mGotInventoryListener);
        } catch (IabAsyncInProgressException e) {
            e.printStackTrace();
        }
    }

    public static boolean isVIPUser(Context context) {
        SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(SharedPreferenceKey.VIP, false);
    }

//  Log.i(TAG, "onQueryInventoryFinished: PurchaseTime  " + oneMonthPurchase.getPurchaseTime() + "  DeveloperPayload: " + oneMonthPurchase.getDeveloperPayload()
//          + "  ItemType:   " + oneMonthPurchase.getItemType() + "  OrderId:  " + oneMonthPurchase.getOrderId() + "  OriginalJson:  " + oneMonthPurchase.getOriginalJson()
//          + "   Signature:   " + oneMonthPurchase.getSignature() + "   Sku:   " + oneMonthPurchase.getSku() + "   Token:  " + oneMonthPurchase.getToken() + "  PurchaseState:  "
//          + oneMonthPurchase.getPurchaseState() + "  AutoRenewing:  " + oneMonthPurchase.isAutoRenewing());
//  SkuDetails skuDetails = inventory.getSkuDetails(PAY_ONE_MONTH);
//  Log.i(TAG, "onQueryInventoryFinished: hasDetails:  " + inventory.hasDetails(PAY_ONE_MONTH) + "  Description:   " + skuDetails.getDescription() +
//          "   Price:   " + skuDetails.getPrice() + "   PriceCurrencyCode:  " + skuDetails.getPriceCurrencyCode() + "  Title:    " + skuDetails.getTitle() + "  Type:  " + skuDetails.getType()
//          + "  PriceAmountMicros:  " + skuDetails.getPriceAmountMicros() + "  Sku:  " + skuDetails.getSku());
//  Log.i(TAG, "onQueryInventoryFinished: 查询成功的finish 一个月 ");
//
//  onQueryInventoryFinished: PurchaseTime  1531280271124  DeveloperPayload: dddd  ItemType:   subs  OrderId:  GPA.3306-5952-9159-63663  OriginalJson:  {"orderId":"GPA.3306-5952-9159-63663","packageName":"com.androapplite.vpn10","productId":"one_2","purchaseTime":1531280271124,"purchaseState":0,"developerPayload":"dddd","purchaseToken":"obmcmldningcmghjgcekkonn.AO-J1OyvUD-p7-amKgW9E4dWVbMqum7z8ve0onhCeSPHmgPppZOQ4fCoGkNPv5-RkhoK_oMrapZ9M__Iw8tupmgJ8B42KbgZaQQ8hHVncvoOrXVM7RFSYGk","autoRenewing":true}   Signature:   ZTHEGvknH/TxzFxBEwRfBSpQOMihW0rXhc3oYWQvRtXCuBwU/kdv4Ft98Jbh6CrBWwdJiAuHIdaR2z+R56E0aQU6Kf53NS5iM3JwoHrR9m0Lg+w45cZg4nIVRcsABKoUZsHwxZiOE1zMj2OQru8izTnBn16SmqYexKfsFi09mnOPkEWK1Y+Q+TqyqdQYGWQLAWJj7tcuBg0qRa56tZcY9h1jC36Ohf9xotUEXCOJsmHARgeh50RvnRRdNveUoAy2U7CwSjmohCcvnx8sofwk98BNFwKCUn8RlxRcF3Ad3uXS+Eh5nv0Rh3WY+7gWAoJcqv1n/qFTaIRBbfYq+4Xzmg==   Sku:   one_2   Token:  obmcmldningcmghjgcekkonn.AO-J1OyvUD-p7-amKgW9E4dWVbMqum7z8ve0onhCeSPHmgPppZOQ4fCoGkNPv5-RkhoK_oMrapZ9M__Iw8tupmgJ8B42KbgZaQQ8hHVncvoOrXVM7RFSYGk  PurchaseState:  0  AutoRenewing:  true
//  onQueryInventoryFinished: hasDetails:  true  Description:   vip   Price:   $8.99   PriceCurrencyCode:  USD  Title:    Green VPN VIP (GREEN VPN)  Type:  subs  PriceAmountMicros:  8990000  Sku:  one_2
//  onQueryInventoryFinished: 查询成功的finish 一个月
}
