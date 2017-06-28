package com.androapplite.shadowsocks.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.design.widget.NavigationView;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.NotificationsUtils;
import com.androapplite.shadowsocks.Rotate3dAnimation;
import com.androapplite.vpn3.R;
import com.androapplite.shadowsocks.ShadowsockServiceHelper;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.fragment.ConnectFragment;
import com.androapplite.shadowsocks.fragment.DisconnectFragment;
import com.androapplite.shadowsocks.model.ServerConfig;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.androapplite.shadowsocks.service.ConnectionTestService;
//import com.bumptech.glide.Glide;
//import com.bumptech.glide.load.resource.drawable.GlideDrawable;
//import com.bumptech.glide.request.animation.GlideAnimation;
//import com.bumptech.glide.request.target.SimpleTarget;
import com.androapplite.shadowsocks.service.ServerListFetcherService;
import com.androapplite.shadowsocks.service.TimeCountDownService;
import com.bestgo.adsplugin.ads.AdAppHelper;
import com.bestgo.adsplugin.ads.AdStateListener;
import com.bestgo.adsplugin.ads.AdType;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.System;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import yyf.shadowsocks.Config;
import yyf.shadowsocks.IShadowsocksService;
import yyf.shadowsocks.IShadowsocksServiceCallback;
import yyf.shadowsocks.utils.Constants;

import static com.bestgo.adsplugin.ads.AdType.ADMOB_FULL;

public class ConnectivityActivity extends BaseShadowsocksActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        ConnectFragment.OnConnectActionListener,
        DisconnectFragment.OnDisconnectActionListener, View.OnClickListener{

    private IShadowsocksService mShadowsocksService;
    private ServiceConnection mShadowsocksServiceConnection;
    private IShadowsocksServiceCallback.Stub mShadowsocksServiceCallbackBinder;
    private static int REQUEST_CONNECT = 1;
    private static int OPEN_SERVER_LIST = 2;
    private BroadcastReceiver mConnectivityReceiver;
    private Menu mMenu;
    private ServerConfig mConnectingConfig;
    private SharedPreferences mSharedPreference;
    private ConnectFragment mConnectFragment;
    private Constants.State mNewState;
    private Constants.State mCurrentState;
    private Snackbar mNoInternetSnackbar;
    private boolean mIsConnecting;
    private Runnable mUpdateVpnStateRunable;
    private ProgressDialog mFetchServerListProgressDialog;
    private Handler mConnectingTimeoutHandler;
    private Runnable mConnectingTimeoutRunnable;
    private HashSet<ServerConfig> mErrorServers;
    private AlertDialog mExitAlert;
    private BroadcastReceiver mConnectCountChangedReceiver;
    private boolean mNeedToCheckNotification;
    private boolean mIsConnectButtonClicked;
    private boolean mIsAdOpen;
    private Runnable mShowRateUsRunnable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connectivity);
        Toolbar toolbar = initToolbar();
        initDrawer(toolbar);
        initNavigationView();
        mSharedPreference = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
        int s = mSharedPreference.getInt(SharedPreferenceKey.VPN_STATE, Constants.State.INIT.ordinal());
        mNewState = Constants.State.values()[s];
        mCurrentState = mNewState;
        mShadowsocksServiceConnection = createShadowsocksServiceConnection();
        ShadowsockServiceHelper.bindService(this, mShadowsocksServiceConnection);
        mShadowsocksServiceCallbackBinder = createShadowsocksServiceCallbackBinder();
        initConnectivityReceiver();
        initVpnFlagAndNation();
        initForegroundBroadcastIntentFilter();
        initForegroundBroadcastReceiver();

        final AdAppHelper adAppHelper = AdAppHelper.getInstance(getApplicationContext());
        adAppHelper.setAdStateListener(new InterstitialAdStateListener(this));

        Firebase firebase = Firebase.getInstance(this);
        if(adAppHelper.isAdSilent()){
            firebase.logEvent("广告","第一天静默", "首页全屏刚进入");
            notificationCheck();
        }else if(adAppHelper.isFullAdLoaded()) {
            if(!mIsAdOpen) {
                if(shouldShowOrLoadAds()) {
                    adAppHelper.showFullAd();
                    mIsAdOpen = true;
                    firebase.logEvent("广告", "加载成功", "首页全屏刚进入");
                    mNeedToCheckNotification = true;
                }else{
                    firebase.logEvent("广告", "加载成功但不显示", "首页全屏刚进入");
                    notificationCheck();
                }
            }
        }else{
            firebase.logEvent("广告","没有加载成功", "首页全屏刚进入");
            notificationCheck();
        }
        mErrorServers = new HashSet<>();
        mConnectCountChangedReceiver = new ConnectCountChangeReceiver(this);
        firebase.logEvent("屏幕","主屏幕");
    }

    private boolean shouldShowOrLoadAds(){
        boolean shouldShow = true;
        if(mCurrentState == Constants.State.CONNECTED){
            final AdAppHelper adAppHelper = AdAppHelper.getInstance(getApplicationContext());
            String defaultChange = adAppHelper.getCustomCtrlValue("default", "1");
            String city = mSharedPreference.getString(SharedPreferenceKey.CONNECTING_VPN_NAME, null);
            if(city != null){
                String chanceString = adAppHelper.getCustomCtrlValue(city, defaultChange);
                float chance = 1;
                try {
                    chance = Float.parseFloat(chanceString);
                    if(chance < 0){
                        chance = 0;
                    }else if(chance > 1){
                        chance = 1;
                    }
                }catch (Exception e){
                    ShadowsocksApplication.handleException(e);
                }

                float random = (float) Math.random();
                shouldShow = random < chance;
            }
        }
        return  shouldShow;

    }

    private static class InterstitialAdStateListener extends AdStateListener{
        private WeakReference<ConnectivityActivity> mActivityReference;
        InterstitialAdStateListener(ConnectivityActivity activity){
            mActivityReference = new WeakReference<ConnectivityActivity>(activity);
        }

        @Override
        public void onAdClosed(AdType adType, int index) {
            ConnectivityActivity activity = mActivityReference.get();
            if(activity != null){
                switch (adType.getType()){
                    case ADMOB_FULL:
                    case AdType.FACEBOOK_FBN:
                    case AdType.FACEBOOK_FULL:
                        activity.mIsAdOpen = false;
                        if(activity.mCurrentState == Constants.State.CONNECTED && activity.mExitAlert == null) {
                            activity.rotateAd();
                        }
                        if(activity.mNeedToCheckNotification){
                            activity.mNeedToCheckNotification = false;
                            activity.notificationCheck();
                        }
                        if(activity.mExitAlert == null){
                            if(activity.mIsConnectButtonClicked){
                                Firebase.getInstance(activity).logEvent("关闭广告", "首页全屏连接成功");
                            }else{
                                Firebase.getInstance(activity).logEvent("关闭广告", "首页全屏刚进入");
                            }
                        }else{
                            Firebase.getInstance(activity).logEvent("关闭广告", "首页全屏退出");
                        }
                        break;
                }
            }

        }
    }

    private void addBottomAd(AdAppHelper adAppHelper) {
        Firebase firebase = Firebase.getInstance(this);
        if(adAppHelper.isNativeLoaded()){
            if(shouldShowOrLoadAds()){
                FrameLayout container = (FrameLayout)findViewById(R.id.ad_view_container);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER);
                try {
                    final View aNative = adAppHelper.getNative();
                    container.addView(aNative, params);
                } catch (Exception ex) {
                    ShadowsocksApplication.handleException(ex);
                }
                firebase.logEvent("广告", "native加载成功", "首页底部");

            }else{
                firebase.logEvent("广告", "native加载成功但不显示", "首页底部");
            }
        }else{
            firebase.logEvent("广告", "native没有加载成功", "首页底部");

        }

    }

    public static final String NAME = "MainActivity";
