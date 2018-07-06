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
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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

import java.util.ArrayList;
import java.util.List;

public class VIPActivity extends AppCompatActivity implements IabBroadcastListener, View.OnClickListener {
    private static final String TAG = "VIPActivity";
    private static final String PUBLICK_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhHtkULOFh9/C7v27NwL4/RSke/jvbzM8w42+Rul4s59fVPz3QYRop7VA5m6ds37nHAOHgZs4Uc+CbXo4hgoQwZKFc+yIMyImKA3KPhWWh/yUQQuCdYWM3G1QfS5KdZfWULFXKyEZo6HfRbT8+Nu0tNaPbTqVzaDGsPeUMv+8G72cY8BnsJUv6PwyM/lGtHDQMjy8+dawVghj23OxmRZCTkAG9oM9ksOKN9vw5ToZ1wQOyhe78iePzOnLGIpjgV4G68C4wnoFyESf06dAsJSN//gAajZZv1SJVA8yB7o+RtIAEluZFr11XRqProziWE7buwjErWpQoAvZw27HxXNgCwIDAQAB";

    private static final int RC_REQUEST = 10001;
    private static String PAY_ONE_MONTH = "greenvpn10";
    private static String PAY_HALF_YEAR = "greenvpn10";

    private IabBroadcastReceiver mBroadcastReceiver;
    private IabHelper mHelper;
    private ImageView mVipOneMonthBt;
    private TextView mVipOneMonthMoneyText;
    private ImageView mVipHalfYearBt;
    private TextView mVipHalfYearMoneyText;
    private TextView mServerMessageText;
    private Button mVipFreeBt;
    private ActionBar mActionBar;

    List<String> productNameList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay_vip);

        PAY_ONE_MONTH = FirebaseRemoteConfig.getInstance().getString("vip_pay_one_money_id");
        PAY_HALF_YEAR = FirebaseRemoteConfig.getInstance().getString("vip_pay_half_year_id");

        mActionBar = getSupportActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setDisplayShowHomeEnabled(true);
        mActionBar.setElevation(0);
        final Drawable upArrow = getResources().getDrawable(R.drawable.ic_arrow_back_black_24dp);
        upArrow.setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_ATOP);
        mActionBar.setHomeAsUpIndicator(upArrow);

        initView();
        initUI();

        mHelper = new IabHelper(this, PUBLICK_KEY.trim());
//        productNameList.add("diyici");
//        productNameList.add("dierci"); // 添加消费的SKU，此字段在Google后台有保存，用来区别当前用户是否支付
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d(TAG, "Setup finished.");

                if (!result.isSuccess()) {
                    //helper设置失败是没法支付的，此处可以弹出提示框
                    return;
                }

                if (mHelper == null) return;

                mBroadcastReceiver = new IabBroadcastReceiver(VIPActivity.this);
                IntentFilter broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
                registerReceiver(mBroadcastReceiver, broadcastFilter);

                // IAB is fully set up. Now, let's get an inventory of stuff we own.
                Log.d(TAG, "Setup successful. Querying inventory.");
