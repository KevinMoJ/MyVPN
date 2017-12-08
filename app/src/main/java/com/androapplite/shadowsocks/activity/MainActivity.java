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
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.VpnService;
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
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import android.util.Log;
import android.util.Pair;

import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;


import com.androapplite.shadowsocks.Rotate3dAnimation;
import com.androapplite.shadowsocks.service.VpnManageService;
import com.androapplite.vpn3.R;
import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.NotificationsUtils;
import com.androapplite.shadowsocks.PromotionTracking;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.fragment.ConnectFragment;
import com.androapplite.shadowsocks.fragment.DisconnectFragment;
import com.androapplite.shadowsocks.model.ServerConfig;
import com.androapplite.shadowsocks.model.VpnState;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.androapplite.shadowsocks.service.ConnectionTestService;
import com.androapplite.shadowsocks.service.ServerListFetcherService;
import com.bestgo.adsplugin.ads.AdAppHelper;
import com.bestgo.adsplugin.ads.AdStateListener;
import com.bestgo.adsplugin.ads.AdType;
import com.vm.shadowsocks.core.LocalVpnService;
import com.vm.shadowsocks.core.TcpTrafficMonitor;
import com.vm.shadowsocks.core.VpnNotification;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.bestgo.adsplugin.ads.AdType.ADMOB_FULL;