//    private boolean startUp = false;

    private void initForegroundBroadcastIntentFilter(){
        mForgroundReceiverIntentFilter = new IntentFilter();
        mForgroundReceiverIntentFilter.addAction(Action.SERVER_LIST_FETCH_FINISH);
    }

    private void initForegroundBroadcastReceiver(){
        mForgroundReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                String serverListString = intent.getStringExtra(SharedPreferenceKey.SERVER_LIST);
                switch(action){
                    case Action.SERVER_LIST_FETCH_FINISH:
                        if(mFetchServerListProgressDialog != null){
                            mFetchServerListProgressDialog.dismiss();
                            mFetchServerListProgressDialog = null;
                        }
                        //双重保险
                        if(!mSharedPreference.contains(SharedPreferenceKey.SERVER_LIST)) {
                            if(serverListString != null) {
                                mSharedPreference.edit().putString(SharedPreferenceKey.SERVER_LIST, serverListString).apply();
                            }
                        }
                        if(!mSharedPreference.contains(SharedPreferenceKey.SERVER_LIST)) {
                            mFetchServerListProgressDialog.setOnDismissListener(null);
                            Snackbar.make(findViewById(R.id.coordinator), R.string.fetch_server_list_failed, Snackbar.LENGTH_SHORT).show();
                            Firebase.getInstance(context).logEvent("VPN连不上", "取服务器列表超时");
                            increaseFailedCount();
                            if(mConnectFragment != null && mConnectFragment.isVisible()){
                                mConnectFragment.updateUI();
                            }
                        }
                        break;
                }
            }
        };
    }

    private void increaseFailedCount(){
        if(mSharedPreference != null){
            long failed = mSharedPreference.getLong(SharedPreferenceKey.FAILED_CONNECT_COUNT, 0) + 1;
            mSharedPreference.edit().putLong(SharedPreferenceKey.FAILED_CONNECT_COUNT, failed).apply();
            Firebase.getInstance(this).logEvent("累计连接成功失败次数", "失败", String.valueOf(failed));
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if(fragment instanceof ConnectFragment){
            mConnectFragment = (ConnectFragment)fragment;
        }
    }

    private void initVpnFlagAndNation() {
        String vpnNation = mSharedPreference.getString(SharedPreferenceKey.VPN_NATION, null);
        if(vpnNation == null){
            mSharedPreference.edit()
                    .putString(SharedPreferenceKey.VPN_NATION, getString(R.string.vpn_nation_opt))
                    .putString(SharedPreferenceKey.VPN_FLAG, getResources().getResourceEntryName(R.drawable.ic_flag_global))
                    .apply();

        }
    }

    private IShadowsocksServiceCallback.Stub createShadowsocksServiceCallbackBinder(){
        return new IShadowsocksServiceCallback.Stub(){
            @Override
            public void stateChanged(final int state, String msg) throws RemoteException {
                mUpdateVpnStateRunable = new UpdateVpnStateRunable(ConnectivityActivity.this, state);
                getWindow().getDecorView().post(mUpdateVpnStateRunable);
            }

            @Override
            public void trafficUpdated(final long txRate, final long rxRate, final long txTotal, final long rxTotal) throws RemoteException {
//                ShadowsocksApplication.debug("traffic", "txRate: " + txRate);
//                ShadowsocksApplication.debug("traffic", "rxRate: " + rxRate);
//                ShadowsocksApplication.debug("traffic", "txTotal: " + txTotal);
//                ShadowsocksApplication.debug("traffic", "rxTotal: " + rxTotal);
//                ShadowsocksApplication.debug("traffic", "txTotal old: " + TrafficMonitor.formatTraffic(ConnectivityActivity.this, mShadowsocksService.getTxTotalMonthly()));
//                ShadowsocksApplication.debug("traffic", "rxTotal old: " + TrafficMonitor.formatTraffic(ConnectivityActivity.this, mShadowsocksService.getRxTotalMonthly()));
//                ShadowsocksApplication.debug("traffic", "txRate: " + TrafficMonitor.formatTrafficRate(ConnectivityActivity.this, txRate));
//                ShadowsocksApplication.debug("traffic", "rxRate: " + TrafficMonitor.formatTrafficRate(ConnectivityActivity.this, rxRate));
            }
        };
    }

    private static class UpdateVpnStateRunable implements Runnable{
        private  WeakReference<ConnectivityActivity> mActivityReference;
        private  int mState;

        UpdateVpnStateRunable(ConnectivityActivity activity, int state){
            mActivityReference = new WeakReference<ConnectivityActivity>(activity);
            mState = state;
        }

        @Override
        public void run() {
            ConnectivityActivity activity = mActivityReference.get();
            if(activity != null){
                activity.mNewState = Constants.State.values()[mState];
                activity.updateConnectionState();
                Log.d("状态", activity.mNewState.name());
            }
        }
    }

    private void updateConnectionState(){
        if(mNewState != mCurrentState) {
            mCurrentState = mNewState;
            switch (mNewState) {
                case INIT:
                    break;
                case CONNECTING:
                    if (mSharedPreference != null && mConnectingConfig != null) {
                        mConnectingConfig.saveInSharedPreference(mSharedPreference);
                    }
                    break;
                case CONNECTED:
                    final AdAppHelper adAppHelper = AdAppHelper.getInstance(getApplicationContext());
                    Firebase firebase = Firebase.getInstance(this);
                    if(adAppHelper.isFullAdLoaded()) {
                        if(!mIsAdOpen) {
                            if(shouldShowOrLoadAds()) {
                                adAppHelper.showFullAd();
                                mIsAdOpen = true;
                                firebase.logEvent("广告", "加载成功", "首页全屏连接成功");
                            }else{
                                firebase.logEvent("广告", "加载成功但不显示", "首页全屏连接成功");
                            }
                        }
                    }else{
                        rotateAd();
                        firebase.logEvent("广告", "没有加载成功", "首页全屏连接成功");
                    }

                    if (mConnectingConfig == null) {
                        mConnectingConfig = ServerConfig.loadFromSharedPreference(mSharedPreference);
                    } else {
                        if (mSharedPreference != null) {
                            mSharedPreference.edit()
                                    .putLong(com.androapplite.shadowsocks.preference.SharedPreferenceKey.CONNECT_TIME, System.currentTimeMillis())
                                    .apply();
                        }
                    }
                    if(mConnectFragment != null && mConnectingConfig != null ) {
                        ConnectionTestService.testConnection(this, mConnectingConfig.name);
                        showRateUsFragment();
                    }
                    mIsConnecting = false;
                    TimeCountDownService.start(this);
                    clearConnectingTimeout();
                    mErrorServers.clear();
                    break;
                case STOPPING:
                    clearConnectingTimeout();
                    break;
                case STOPPED:
                    mIsConnecting = false;
                    clearConnectingTimeout();
                    break;
                case ERROR:
                    clearConnectingTimeout();
                    mIsConnecting = false;
                    mErrorServers.add(mConnectingConfig);
                    Firebase.getInstance(this).logEvent("VPN连不上", "ERROR");
                    break;
            }
            if (mConnectFragment != null) {
                mConnectFragment.setConnectResult(mNewState);
            }
        }
        changeProxyFlagIcon();
    }

    private void showRateUsFragment() {
//        if(!mSharedPreference.getBoolean(SharedPreferenceKey.IS_RATE_US_FRAGMENT_SHOWN, false)) {
//            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.rate_us_frame_layout);
//            if(fragment == null) {
//                mShowRateUsRunnable = new ShowRateUsRunnable(this);
//                getWindow().getDecorView().postDelayed(mShowRateUsRunnable, 2000);
//            }
//        }
    }


    private ServiceConnection createShadowsocksServiceConnection(){
        return new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mShadowsocksService = IShadowsocksService.Stub.asInterface(service);
                try {
                    int state = mShadowsocksService.getState();
                    mNewState = Constants.State.values()[state];
                    updateConnectionState();
                    mShadowsocksService.enableNotification(true);
                } catch (RemoteException e) {
                    ShadowsocksApplication.handleException(e);
                }
                updateConnectionState();
                registerShadowsocksCallback();
                autoConnect();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                unregisterShadowsocksCallback();
                mShadowsocksService = null;
            }
        };
    }

    private void autoConnect() {
        if(mSharedPreference != null
                && (mNewState == Constants.State.INIT || mNewState == Constants.State.STOPPED || mNewState == Constants.State.ERROR)) {
            boolean autoConnect = mSharedPreference.getBoolean(SharedPreferenceKey.AUTO_CONNECT, false);
            if (autoConnect) {
                connectVpnServerAsync();
                Firebase.getInstance(this).logEvent("连接VPN", "自动连接", mNewState.name());
            }
        }

    }

    private void initNavigationView(){
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
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

    private Toolbar initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        return toolbar;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            mExitAlert = new AlertDialog.Builder(this).setTitle("Exit")
                    .setMessage("Would you like to exit VPN?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            mExitAlert = null;
                        }
                    })
                    .show();

            final AdAppHelper adAppHelper = AdAppHelper.getInstance(this);
            Firebase firebase = Firebase.getInstance(this);
            if(adAppHelper.isAdSilent()){
                firebase.logEvent("广告", "第一天静默", "首页全屏退出");
            }else if(adAppHelper.isFullAdLoaded()){
                if(shouldShowOrLoadAds()) {
                    adAppHelper.showFullAd();
                    firebase.logEvent("广告", "加载成功", "首页全屏退出");
                }else{
                    firebase.logEvent("广告", "加载成功但不显示", "首页全屏退出");
                }
            }else{
                firebase.logEvent("广告", "没有加载成功", "首页全屏退出");
            }
