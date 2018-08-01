package com.androapplite.shadowsocks.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.ad.AdFullType;
import com.androapplite.shadowsocks.ad.AdUtils;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.connect.ConnectVpnHelper;
import com.androapplite.shadowsocks.fragment.NetworkAccelerationFinishFragment;
import com.androapplite.shadowsocks.fragment.NetworkAccelerationFragment;
import com.androapplite.shadowsocks.model.VpnState;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.androapplite.shadowsocks.service.ServerListFetcherService;
import com.androapplite.shadowsocks.service.VpnManageService;
import com.androapplite.shadowsocks.utils.RealTimeLogger;
import com.androapplite.shadowsocks.utils.RuntimeSettings;
import com.androapplite.vpn3.R;
import com.bestgo.adsplugin.ads.AdAppHelper;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.vm.shadowsocks.core.LocalVpnService;
import com.vm.shadowsocks.core.TcpTrafficMonitor;
import com.vm.shadowsocks.core.VpnNotification;

import java.lang.ref.WeakReference;

public class NetworkAccelerationActivity extends AppCompatActivity implements
        NetworkAccelerationFragment.NetworkAccelerationFragmentListener,
        LocalVpnService.onStatusChangedListener, Handler.Callback, View.OnClickListener,
        DialogInterface.OnDismissListener, NetworkAccelerationFinishFragment.NetworkAccelerationFinishFragmentListener {
    private static final String TAG = "NetworkAccelerationActi";

    private SharedPreferences mSharedPreference;
    private boolean mIsRestart;
    private Snackbar mSnackbar;
    private BroadcastReceiver mBroadcastReceiver;
    private ProgressDialog mFetchServerListProgressDialog;

    private Handler mHandler;
    private static final int MSG_DELAY_SHOW_INSTITIAL_AD = 1;
    private static int REQUEST_CONNECT = 1;
    private static final String EXTRA_AUTO = "EXTRA_AUTO";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_acceleration);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setElevation(0);
        final Drawable upArrow = getResources().getDrawable(R.drawable.ic_arrow_back_black_24dp);
        upArrow.setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_ATOP);
        actionBar.setHomeAsUpIndicator(upArrow);
        getSupportFragmentManager().beginTransaction().replace(R.id.content, new NetworkAccelerationFragment()).commitAllowingStateLoss();
        AdAppHelper.getInstance(this).loadNewNative();
        LocalVpnService.addOnStatusChangedListener(this);

        if (mSharedPreference == null) {
            mSharedPreference = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
        }
        if (mHandler == null) {
            mHandler = new Handler(this);
        }
        mBroadcastReceiver = new MyBroadcastReceiver(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Action.ACTION_NO_AVAILABLE_VPN);
        intentFilter.addAction(Action.SERVER_LIST_FETCH_FINISH);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof NetworkAccelerationFragment) {
            if (mSharedPreference == null) {
                mSharedPreference = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
            }
            if (mHandler == null) {
                mHandler = new Handler(this);
            }

            Intent intent = getIntent();
            boolean autoAccelerate = intent.getBooleanExtra(EXTRA_AUTO, false);
            if (autoAccelerate) {
                ((NetworkAccelerationFragment) fragment).rocketShake();
                startAccelerate();
            }
            if (FirebaseRemoteConfig.getInstance().getBoolean("is_full_rocket_enter_ad") && !RuntimeSettings.isVIP()) {
                Message message = mHandler.obtainMessage();
                message.what = MSG_DELAY_SHOW_INSTITIAL_AD;
                message.arg1 = 0;
                mHandler.sendMessage(message);
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (mBroadcastReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        }
        LocalVpnService.removeOnStatusChangedListener(this);
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        super.onDestroy();
    }

    @Override
    public void onAnimationFinish() { // 小火箭起飞动画结束的回调
        if (FirebaseRemoteConfig.getInstance().getBoolean("is_full_rocket_success_ad") && !RuntimeSettings.isVIP()) {
            Message message = mHandler.obtainMessage();
            message.what = MSG_DELAY_SHOW_INSTITIAL_AD;
            message.arg1 = 1;
            mHandler.sendMessage(message);
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.content, new NetworkAccelerationFinishFragment()).commitAllowingStateLoss();
    }


    @Override
    public void onVIPImageClick() {
        Firebase.getInstance(this).logEvent("加速成功结果页", "vip按钮", "点击");
        VIPActivity.startVIPActivity(this, VIPActivity.TYPE_NET_SPEED_FINISH);
    }

    @Override
    public void onStatusChanged(String status, Boolean isRunning) {
        if (isRunning) {
            Fragment fragment = getCurrentFragment();
            if (fragment instanceof NetworkAccelerationFragment) {
                ((NetworkAccelerationFragment) fragment).rocketFly();
                Firebase.getInstance(this).logEvent("网络加速", "加速", "加速成功");
            }
            RuntimeSettings.setVPNState(VpnState.Connected.ordinal());
        } else {
            if (mIsRestart) {
                mIsRestart = false;
                connectVpnServerAsync();
            } else {
                Fragment fragment = getCurrentFragment();
                if (getCurrentFragment() instanceof NetworkAccelerationFragment) {
                    ((NetworkAccelerationFragment) fragment).stopShake();
                }
            }
        }

    }

    @Override
    public void onLogReceived(String logString) {

    }

    @Override
    public void onTrafficUpdated(@Nullable TcpTrafficMonitor tcpTrafficMonitor) {

    }

    @Override
    public void onAccelerateImmediately() {
        Firebase.getInstance(this).logEvent("网络加速", "加速", "立即加速");
        mSharedPreference.edit().putInt("CLICK_SPEED_BT_COUNT", mSharedPreference.getInt("CLICK_SPEED_BT_COUNT", 0) + 1).apply();
        RealTimeLogger.answerLogEvent("click_speed_bt_count", "speed", "click_count:" + mSharedPreference.getInt("CLICK_SPEED_BT_COUNT", 0));
        RuntimeSettings.setRocketSpeedConnect(true);
        startAccelerate();
    }

    private void startAccelerate() {
        if (!ConnectVpnHelper.isFreeUse(this, ConnectVpnHelper.FREE_OVER_DIALOG_NET_SPEED)) { // 达到免费试用的时间
            return;
        }

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isAvailable()) {
                if (!AdUtils.isBadFullAdReady)
                    AdAppHelper.getInstance(this).loadFullAd(AdUtils.FULL_AD_BAD, 0);
                if (!LocalVpnService.IsRunning) {
                    mIsRestart = false;
                    connectVpnServerAsync();
                } else {
                    mIsRestart = true;
                    VpnManageService.stopVpnByUserSwitchProxy();
                    VpnNotification.gSupressNotification = true;
                }
                RuntimeSettings.setAutoSwitchProxy(false);
                RuntimeSettings.setVPNState(VpnState.Connecting.ordinal());
            } else {
                showNoInternetSnackbar(R.string.no_internet_message, false);
            }
        }
        RuntimeSettings.setVPNState(VpnState.Connecting.ordinal());
    }

    private Fragment getCurrentFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.content);
    }

    private class SwitchServerBackground implements Runnable {
        private Thread mThread;

        public SwitchServerBackground() {
            mThread = new Thread(this);
        }

        public void start() {
            mThread.start();
        }

        @Override
        public void run() {
            ConnectVpnHelper.getInstance(NetworkAccelerationActivity.this).reconnectVpn();
        }
    }

    private void connectVpnServerAsync() {
        if (mSharedPreference.contains(SharedPreferenceKey.FETCH_SERVER_LIST)) {
            prepareStartService();
            RuntimeSettings.setAutoSwitchProxy(false);
        } else {
            mFetchServerListProgressDialog = ProgressDialog.show(this, null, getString(R.string.fetch_server_list), true, false);
            ServerListFetcherService.fetchServerListAsync(this);
            mFetchServerListProgressDialog.setOnDismissListener(this);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        prepareStartService();
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_DELAY_SHOW_INSTITIAL_AD: // 0 进入  1 加速成功
                if (msg.arg1 == 0)
                    AdAppHelper.getInstance(this).showFullAd(AdUtils.FULL_AD_BAD, AdFullType.ROCKET_SPEED_ENTER_FULL_AD);
                else if (msg.arg1 == 1)
                    AdAppHelper.getInstance(this).showFullAd(AdUtils.FULL_AD_BAD, AdFullType.ROCKET_SPEED_FINISH_FULL_AD);
                break;
        }
        return true;
    }

    private void showNoInternetSnackbar(@StringRes int messageId, boolean hasAction) {
        Fragment fragment = getCurrentFragment();
        if (fragment instanceof NetworkAccelerationFragment) {
            ((NetworkAccelerationFragment) fragment).mNeedToShake = true;
            ((NetworkAccelerationFragment) fragment).stopShake();
        }
        final View decorView = findViewById(android.R.id.content);
        clearSnackbar();
        mSnackbar = Snackbar.make(decorView, messageId, Snackbar.LENGTH_LONG);
        if (hasAction) {
            mSnackbar.setAction(android.R.string.yes, this);
        }
        mSnackbar.getView().setTag(messageId);
        mSnackbar.show();
    }

    private void clearSnackbar() {
        if (mSnackbar != null && mSnackbar.isShown()) {
            mSnackbar.dismiss();
            mSnackbar = null;
        }
    }

    @Override
    public void onClick(View v) {

    }

    private static class MyBroadcastReceiver extends BroadcastReceiver {
        WeakReference<NetworkAccelerationActivity> mReference;

        MyBroadcastReceiver(NetworkAccelerationActivity accelerationActivity) {
            mReference = new WeakReference<>(accelerationActivity);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            NetworkAccelerationActivity activity = mReference.get();
            if (activity != null) {
                String action = intent.getAction();
                switch (action) {
                    case Action.SERVER_LIST_FETCH_FINISH:
                        activity.handleServerList();
                        break;
                    case Action.ACTION_NO_AVAILABLE_VPN:
                        activity.showNoInternetSnackbar(R.string.timeout_tip, false);
                        Fragment fragment = activity.getCurrentFragment();
                        if (fragment instanceof NetworkAccelerationFragment) {
                            ((NetworkAccelerationFragment) fragment).stopShake();
                        }
                        break;
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static void start(Context context, boolean auto) {
        Intent intent = new Intent(context, NetworkAccelerationActivity.class);
        intent.putExtra(EXTRA_AUTO, auto);
        context.startActivity(intent);
    }


    private void handleServerList() {
        if (mFetchServerListProgressDialog != null) {
            mFetchServerListProgressDialog.dismiss();
            mFetchServerListProgressDialog = null;

        }
    }

    private void prepareStartService() {
        try {
            Intent intent = VpnService.prepare(this);
            if (intent != null) {
                startActivityForResult(intent, REQUEST_CONNECT);
                ShadowsocksApplication.debug("ss-vpn", "startActivityForResult");
            } else {
                onActivityResult(REQUEST_CONNECT, Activity.RESULT_OK, null);
                ShadowsocksApplication.debug("ss-vpn", "onActivityResult");
            }
        } catch (Exception e) {
            showNoInternetSnackbar(R.string.not_start_vpn, false);
            ShadowsocksApplication.handleException(e);
            Firebase.getInstance(this).logEvent("VPN连不上", "VPN Prepare错误", e.getMessage());
            Fragment fragment = getCurrentFragment();
            if (fragment instanceof NetworkAccelerationFragment) {
                ((NetworkAccelerationFragment) fragment).stopShake();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CONNECT) {
                new SwitchServerBackground().start();
            }
        } else {
            if (requestCode == REQUEST_CONNECT) {
                showNoInternetSnackbar(R.string.enable_vpn_connection, true);
                Fragment fragment = getCurrentFragment();
                if (fragment instanceof NetworkAccelerationFragment) {
                    ((NetworkAccelerationFragment) fragment).stopShake();
                }

            }
        }
    }

    @Override
    protected void onStop() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        super.onStop();
    }
}