public class MainActivity extends AppCompatActivity implements ConnectFragment.OnConnectActionListener,
        Handler.Callback, View.OnClickListener, DialogInterface.OnDismissListener,
        DisconnectFragment.OnDisconnectActionListener, LocalVpnService.onStatusChangedListener,
        NavigationView.OnNavigationItemSelectedListener{
    private Snackbar mSnackbar;
    private SharedPreferences mSharedPreference;
    private ProgressDialog mFetchServerListProgressDialog;
    private BroadcastReceiver mReceiver;
    private IntentFilter mIntentFilter;
    private ConnectFragment mConnectFragment;
    private Handler mForgroundHandler;
    private static final int MSG_CONNECTION_TIMEOUT = 1;
    private ServerConfig mConnectingConfig;
    private HashSet<ServerConfig> mErrorServers;
    private Handler mBackgroundHander;
    private HandlerThread mHandlerThread;
    private static final int MSG_PREPARE_START_VPN_BACKGROUND = 2;
    private static final int MSG_PREPARE_START_VPN_FORGROUND = 3;
    private static final int MSG_NO_AVAILABE_VPN = 4;
    private static int REQUEST_CONNECT = 1;
    private static int OPEN_SERVER_LIST = 2;
    private Menu mMenu;
    private VpnState mVpnState;
    private boolean mIsRestart;
    private AlertDialog mExitAlertDialog;
    private DisconnectFragment mDisconnectFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connectivity);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        initDrawer(toolbar);
        initNavigationView();
        mSharedPreference = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
        mReceiver = new MyReceiver(this);
        mIntentFilter = new IntentFilter(Action.SERVER_LIST_FETCH_FINISH);
        mForgroundHandler = new Handler(this);
        mErrorServers = new HashSet<>();
        mHandlerThread = new HandlerThread("background handler");
        mHandlerThread.start();
        mBackgroundHander = new Handler(mHandlerThread.getLooper(), this);
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
        adAppHelper.setAdStateListener(new InterstitialAdStateListener(this));
        adAppHelper.showFullAd();
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
            PromotionTracking.getInstance(this).reportClickConnectButtonCount();
        } else {
            mDisconnectFragment = new DisconnectFragment();
            mDisconnectFragment.show(getSupportFragmentManager(), "disconnect");
            firebase.logEvent("连接VPN", "断开");
        }
    }

    private void connectVpnServerAsync() {
        if(mSharedPreference.contains(SharedPreferenceKey.SERVER_LIST)){
            connectVpnServerAsyncCore();
        }else{
            mFetchServerListProgressDialog = ProgressDialog.show(this, null, getString(R.string.fetch_server_list), true, false);
            ServerListFetcherService.fetchServerListAsync(this);
            mFetchServerListProgressDialog.setOnDismissListener(this);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        connectVpnServerAsyncCore();
    }

    private void connectVpnServerAsyncCore(){
        if(mConnectFragment != null){
            mConnectFragment.animateConnecting();
        }
        mForgroundHandler.sendEmptyMessageDelayed(MSG_CONNECTION_TIMEOUT, TimeUnit.SECONDS.toMillis(32));
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
                String serverListString = intent.getStringExtra(SharedPreferenceKey.SERVER_LIST);
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
        if(!mSharedPreference.contains(SharedPreferenceKey.SERVER_LIST)) {
            if(serverListString != null) {
                mSharedPreference.edit().putString(SharedPreferenceKey.SERVER_LIST, serverListString).apply();
            }
        }
        if(!mSharedPreference.contains(SharedPreferenceKey.SERVER_LIST)) {
            mFetchServerListProgressDialog.setOnDismissListener(null);
            showNoInternetSnackbar(R.string.fetch_server_list_failed, false);
            Firebase.getInstance(this).logEvent("VPN连不上", "取服务器列表超时");
            if(mConnectFragment != null){
                mConnectFragment.updateUI();
            }
            ConnectionTestService.testConnectionWithoutVPN(this);
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
        Integer msg = (Integer) mSnackbar.getView().getTag();
        if(msg != null){
            int msgId = msg;
            switch (msgId){
                case R.string.no_internet_message:
                case R.string.not_available_internet:
                case R.string.no_network:
                case R.string.failed_to_open_wifi_setting:
                    try{
                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                        Firebase.getInstance(v.getContext()).logEvent("网络连接", "异常", "打开WIFI");
                    }catch (ActivityNotFoundException e){
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
        switch (msg.what){
            case MSG_CONNECTION_TIMEOUT:
                showNoInternetSnackbar(R.string.timeout_tip, false);
                long error = mSharedPreference.getLong(SharedPreferenceKey.FAILED_CONNECT_COUNT, 0);
                mSharedPreference.edit().putLong(SharedPreferenceKey.FAILED_CONNECT_COUNT, error+1).apply();
                mVpnState = VpnState.Error;
                if(mConnectingConfig != null){
                    mErrorServers.add(mConnectingConfig);
                    Firebase.getInstance(this).logEvent("VPN连不上", "VPN连接超时", mConnectingConfig.server);
                }else {
                    Firebase.getInstance(this).logEvent("VPN连不上", "VPN连接超时");
                }
                if(mConnectFragment != null){
                    mConnectFragment.updateUI();
                }
                ConnectionTestService.testConnectionWithoutVPN(this);
                break;
            case MSG_PREPARE_START_VPN_BACKGROUND:
                prepareStartVpnBackground();
                break;
            case MSG_PREPARE_START_VPN_FORGROUND:
                prepareStartService();
                break;
            case MSG_NO_AVAILABE_VPN:
                showNoInternetSnackbar(R.string.server_not_available, false);
                if(mConnectFragment != null){
                    mConnectFragment.updateUI();
                }
                break;
        }

        return true;
    }

    private void prepareStartVpnBackground(){
        final ServerConfig serverConfig = findVPNServer();
        if(serverConfig != null) {
            mConnectingConfig = serverConfig;
            mForgroundHandler.sendEmptyMessage(MSG_PREPARE_START_VPN_FORGROUND);
            Log.d("MainActivity", String.format("server config: %s:%d", serverConfig.server, serverConfig.port));
        }else{
            boolean isValidation = ServerConfig.checkServerConfigJsonString(mSharedPreference.getString(SharedPreferenceKey.SERVER_LIST, null));
            Firebase.getInstance(this).logEvent("VPN连不上", "没有可用的服务器", "服务器列表合法 " + isValidation);
            mForgroundHandler.removeMessages(MSG_CONNECTION_TIMEOUT);
            mErrorServers.clear();
            mForgroundHandler.sendEmptyMessage(MSG_NO_AVAILABE_VPN);
            ConnectionTestService.testConnectionWithoutVPN(this);

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
                            connectVpnServerAsync();
                        }
                        VpnManageService.stopVpnByUserSwitchProxy();
                        VpnNotification.gSupressNotification = true;
                        mVpnState = VpnState.Connecting;
                        mSharedPreference.edit().putInt(SharedPreferenceKey.VPN_STATE, mVpnState.ordinal()).apply();
                        mIsRestart = true;
                    }
                }
                PromotionTracking.getInstance(this).reportSwitchCountry();
            }
        }else{
            if(requestCode == REQUEST_CONNECT){
                mVpnState = VpnState.Stopped;
                mSharedPreference.edit().putInt(SharedPreferenceKey.VPN_STATE, mVpnState.ordinal()).apply();
                mForgroundHandler.removeMessages(MSG_CONNECTION_TIMEOUT);
                if(mConnectFragment != null && mConnectFragment.isAdded()){
                    mConnectFragment.setConnectResult(mVpnState);
                }
                showNoInternetSnackbar(R.string.enable_vpn_connection, true);
            }
        }
    }

    private ServerConfig findVPNServer(){
        ServerConfig serverConfig = null;
        ArrayList<ServerConfig> serverConfigs = loadServerList();
        if(serverConfigs != null && !serverConfigs.isEmpty()) {
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
            ArrayList<MyCallable> tasks = new ArrayList<>();
            if (isGlobalOption) {
                serverConfigs.remove(0);
                for(ServerConfig config: serverConfigs) {
                    tasks.add(new MyCallable(this, config));
                }
            } else {
                for (ServerConfig config : serverConfigs) {
                    if (nation.equals(config.nation)) {
                        tasks.add(new MyCallable(this, config));
                    }
                }
            }

            ExecutorService executorService = Executors.newCachedThreadPool();
            ExecutorCompletionService<ServerConfig> ecs = new ExecutorCompletionService<>(executorService);
            for (MyCallable callable: tasks) {
                ecs.submit(callable);
            }

            for (int i = 0; i < tasks.size(); i++) {
                try {
                    Future<ServerConfig> future = ecs.take();
                    ServerConfig sc = future.get(10, TimeUnit.SECONDS);
                    if (sc != null) {
                        serverConfig = sc;
                        break;
                    }
                } catch (Exception e) {
                }
            }
            executorService.shutdown();
        }
        return serverConfig;
    }

    private static class MyCallable implements Callable<ServerConfig> {
        private WeakReference<MainActivity> mReference;
        private ServerConfig mConfig;
        MyCallable(MainActivity mainActivity, ServerConfig config) {
            mReference = new WeakReference<>(mainActivity);
            mConfig = config;
        }
        @Override
        public ServerConfig call() throws Exception {
            Log.d("MyCallable", String.format("test server %s:%d", mConfig.server, mConfig.port));
            MainActivity activity = mReference.get();
            if (activity != null) {
                return activity.testServerIpAndPort(mConfig);
            } else {
                throw new Exception("activity is null");
            }
        }
    }

    private ServerConfig testServerIpAndPort(ServerConfig config) throws Exception{
        if (ping(config.server) && isPortOpen(config.server, config.port, 5000)) {
            return config;
        }
        return null;
    }

    private ArrayList<ServerConfig> loadServerList() {
        ArrayList<ServerConfig> result = null;
        String serverlist = mSharedPreference.getString(SharedPreferenceKey.SERVER_LIST, null);
        ArrayList<ServerConfig> serverList = null;
        if(serverlist != null){
            serverList = ServerConfig.createServerList(this, serverlist);
        }
        if(serverList != null && serverList.size() > 1) {
            result = serverList;
        }
        return result;
    }

    private boolean ping(String ipAddress){
        boolean status = false;
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(String.format("http://%s/ping.html", ipAddress)).openConnection();
            connection.setConnectTimeout(1000 * 5);
            connection.setReadTimeout(1000 * 5);
            status = connection.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            ShadowsocksApplication.handleException(e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        Log.d("MyCaller", "ping: " + ipAddress + " " + status);
        if (!status) {
            Firebase.getInstance(this).logEvent("ping", ipAddress, String.valueOf(status));
        }
        return status;
    }

    private boolean isPortOpen(final String ip, final int port, final int timeout) {
        Socket socket = null;
        OutputStreamWriter osw;
        boolean result = false;
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
            try {
                socket.close();
            } catch (IOException e) {
                ShadowsocksApplication.handleException(e);
            }
        }
        Log.d("MyCaller", ip + ":" + port + " " + result);
        if (!result) {
            Firebase.getInstance(this).logEvent("port", ip + ":" + String.valueOf(port), String.valueOf(result));
        }
        return result;
    }


    @Override
    protected void onDestroy() {
        mForgroundHandler.removeCallbacksAndMessages(null);
        mHandlerThread.quit();
        mBackgroundHander.removeCallbacksAndMessages(null);
        LocalVpnService.removeOnStatusChangedListener(this);
        super.onDestroy();
    }

    @Override
    public void onCancel(DisconnectFragment disconnectFragment) {
        Firebase.getInstance(this).logEvent("连接VPN", "断开", "取消断开");
    }

    @Override
    public void onDisconnect(DisconnectFragment disconnectFragment) {
        Firebase.getInstance(this).logEvent("连接VPN", "断开", "确认断开");
        disconnectVpnServiceAsync();
    }

    private void disconnectVpnServiceAsync(){
        if(mConnectFragment != null){
            if (LocalVpnService.IsRunning) {
                mConnectFragment.animateStopping();
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

        final AdAppHelper adAppHelper = AdAppHelper.getInstance(this);
        String exitAdSwitch = adAppHelper.getCustomCtrlValue("exit_ad", "-1");
        if ("1".equals(exitAdSwitch)) {
            adAppHelper.showFullAd();
            firebase.logEvent("主页", "退出", "后退键");
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
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_flag:
                startActivityForResult(new Intent(this, ServerListActivity.class), OPEN_SERVER_LIST);
                Firebase.getInstance(this).logEvent("菜单", "打开服务器列表");
                return true;
        }
        return super.onOptionsItemSelected(item);
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
                ConnectionTestService.testConnectionWithVPN(this, mConnectingConfig.server);
                mForgroundHandler.removeMessages(MSG_CONNECTION_TIMEOUT);
                mErrorServers.clear();
                AdAppHelper adAppHelper = AdAppHelper.getInstance(getApplicationContext());
                adAppHelper.showFullAd();
                mVpnState = VpnState.Connected;
            } else {
                mForgroundHandler.removeMessages(MSG_CONNECTION_TIMEOUT);
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
            container.addView(adAppHelper.getNative(), params);
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