//            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.connectivity, menu);
        mMenu = menu;
        updateConnectionState();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_flag) {
            if(mNewState != Constants.State.CONNECTING && mNewState != Constants.State.STOPPING) {
                startActivityForResult(new Intent(this, ServerListActivity.class), OPEN_SERVER_LIST);
            }else if(mNewState == Constants.State.CONNECTING){
                Snackbar.make(findViewById(R.id.coordinator), R.string.connecting_tip, Snackbar.LENGTH_SHORT).show();
            }else{
                Snackbar.make(findViewById(R.id.coordinator), R.string.stopping_tip, Snackbar.LENGTH_SHORT).show();
            }
            Firebase.getInstance(this).logEvent("打开服务器列表", mNewState.name());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
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
        } else if(id == R.id.nav_settings){
            startActivity(new Intent(this, SettingsActivity.class));
            firebase.logEvent("菜单", "设置");
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void share(){
        startActivity(new Intent(this, ShareActivity.class));
    }

    @NonNull
    private String getPlayStoreUrlString() {
        return " https://play.google.com/store/apps/details?id=" + getPackageName();
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
            Snackbar.make(findViewById(R.id.coordinator), R.string.not_start_vpn, Snackbar.LENGTH_SHORT).show();
            ShadowsocksApplication.handleException(e);
            Firebase.getInstance(this).logEvent("VPN连不上", "VPN Prepare错误", e.getMessage());
            increaseFailedCount();
            if(mConnectFragment != null && mConnectFragment.isVisible()){
                mConnectFragment.updateUI();
            }
        }
    }

    private void connectVpnServerAsync() {
        if(mSharedPreference.contains(SharedPreferenceKey.SERVER_LIST)){
            connectVpnServerAsyncCore();
        }else{
            mFetchServerListProgressDialog = ProgressDialog.show(this, null, getString(R.string.fetch_server_list), true, false);
            ServerListFetcherService.fetchServerListAsync(this);
            mFetchServerListProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    connectVpnServerAsyncCore();
                }
            });
        }
    }

    private void connectVpnServerAsyncCore(){
        if(mConnectFragment != null){
            mConnectFragment.animateConnecting();
            mIsConnecting = true;
        }
        mConnectingTimeoutHandler = new Handler();
        mConnectingTimeoutRunnable = new ConnectingTimeoutRunnable(this);
        mConnectingTimeoutHandler.postDelayed(mConnectingTimeoutRunnable, TimeUnit.SECONDS.toMillis(32));//时间要超过等待并行测试服务器连接的时间
        new Thread(new PrepareStartServiceRunnable(this)).start();
    }

    private static class PrepareStartServiceRunnable implements Runnable{
        private WeakReference<ConnectivityActivity> mActivityReference;
        PrepareStartServiceRunnable(ConnectivityActivity activity){
            mActivityReference = new WeakReference<ConnectivityActivity>(activity);
        }

        @Override
        public void run() {
            ConnectivityActivity activity = mActivityReference.get();
            if(activity != null){
                final ServerConfig serverConfig = activity.findVPNServer();
                if(serverConfig != null) {
                    activity.mConnectingConfig = serverConfig;
                    activity.runOnUiThread(new PrepareStartServiceOnMainThreadRunnable(activity));
                }else{
                    boolean isValidation = ServerConfig.checkServerConfigJsonString(activity.mSharedPreference.getString(SharedPreferenceKey.SERVER_LIST, null));
                    Firebase.getInstance(activity).logEvent("VPN连不上", "没有可用的服务器", "服务器列表合法 " + isValidation);
                    activity.mIsConnecting = false;
                    activity.increaseFailedCount();
                    activity.clearConnectingTimeout();
                    activity.mErrorServers.clear();
                    activity.runOnUiThread(new NoAvailableServerErrorRunable(activity));
                }
            }
        }
    }

    private static class PrepareStartServiceOnMainThreadRunnable implements Runnable{
        private WeakReference<ConnectivityActivity> mActivityReference;
        PrepareStartServiceOnMainThreadRunnable(ConnectivityActivity activity){
            mActivityReference = new WeakReference<ConnectivityActivity>(activity);
        }

        @Override
        public void run() {
            ConnectivityActivity activity = mActivityReference.get();
            if(activity != null){
                activity.prepareStartService();
            }
        }
    }

    private static class NoAvailableServerErrorRunable implements Runnable{
        private WeakReference<ConnectivityActivity> mActivityReference;
        NoAvailableServerErrorRunable(ConnectivityActivity activity){
            mActivityReference = new WeakReference<ConnectivityActivity>(activity);
        }

        @Override
        public void run() {
            ConnectivityActivity activity = mActivityReference.get();
            if(activity != null){
                Snackbar.make(activity.findViewById(R.id.coordinator), R.string.server_not_available, Snackbar.LENGTH_LONG).show();
                if(activity.mConnectFragment != null && activity.mConnectFragment.isAdded()){
                    activity.mConnectFragment.setConnectResult(Constants.State.ERROR);
                    activity.mConnectFragment.updateUI();
                }
            }
        }
    }

    private static class ConnectingTimeoutRunnable implements Runnable{
        WeakReference<ConnectivityActivity> mActivityReference;

        ConnectingTimeoutRunnable(ConnectivityActivity activity){
            mActivityReference = new WeakReference<ConnectivityActivity>(activity);
        }

        @Override
        public void run() {
            ConnectivityActivity activity = mActivityReference.get();
            if(activity != null){
                if(activity.mShadowsocksService != null){
                    try {
                        activity.mShadowsocksService.stop();
                    } catch (RemoteException e) {
                        ShadowsocksApplication.handleException(e);
                    }
                }
                activity.mConnectingTimeoutHandler = null;
                activity.mConnectingTimeoutRunnable = null;
                Snackbar.make(activity.findViewById(R.id.coordinator), R.string.timeout_tip, Snackbar.LENGTH_SHORT).show();
                Firebase.getInstance(activity).logEvent("VPN连不上", "VPN连接超时");
                if(activity.mConnectingConfig != null) {
                    activity.mErrorServers.add(activity.mConnectingConfig);
                }
                activity.increaseFailedCount();
                if(activity.mConnectFragment != null && activity.mConnectFragment.isVisible()){
                    activity.mConnectFragment.updateUI();
                }
            }
        }
    }


    private void disconnectVpnServiceAsync(){
        if(mConnectFragment != null){
            mConnectFragment.animateStopping();
        }
        if(mShadowsocksService != null){
            try {
                mShadowsocksService.stop();
            } catch (RemoteException e) {
                ShadowsocksApplication.handleException(e);
            }
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        registerShadowsocksCallback();
        checkNetworkConnectivity();
        registerConnectivityReceiver();
//        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Action.CONNECTION_ACTIVITY_SHOW));
        if(mShadowsocksService != null){
            try {
                mNewState = Constants.State.values()[mShadowsocksService.getState()];
            } catch (RemoteException e) {
                ShadowsocksApplication.handleException(e);
            }
        }
        updateConnectionState();
        addBottomAd(AdAppHelper.getInstance(this));
        LocalBroadcastManager.getInstance(this).registerReceiver(mConnectCountChangedReceiver,
                new IntentFilter(Action.CONNECT_COUNT_CHANGED));
    }

    private void registerShadowsocksCallback() {
        if(mShadowsocksService != null){
            try {
                mShadowsocksService.registerCallback(mShadowsocksServiceCallbackBinder);
            } catch (RemoteException e) {
                ShadowsocksApplication.handleException(e);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterShadowsocksCallback();
        unregisterConnectivityReceiver();
        if(mNoInternetSnackbar != null){
            mNoInternetSnackbar.dismiss();
            mNoInternetSnackbar = null;
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mConnectCountChangedReceiver);
    }

    private void unregisterShadowsocksCallback() {
        if(mShadowsocksService != null && mShadowsocksServiceCallbackBinder != null){
            try {
                mShadowsocksService.unregisterCallback(mShadowsocksServiceCallbackBinder);
            } catch (RemoteException e) {
                ShadowsocksApplication.handleException(e);
            }
        }
    }

    @Override
    protected void onPause() {
        AdAppHelper.getInstance(getApplicationContext()).onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mShadowsocksServiceConnection != null) {
            unbindService(mShadowsocksServiceConnection);
        }
        if(mUpdateVpnStateRunable != null) {
            getWindow().getDecorView().removeCallbacks(mUpdateVpnStateRunable);
        }
        if(mShowRateUsRunnable != null){
            getWindow().getDecorView().removeCallbacks(mShowRateUsRunnable);
        }
        clearConnectingTimeout();
        if(mFetchServerListProgressDialog != null){
            mFetchServerListProgressDialog.setOnDismissListener(null);
            mFetchServerListProgressDialog.dismiss();
            mFetchServerListProgressDialog = null;
        }
    }

    private void clearConnectingTimeout() {
        if(mConnectingTimeoutHandler != null && mConnectingTimeoutRunnable != null){
            mConnectingTimeoutHandler.removeCallbacks(mConnectingTimeoutRunnable);
            mConnectingTimeoutHandler = null;
            mConnectingTimeoutRunnable = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if(requestCode == REQUEST_CONNECT){
                if(mShadowsocksService != null && mConnectingConfig != null){
                    Config config = new Config(mConnectingConfig.server, mConnectingConfig.port);
                    try {
                        mShadowsocksService.start(config);
                        ShadowsocksApplication.debug("ss-vpn", "bgService.StartVpn");
                    } catch (RemoteException e) {
                        ShadowsocksApplication.handleException(e);
                    }
                }
//                try {
//                    if (DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this).getBoolean(SharedPreferenceKey.FIRST_CONNECT_SUCCESS, false)) {
//                        AdAppHelper.getInstance(getApplicationContext()).showFullAd();
//                    }
//                } catch (Exception ex) {
//                }
            }else if(requestCode == OPEN_SERVER_LIST){
                ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                if(connectivityManager != null){
                    NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                    if(networkInfo != null && networkInfo.isAvailable()){
                        connectVpnServerAsync();
                    }
                }

            }
        }
    }


    private boolean checkNetworkConnectivity(){
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        final View decorView = findViewById(R.id.coordinator);
        boolean r = false;
        Firebase firebase = Firebase.getInstance(this);
        if(connectivityManager == null){
            Snackbar.make(decorView, R.string.no_network, Snackbar.LENGTH_INDEFINITE).show();
            firebase.logEvent("网络连接","异常","没有网络服务");
        }else{
            final NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if(activeNetworkInfo == null){
                showNoInternetSnackbar(R.string.no_internet_message);
                firebase.logEvent("网络连接","异常","没有网络连接");

            }else if(!activeNetworkInfo.isConnected()){
                showNoInternetSnackbar(R.string.not_available_internet);
                firebase.logEvent("网络连接","异常","当前网络连接不可用");

//            }else if(activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
//                final WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
//                final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
//                NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(connectionInfo.getSupplicantState());
//                if (state == NetworkInfo.DetailedState.CONNECTED){
//                    r = true;
//                }else{
//                    showNoInternetSnackbar(R.string.not_available_internet);
//                    GAHelper.sendEvent(this, "网络连接", "异常", "当前网络连接不可用");
//                }
            }else{
//                NetworkInfo.State state = activeNetworkInfo.getState();
//                NetworkInfo.DetailedState detailedState = activeNetworkInfo.getDetailedState();

                r = true;
            }
        }
        return r;
    }

    private void showNoInternetSnackbar(@StringRes int messageId) {
        final View decorView = findViewById(R.id.coordinator);
        mNoInternetSnackbar = Snackbar.make(decorView, messageId, Snackbar.LENGTH_LONG);
        mNoInternetSnackbar.setAction(android.R.string.yes, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                }catch (ActivityNotFoundException e){
                    ShadowsocksApplication.handleException(e);
                    mNoInternetSnackbar.dismiss();
                    mNoInternetSnackbar = Snackbar.make(decorView, R.string.failed_to_open_wifi_setting, Snackbar.LENGTH_LONG);
                    mNoInternetSnackbar.show();
                }

                Firebase.getInstance(v.getContext()).logEvent("网络连接", "异常", "打开WIFI");
            }
        });
        mNoInternetSnackbar.show();
    }

    private void initConnectivityReceiver(){
        mConnectivityReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if(action.equals(ConnectivityManager.CONNECTIVITY_ACTION)){
                    checkNetworkConnectivity();
                }
            }
        };
    }

    private void registerConnectivityReceiver(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mConnectivityReceiver, intentFilter);
    }

    private void unregisterConnectivityReceiver(){
        unregisterReceiver(mConnectivityReceiver);
    }

    private ServerConfig findVPNServer(){
        ServerConfig serverConfig = null;
        ArrayList<ServerConfig> serverConfigs = loadServerList();
        if(serverConfigs != null && !serverConfigs.isEmpty()) {
            if(mNewState == Constants.State.INIT || mNewState == Constants.State.STOPPED){
                serverConfig = ServerConfig.loadFromSharedPreference(mSharedPreference);
            }

            Firebase firebase = Firebase.getInstance(this);
            if (serverConfig != null) {
                if (!serverConfigs.contains(serverConfig) ||
                        mErrorServers.contains(serverConfig)) {
                    serverConfig = null;
                } else {
                    long t1 = System.currentTimeMillis();
                    boolean isReachable = ping(serverConfig.server);
                    long dur = System.currentTimeMillis() - t1;
                    if(isReachable){
                        firebase.logEvent("ping成功", serverConfig.server, dur);
                    }else{
                        firebase.logEvent("ping失败", serverConfig.server, dur);
                    }
                    if(!isReachable){
                        serverConfig = null;
                    }else {
                        Pair<Boolean, Long> pair = isPortOpen(serverConfig.server, serverConfig.port, 2000);
                        if (pair.first) {
                            firebase.logEvent("连接测试成功", serverConfig.name, pair.second);
                        } else {
                            firebase.logEvent("连接测试失败", serverConfig.name, pair.second);
                            serverConfig = null;
                        }
                    }

                }
            }

            if (serverConfig == null) {
                final String defaultNation = getString(R.string.vpn_nation_opt);
                String nation = mSharedPreference.getString(SharedPreferenceKey.VPN_NATION, defaultNation);
                //处理换语言的情况
                if(!nation.equals(defaultNation)){
                    TypedArray array = getResources().obtainTypedArray(R.array.vpn_nations);
                    int i = 0;
                    for(;i<array.length();i++){
                        String n = array.getString(i);
                        if(nation.equals(n)){
                            break;
                        }
                    }
                    if(i >= array.length()){
                        nation = defaultNation;
                        mSharedPreference.edit()
                                .putString(SharedPreferenceKey.VPN_NATION, nation)
                                .putString(SharedPreferenceKey.VPN_FLAG, getResources().getResourceEntryName(R.drawable.ic_flag_global))
                                .apply();
                    }
                }
                //处理本地和服务器列表切换的问题
                String defaultName = getString(R.string.vpn_name_opt);
                String name = mSharedPreference.getString(SharedPreferenceKey.CONNECTING_VPN_NAME, defaultName);
                if(!name.equals(defaultName)){
                    String serverlist = mSharedPreference.getString(SharedPreferenceKey.SERVER_LIST, null);
                    if(serverlist != null && !serverlist.contains(name)){
                        nation = defaultNation;
                        mSharedPreference.edit()
                                .putString(SharedPreferenceKey.VPN_NATION, nation)
                                .putString(SharedPreferenceKey.VPN_FLAG, getResources().getResourceEntryName(R.drawable.ic_flag_global))
                                .apply();
                    }
                }

                final boolean isGlobalOption = nation.equals(defaultNation);
                ArrayList<ServerConfig> filteredConfigs = null;
                if (isGlobalOption) {
                    filteredConfigs = serverConfigs;
                    filteredConfigs.remove(0);
                } else {
                    filteredConfigs = new ArrayList<>();
                    for (ServerConfig config : serverConfigs) {
                        if (nation.equals(config.nation)) {
                            filteredConfigs.add(config);
                        }
                    }
                }
                ExecutorService executorService = Executors.newFixedThreadPool(5);
                GetFirstAvailableServerPortHandler handler = new GetFirstAvailableServerPortHandler(Looper.getMainLooper(), executorService);
                for(ServerConfig sc: filteredConfigs){
                    if(!executorService.isShutdown()) {
                        try {
                            executorService.submit(new GetAvailableServerPortRunnable(this, sc, handler));
                        }catch (RejectedExecutionException e){
                            ShadowsocksApplication.handleException(e);
                        }
                    }
                }
                try {
                    executorService.awaitTermination(30, TimeUnit.SECONDS);//时间不要超过timeouthander的时间
                } catch (InterruptedException e) {
                    ShadowsocksApplication.handleException(e);
                }
                serverConfig = handler.mServerConfig;
            }
        }
        return serverConfig;
    }

    private static class GetFirstAvailableServerPortHandler extends Handler{
        public ServerConfig mServerConfig;
        private ExecutorService mExecutorService;

        GetFirstAvailableServerPortHandler(Looper looper, ExecutorService executorService){
            super(looper);
            mExecutorService = executorService;
        }

        @Override
        public void handleMessage(Message msg) {
            if(mServerConfig == null) {
                mServerConfig = (ServerConfig) msg.obj;
            }
            mExecutorService.shutdown();
            mExecutorService.shutdownNow();
        }
    }



    private static class GetAvailableServerPortRunnable implements Runnable{
        private WeakReference<ConnectivityActivity> mActivityReference;
        private ServerConfig mServerConfig;
        private Handler mHandler;

        GetAvailableServerPortRunnable(ConnectivityActivity activity, ServerConfig serverConfig, Handler handler){
            mActivityReference = new WeakReference<ConnectivityActivity>(activity);
            mServerConfig = serverConfig;
            mHandler = handler;
        }

        @Override
        public void run() {
            ConnectivityActivity activity = mActivityReference.get();
            if(activity != null){
                if(Thread.currentThread().isInterrupted()) return;
                long t1 = System.currentTimeMillis();
                boolean isReachable = activity.ping(mServerConfig.server);
                long dur = System.currentTimeMillis() - t1;
                if(isReachable){
                    Firebase.getInstance(activity).logEvent("ping成功", mServerConfig.server, dur);
                }else{
                    Firebase.getInstance(activity).logEvent("ping失败", mServerConfig.server, dur);
                }
                if(Thread.currentThread().isInterrupted()) return;
                if (isReachable) {
                    Pair<Boolean, Long> pair = activity.isPortOpen(mServerConfig.server, mServerConfig.port, 2000);
                    if (pair.first) {
                        Message message = mHandler.obtainMessage();
                        message.obj = mServerConfig;
                        mHandler.sendMessage(message);
                        Firebase.getInstance(activity).logEvent("连接测试成功", mServerConfig.server, pair.second);
                    } else {
                        Firebase.getInstance(activity).logEvent("连接测试失败", mServerConfig.server, pair.second);
                    }
                }
            }
        }
    }




    private void changeProxyFlagIcon(){
        if(mMenu != null && mShadowsocksService != null && mSharedPreference != null){
            final String globalFlag = getResources().getResourceEntryName(R.drawable.ic_flag_global);
            final String flagKey = mNewState == Constants.State.CONNECTED ? SharedPreferenceKey.CONNECTING_VPN_FLAG : SharedPreferenceKey.VPN_FLAG;
            final String flag = mSharedPreference.getString(flagKey, globalFlag);
            int resId = getResources().getIdentifier(flag, "drawable", getPackageName());
//            Drawable drawable = getResources().getDrawable(resId);
            mMenu.findItem(R.id.action_flag).setIcon(resId);
//            final SimpleTarget<GlideDrawable> target = new SimpleTarget<GlideDrawable>() {
//                @Override
//                public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
//                    mMenu.findItem(R.id.action_flag).setIcon(resource);
//                }
//            };
//            if (flag.startsWith("http")) {
//                Glide.with(this).load(flag).into(target);
//            } else {
//                int resId = getResources().getIdentifier(flag, "drawable", getPackageName());
//                Glide.with(this).load(resId).into(target);
//            }
        }
    }

    private ArrayList<ServerConfig> loadServerList(){
        ArrayList<ServerConfig> result = null;
        String serverlist = mSharedPreference.getString(SharedPreferenceKey.SERVER_LIST, null);
        ArrayList<ServerConfig> serverList = null;
        if(serverlist != null){
            serverList = ServerConfig.createServerList(this, serverlist);
        }
        if(serverList != null && serverList.size() > 1){
            result = serverList;
        }
        return result;
    }

    private boolean ping(String ipAddress){
        int  timeOut =  3000 ;  //超时应该在3钞以上
        boolean status = false;
        try {
            status = InetAddress.getByName(ipAddress).isReachable(timeOut);     // 当返回值是true时，说明host是可用的，false则不可。
        }catch (Exception e){
            ShadowsocksApplication.handleException(e);
        }
        return status;
    }

    private Pair<Boolean, Long> isPortOpen(final String ip, final int port, final int timeout) {
        Socket socket = null;
        OutputStreamWriter osw;
        boolean result = false;
        long t = System.currentTimeMillis();
        long duration = 0;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), timeout);
            osw = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
            osw.write(ip, 0, ip.length());
            osw.flush();
            result = true;
        } catch (Exception ex) {
            ShadowsocksApplication.handleException(ex);
        }finally {
            duration = System.currentTimeMillis() - t;
            try {
                socket.close();
            } catch (IOException e) {
                ShadowsocksApplication.handleException(e);
            }
        }
        return new Pair<>(result, duration);
    }

    @Override
    public void onConnectButtonClick() {
        if(mShadowsocksService != null) {
            Firebase firebase = Firebase.getInstance(this);
            if (!mIsConnecting && (mNewState == Constants.State.INIT || mNewState == Constants.State.STOPPED || mNewState == Constants.State.ERROR)) {
                if(checkNetworkConnectivity()) {
                    connectVpnServerAsync();
                }
                firebase.logEvent("连接VPN", "连接", mNewState.name());
                if(mSharedPreference != null) {
                    String nation = mSharedPreference.getString(SharedPreferenceKey.VPN_NATION, "空");
                    firebase.logEvent("选择国家", nation);
                }

            } else {
                DisconnectFragment disconnectFragment = new DisconnectFragment();
                disconnectFragment.show(getSupportFragmentManager(), "disconnect");
                firebase.logEvent("连接VPN", "断开", mNewState.name());
            }
            mIsConnectButtonClicked = true;
        }

    }

    @Override
    public void onCancel(DisconnectFragment disconnectFragment) {
        Firebase.getInstance(this).logEvent("连接VPN", "断开", "取消断开");
    }

    @Override
    public void onDisconnect(DisconnectFragment disconnectFragment) {
        disconnectVpnServiceAsync();
        Firebase.getInstance(this).logEvent("连接VPN", "断开", "确认断开");
    }

    @Override
    public void onDismiss(DisconnectFragment disconnectFragment) {
        addBottomAd(AdAppHelper.getInstance(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        final AdAppHelper adAppHelper = AdAppHelper.getInstance(getApplicationContext());
        adAppHelper.onResume();
    }

    private void rotateAd(){
        FrameLayout view = (FrameLayout)findViewById(R.id.ad_view_container);
        float centerX = view.getWidth() / 2.0f;
        float centerY = view.getHeight() / 2.0f;
        Rotate3dAnimation rotate3dAnimation = new Rotate3dAnimation(this, 0, 360, centerX, centerY, 0f, false, true);
        rotate3dAnimation.setDuration(1000);
        rotate3dAnimation.setFillAfter(false);
        view.startAnimation(rotate3dAnimation);
    }


    private static class ConnectCountChangeReceiver extends BroadcastReceiver{
        private WeakReference<ConnectivityActivity> mActivityReference;

        ConnectCountChangeReceiver(ConnectivityActivity activity){
            mActivityReference = new WeakReference<ConnectivityActivity>(activity);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityActivity activity = mActivityReference.get();
            if(activity != null){
                if(activity.mConnectFragment != null && activity.mConnectFragment.isVisible()){
                    activity.mConnectFragment.updateUI();
                }
            }
        }
    }


    private void notificationCheck(){
        boolean isNotificationEnable = NotificationsUtils.isNotificationEnabled(this);
        boolean isNotificationDisabledCheck = mSharedPreference.getBoolean(SharedPreferenceKey.NOTIFICATION_DISABLE_CHECK, false);
        if(!isNotificationEnable){
            if(isNotificationDisabledCheck){
                Firebase.getInstance(this).logEvent("通知设置", "禁用","禁用未启用");
            }else{
                Firebase.getInstance(this).logEvent("通知设置", "禁用", "禁用首次发现");
            }
            mSharedPreference.edit().putBoolean(SharedPreferenceKey.NOTIFICATION_DISABLE_CHECK, true).apply();

            Snackbar.make(findViewById(R.id.coordinator),
                    R.string.enable_notification,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.yes, this)
                    .show();
        }else{
            mSharedPreference.edit().putBoolean(SharedPreferenceKey.NOTIFICATION_DISABLE_CHECK, false).apply();
            if(isNotificationDisabledCheck){
                Firebase.getInstance(this).logEvent("通知设置", "启用", "通过设置打开通知");
            }else{
                Firebase.getInstance(this).logEvent("通知设置", "启用");
            }
            if(mShadowsocksService != null){
                try {
                    mShadowsocksService.enableNotification(true);
                } catch (RemoteException e) {
                    ShadowsocksApplication.handleException(e);
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        NotificationsUtils.goToSet(ConnectivityActivity.this);
    }
}
