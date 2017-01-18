package com.androapplite.shadowsocks.activity;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.androapplite.shadowsocks.R;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.androapplite.shadowsocks.util.IabBroadcastReceiver;
import com.androapplite.shadowsocks.util.IabException;
import com.androapplite.shadowsocks.util.IabHelper;
import com.androapplite.shadowsocks.util.IabResult;
import com.androapplite.shadowsocks.util.Inventory;
import com.androapplite.shadowsocks.util.Purchase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class VIPActivity extends AppCompatActivity implements IabBroadcastReceiver.IabBroadcastListener
{
    private IabHelper mHelper;
    private IabBroadcastReceiver mBroadcastReceiver;
    private static final String TAG = "VIPActivity";
    private static final String MONTH_1 = "1_month";
    private static final String MONTH_3 = "3_month";
    private static final String MONTH_6 = "6_month";
    private static final String MONTH_12 = "12_month";
    private SharedPreferences mSharedPreference;
    private TextView mExipreDateTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vip);

        RadioButton month3 = (RadioButton) findViewById(R.id.month_3);
        month3.setChecked(true);

        mExipreDateTextView = (TextView) findViewById(R.id.exipre_date);
        mSharedPreference = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(VIPActivity.this);
        long expiredDate = mSharedPreference.getLong(SharedPreferenceKey.EXPIRED_DATE, 0);
        if(expiredDate >= 0){
            mExipreDateTextView.setVisibility(View.VISIBLE);
            mExipreDateTextView.setText("VIP is expired on " + new SimpleDateFormat().format(new Date(expiredDate)));
        }else{
            mExipreDateTextView.setVisibility(View.INVISIBLE);
        }

        String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqiGgwJ4lRgcxm6Me89gDA06hxa6ai7osITKPN4hX/+pJydF1KokAPr54Me0RDmhhKS/bAHqdjqma6NQWx0aHd5uOM6rRaXWhMBFIEdRFSi6WFcNUytinDRD1e7MhOdOyguAYIxiPnVCn0SlHCYioILCuNh55s/7jsFgStGj0qCkZHX+gW46Sei7XPUatMkXHatYHoJpyqvwJr24pIok6+kQOTSarNvScaMlP3Dj8hTDSRQ5PsQeN18ystKvEVW6g8e+gCHef/PwqSOkfr49cbbsMaYhjnddbUn423BI++wR56N3KMIpJkAjw5X4wlyD1HfP3QK4Dez1gLpwHdsUxLQIDAQAB";

        // compute your public key and store it in base64EncodedPublicKey
        mHelper = new IabHelper(this, base64EncodedPublicKey);
        mHelper.enableDebugLogging(true);
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d(TAG, "Setup finished.");

                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    complain("Problem setting up in-app billing: " + result);
                    return;
                }

                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null) return;

                // Important: Dynamically register for broadcast messages about updated purchases.
                // We register the receiver here instead of as a <receiver> in the Manifest
                // because we always call getPurchases() at startup, so therefore we can ignore
                // any broadcasts sent while the app isn't running.
                // Note: registering this listener in an Activity is a bad idea, but is done here
                // because this is a SAMPLE. Regardless, the receiver must be registered after
                // IabHelper is setup, but before first call to getPurchases().
                mBroadcastReceiver = new IabBroadcastReceiver(VIPActivity.this);
                IntentFilter broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
                registerReceiver(mBroadcastReceiver, broadcastFilter);

                // IAB is fully set up. Now, let's get an inventory of stuff we own.
                Log.d(TAG, "Setup successful. Querying inventory.");
                try {
                    mHelper.queryInventoryAsync(mGotInventoryListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    complain("Error querying inventory. Another async operation in progress.");
                }
            }
        });
    }

    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "Query inventory finished.");

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;

            // Is it a failure?
            if (result.isFailure()) {
                complain("Failed to query inventory: " + result);
                return;
            }

            Log.d(TAG, "Query inventory was successful.");

            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */

            Log.d(TAG, "Initial inventory query finished; enabling main UI.");
        }
    };

    void complain(String message) {
        Log.e(TAG, "**** TrivialDrive Error: " + message);
        alert("Error: " + message);
    }


    void alert(String message) {
        AlertDialog.Builder bld = new AlertDialog.Builder(this);
        bld.setMessage(message);
        bld.setNeutralButton("OK", null);
        Log.d(TAG, "Showing alert dialog: " + message);
        bld.create().show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // very important:
        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
        }

        // very important:
        Log.d(TAG, "Destroying helper.");
        if (mHelper != null) {
            mHelper.disposeWhenFinished();
            mHelper = null;
        }
    }

    @Override
    public void receivedBroadcast() {
        // Received a broadcast notification that the inventory of items has changed
        Log.d(TAG, "Received broadcast notification. Querying inventory.");
        try {
            mHelper.queryInventoryAsync(mGotInventoryListener);
        } catch (IabHelper.IabAsyncInProgressException e) {
            complain("Error querying inventory. Another async operation in progress.");
        }
    }

    public void buyVip(View v){
        RadioGroup radioGroup = (RadioGroup)findViewById(R.id.payment_group);
        int id = radioGroup.getCheckedRadioButtonId();
        switch (id){
            case R.id.month_1:
                purchaseVip(MONTH_1);
                break;
            case R.id.month_3:
                purchaseVip(MONTH_3);
                break;
            case R.id.month_6:
                purchaseVip(MONTH_6);
                break;
            case R.id.month_12:
                purchaseVip(MONTH_12);
                break;
        }
    }


    private void purchaseVip(String sku){
        try {
            mHelper.launchPurchaseFlow(this, sku, 1001, mPurchaseFinishedListener, "bGoa+V7g/yqDXvKRqq+JTFn4uQZbPiQJo4pf9RzJ");
        } catch (IabHelper.IabAsyncInProgressException e) {
            ShadowsocksApplication.handleException(e);
        }
    }

