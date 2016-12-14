package com.androapplite.shadowsocks.activity;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.VpnService;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.androapplite.shadowsocks.GAHelper;
import com.androapplite.shadowsocks.R;
import com.androapplite.shadowsocks.ShadowsockServiceHelper;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.fragment.ConnectFragment;
import com.androapplite.shadowsocks.fragment.RateUsFragment;
import com.androapplite.shadowsocks.model.ServerConfig;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.androapplite.shadowsocks.service.ConnectionTestService;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.facebook.appevents.AppEventsLogger;

import junit.framework.Assert;

import java.io.IOException;
import java.lang.System;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

import yyf.shadowsocks.Config;
import yyf.shadowsocks.IShadowsocksService;
import yyf.shadowsocks.IShadowsocksServiceCallback;
import yyf.shadowsocks.utils.Constants;

public class ConnectivityActivity extends BaseShadowsocksActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        ConnectFragment.OnConnectActionListener, RateUsFragment.OnFragmentInteractionListener{

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connectivity);
        Toolbar toolbar = initToolbar();
        initDrawer(toolbar);
        initNavigationView();
        mNewState = Constants.State.INIT;
        mCurrentState = mNewState;
        mSharedPreference = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
        mShadowsocksServiceConnection = createShadowsocksServiceConnection();
        ShadowsockServiceHelper.bindService(this, mShadowsocksServiceConnection);
        mShadowsocksServiceCallbackBinder = createShadowsocksServiceCallbackBinder();
        GAHelper.sendScreenView(this, "VPN连接屏幕");
        initConnectivityReceiver();
        initVpnFlagAndNation();
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
                getWindow().getDecorView().post(new Runnable() {
                    @Override
                    public void run() {
                        mNewState = Constants.State.values()[state];
                        updateConnectionState();
                        Log.d("状态", mNewState.name());
                    }
                });
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

    private void updateConnectionState(){
        if(mNewState != mCurrentState) {
            mCurrentState = mNewState;
            switch (mNewState) {
                case INIT:
                    break;
                case CONNECTING:
                    if (mSharedPreference != null) {
                        mSharedPreference.edit()
                                .putString(SharedPreferenceKey.CONNECTING_VPN_NAME, mConnectingConfig.name)
                                .putString(SharedPreferenceKey.CONNECTING_VPN_SERVER, mConnectingConfig.server)
                                .putString(SharedPreferenceKey.CONNECTING_VPN_FLAG, mConnectingConfig.flag)
                                .putString(SharedPreferenceKey.CONNECTING_VPN_NATION, mConnectingConfig.nation)
                                .apply();
                    }
                    break;
                case CONNECTED:
                    if (mConnectFragment != null) {
                        mConnectFragment.setConnectResult(true);
                    }

                    if (mConnectingConfig == null) {
                        String vpnName = mSharedPreference.getString(SharedPreferenceKey.CONNECTING_VPN_NAME, null);
                        String server = mSharedPreference.getString(SharedPreferenceKey.CONNECTING_VPN_SERVER, null);
                        String flag = mSharedPreference.getString(SharedPreferenceKey.CONNECTING_VPN_FLAG, null);
                        String nation = mSharedPreference.getString(SharedPreferenceKey.CONNECTING_VPN_NATION, null);
                        mConnectingConfig = new ServerConfig(vpnName, server, flag, nation);
                    } else {
                        if (mSharedPreference != null) {
                            mSharedPreference.edit()
                                    .putLong(com.androapplite.shadowsocks.preference.SharedPreferenceKey.CONNECT_TIME, System.currentTimeMillis())
                                    .apply();
                        }
                    }
                    ConnectionTestService.testConnection(this);
                    showRateUsFragment();
                    break;
                case STOPPING:
                    break;
                case STOPPED:
                    if (mConnectFragment != null) {
                        mConnectFragment.setConnectResult(false);
                    }
                    break;
                case ERROR:
                    if (mConnectFragment != null) {
                        mConnectFragment.setConnectResult(false);
                    }
                    break;
            }
        }
        changeProxyFlagIcon();
    }

    private void showRateUsFragment() {
        if(!mSharedPreference.getBoolean(SharedPreferenceKey.IS_RATE_US_FRAGMENT_SHOWN, false)) {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.rate_us_frame_layout);
            if(fragment == null) {
                getWindow().getDecorView().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.rate_us_frame_layout, RateUsFragment.newInstance())
                                .commitAllowingStateLoss();
                    }
                }, 2000);
            }
        }
    }

    private ServiceConnection createShadowsocksServiceConnection(){
        return new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mShadowsocksService = IShadowsocksService.Stub.asInterface(service);
                try {
                    int state = mShadowsocksService.getState();
                    mNewState = Constants.State.values()[state];
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
                GAHelper.sendEvent(this, "连接VPN", "自动连接", mNewState.name());
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
                GAHelper.sendEvent(drawerView.getContext(), "菜单", "关闭");
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                GAHelper.sendEvent(drawerView.getContext(), "菜单", "打开");
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
            super.onBackPressed();
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
            if(mNewState != Constants.State.CONNECTING || mNewState != Constants.State.STOPPED) {
                startActivityForResult(new Intent(this, ServerListActivity.class), OPEN_SERVER_LIST);
            }else if(mNewState == Constants.State.CONNECTING){
                Snackbar.make(findViewById(R.id.coordinator), R.string.connecting_tip, Snackbar.LENGTH_SHORT).show();
            }else{
                Snackbar.make(findViewById(R.id.coordinator), R.string.stopping, Snackbar.LENGTH_SHORT).show();
            }
            GAHelper.sendEvent(this, "打开服务器列表", mNewState.name());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_rate_us) {
            rateUs();
            GAHelper.sendEvent(this, "菜单", "给我们打分");
        } else if (id == R.id.nav_share) {
            share();
            GAHelper.sendEvent(this, "抽屉", "分享");
        } else if (id == R.id.nav_contact_us) {
            contactUs();
            GAHelper.sendEvent(this, "菜单", "联系我们");
        } else if (id == R.id.nav_about) {
            about();
            GAHelper.sendEvent(this, "菜单", "关于");
        } else if(id == R.id.nav_settings){
            startActivity(new Intent(this, SettingsActivity.class));
            GAHelper.sendEvent(this, "菜单", "设置");
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
        }
    }

    private void connectVpnServerAsync() {
        new AsyncTask<Void, Void, ServerConfig>(){
            @Override
            protected void onPreExecute() {
                if(mConnectFragment != null){
                    mConnectFragment.animateConnecting();
                }
            }

            @Override
            protected ServerConfig doInBackground(Void... params) {
                return findVPNServer();
            }

            @Override
            protected void onPostExecute(ServerConfig serverConfig) {
                mConnectingConfig = serverConfig;
                prepareStartService();
            }
        }.execute();
    }

    private void disconnectVpnServiceAsync(){
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected void onPreExecute() {
                if(mConnectFragment != null){
                    mConnectFragment.animateStopping();
                }
            }

            @Override
            protected Void doInBackground(Void... params) {
                if(mShadowsocksService != null){
                    try {
                        mShadowsocksService.stop();
                    } catch (RemoteException e) {
                        ShadowsocksApplication.handleException(e);
                    }
                }
                return null;
            }
        }.execute();
    }


    @Override
    protected void onStart() {
        super.onStart();
        registerShadowsocksCallback();
        checkNetworkConnectivity();
        registerConnectivityReceiver();
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Action.CONNECTION_ACTIVITY_SHOW));
        updateConnectionState();
        AppEventsLogger.activateApp(this);

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
        AppEventsLogger.deactivateApp(this);
        if(mNoInternetSnackbar != null){
            mNoInternetSnackbar.dismiss();
            mNoInternetSnackbar = null;
        }
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
    protected void onDestroy() {
        super.onDestroy();
        if (mShadowsocksServiceConnection != null) {
            unbindService(mShadowsocksServiceConnection);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if(requestCode == REQUEST_CONNECT){
                if(mShadowsocksService != null && mConnectingConfig != null){
                    Config config = new Config(mConnectingConfig.server);
                    try {
                        mShadowsocksService.start(config);
                        ShadowsocksApplication.debug("ss-vpn", "bgService.StartVpn");
                    } catch (RemoteException e) {
                        ShadowsocksApplication.handleException(e);
                    }
                }

            }else if(requestCode == OPEN_SERVER_LIST){
                connectVpnServerAsync();
            }
        }
    }


    private boolean checkNetworkConnectivity(){
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        final View decorView = findViewById(R.id.coordinator);
        boolean r = false;
        if(connectivityManager == null){
            Snackbar.make(decorView, R.string.no_network, Snackbar.LENGTH_INDEFINITE).show();
            GAHelper.sendEvent(this, "网络连接","异常","没有网络服务");
        }else{
            final NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if(activeNetworkInfo == null){
                showNoInternetSnackbar(R.string.no_internet_message);
                GAHelper.sendEvent(this, "网络连接", "异常", "没有网络连接");
            }else if(!activeNetworkInfo.isConnected()){
                showNoInternetSnackbar(R.string.not_available_internet);
                GAHelper.sendEvent(this, "网络连接", "异常", "当前网络连接不可用");
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

                GAHelper.sendEvent(v.getContext(), "网络连接", "异常", "打开WIFI");
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
        if(mNewState == Constants.State.INIT || mNewState == Constants.State.STOPPED){
            String vpnName = mSharedPreference.getString(SharedPreferenceKey.CONNECTING_VPN_NAME, null);
            String server = mSharedPreference.getString(SharedPreferenceKey.CONNECTING_VPN_SERVER, null);
            String flag = mSharedPreference.getString(SharedPreferenceKey.CONNECTING_VPN_FLAG, null);
            String nation = mSharedPreference.getString(SharedPreferenceKey.CONNECTING_VPN_NATION, null);
            if(vpnName != null && server != null && flag != null && nation != null) {
                serverConfig = new ServerConfig(vpnName, server, flag, nation);
            }
        }
        ArrayList<ServerConfig> serverConfigs = loadServerList();
        if(serverConfig != null && !serverConfigs.contains(serverConfig)){
            serverConfig = null;
        }

        if(serverConfig == null){
            final String global = getString(R.string.vpn_nation_opt);
            final String nation = mSharedPreference.getString(SharedPreferenceKey.VPN_NATION, global);
            int index = 0;
            if(nation.equals(global)){
                index =  (int) (Math.random() * (serverConfigs.size() -1) + 1);
            }else{
                Assert.assertTrue(serverConfigs.size() > 1);
                for(int i = index + 1; i < serverConfigs.size(); i++){
                    ServerConfig config = serverConfigs.get(i);
                    if(nation.equals(config.nation)){
                        index = i;
                        break;
                    }
                }
            }
            serverConfig = serverConfigs.get(index);
        }
        return serverConfig;
    }


    private void changeProxyFlagIcon(){
        if(mMenu != null && mShadowsocksService != null && mSharedPreference != null){
            final String globalFlag = getResources().getResourceEntryName(R.drawable.ic_flag_global);
            final String flagKey = mNewState == Constants.State.CONNECTED ? SharedPreferenceKey.CONNECTING_VPN_FLAG : SharedPreferenceKey.VPN_FLAG;
            final String flag = mSharedPreference.getString(flagKey, globalFlag);
            final SimpleTarget<GlideDrawable> target = new SimpleTarget<GlideDrawable>() {
                @Override
                public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                    mMenu.findItem(R.id.action_flag).setIcon(resource);
                }
            };
            if (flag.startsWith("http")) {
                Glide.with(this).load(flag).into(target);
            } else {
                int resId = getResources().getIdentifier(flag, "drawable", getPackageName());
                Glide.with(this).load(resId).into(target);
            }
        }
    }

    private ArrayList<ServerConfig> loadServerList(){
        ArrayList<ServerConfig> result = null;
        String serverlist = mSharedPreference.getString(SharedPreferenceKey.SERVER_LIST, null);
        if(serverlist != null){
            ArrayList<ServerConfig> serverList = ServerConfig.createServerList(this, serverlist);
            if(serverList != null && serverList.size() > 1){
                result = serverList;
            }
        }
        if(result == null){
            result = ServerConfig.createDefaultServerList(getResources());
        }
        return result;
    }

    public static boolean ping(String ipAddress) {
        int  timeOut =  3000 ;  //超时应该在3钞以上
        boolean status = false;     // 当返回值是true时，说明host是可用的，false则不可。
        try {
            status = InetAddress.getByName(ipAddress).isReachable(timeOut);
        } catch (IOException e) {
            ShadowsocksApplication.handleException(e);
        }
        return status;
    }

    public static boolean isPortOpen(final String ip, final int port, final int timeout) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), timeout);
            socket.close();
            return true;
        } catch(ConnectException ce){
            ShadowsocksApplication.handleException(ce);
            return false;
        } catch (Exception ex) {
            ShadowsocksApplication.handleException(ex);
            return false;
        }
    }

    @Override
    public void onConnectButtonClick() {
        if(mShadowsocksService != null) {
            if (mNewState == Constants.State.INIT || mNewState == Constants.State.STOPPED || mNewState == Constants.State.ERROR) {
                if(checkNetworkConnectivity()) {
                    connectVpnServerAsync();
                }
                GAHelper.sendEvent(this, "连接VPN", "打开", mNewState.name());
            } else {
                disconnectVpnServiceAsync();
                GAHelper.sendEvent(this, "连接VPN", "关闭", mNewState.name());
            }
        }

    }

    @Override
    public void onCloseRateUs(final RateUsFragment fragment) {
        remoeRateUsFragment(fragment);
    }

    private void remoeRateUsFragment(RateUsFragment fragment) {
        getSupportFragmentManager().beginTransaction().remove(fragment).commitAllowingStateLoss();
        mSharedPreference.edit().putBoolean(SharedPreferenceKey.IS_RATE_US_FRAGMENT_SHOWN, true).apply();
    }

    @Override
    public void onRateUs(final RateUsFragment fragment) {
        remoeRateUsFragment(fragment);
        rateUs();
    }
}
