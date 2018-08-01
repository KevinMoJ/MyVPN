package com.androapplite.shadowsocks.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.NotificationsUtils;
import com.androapplite.shadowsocks.Rotate3dAnimation;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.ad.AdFullType;
import com.androapplite.shadowsocks.ad.AdUtils;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.connect.ConnectVpnHelper;
import com.androapplite.shadowsocks.fragment.ConnectFragment;
import com.androapplite.shadowsocks.fragment.DisconnectFragment;
import com.androapplite.shadowsocks.model.ServerConfig;
import com.androapplite.shadowsocks.model.VpnState;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.androapplite.shadowsocks.serverlist.ServerListActivity;
import com.androapplite.shadowsocks.service.ServerListFetcherService;
import com.androapplite.shadowsocks.service.VpnManageService;
import com.androapplite.shadowsocks.utils.DialogUtils;
import com.androapplite.shadowsocks.utils.NetWorkSpeedUtils;
import com.androapplite.shadowsocks.utils.RealTimeLogger;
import com.androapplite.shadowsocks.utils.RuntimeSettings;
import com.androapplite.shadowsocks.view.ConnectTimeoutDialog;
import com.androapplite.shadowsocks.view.SnowFlakesLayout;
import com.androapplite.vpn3.R;
import com.bestgo.adsplugin.ads.AdAppHelper;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.vm.shadowsocks.core.LocalVpnService;
import com.vm.shadowsocks.core.TcpTrafficMonitor;
import com.vm.shadowsocks.core.VpnNotification;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements ConnectFragment.OnConnectActionListener,
        Handler.Callback, View.OnClickListener, DialogInterface.OnDismissListener,
        DisconnectFragment.OnDisconnectActionListener, LocalVpnService.onStatusChangedListener,
        NavigationView.OnNavigationItemSelectedListener, Animation.AnimationListener, ConnectTimeoutDialog.OnDialogBtClickListener {
    private Snackbar mSnackbar;
    private SharedPreferences mSharedPreference;
    private ProgressDialog mFetchServerListProgressDialog;
    private BroadcastReceiver mReceiver;
    private IntentFilter mIntentFilter;
    private ConnectFragment mConnectFragment;
    private Handler mForegroundHandler;
    private static final int MSG_CONNECTION_TIMEOUT = 1;
    private ServerConfig mConnectingConfig;
    private HashSet<ServerConfig> mErrorServers;
    private Handler mBackgroundHander;
    private HandlerThread mHandlerThread;
    private static final int MSG_PREPARE_START_VPN_BACKGROUND = 2;
    private static final int MSG_PREPARE_START_VPN_FORGROUND = 3;
    private static final int MSG_NO_AVAILABE_VPN = 4;
    private static final int MSG_REPEAT_MENU_ROCKET = 5;
    public static final int MSG_TEST_CONNECT_STATUS = 8;
    public static final int MSG_VPN_DELAYED_DISCONNECT = 9; // vpn延时断开，为了去加载广告
    public static final int MSG_DELAYED_UPDATE_VPN_CONNECT_STATE = 10; // vpn延时更新主界面按钮的状态，为了去加载广告
    private static int REQUEST_CONNECT = 1;
    private static int OPEN_SERVER_LIST = 2;
    private Menu mMenu;
    private VpnState mVpnState;
    private boolean mIsRestart;
    private AlertDialog mExitAlertDialog;
    private DisconnectFragment mDisconnectFragment;
    private AnimationSet mMenuRocketAnimation;
    private NetWorkSpeedUtils netWorkSpeedUtils;
    private SnowFlakesLayout mSnowFlakesLayout;
    private boolean isVIP;
    private boolean isExitAnimShow;//退出动画是否显示

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connectivity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        mSnowFlakesLayout = (SnowFlakesLayout) findViewById(R.id.main_snow_layout);
        setSupportActionBar(toolbar);
        initDrawer(toolbar);
        initNavigationView();
        mSharedPreference = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
        notProvideServiceInChina();
        mReceiver = new MyReceiver(this);
        mIntentFilter = new IntentFilter(Action.SERVER_LIST_FETCH_FINISH);
        mForegroundHandler = new Handler(this);
        mErrorServers = new HashSet<>();
        mHandlerThread = new HandlerThread("background handler");
        mHandlerThread.start();
        mBackgroundHander = new Handler(mHandlerThread.getLooper(), this);
        //用来判断活跃不活跃用户所存的时间
        RuntimeSettings.setOpenAppToDecideInactiveTime(System.currentTimeMillis());
        Firebase firebase = Firebase.getInstance(this);
        firebase.logEvent("屏幕", "主屏幕");
        checkNotification();

        LocalVpnService.addOnStatusChangedListener(this);
        int state = RuntimeSettings.getVPNState();
        mVpnState = VpnState.values()[state];
        if (LocalVpnService.IsRunning && mVpnState != VpnState.Connected) {
            mVpnState = VpnState.Connected;
            RuntimeSettings.setVPNState(mVpnState.ordinal());
        } else if (!LocalVpnService.IsRunning && mVpnState != VpnState.Stopped && mVpnState != VpnState.Init) {
            mVpnState = VpnState.Stopped;
            RuntimeSettings.setVPNState(mVpnState.ordinal());
        }
        isVIP = RuntimeSettings.isVIP();
        mConnectingConfig = ServerConfig.loadFromSharedPreference(mSharedPreference);
        if (FirebaseRemoteConfig.getInstance().getBoolean("is_full_enter_ad") && !isVIP) {
            AdAppHelper.getInstance(this).showFullAd(AdUtils.FULL_AD_GOOD, AdFullType.MAIN_ENTER_FULL_AD);
        }
    }

    private void notProvideServiceInChina() {
        String countryCode = mSharedPreference.getString(SharedPreferenceKey.COUNTRY_CODE, null);
        if (countryCode != null && "CN".equals(countryCode)) {
            File root = Environment.getExternalStorageDirectory();
            File packageFolder = new File(root, "." + getPackageName());
            File exception = new File(packageFolder, "exception.jim");
            if (!exception.exists()) {
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setMessage(R.string.china_ip)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .create();
                dialog.show();
            }
        }
    }

    private void initNavigationView() {
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setItemIconTintList(null);//取消统一着色
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void initDrawer(Toolbar toolbar) {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        drawer.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerClosed(View drawerView) {
                Firebase.getInstance(drawerView.getContext()).logEvent("菜单", "关闭");
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                Firebase.getInstance(drawerView.getContext()).logEvent("菜单", "打开");
            }
        });
        toggle.syncState();
    }

    @Override
    public void onConnectButtonClick() {
        startConnectVPN();
    }

    @Override
    public void onVIPImageClick() {
        Firebase.getInstance(this).logEvent("主页面小泡泡", "图片", "点击");
        jumpToVip(VIPActivity.TYPE_MAIN_PAO);
    }

    private void startConnectVPN() {
        Firebase firebase = Firebase.getInstance(this);
        if (!ConnectVpnHelper.isFreeUse(this, ConnectVpnHelper.FREE_OVER_DIALOG_MAIN)) // 达到免费试用的时间
            return;

        if (mVpnState == VpnState.Init || mVpnState == VpnState.Stopped ||
                mVpnState == VpnState.Error) {
            if (netWorkSpeedUtils == null) {
                netWorkSpeedUtils = new NetWorkSpeedUtils(this);
            }
            netWorkSpeedUtils.startShowNetSpeed();
            long cloudLoadAdTime = FirebaseRemoteConfig.getInstance().getLong("main_load_full_ad_time");
            if (!AdUtils.isGoodFullAdReady)
                AdAppHelper.getInstance(this).loadFullAd(AdUtils.FULL_AD_GOOD, (int) cloudLoadAdTime);

            if (checkConnection(isConnectionAvailable())) {
                connectVpnServerAsync();
            }
            firebase.logEvent("连接VPN", "连接");
            mSharedPreference.edit().putInt("CLICK_CONNECT_BT_COUNT", mSharedPreference.getInt("CLICK_CONNECT_BT_COUNT", 0) + 1).apply();
            RealTimeLogger.answerLogEvent("click_connect_bt_count", "connect", "connect_count:" + mSharedPreference.getInt("CLICK_CONNECT_BT_COUNT", 0));
            //不是小火箭加速的连接
            RuntimeSettings.setRocketSpeedConnect(false);
            if (mSharedPreference != null) {
                String nation = RuntimeSettings.getVPNNation("空");
                firebase.logEvent("选择国家", nation);
            }
            RuntimeSettings.setVPNStartConnectTime(System.currentTimeMillis());
//            PromotionTracking.getInstance(this).reportClickConnectButtonCount();
        } else {
            mDisconnectFragment = new DisconnectFragment();
            mDisconnectFragment.show(getSupportFragmentManager(), "disconnect");
            firebase.logEvent("连接VPN", "断开");
        }
    }

    private void connectVpnServerAsync() {
        //当拉不到服务器列表的时候重新拉一次  && mConnectingConfig != null
        if (mSharedPreference.contains(SharedPreferenceKey.FETCH_SERVER_LIST)) {
            connectVpnServerAsyncCore();
            RuntimeSettings.setAutoSwitchProxy(false);
        } else {
            mFetchServerListProgressDialog = ProgressDialog.show(this, null, getString(R.string.fetch_server_list), true, false);
            ServerListFetcherService.fetchServerListAsync(this);
            mFetchServerListProgressDialog.setOnDismissListener(this);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        connectVpnServerAsyncCore();
    }


    @Override
    public void onChangeServer() {
        startActivityForResult(new Intent(MainActivity.this, ServerListActivity.class), OPEN_SERVER_LIST);
    }

    @Override
    public void onTryAgain() {
        startConnectVPN();
    }

    private void connectVpnServerAsyncCore() {
        if (mConnectFragment != null) {
            mConnectFragment.animateConnecting();
            mVpnState = VpnState.Connecting;
        }
        mForegroundHandler.sendEmptyMessageDelayed(MSG_CONNECTION_TIMEOUT, TimeUnit.SECONDS.toMillis(60));
        mBackgroundHander.sendEmptyMessage(MSG_PREPARE_START_VPN_BACKGROUND);
    }


    private static class MyReceiver extends BroadcastReceiver {
        private WeakReference<MainActivity> mActivityReference;

        MyReceiver(MainActivity activity) {
            mActivityReference = new WeakReference<>(activity);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            MainActivity activity = mActivityReference.get();
            if (activity != null) {
                String action = intent.getAction();
                String serverListString = intent.getStringExtra(SharedPreferenceKey.FETCH_SERVER_LIST);
                switch (action) {
                    case Action.SERVER_LIST_FETCH_FINISH:
                        activity.handleServerList(serverListString);
                        break;
                }
            }
        }
    }

    private void updateFlagMenuIcon() {
        if (mMenu != null) {
            final String globalFlag = getResources().getResourceEntryName(isVIP ? R.drawable.icon_vip_server : R.drawable.ic_flag_global);
            final String flagKey = mVpnState == VpnState.Connected ? SharedPreferenceKey.CONNECTING_VPN_FLAG : SharedPreferenceKey.VPN_FLAG;
            final String flag = mSharedPreference.getString(flagKey, globalFlag);
            int resId = getResources().getIdentifier(flag, "drawable", getPackageName());
            MenuItem item = mMenu.findItem(R.id.action_flag);
            if (item != null) {
                if (resId == R.drawable.ic_flag_global && isVIP)
                    item.setIcon(R.drawable.icon_vip_server);
                else
                    item.setIcon(resId);
            }
        }
    }

    private void handleServerList(String serverListString) {
        if (mFetchServerListProgressDialog != null) {
            mFetchServerListProgressDialog.dismiss();
            mFetchServerListProgressDialog = null;
        }
        //双重保险
        if (!mSharedPreference.contains(SharedPreferenceKey.FETCH_SERVER_LIST)) {
            if (serverListString != null) {
                RuntimeSettings.setServerList(serverListString);
            }
        }
        if (!mSharedPreference.contains(SharedPreferenceKey.FETCH_SERVER_LIST)) {
            if (mFetchServerListProgressDialog != null)
                mFetchServerListProgressDialog.setOnDismissListener(null);
            showNoInternetSnackbar(R.string.fetch_server_list_failed, false);
            Firebase.getInstance(this).logEvent("VPN连不上", "取服务器列表超时");
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null) {
                Firebase.getInstance(this).logEvent("VPN连不上", "网络", networkInfo.getTypeName());
            } else {
                Firebase.getInstance(this).logEvent("VPN连不上", "网络", "未知");
            }
            if (mConnectFragment != null) {
                mConnectFragment.updateUI();
            }
            ConnectVpnHelper.getInstance(this).startTestConnectionWithOutVPN(ConnectVpnHelper.URL_BING, mConnectingConfig);
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof ConnectFragment) {
            mConnectFragment = (ConnectFragment) fragment;
        }
    }

    private Pair<Boolean, Integer> isConnectionAvailable() {
        Pair<Boolean, Integer> result = Pair.create(true, 0);
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Firebase firebase = Firebase.getInstance(this);
        if (connectivityManager == null) {
            result = Pair.create(false, R.string.no_network);
            firebase.logEvent("网络连接", "异常", "没有网络服务");
        } else {
            final NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo == null) {
                result = Pair.create(false, R.string.no_internet_message);
                firebase.logEvent("网络连接", "异常", "没有网络连接");

            } else if (!activeNetworkInfo.isConnected()) {
                result = Pair.create(false, R.string.not_available_internet);
                firebase.logEvent("网络连接", "异常", "当前网络连接不可用");
            }
        }
        return result;
    }

    private boolean checkConnection(Pair<Boolean, Integer> connectionStatus) {
        if (connectionStatus.first == false) {
            showNoInternetSnackbar(connectionStatus.second, true);
        }
        return connectionStatus.first;
    }

    private void clearSnackbar() {
        if (mSnackbar != null && mSnackbar.isShown()) {
            mSnackbar.dismiss();
            mSnackbar = null;
        }
    }

    private void showNoInternetSnackbar(@StringRes int messageId, boolean hasAction) {
        final View decorView = findViewById(android.R.id.content);
        clearSnackbar();
        mSnackbar = Snackbar.make(decorView, messageId, Snackbar.LENGTH_LONG);
        if (hasAction) {
            mSnackbar.setAction(android.R.string.yes, this);
        }
        mSnackbar.getView().setTag(messageId);
        mSnackbar.show();
    }

    public static void startLuckRotateActivity(Context context, boolean luckRotateShowAd) {
        Intent luckRotateIntent = new Intent(context, LuckRotateActivity.class);
        luckRotateIntent.putExtra(LuckRotateActivity.TYPE, luckRotateShowAd);
        Intent[] intents = {new Intent(context, MainActivity.class), luckRotateIntent};
        context.startActivities(intents);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.action_rocket:
                if (mVpnState == VpnState.Connecting) {
                    Toast.makeText(this, R.string.vpn_is_connecting, Toast.LENGTH_SHORT).show();
                } else if (mVpnState == VpnState.Stopping) {
                    Toast.makeText(this, R.string.vpn_is_stopping, Toast.LENGTH_SHORT).show();
                } else {
                    if (!AdUtils.isBadFullAdReady)
                        AdAppHelper.getInstance(this).loadFullAd(AdUtils.FULL_AD_BAD, 0);
                    Firebase.getInstance(this).logEvent("菜单", "小火箭");
                    NetworkAccelerationActivity.start(this, false);
                }
                break;
            case android.support.design.R.id.snackbar_action:
                Integer msg = (Integer) mSnackbar.getView().getTag();
                if (msg != null) {
                    int msgId = msg;
                    switch (msgId) {
                        case R.string.no_internet_message:
                        case R.string.not_available_internet:
                        case R.string.no_network:
                        case R.string.failed_to_open_wifi_setting:
                            try {
                                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                                Firebase.getInstance(v.getContext()).logEvent("网络连接", "异常", "打开WIFI");
                            } catch (ActivityNotFoundException e) {
                                ShadowsocksApplication.handleException(e);
                                showNoInternetSnackbar(R.string.failed_to_open_wifi_setting, false);
                                Firebase.getInstance(v.getContext()).logEvent("网络连接", "异常", "打开WIFI失败");
                            }
                            break;
                        case R.string.timeout_tip:
                            Log.d("main activyt", "timeout");
                            break;
                        case R.string.enable_notification:
                            NotificationsUtils.goToSet(this);
                            break;
                        case R.string.enable_vpn_connection:
                            connectVpnServerAsync();
                            break;
                    }
                }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkNotificationAndShowRemainder();
        checkConnection(isConnectionAvailable());
        registerReceiver();
        AdAppHelper.getInstance(this).onResume();
        isVIP = RuntimeSettings.isVIP();
        updateFlagMenuIcon();
        try {
            VpnService.prepare(this);
        } catch (Exception e) {
            ShadowsocksApplication.handleException(e);
        }
        if (mDisconnectFragment == null) {//adAppHelper.isNativeLoaded() &&
            addBottomAd();
        }
        AdAppHelper.getInstance(getApplicationContext()).loadNewNative();
        int state = RuntimeSettings.getVPNState();
        mVpnState = VpnState.values()[state];
        if (mConnectFragment != null)
            mConnectFragment.setConnectResult(mVpnState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver();
        AdAppHelper.getInstance(this).onPause();
    }

    private void registerReceiver() {
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.registerReceiver(mReceiver, mIntentFilter);
    }

    private void unregisterReceiver() {
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.unregisterReceiver(mReceiver);
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_REPEAT_MENU_ROCKET:
                if (mMenu != null) {
                    final ImageView rocketIV = (ImageView) mMenu.findItem(R.id.action_rocket).getActionView();
                    if (rocketIV != null) {
                        rocketIV.startAnimation(mMenuRocketAnimation);
                    }
                }
                break;
            case MSG_CONNECTION_TIMEOUT:
//                showNoInternetSnackbar(R.string.timeout_tip, false);
                ConnectTimeoutDialog connectTimeoutDialog = new ConnectTimeoutDialog();
                connectTimeoutDialog.show(getSupportFragmentManager(), "connectTimeOut");
                mVpnState = VpnState.Error;
                if (mConnectingConfig != null) {
                    mErrorServers.add(mConnectingConfig);
                    Firebase.getInstance(this).logEvent("VPN连不上", "VPN连接超时", mConnectingConfig.server);
                } else {
                    Firebase.getInstance(this).logEvent("VPN连不上", "VPN连接超时");
                }
                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo != null) {
                    Firebase.getInstance(this).logEvent("VPN连不上", "网络", networkInfo.getTypeName());
                } else {
                    Firebase.getInstance(this).logEvent("VPN连不上", "网络", "未知");
                }
                if (mConnectFragment != null) {
                    mConnectFragment.setConnectResult(mVpnState);
                    mConnectFragment.updateUI();
                }
                ConnectVpnHelper.getInstance(this).startTestConnectionWithOutVPN(ConnectVpnHelper.URL_BING, mConnectingConfig);
                ConnectVpnHelper.getInstance(this).release();
                break;
            case MSG_PREPARE_START_VPN_BACKGROUND:
                prepareStartVpnBackground();
                break;
            case MSG_PREPARE_START_VPN_FORGROUND:
                prepareStartService();
                break;
            case MSG_TEST_CONNECT_STATUS:
                if (!RuntimeSettings.isAutoSwitchProxy())
                    ConnectVpnHelper.getInstance(this).startConnectAfterFirstTest();
                break;
            case MSG_NO_AVAILABE_VPN:
                showNoInternetSnackbar(R.string.server_not_available, false);
                mVpnState = VpnState.Error;
                if (mConnectFragment != null) {
                    mConnectFragment.setConnectResult(mVpnState);
                    mConnectFragment.updateUI();
                }
                break;

            case MSG_VPN_DELAYED_DISCONNECT:
                VpnManageService.stopVpnByUser();
                disconnectVpnServiceAsync();
                break;
            case MSG_DELAYED_UPDATE_VPN_CONNECT_STATE:
                updateVPNConnectUIState();
                break;
        }

        return true;
    }

    private void prepareStartVpnBackground() {
        final ServerConfig serverConfig = ConnectVpnHelper.getInstance(this).findVPNServer();
        if (serverConfig != null) {
            mConnectingConfig = serverConfig;
            mForegroundHandler.sendEmptyMessage(MSG_PREPARE_START_VPN_FORGROUND);
            Log.d("MainActivity", String.format("server config: %s:%d", serverConfig.server, serverConfig.port));
        } else {
            boolean isValidation = ServerConfig.checkServerConfigJsonString(RuntimeSettings.getServerList());
            Firebase.getInstance(this).logEvent("VPN连不上", "没有可用的服务器", "服务器列表合法 " + isValidation);
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo != null) {
                    Firebase.getInstance(this).logEvent("VPN连不上", "网络", networkInfo.getTypeName());
                } else {
                    Firebase.getInstance(this).logEvent("VPN连不上", "网络", "未知");
                }
                mForegroundHandler.removeMessages(MSG_CONNECTION_TIMEOUT);
                mErrorServers.clear();
                mForegroundHandler.sendEmptyMessage(MSG_NO_AVAILABE_VPN);
                ConnectVpnHelper.getInstance(this).startTestConnectionWithOutVPN(ConnectVpnHelper.URL_BING, mConnectingConfig);
            }
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
            if (mConnectFragment != null) {
                mConnectFragment.updateUI();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CONNECT) {
                if (mConnectingConfig != null) {
                    LocalVpnService.ProxyUrl = mConnectingConfig.toProxyUrl();
                    Firebase.getInstance(this).logEvent("代理", mConnectingConfig.server);
                    if (LocalVpnService.ProxyUrl != null) {
                        startService(new Intent(this, LocalVpnService.class));
                    }
                }
            } else if (requestCode == OPEN_SERVER_LIST) {
                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                if (connectivityManager != null) {
                    NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                    if (networkInfo != null && networkInfo.isAvailable()) {
                        //为了广告的加载，在服务器列表切换的时候更新链接时间
                        RuntimeSettings.setVPNStartConnectTime(System.currentTimeMillis());
                        if (!LocalVpnService.IsRunning) {
                            mIsRestart = false;
                            connectVpnServerAsync();
                        } else {
                            mIsRestart = true;
                        }
                        mBackgroundHander.removeMessages(MSG_TEST_CONNECT_STATUS);
                        RuntimeSettings.setAutoSwitchProxy(false);
                        VpnManageService.stopVpnByUserSwitchProxy();
                        VpnNotification.gSupressNotification = true;
                        mVpnState = VpnState.Connecting;
                        RuntimeSettings.setVPNState(mVpnState.ordinal());
                    }
                }
//                PromotionTracking.getInstance(this).reportSwitchCountry();
            }
        } else {
            if (requestCode == REQUEST_CONNECT) {
                mVpnState = VpnState.Stopped;
                RuntimeSettings.setVPNState(mVpnState.ordinal());
                mForegroundHandler.removeMessages(MSG_CONNECTION_TIMEOUT);
                if (mConnectFragment != null && mConnectFragment.isAdded()) {
                    mConnectFragment.setConnectResult(mVpnState);
                }
                showNoInternetSnackbar(R.string.enable_vpn_connection, true);
            }
        }
    }

    @Override
    protected void onDestroy() {
        mForegroundHandler.removeCallbacksAndMessages(null);
        mHandlerThread.quit();
        mBackgroundHander.removeCallbacksAndMessages(null);
        LocalVpnService.removeOnStatusChangedListener(this);
        if (mMenuRocketAnimation != null) {
            mMenuRocketAnimation.cancel();
        }
        AdAppHelper.getInstance(this).appQuit();
        super.onDestroy();
    }

    @Override
    public void onDisconnect(DisconnectFragment disconnectFragment) {
        Firebase.getInstance(this).logEvent("连接VPN", "断开", "确认断开");
        if (!LocalVpnService.IsRunning) {
            //记录VPN连接开始的时间
            RuntimeSettings.setVPNStartTime(System.currentTimeMillis());
        }

        ConnectVpnHelper.getInstance(MainActivity.this).clearErrorList();
        ConnectVpnHelper.getInstance(MainActivity.this).release();
        long cloudLoadAdTime = FirebaseRemoteConfig.getInstance().getLong("main_load_full_ad_time");
        if (mConnectFragment != null) {
            if (!AdUtils.isGoodFullAdReady) {
                AdAppHelper.getInstance(this).loadFullAd(AdUtils.FULL_AD_GOOD, (int) cloudLoadAdTime);
                mForegroundHandler.sendEmptyMessageDelayed(MSG_VPN_DELAYED_DISCONNECT, cloudLoadAdTime * 1000);
                if (LocalVpnService.IsRunning) {
                    mConnectFragment.animateStopping();
                    mVpnState = VpnState.Stopping;
                } else {
                    mVpnState = VpnState.Stopped;
                    mConnectFragment.setConnectResult(mVpnState);
                }
            } else {
                if (LocalVpnService.IsRunning) {
                    mConnectFragment.animateStopping();
                    mVpnState = VpnState.Stopping;
                    VpnManageService.stopVpnByUser();
                } else {
                    mVpnState = VpnState.Stopped;
                    mConnectFragment.setConnectResult(mVpnState);
                }
                disconnectVpnServiceAsync();
            }
        }
    }

    private void disconnectVpnServiceAsync() {
        if (FirebaseRemoteConfig.getInstance().getBoolean("is_show_native_result_full")) {
            startResultActivity(VPNConnectResultActivity.VPN_RESULT_DISCONNECT);
        }
        mForegroundHandler.removeMessages(MSG_CONNECTION_TIMEOUT);
        if (netWorkSpeedUtils != null) {
            netWorkSpeedUtils.release();
            netWorkSpeedUtils = null;
        }
        mVpnState = VpnState.Stopped;
        RuntimeSettings.setVPNState(mVpnState.ordinal());
        if (mConnectFragment != null) {
            mConnectFragment.setConnectResult(mVpnState);
        }
    }

    @Override
    public void onDismiss(DisconnectFragment disconnectFragment) {
        mDisconnectFragment = null;
        addBottomAd();
    }

    private void checkNotification() {
        boolean isNotificationEnable = NotificationsUtils.isNotificationEnabled(this);
        boolean isNotificationDisabledCheck = mSharedPreference.getBoolean(SharedPreferenceKey.NOTIFICATION_DISABLE_CHECK, false);
        if (!isNotificationEnable) {
            if (isNotificationDisabledCheck) {
                Firebase.getInstance(this).logEvent("通知设置", "禁用", "禁用未启用");
            } else {
                Firebase.getInstance(this).logEvent("通知设置", "禁用", "禁用首次发现");
            }
            mSharedPreference.edit().putBoolean(SharedPreferenceKey.NOTIFICATION_DISABLE_CHECK, true).apply();
//            showNoInternetSnackbar(R.string.enable_notification, true);

        } else {
            mSharedPreference.edit().putBoolean(SharedPreferenceKey.NOTIFICATION_DISABLE_CHECK, false).apply();
            if (isNotificationDisabledCheck) {
                Firebase.getInstance(this).logEvent("通知设置", "启用", "通过设置打开通知");
            } else {
                Firebase.getInstance(this).logEvent("通知设置", "启用", NotificationsUtils.getNotificationImportance(this));
            }
        }
    }

    private void checkNotificationAndShowRemainder() {
        boolean isNotificationEnable = NotificationsUtils.isNotificationEnabled(this);
        if (!isNotificationEnable) {
            showNoInternetSnackbar(R.string.enable_notification, true);
        }
    }

    @NonNull
    private String getPlayStoreUrlString() {
        return " https://play.google.com/store/apps/details?id=" + getPackageName();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (isExitAnimShow) {
                isExitAnimShow = false;
                mSnowFlakesLayout.stopSnowingClear();
            } else {
                showAnim(1);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showAnim(int index) {//=0表示进入动画；=1表示退出动画
        switch (index) {
            case 0:
//      进入动画的操作
                break;
            case 1:
                if (isVIP) {
                    exitAppDialog();
                    break;
                }

                if (FirebaseRemoteConfig.getInstance().getBoolean("is_full_exit_ad")) {
                    if (AdUtils.isGoodFullAdReady) {
                        exitAppDialog();
                        break;
                    } else {
                        AdAppHelper.getInstance(this).loadFullAd(AdUtils.FULL_AD_GOOD, 5);
                        initSnowFlakes(1);
                        break;
                    }
                } else {
                    exitAppDialog();
                }
        }
    }

    private void initSnowFlakes(final int index) {
        isExitAnimShow = true;
        mSnowFlakesLayout.init();
        mSnowFlakesLayout.setWholeAnimateTiming(5000);
        mSnowFlakesLayout.setAnimateDuration(3000);
        mSnowFlakesLayout.setGenerateSnowTiming(100);
        mSnowFlakesLayout.setImageResourceID(R.drawable.icon_music_small);
        mSnowFlakesLayout.setEnableRandomCurving(true);
        mSnowFlakesLayout.setEnableAlphaFade(true);
        mSnowFlakesLayout.setFlakesEndListener(new SnowFlakesLayout.FlakesEndListener() {
            @Override
            public void onEndListener() {
                isExitAnimShow = false;
                if (index == 0) {
                    Firebase.getInstance(MainActivity.this).logEvent("进入广告未显示", "进入弹窗显示");
                } else {
                    Firebase.getInstance(MainActivity.this).logEvent("退出广告未显示", "退出弹窗显示");
                    exitAppDialog();
                }
            }
        });

        if (index == 0) {
            mSnowFlakesLayout.startSnowing(SnowFlakesLayout.LEFT_BOTTOM);
        } else {
            mSnowFlakesLayout.startSnowing(SnowFlakesLayout.RIGHT_BOTTOM);
        }
    }

    private void exitAppDialog() {
        isExitAnimShow = false;
        if (!isVIP && FirebaseRemoteConfig.getInstance().getBoolean("is_full_exit_ad")) {
            AdAppHelper.getInstance(this).showFullAd(AdUtils.FULL_AD_GOOD, AdFullType.MAIN_EXIT_FULL_AD);
        }

        final Firebase firebase = Firebase.getInstance(this);
        if (!isFinishing()) {
            mExitAlertDialog = new AlertDialog.Builder(this).setTitle(getResources().getString(R.string.exit))
                    .setMessage(getResources().getString(R.string.exit_vpn))
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                            firebase.logEvent("主页", "退出", "确定");
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            firebase.logEvent("主页", "退出", "取消");
                        }
                    })
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            mExitAlertDialog = null;
                        }
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;
        getMenuInflater().inflate(R.menu.connectivity, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        updateFlagMenuIcon();
        final ImageView rocketIV = (ImageView) menu.findItem(R.id.action_rocket).getActionView();
        if (rocketIV != null) {
            rocketIV.setScaleX(0.7f);
            rocketIV.setScaleY(0.7f);
            rocketIV.setImageResource(R.drawable.ic_flag_rocket);
            rocketIV.setOnClickListener(this);
            mMenuRocketAnimation = (AnimationSet) AnimationUtils.loadAnimation(this, R.anim.rocket_anim);
            mMenuRocketAnimation.setAnimationListener(this);
            mMenuRocketAnimation.getAnimations().get(0).setRepeatCount(5);
            mMenuRocketAnimation.getAnimations().get(0).setRepeatMode(Animation.REVERSE);
            mMenuRocketAnimation.getAnimations().get(0).setFillAfter(true);
            rocketIV.startAnimation(mMenuRocketAnimation);

        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_flag:
                if (mVpnState == VpnState.Connecting) {
                    Toast.makeText(this, R.string.vpn_is_connecting, Toast.LENGTH_SHORT).show();
                } else if (mVpnState == VpnState.Stopping) {
                    Toast.makeText(this, R.string.vpn_is_stopping, Toast.LENGTH_SHORT).show();
                } else {
                    if (!AdUtils.isBadFullAdReady)
                        AdAppHelper.getInstance(this).loadFullAd(AdUtils.FULL_AD_BAD, 0);
                    startActivityForResult(new Intent(this, ServerListActivity.class), OPEN_SERVER_LIST);
                    Firebase.getInstance(this).logEvent("菜单", "打开服务器列表");
                }
                return true;
            case R.id.luck_pan:
                if (!isVIP) {
                    if (!AdUtils.isBadFullAdReady)
                        AdAppHelper.getInstance(this).loadFullAd(AdUtils.FULL_AD_BAD, 0);
                    Firebase.getInstance(this).logEvent("主界面转盘按钮", "按钮", "点击");
                    LuckRotateActivity.startLuckActivity(this);
                } else {
                    Firebase.getInstance(this).logEvent("主界面转盘按钮", "会员", "点击");
                    DialogUtils.showVIPWelcomeDialog(this, null);
                }

                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onAnimationStart(Animation animation) {

    }

    @Override
    public void onAnimationEnd(Animation animation) {
        if (mMenu != null) {
            final ImageView rocketIV = (ImageView) mMenu.findItem(R.id.action_rocket).getActionView();
            if (rocketIV != null) {
                rocketIV.setScaleX(0.7f);
                rocketIV.setScaleY(0.7f);
                mForegroundHandler.sendEmptyMessageDelayed(MSG_REPEAT_MENU_ROCKET, 5000);
            }
        }
    }

    @Override
    public void onAnimationRepeat(Animation animation) {

    }

    private void updateVPNConnectUIState() {
        mConnectingConfig.saveInSharedPreference(mSharedPreference);
        if (!AdUtils.isGoodFullAdReady) {
            rotatedBottomAd();
        }
        mForegroundHandler.removeMessages(MSG_CONNECTION_TIMEOUT);
        mErrorServers.clear();
        boolean isFullConnectSuccessAdShow = FirebaseRemoteConfig.getInstance().getBoolean("is_full_connect_success_ad");
        if (!RuntimeSettings.isAutoSwitchProxy()
                && isFullConnectSuccessAdShow && !isVIP) {
            AdAppHelper.getInstance(getApplicationContext()).showFullAd(AdFullType.CONNECT_SUCCESS_FULL_AD);
        } else if (!RuntimeSettings.isAutoSwitchProxy()
                && FirebaseRemoteConfig.getInstance().getBoolean("is_show_native_result_full")
                && !RuntimeSettings.isRocketSpeedConnect()) {
            //记录VPN连接开始的时间
            RuntimeSettings.setVPNStartTime(System.currentTimeMillis());
            startResultActivity(VPNConnectResultActivity.VPN_RESULT_CONNECT);
        }
        mVpnState = VpnState.Connected;
        Firebase.getInstance(this).logEvent("VPN链接成功", mConnectingConfig.nation, mConnectingConfig.server);
        if (FirebaseRemoteConfig.getInstance().getBoolean("open_connect_test"))
            mBackgroundHander.sendEmptyMessageDelayed(MSG_TEST_CONNECT_STATUS, TimeUnit.SECONDS.toMillis(5));

        RuntimeSettings.setVPNState(mVpnState.ordinal());
        if (mConnectFragment != null) {
            mConnectFragment.setConnectResult(mVpnState);
        }

        updateFlagMenuIcon();
    }

    @Override
    public void onStatusChanged(String status, Boolean isRunning) {
        if (ConnectVpnHelper.getInstance(this).getCurrentConfig() != null)
            mConnectingConfig = ConnectVpnHelper.getInstance(this).getCurrentConfig();
        if (mConnectingConfig != null && mSharedPreference != null) {
            if (isRunning) {
                if (!RuntimeSettings.isRocketSpeedConnect()) {
                    long diff = System.currentTimeMillis() - RuntimeSettings.getVPNStartConnectTime();
                    long cloudLoadAdTime = FirebaseRemoteConfig.getInstance().getLong("main_load_full_ad_time") * 1000;
                    if (diff < cloudLoadAdTime) {
                        mForegroundHandler.sendEmptyMessageDelayed(MSG_DELAYED_UPDATE_VPN_CONNECT_STATE, cloudLoadAdTime - diff);
                    } else {
                        updateVPNConnectUIState();
                    }
                } else {
                    updateVPNConnectUIState();
                }
            } else {
                mForegroundHandler.removeMessages(MSG_CONNECTION_TIMEOUT);
                mVpnState = VpnState.Stopped;
                if (mIsRestart) {
                    mIsRestart = false;
                    mVpnState = VpnState.Connecting;
                    connectVpnServerAsync();
                }

                RuntimeSettings.setVPNState(mVpnState.ordinal());
                if (mConnectFragment != null) {
                    mConnectFragment.setConnectResult(mVpnState);
                }

                updateFlagMenuIcon();
            }
        }
    }

    private void startResultActivity(int type) {
        Intent intent = new Intent(this, VPNConnectResultActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(VPNConnectResultActivity.VPV_RESULT_TYPE, type);
        startActivity(intent);
    }

    @Override
    public void onLogReceived(String logString) {

    }

    @Override
    public void onTrafficUpdated(@Nullable TcpTrafficMonitor tcpTrafficMonitor) {

    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Firebase firebase = Firebase.getInstance(this);
        if (id == R.id.nav_rate_us) {
            rateUs();
            firebase.logEvent("菜单", "给我们打分");
        } else if (id == R.id.nav_share) {
            share();
            firebase.logEvent("抽屉", "分享");
        } else if (id == R.id.nav_contact_us) {
            contactUs();
            firebase.logEvent("菜单", "联系我们");
        } else if (id == R.id.nav_about) {
            about();
            firebase.logEvent("菜单", "关于");
        } else if (id == R.id.nav_vip) {
            jumpToVip(VIPActivity.TYPE_NAV);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void jumpToVip(int type) {
        if (!isVIP) {
            Firebase.getInstance(this).logEvent("侧边栏点击", "vip", "点击");
            VIPActivity.startVIPActivity(this, type);
        } else
            VIPFinishActivity.startVIPFinishActivity(this, false);
    }

    private void rateUs() {
        Uri uri = Uri.parse("market://details?id=" + getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        // To count with Play market backstack, After pressing back button,
        // to taken back to our application, we need to add following flags to intent.
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(getPlayStoreUrlString())));
            } catch (Exception ex) {
                ShadowsocksApplication.handleException(ex);
            }
        }
    }

    private void contactUs() {
        Intent data = new Intent(Intent.ACTION_SENDTO);
        data.setData(Uri.parse("mailto:watchfacedev@gmail.com"));
        data.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.app_name));
        data.putExtra(Intent.EXTRA_TEXT, "");
        try {
            startActivity(data);
        } catch (ActivityNotFoundException e) {
            ShadowsocksApplication.handleException(e);
        }
    }

    private void about() {
        PackageManager packageManager = getPackageManager();
        String packageName = getPackageName();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            String version = packageInfo.versionName;

            String appName = getResources().getString(R.string.app_name);

            new AlertDialog.Builder(this)
                    .setTitle(R.string.about)
                    .setMessage(appName + " (" + version + ")")
                    .show();
        } catch (PackageManager.NameNotFoundException e) {
            ShadowsocksApplication.handleException(e);
        }
    }

    private void share() {
        startActivity(new Intent(this, ShareActivity.class));
    }

    private void addBottomAd() {
        FrameLayout container = (FrameLayout) findViewById(R.id.ad_view_container);
        if (!isVIP) {
            container.setVisibility(View.VISIBLE);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER);
            try {
                AdAppHelper.getInstance(this).getNative(container, params);
                Firebase.getInstance(this).logEvent("NATIVE广告", "显示成功", "首页底部");
            } catch (Exception ex) {
                ShadowsocksApplication.handleException(ex);
                Firebase.getInstance(this).logEvent("NATIVE广告", "显示失败", "首页底部");
            }
        } else {
            container.setVisibility(View.GONE);
        }
    }

    private void rotatedBottomAd() {
        FrameLayout view = (FrameLayout) findViewById(R.id.ad_view_container);
        float centerX = view.getWidth() / 2.0f;
        float centerY = view.getHeight() / 2.0f;
        Rotate3dAnimation rotate3dAnimation = new Rotate3dAnimation(this, 0, 360, centerX, centerY, 0f, false, true);
        rotate3dAnimation.setDuration(1000);
        rotate3dAnimation.setFillAfter(false);
        view.startAnimation(rotate3dAnimation);
    }


}