//    // Callback for when a purchase is finished
//    @Override
//    public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
//        Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);
//
//        // if we were disposed of in the meantime, quit.
//        if (mHelper == null) return;
//
//        if (result.isFailure()) {
//            complain("Error purchasing: " + result);
//            return;
//        }
//
//        try {
//            mHelper.consumeAsync(purchase, m);
//        } catch (IabHelper.IabAsyncInProgressException e) {
//            ShadowsocksApplication.handleException(e);
//        }
//        Log.d(TAG, purchase.toString());
//        Toast.makeText(this, purchase.toString(),Toast.LENGTH_SHORT).show();
//    }
//
//    @Override
//    public void onConsumeFinished(Purchase purchase, IabResult result) {
//        if (result.isFailure()) {
//            complain("Error purchasing: " + result);
//            return;
//        }
//        String sku = purchase.getSku();
//        switch (sku){
//            case MONTH_1:
//                break;
//            case MONTH_3:
//                break;
//            case MONTH_6:
//                break;
//            case MONTH_12:
//                break;
//        }
//        Log.d(TAG, purchase.toString());
//        Toast.makeText(this, purchase.toString(),Toast.LENGTH_SHORT).show();
//
//    }

    private Purchase getPurchase(String sku){
        try {
            Inventory inventory = mHelper.queryInventory();
            Purchase purchase = inventory.getPurchase(sku);
            return purchase;
        } catch (IabException e) {
            e.printStackTrace();
        }
        return  null;
    }

    private void consumePurchase(Purchase purchase){
        try {
            mHelper.consumeAsync(purchase, mConsumeFinishedListener);
        } catch (IabHelper.IabAsyncInProgressException e) {
            ShadowsocksApplication.handleException(e);
        }
    }

    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        @Override
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
                complain("Error purchasing: " + result);
                return;
            }

            try {
                mHelper.consumeAsync(purchase, mConsumeFinishedListener);
            } catch (IabHelper.IabAsyncInProgressException e) {
                ShadowsocksApplication.handleException(e);
            }
            Log.d(TAG, purchase.toString());
//            Toast.makeText(VIPActivity.this, purchase.toString(),Toast.LENGTH_SHORT).show();
        }
    };

    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        @Override
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            if (result.isFailure()) {
                complain("Error purchasing: " + result);
                return;
            }

            long expireDate = mSharedPreference.getLong(SharedPreferenceKey.EXPIRED_DATE, 0);
            Calendar calendar = Calendar.getInstance();
            if(expireDate > calendar.getTimeInMillis()){
                calendar.setTimeInMillis(expireDate);
            }
            String sku = purchase.getSku();
            switch (sku){
                case MONTH_1:
                    calendar.add(Calendar.MONTH, 1);
                    break;
                case MONTH_3:
                    calendar.add(Calendar.MONTH, 3);
                    break;
                case MONTH_6:
                    calendar.add(Calendar.MONTH, 6);
                    break;
                case MONTH_12:
                    calendar.add(Calendar.MONTH, 12);
                    break;
            }
            expireDate = calendar.getTimeInMillis();
            mSharedPreference.edit()
                    .putLong(SharedPreferenceKey.EXPIRED_DATE, expireDate)
                    .remove(SharedPreferenceKey.SERVER_LIST)
                    .commit();

            mExipreDateTextView.setVisibility(View.VISIBLE);
            mExipreDateTextView.setText("VIP is expired on " + new SimpleDateFormat().format(calendar.getTime()));

            Log.d(TAG, purchase.toString());
//            Toast.makeText(VIPActivity.this, purchase.toString(),Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        if (mHelper == null) return;

        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        }
        else {
            Log.d(TAG, "onActivityResult handled by IABUtil.");
        }
    }
}