//                try {
//                    //必须查询是否有未消费的订单
//                    mHelper.queryInventoryAsync(mGotInventoryListener);
//                } catch (IabAsyncInProgressException e) {
//                    complain("Error querying inventory. Another async operation in progress.");
//                }
            }
        });

    }

    private void initView() {
        mVipOneMonthBt = (ImageView) findViewById(R.id.vip_one_month_bt);
        mVipOneMonthMoneyText = (TextView) findViewById(R.id.vip_one_month_money_text);
        mVipHalfYearBt = (ImageView) findViewById(R.id.vip_half_year_bt);
        mVipHalfYearMoneyText = (TextView) findViewById(R.id.vip_half_year_money_text);
        mServerMessageText = (TextView) findViewById(R.id.server_message_text);
        mVipFreeBt = (Button) findViewById(R.id.vip_free_bt);

        mVipOneMonthBt.setOnClickListener(this);
        mVipHalfYearBt.setOnClickListener(this);

        mActionBar.setTitle("VIP");
    }

    private void initUI() {
        mServerMessageText.setText(getResources().getString(R.string.more_than_countries, String.valueOf(3)));
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

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void complain(String message) {
        Log.e(TAG, "**** TrivialDrive Error: " + message);
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
    protected void onDestroy() {
        super.onDestroy();
        if (mHelper != null) {
            try {
                mHelper.dispose();
            } catch (IabAsyncInProgressException e) {
                e.printStackTrace();
            }
        }
        mHelper = null;

        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
        }
    }

    private IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        @Override
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            if (mHelper == null) return;

//            String diyici = inv.getSkuDetails("diyici").getPrice();

            if (result.isFailure()) {
                //在商品仓库中查询失败
                complain("Failed to queryinventory: " + result);
                return;
            }
            //获取未消费的订单对象Inventory
            Purchase purchase = inventory.getPurchase(PAY_ONE_MONTH);
            if (purchase != null && verifyDeveloperPayload(purchase)) {
                Log.e(TAG, "We have goods.");
//                try {
//                    mHelper.consumeAsync(inventory.getPurchase(SKU_GAS), mConsumeFinishedListener);
//                } catch (IabHelper.IabAsyncInProgressException e) {
//                    Log.e(TAG,"Error consuming gas. Another async operation in progress.");
//                }
                return;
            }
        }
    };

    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();
        return true;
    }

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
                complain("Error whileconsuming: " + result);
            }
        }
    };

    @Override
    public void receivedBroadcast() {

    }

    private void payOneMonth() {
        if (!mHelper.subscriptionsSupported()) {
            complain("Subscriptions not supported on your device yet. Sorry!");
            return;
        }

        mHelper.flagEndAsync();
        try {
            mHelper.launchPurchaseFlow(this, PAY_ONE_MONTH, RC_REQUEST, new IabHelper.OnIabPurchaseFinishedListener() {
                @Override
                public void onIabPurchaseFinished(IabResult result, Purchase info) {
                    if (result.isFailure()) {
                        if (result.getResponse() == IabHelper.IABHELPER_USER_CANCELLED) {
                            // 交易取消
                        } else {
                            // 交易失败

                        }
                    } else {
                        //存个字段，说明是VIP用户
                        SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(VIPActivity.this);
                        sharedPreferences.edit().putBoolean(SharedPreferenceKey.VIP, true).apply();
                    }
//                            try {
//                                mHelper.consumeAsync(info, mConsumeFinishedListener);
//                            } catch (IabAsyncInProgressException e) {
//                                e.printStackTrace();
//                            }
                }
            }, "");
        } catch (IabAsyncInProgressException e) {
            complain("Error launching purchase flow. Another async operation in progress.");
        }
    }

    private void payHalfYear() {
        if (!mHelper.subscriptionsSupported()) {
            complain("Subscriptions not supported on your device yet. Sorry!");
            return;
        }

        mHelper.flagEndAsync();
        try {
            mHelper.launchPurchaseFlow(this, PAY_HALF_YEAR, RC_REQUEST, new IabHelper.OnIabPurchaseFinishedListener() {
                @Override
                public void onIabPurchaseFinished(IabResult result, Purchase info) {
                    if (result.isFailure()) {
                        if (result.getResponse() == IabHelper.IABHELPER_USER_CANCELLED) {
                            // 交易取消
                        } else {
                            // 交易失败

                        }
                    } else {
                        //存个字段，说明是VIP用户
                        SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(VIPActivity.this);
                        sharedPreferences.edit().putBoolean(SharedPreferenceKey.VIP, true).apply();
                    }

//            Log.i(TAG, "onIabPurchaseFinished:  " + result.isSuccess() + "   " + result.isFailure() + "    " + result.getMessage()
//                    + "    " + info.getDeveloperPayload() + "   " + info.getItemType() + "   " + info.getOrderId() + "   " + info.getPackageName()
//                    + "    " + info.getSignature() + "   " + info.getSku() + "   " + info.getToken() + "    " + info.getPurchaseState()
//                    + "   " + info.getPurchaseTime());

//            try {
//                mHelper.consumeAsync(info, mConsumeFinishedListener);
//            } catch (IabAsyncInProgressException e) {
//                e.printStackTrace();
//            }
                }
            }, "");
        } catch (IabAsyncInProgressException e) {
            complain("Error launching purchase flow. Another async operation in progress.");
        }
    }

    public static boolean isVIPUser(Context context) {
        SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(SharedPreferenceKey.VIP, false);
    }
}
