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
import com.androapplite.shadowsocks.PromotionTracking;
import com.androapplite.shadowsocks.Rotate3dAnimation;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.connect.ConnectVpnHelper;
import com.androapplite.shadowsocks.fragment.ConnectFragment;
import com.androapplite.shadowsocks.fragment.DisconnectFragment;
import com.androapplite.shadowsocks.model.ServerConfig;
import com.androapplite.shadowsocks.model.VpnState;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.androapplite.shadowsocks.service.ServerListFetcherService;
import com.androapplite.shadowsocks.service.VpnManageService;
import com.androapplite.shadowsocks.view.ConnectTimeoutDialog;
import com.androapplite.vpn3.R;
import com.bestgo.adsplugin.ads.AdAppHelper;
import com.bestgo.adsplugin.ads.AdType;
import com.bestgo.adsplugin.ads.listener.AdStateListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.vm.shadowsocks.core.LocalVpnService;
import com.vm.shadowsocks.core.TcpTrafficMonitor;
import com.vm.shadowsocks.core.VpnNotification;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import static com.bestgo.adsplugin.ads.AdType.ADMOB_FULL;

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
    public static final int MSG_SHOW_AD_BUTTON_RECOMMEND = 6;
    public static final int MSG_SHOW_AD_BUTTON_FULL = 7;
    public static final int MSG_TEST_CONNECT_STATUS = 8;
    public static final int MSG_SHOW_INTERSTITIAL_ENTER = 9;
    public static final int MSG_SHOW_INTERSTITIAL_EXIT = 10;
    private static int REQUEST_CONNECT = 1;
    private static int OPEN_SERVER_LIST = 2;
    private Menu mMenu;
    private VpnState mVpnState;
    private boolean mIsRestart;
    private AlertDialog mExitAlertDialog;
    private DisconnectFragment mDisconnectFragment;
    private AnimationSet mMenuRocketAnimation;
    private int mAdMsgType;
    private FullAdStatusListener mFullAdStatusListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connectivity);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
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
        mSharedPreference.edit().putLong(SharedPreferenceKey.OPEN_APP_TIME_TO_DECIDE_INACTIVE_USER, System.currentTimeMillis()).apply();
        Firebase firebase = Firebase.getInstance(this);
        firebase.logEvent("屏幕","主屏幕");
        checkNotification();

        LocalVpnService.addOnStatusChangedListener(this);
        int state = mSharedPreference.getInt(SharedPreferenceKey.VPN_STATE, VpnState.Init.ordinal());
        mVpnState = VpnState.values()[state];
        if (LocalVpnService.IsRunning && mVpnState != VpnState.Connected) {
            mVpnState = VpnState.Connected;
            mSharedPreference.edit().putInt(SharedPreferenceKey.VPN_STATE, mVpnState.ordinal()).apply();
        } else if (!LocalVpnService.IsRunning && mVpnState != VpnState.Stopped && mVpnState != VpnState.Init) {
            mVpnState = VpnState.Stopped;
            mSharedPreference.edit().putInt(SharedPreferenceKey.VPN_STATE, mVpnState.ordinal()).apply();
        }
        mConnectingConfig = ServerConfig.loadFromSharedPreference(mSharedPreference);

        final AdAppHelper adAppHelper = AdAppHelper.getInstance(getApplicationContext());
        adAppHelper.checkUpdate(this);
        showInterstitialWithDelay(MSG_SHOW_INTERSTITIAL_ENTER, "enter_ad", "enter_ad_min", "200", "enter_ad_max", "200");
    }

    private void showInterstitialWithDelay(int msg, String adShowRate, String adDelayMin, String adDelayMinDefault, String adDelayMax, String adDelayMaxDefault ) {
        AdAppHelper adAppHelper = AdAppHelper.getInstance(this);
        String enterAdSwitch = adAppHelper.getCustomCtrlValue(adShowRate, "1");
        float enterAdRate;
        try {
            enterAdRate = Float.parseFloat(enterAdSwitch);
        } catch (Exception e) {
            enterAdRate = 0;
        }
        if (Math.random() < enterAdRate) {
            if (adAppHelper.isFullAdLoaded()) {
                String enterAdMinS = adAppHelper.getCustomCtrlValue(adDelayMin, adDelayMinDefault);
                int enterAdMin;
                try{
                    enterAdMin = Integer.valueOf(enterAdMinS);
                } catch (Exception e) {
                    enterAdMin = 0;
                }
                String enterAdMaxS = adAppHelper.getCustomCtrlValue(adDelayMax, adDelayMaxDefault);
                int enterAdMax;
                try{
                    enterAdMax = Integer.valueOf(enterAdMaxS);
                } catch (Exception e) {
                    enterAdMax = 0;
                }
                mForegroundHandler.sendEmptyMessageDelayed(msg, (long) (Math.random() * enterAdMax + enterAdMin));
                if (mFullAdStatusListener == null)
                    mFullAdStatusListener = new FullAdStatusListener(this);
                mAdMsgType = msg;
                adAppHelper.addAdStateListener(mFullAdStatusListener);
            }
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

    private void initNavigationView(){
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

    private static class InterstitialAdStateListener extends AdStateListener {
        private WeakReference<MainActivity> mActivityReference;
        InterstitialAdStateListener(MainActivity activity){
            mActivityReference = new WeakReference<>(activity);
        }

        @Override
        public void onAdLoaded(AdType adType, int index) {
            MainActivity activity = mActivityReference.get();
            if(activity != null) {
                switch (adType.getType()) {
                    case AdType.ADMOB_NATIVE:
                    case AdType.ADMOB_BANNER:
                    case AdType.FACEBOOK_BANNER:
                    case AdType.FACEBOOK_NATIVE:
                    case AdType.FACEBOOK_FBN_BANNER:
                        activity.addBottomAd();
                        break;
                }
            }
        }

        @Override
        public void onAdOpen(AdType adType, int index) {
            MainActivity activity = mActivityReference.get();
            if(activity != null){
                switch (adType.getType()){
                    case ADMOB_FULL:
                    case AdType.FACEBOOK_FBN:
                    case AdType.FACEBOOK_FULL:
                        break;
                }
            }
        }

        @Override
        public void onAdClick(AdType adType, int index) {
            MainActivity activity = mActivityReference.get();
            if (activity != null) {
                switch (adType.getType()) {
                    case AdType.ADMOB_FULL:
                    case AdType.ADMOB_NATIVE_FULL:
                    case AdType.FACEBOOK_FULL:
                    case AdType.FACEBOOK_FBN:
                        Firebase.getInstance(activity).logEvent("主界面", "全屏", "点击");
                        break;
                }
                AdAppHelper.getInstance(activity).removeAdStateListener(this);
            }
        }

        @Override
        public void onAdClosed(AdType adType, int index) {
            MainActivity activity = mActivityReference.get();
            if(activity != null){
                switch (adType.getType()){
                    case ADMOB_FULL:
                    case AdType.FACEBOOK_FBN:
                    case AdType.FACEBOOK_FULL:
                        if (activity.mExitAlertDialog == null && activity.mVpnState == VpnState.Connected) {
                            activity.rotatedBottomAd();
                        }
                        break;
                }
            }

        }
    }

    @Override
    public void onConnectButtonClick() {
       startConnectVPN();
    }

    private void startConnectVPN() {
        Firebase firebase = Firebase.getInstance(this);
        if (mVpnState == VpnState.Init || mVpnState == VpnState.Stopped ||
                mVpnState == VpnState.Error) {
            if(checkConnection(isConnectionAvailable())) {
                connectVpnServerAsync();
            }
            firebase.logEvent("连接VPN", "连接");
            if(mSharedPreference != null) {
                String nation = mSharedPreference.getString(SharedPreferenceKey.VPN_NATION, "空");
                firebase.logEvent("选择国家", nation);
            }
            if (mSharedPreference != null)
                mSharedPreference.edit().putLong(SharedPreferenceKey.VPN_CONNECT_START_TIME, System.currentTimeMillis()).apply();
            PromotionTracking.getInstance(this).reportClickConnectButtonCount();
        } else {
            mDisconnectFragment = new DisconnectFragment();
            mDisconnectFragment.show(getSupportFragmentManager(), "disconnect");
            firebase.logEvent("连接VPN", "断开");
        }
    }

    private void connectVpnServerAsync() {
        //当拉不到服务器列表的时候重新拉一次
        if (mSharedPreference.contains(SharedPreferenceKey.FETCH_SERVER_LIST) && mConnectingConfig != null) {
            connectVpnServerAsyncCore();
            mSharedPreference.edit().putBoolean(SharedPreferenceKey.IS_AUTO_SWITCH_PROXY, false).apply();
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

    private void connectVpnServerAsyncCore(){
        if(mConnectFragment != null){
            mConnectFragment.animateConnecting();
            mVpnState = VpnState.Connecting;
        }
        mForegroundHandler.sendEmptyMessageDelayed(MSG_CONNECTION_TIMEOUT, TimeUnit.SECONDS.toMillis(32));
        mBackgroundHander.sendEmptyMessage(MSG_PREPARE_START_VPN_BACKGROUND);
    }


    private static class MyReceiver extends BroadcastReceiver{
        private WeakReference<MainActivity> mActivityReference;

        MyReceiver(MainActivity activity){
            mActivityReference = new WeakReference<>(activity);
        }
        @Override
        public void onReceive(Context context, Intent intent) {
            MainActivity activity = mActivityReference.get();
            if(activity != null){
                String action = intent.getAction();
                String serverListString = intent.getStringExtra(SharedPreferenceKey.FETCH_SERVER_LIST);
                switch(action){
                    case Action.SERVER_LIST_FETCH_FINISH:
                        activity.handleServerList(serverListString);
                        break;
                }
            }
        }
    }

    private void updateFlagMenuIcon() {
        if(mMenu != null) {
            final String globalFlag = getResources().getResourceEntryName(R.drawable.ic_flag_global);
            final String flagKey = mVpnState == VpnState.Connected ? SharedPreferenceKey.CONNECTING_VPN_FLAG : SharedPreferenceKey.VPN_FLAG;
            final String flag = mSharedPreference.getString(flagKey, globalFlag);
            int resId = getResources().getIdentifier(flag, "drawable", getPackageName());
            MenuItem item = mMenu.findItem(R.id.action_flag);
            if (item != null) {
                item.setIcon(resId);
            }
        }
    }

    private void handleServerList(String serverListString){
        if(mFetchServerListProgressDialog != null){
            mFetchServerListProgressDialog.dismiss();
            mFetchServerListProgressDialog = null;
        }
        //双重保险
        if(!mSharedPreference.contains(SharedPreferenceKey.FETCH_SERVER_LIST)) {
            if(serverListString != null) {
                mSharedPreference.edit().putString(SharedPreferenceKey.FETCH_SERVER_LIST, serverListString).apply();
            }
        }
        if(!mSharedPreference.contains(SharedPreferenceKey.FETCH_SERVER_LIST)) {
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
            if(mConnectFragment != null){
                mConnectFragment.updateUI();
            }
            ConnectVpnHelper.getInstance(this).startTestConnectionWithOutVPN(ConnectVpnHelper.URL_BING, mConnectingConfig);
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if(fragment instanceof ConnectFragment){
            mConnectFragment = (ConnectFragment)fragment;
        }
    }

    private Pair<Boolean, Integer> isConnectionAvailable(){
        Pair<Boolean, Integer> result = Pair.create(true, 0);
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        Firebase firebase = Firebase.getInstance(this);
        if(connectivityManager == null){
            result = Pair.create(false, R.string.no_network);
            firebase.logEvent("网络连接","异常","没有网络服务");
        }else{
            final NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if(activeNetworkInfo == null){
                result = Pair.create(false, R.string.no_internet_message);
                firebase.logEvent("网络连接","异常","没有网络连接");

            }else if(!activeNetworkInfo.isConnected()){
                result = Pair.create(false, R.string.not_available_internet);
                firebase.logEvent("网络连接","异常","当前网络连接不可用");
            }
        }
        return result;
    }

    private boolean checkConnection(Pair<Boolean, Integer> connectionStatus){
        if(connectionStatus.first == false){
            showNoInternetSnackbar(connectionStatus.second, true);
        }
        return connectionStatus.first;
    }

    private void clearSnackbar() {
        if(mSnackbar != null && mSnackbar.isShown()){
            mSnackbar.dismiss();
            mSnackbar = null;
        }
    }

    private void showNoInternetSnackbar(@StringRes int messageId, boolean hasAction) {
        final View decorView = findViewById(android.R.id.content);
        clearSnackbar();
        mSnackbar = Snackbar.make(decorView, messageId, Snackbar.LENGTH_LONG);
        if(hasAction) {
            mSnackbar.setAction(android.R.string.yes, this);
        }
        mSnackbar.getView().setTag(messageId);
        mSnackbar.show();
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

    private class FullAdStatusListener extends AdStateListener {
        private WeakReference<MainActivity> mReference;

        FullAdStatusListener(MainActivity activity) {
            mReference = new WeakReference<>(activity);
        }

        @Override
        public void onAdClick(AdType adType, int index) {
            MainActivity activity = mReference.get();
            switch (adType.getType()) {
                case AdType.ADMOB_FULL:
                case AdType.FACEBOOK_FBN:
                case AdType.FACEBOOK_FULL:
                case AdType.RECOMMEND_AD:
                    switch (activity.mAdMsgType) {
                        case MSG_SHOW_AD_BUTTON_FULL:
                            Firebase.getInstance(activity).logEvent("全屏广告", "点击", "主界面广告按钮全屏");
                            break;
                        case MSG_SHOW_AD_BUTTON_RECOMMEND:
                            Firebase.getInstance(activity).logEvent("全屏广告", "点击", "主界面广告按钮线上互推全屏");
                            break;
                        case MSG_SHOW_INTERSTITIAL_ENTER:
                            Firebase.getInstance(activity).logEvent("主界面", "进入全屏", "点击");
                            break;
                        case MSG_SHOW_INTERSTITIAL_EXIT:
                            Firebase.getInstance(activity).logEvent("主界面", "退出全屏", "点击");
                            break;
                    }
                    AdAppHelper.getInstance(activity).removeAdStateListener(this);
                    break;
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
        updateFlagMenuIcon();
        try {
            VpnService.prepare(this);
        } catch (Exception e) {
            ShadowsocksApplication.handleException(e);
        }
        final AdAppHelper adAppHelper = AdAppHelper.getInstance(getApplicationContext());
        if (adAppHelper.isNativeLoaded() && mDisconnectFragment == null) {
            addBottomAd();
        }
        adAppHelper.loadNewNative();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver();
        AdAppHelper.getInstance(this).onPause();
    }

    private void registerReceiver(){
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.registerReceiver(mReceiver, mIntentFilter);
    }

    private void unregisterReceiver(){
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.unregisterReceiver(mReceiver);
    }

    @Override
    public boolean handleMessage(Message msg) {
        AdAppHelper adAppHelper = AdAppHelper.getInstance(this);
        switch (msg.what){
            case MSG_REPEAT_MENU_ROCKET:
                if (mMenu != null) {
                    final ImageView rocketIV = (ImageView)mMenu.findItem(R.id.action_rocket).getActionView();
                    if (rocketIV != null) {
                        rocketIV.startAnimation(mMenuRocketAnimation);
                    }
                }
                break;
            case MSG_CONNECTION_TIMEOUT:
//                showNoInternetSnackbar(R.string.timeout_tip, false);
                ConnectTimeoutDialog connectTimeoutDialog = new ConnectTimeoutDialog();
                connectTimeoutDialog.show(getSupportFragmentManager(),"connectTimeOut");
                long error = mSharedPreference.getLong(SharedPreferenceKey.FAILED_CONNECT_COUNT, 0);
                mSharedPreference.edit().putLong(SharedPreferenceKey.FAILED_CONNECT_COUNT, error+1).apply();
                mVpnState = VpnState.Error;
                if(mConnectingConfig != null){
                    mErrorServers.add(mConnectingConfig);
                    Firebase.getInstance(this).logEvent("VPN连不上", "VPN连接超时", mConnectingConfig.server);
                }else {
                    Firebase.getInstance(this).logEvent("VPN连不上", "VPN连接超时");
                }
                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo != null) {
                    Firebase.getInstance(this).logEvent("VPN连不上", "网络", networkInfo.getTypeName());
                } else {
                    Firebase.getInstance(this).logEvent("VPN连不上", "网络", "未知");
                }
                if(mConnectFragment != null){
                    mConnectFragment.setConnectResult(mVpnState);
                    mConnectFragment.updateUI();
                }
                ConnectVpnHelper.getInstance(this).startTestConnectionWithOutVPN(ConnectVpnHelper.URL_BING, mConnectingConfig);
                break;
            case MSG_PREPARE_START_VPN_BACKGROUND:
                prepareStartVpnBackground();
                break;
            case MSG_PREPARE_START_VPN_FORGROUND:
                prepareStartService();
                break;
            case MSG_TEST_CONNECT_STATUS:
                if (!mSharedPreference.getBoolean(SharedPreferenceKey.IS_AUTO_SWITCH_PROXY, false))
                    ConnectVpnHelper.getInstance(this).startConnectAfterFirstTest();
                break;
            case MSG_NO_AVAILABE_VPN:
                showNoInternetSnackbar(R.string.server_not_available, false);
                mVpnState = VpnState.Error;
                if(mConnectFragment != null){
                    mConnectFragment.setConnectResult(mVpnState);
                    mConnectFragment.updateUI();
                }
                break;
            case MSG_SHOW_INTERSTITIAL_ENTER:
                adAppHelper.showFullAd();
                Firebase.getInstance(this).logEvent("主界面", "进入全屏", "显示");
                break;
            case MSG_SHOW_INTERSTITIAL_EXIT:
                adAppHelper.showFullAd();
                Firebase.getInstance(this).logEvent("主界面", "退出全屏", "显示");
                break;

        }

        return true;
    }

    private void prepareStartVpnBackground(){
        final ServerConfig serverConfig = ConnectVpnHelper.getInstance(this).findVPNServer();
        if(serverConfig != null) {
            mConnectingConfig = serverConfig;
            mForegroundHandler.sendEmptyMessage(MSG_PREPARE_START_VPN_FORGROUND);
            Log.d("MainActivity", String.format("server config: %s:%d", serverConfig.server, serverConfig.port));
        }else{
            boolean isValidation = ServerConfig.checkServerConfigJsonString(mSharedPreference.getString(SharedPreferenceKey.FETCH_SERVER_LIST, null));
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

    private void prepareStartService(){
        try {
            Intent intent = VpnService.prepare(this);
            if (intent != null) {
                startActivityForResult(intent, REQUEST_CONNECT);
                ShadowsocksApplication.debug("ss-vpn", "startActivityForResult");
            } else {
                onActivityResult(REQUEST_CONNECT, Activity.RESULT_OK, null);
                ShadowsocksApplication.debug("ss-vpn", "onActivityResult");
            }
        }catch(Exception e){
            showNoInternetSnackbar(R.string.not_start_vpn, false);
            ShadowsocksApplication.handleException(e);
            Firebase.getInstance(this).logEvent("VPN连不上", "VPN Prepare错误", e.getMessage());
            if(mConnectFragment != null){
                mConnectFragment.updateUI();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if(requestCode == REQUEST_CONNECT){
                if (mConnectingConfig != null) {
                    LocalVpnService.ProxyUrl = mConnectingConfig.toProxyUrl();
                    Firebase.getInstance(this).logEvent("代理", mConnectingConfig.server);
                    if (LocalVpnService.ProxyUrl != null) {
                        startService(new Intent(this, LocalVpnService.class));
                    }
                }
            } else if(requestCode == OPEN_SERVER_LIST){
                ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                if(connectivityManager != null){
                    NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                    if(networkInfo != null && networkInfo.isAvailable()){
                        if (!LocalVpnService.IsRunning) {
                            mIsRestart = false;
                            connectVpnServerAsync();
                        } else {
                            mIsRestart = true;
                        }
                        mBackgroundHander.removeMessages(MSG_TEST_CONNECT_STATUS);
                        mSharedPreference.edit().putBoolean(SharedPreferenceKey.IS_AUTO_SWITCH_PROXY, false).apply();
                        VpnManageService.stopVpnByUserSwitchProxy();
                        VpnNotification.gSupressNotification = true;
                        mVpnState = VpnState.Connecting;
                        mSharedPreference.edit().putInt(SharedPreferenceKey.VPN_STATE, mVpnState.ordinal()).apply();
                    }
                }
                PromotionTracking.getInstance(this).reportSwitchCountry();
            }
        }else{
            if(requestCode == REQUEST_CONNECT){
                mVpnState = VpnState.Stopped;
                mSharedPreference.edit().putInt(SharedPreferenceKey.VPN_STATE, mVpnState.ordinal()).apply();
                mForegroundHandler.removeMessages(MSG_CONNECTION_TIMEOUT);
                if(mConnectFragment != null && mConnectFragment.isAdded()){
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
        super.onDestroy();
    }

    @Override
    public void onDisconnect(DisconnectFragment disconnectFragment) {
        Firebase.getInstance(this).logEvent("连接VPN", "断开", "确认断开");
        disconnectVpnServiceAsync();
        ConnectVpnHelper.getInstance(MainActivity.this).clearErrorList();
        ConnectVpnHelper.getInstance(MainActivity.this).release();
    }

    private void disconnectVpnServiceAsync(){
        if(mConnectFragment != null){
            if (LocalVpnService.IsRunning) {
                mConnectFragment.animateStopping();
                mVpnState = VpnState.Stopping;
                VpnManageService.stopVpnByUser();
            } else {
                mVpnState = VpnState.Stopped;
                mConnectFragment.setConnectResult(mVpnState);
            }
        }
    }

    @Override
    public void onDismiss(DisconnectFragment disconnectFragment) {
        mDisconnectFragment = null;
        addBottomAd();
    }

    private void checkNotification(){
        boolean isNotificationEnable = NotificationsUtils.isNotificationEnabled(this);
        boolean isNotificationDisabledCheck = mSharedPreference.getBoolean(SharedPreferenceKey.NOTIFICATION_DISABLE_CHECK, false);
        if(!isNotificationEnable){
            if(isNotificationDisabledCheck){
                Firebase.getInstance(this).logEvent("通知设置", "禁用","禁用未启用");
            }else{
                Firebase.getInstance(this).logEvent("通知设置", "禁用", "禁用首次发现");
            }
            mSharedPreference.edit().putBoolean(SharedPreferenceKey.NOTIFICATION_DISABLE_CHECK, true).apply();
//            showNoInternetSnackbar(R.string.enable_notification, true);

        }else{
            mSharedPreference.edit().putBoolean(SharedPreferenceKey.NOTIFICATION_DISABLE_CHECK, false).apply();
            if(isNotificationDisabledCheck){
                Firebase.getInstance(this).logEvent("通知设置", "启用", "通过设置打开通知");
            }else{
                Firebase.getInstance(this).logEvent("通知设置", "启用", NotificationsUtils.getNotificationImportance(this));
            }
        }
    }

    private void checkNotificationAndShowRemainder(){
        boolean isNotificationEnable = NotificationsUtils.isNotificationEnabled(this);
        if(!isNotificationEnable){
            showNoInternetSnackbar(R.string.enable_notification, true);
        }
    }

    @NonNull
    private String getPlayStoreUrlString() {
        return " https://play.google.com/store/apps/details?id=" + getPackageName();
    }

    @Override
    public void onBackPressed() {
        final Firebase firebase = Firebase.getInstance(this);

        mExitAlertDialog = new AlertDialog.Builder(this).setTitle("Exit")
                .setMessage("Would you like to exit VPN?")
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
        showInterstitialWithDelay(MSG_SHOW_INTERSTITIAL_EXIT, "exit_ad", "exit_ad_min", "200", "exit_ad_max", "200");
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
        final ImageView rocketIV = (ImageView)menu.findItem(R.id.action_rocket).getActionView();
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
        switch (item.getItemId()){
            case R.id.action_flag:
                if (mVpnState == VpnState.Connecting) {
                    Toast.makeText(this, R.string.vpn_is_connecting, Toast.LENGTH_SHORT).show();
                } else if (mVpnState == VpnState.Stopping) {
                    Toast.makeText(this, R.string.vpn_is_stopping, Toast.LENGTH_SHORT).show();
                } else {
                    startActivityForResult(new Intent(this, ServerListActivity.class), OPEN_SERVER_LIST);
                    Firebase.getInstance(this).logEvent("菜单", "打开服务器列表");
                }
                return true;
            case R.id.action_ad:
                Firebase.getInstance(this).logEvent("主界面广告按钮", "显示", "点击");
                AdAppHelper adAppHelper = AdAppHelper.getInstance(this);
                if (mFullAdStatusListener == null)
                    mFullAdStatusListener = new FullAdStatusListener(this);
                adAppHelper.addAdStateListener(mFullAdStatusListener);

                if (adAppHelper.isFullAdLoaded()) {
                    adAppHelper.showFullAd();
                    mAdMsgType = MSG_SHOW_AD_BUTTON_FULL;
                    Firebase.getInstance(this).logEvent("主界面广告按钮", "显示", "线上全屏");
                } else if (adAppHelper.isRecommendAdLoaded()) {
                    adAppHelper.showFullAd();
                    mAdMsgType = MSG_SHOW_AD_BUTTON_RECOMMEND;
                    Firebase.getInstance(this).logEvent("主界面广告按钮", "显示", "线上互推");
                } else {
                    startActivity(new Intent(this, RecommendActivity.class));
                    Firebase.getInstance(this).logEvent("主界面广告按钮", "显示", "本地互推");
                    Log.i("ssss", "onClick:   本地互推");
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
            final ImageView rocketIV = (ImageView)mMenu.findItem(R.id.action_rocket).getActionView();
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

    @Override
    public void onStatusChanged(String status, Boolean isRunning) {
        Log.d("MainActivity", "isRunning " + isRunning);
        if (mConnectingConfig != null && mSharedPreference != null) {
            if (isRunning) {
                mConnectingConfig.saveInSharedPreference(mSharedPreference);
                if (!AdAppHelper.getInstance(this).isFullAdLoaded()) {
                    rotatedBottomAd();
                }
                mForegroundHandler.removeMessages(MSG_CONNECTION_TIMEOUT);
                mErrorServers.clear();
                if (!mSharedPreference.getBoolean(SharedPreferenceKey.IS_AUTO_SWITCH_PROXY, false)) {
                    AdAppHelper adAppHelper = AdAppHelper.getInstance(getApplicationContext());
                    adAppHelper.showFullAd();
                }
                mVpnState = VpnState.Connected;
                Firebase.getInstance(this).logEvent("VPN链接成功", mConnectingConfig.nation, mConnectingConfig.server);
                if (FirebaseRemoteConfig.getInstance().getBoolean("open_connect_test"))
                    mBackgroundHander.sendEmptyMessageDelayed(MSG_TEST_CONNECT_STATUS, TimeUnit.SECONDS.toMillis(5));
            } else {
                mForegroundHandler.removeMessages(MSG_CONNECTION_TIMEOUT);
                ConnectVpnHelper.getInstance(this).release();
                mVpnState = VpnState.Stopped;
                if (mIsRestart) {
                    mIsRestart = false;
                    mVpnState = VpnState.Connecting;
                    connectVpnServerAsync();
                }
            }
            mSharedPreference.edit().putInt(SharedPreferenceKey.VPN_STATE, mVpnState.ordinal()).apply();
            if (mConnectFragment != null) {
                mConnectFragment.setConnectResult(mVpnState);
            }

            updateFlagMenuIcon();
        }
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
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void rateUs(){
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
            }catch (Exception ex){
                ShadowsocksApplication.handleException(ex);
            }
        }
    }

    private void contactUs(){
        Intent data=new Intent(Intent.ACTION_SENDTO);
        data.setData(Uri.parse("mailto:watchfacedev@gmail.com"));
        data.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.app_name));
        data.putExtra(Intent.EXTRA_TEXT, "");
        try {
            startActivity(data);
        }catch(ActivityNotFoundException e){
            ShadowsocksApplication.handleException(e);
        }
    }

    private void about(){
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

    private void share(){
        startActivity(new Intent(this, ShareActivity.class));
    }

    private void addBottomAd() {
        FrameLayout container = (FrameLayout) findViewById(R.id.ad_view_container);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER);
        try {
            AdAppHelper adAppHelper = AdAppHelper.getInstance(this);
//            container.addView(adAppHelper.getNative(), params);
            adAppHelper.getNative(container, params);
            Firebase.getInstance(this).logEvent("NATIVE广告", "显示成功", "首页底部");
        } catch (Exception ex) {
            ShadowsocksApplication.handleException(ex);
            Firebase.getInstance(this).logEvent("NATIVE广告", "显示失败", "首页底部");
        }
    }

    private void rotatedBottomAd(){
        FrameLayout view = (FrameLayout)findViewById(R.id.ad_view_container);
        float centerX = view.getWidth() / 2.0f;
        float centerY = view.getHeight() / 2.0f;
        Rotate3dAnimation rotate3dAnimation = new Rotate3dAnimation(this, 0, 360, centerX, centerY, 0f, false, true);
        rotate3dAnimation.setDuration(1000);
        rotate3dAnimation.setFillAfter(false);
        view.startAnimation(rotate3dAnimation);
    }


}
