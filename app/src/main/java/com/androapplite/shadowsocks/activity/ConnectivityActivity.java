package com.androapplite.shadowsocks.activity;

import android.app.Activity;
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
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.VpnService;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentTransaction;
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
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

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
import yyf.shadowsocks.utils.TrafficMonitor;

public class ConnectivityActivity extends BaseShadowsocksActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        ConnectFragment.OnConnectActionListener, RateUsFragment.OnFragmentInteractionListener{

    private IShadowsocksService mShadowsocksService;
    private ServiceConnection mShadowsocksServiceConnection;
    private IShadowsocksServiceCallback.Stub mShadowsocksServiceCallbackBinder;
    private static int REQUEST_CONNECT = 1;
    private static int OPEN_SERVER_LIST = 2;
//    private long mConnectOrDisconnectStartTime;
    private Snackbar mSnackbar;
    private AlertDialog mNoInternetDialog;
    private BroadcastReceiver mConnectivityReceiver;
    private Menu mMenu;
    private ServerConfig mConnectingConfig;
    private SharedPreferences mSharedPreference;
    private ConnectFragment mConnectFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connectivity);
        Toolbar toolbar = initToolbar();
        initDrawer(toolbar);
        initNavigationView();
        mSharedPreference = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
        mShadowsocksServiceConnection = createShadowsocksServiceConnection();
        ShadowsockServiceHelper.bindService(this, mShadowsocksServiceConnection);
        mShadowsocksServiceCallbackBinder = createShadowsocksServiceCallbackBinder();
        GAHelper.sendScreenView(this, "VPN连接屏幕");
        initConnectivityReceiver();
        initVpnNameAndNation();
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if(fragment instanceof ConnectFragment){
            mConnectFragment = (ConnectFragment)fragment;
        }
    }

    private void initVpnNameAndNation() {
        String vpnName = mSharedPreference.getString(SharedPreferenceKey.VPN_NAME, null);
        if(vpnName == null){
            mSharedPreference.edit()
                    .putString(SharedPreferenceKey.VPN_NAME, getString(R.string.vpn_name_opt))
                    .putString(SharedPreferenceKey.VPN_NATION, getString(R.string.vpn_nation_opt))
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
                        updateConnectionState(state);
                    }
                });
            }

            @Override
            public void trafficUpdated(final long txRate, final long rxRate, final long txTotal, final long rxTotal) throws RemoteException {
                ShadowsocksApplication.debug("traffic", "txRate: " + txRate);
                ShadowsocksApplication.debug("traffic", "rxRate: " + rxRate);
                ShadowsocksApplication.debug("traffic", "txTotal: " + txTotal);
                ShadowsocksApplication.debug("traffic", "rxTotal: " + rxTotal);
                ShadowsocksApplication.debug("traffic", "txTotal old: " + TrafficMonitor.formatTraffic(ConnectivityActivity.this, mShadowsocksService.getTxTotalMonthly()));
                ShadowsocksApplication.debug("traffic", "rxTotal old: " + TrafficMonitor.formatTraffic(ConnectivityActivity.this, mShadowsocksService.getRxTotalMonthly()));
                ShadowsocksApplication.debug("traffic", "txRate: " + TrafficMonitor.formatTrafficRate(ConnectivityActivity.this, txRate));
                ShadowsocksApplication.debug("traffic", "rxRate: " + TrafficMonitor.formatTrafficRate(ConnectivityActivity.this, rxRate));
            }
        };
    }

    private void updateConnectionState(int state){
        if(state == Constants.State.CONNECTING.ordinal()){

        }else if(state == Constants.State.CONNECTED.ordinal()){
            if(mConnectFragment != null){
                mConnectFragment.setConnectResult(true);
            }
            if(mConnectingConfig != null){
                mSharedPreference.edit()
                        .putString(SharedPreferenceKey.CONNECTING_VPN_NAME, mConnectingConfig.name)
                        .putString(SharedPreferenceKey.CONNECTING_VPN_SERVER, mConnectingConfig.server)
                        .putString(SharedPreferenceKey.CONNECTING_VPN_FLAG, mConnectingConfig.flag)
                        .putString(SharedPreferenceKey.CONNECTING_VPN_NATION, mConnectingConfig.nation)
                        .putLong(SharedPreferenceKey.CONNECT_TIME, System.currentTimeMillis())
                        .apply();
            }else{
                mConnectingConfig = loadConnectingServerConfig(mConnectingConfig);
                changeProxyFlagIcon();
            }
            ConnectionTestService.testConnection(this);
            showRateUsFragment();
        }else if(state == Constants.State.ERROR.ordinal()){
            if(mConnectFragment != null){
                mConnectFragment.setConnectResult(false);
            }
            restoreServerConfig();
            changeProxyFlagIcon();
        }else if(state == Constants.State.INIT.ordinal()){

            restoreServerConfig();
            changeProxyFlagIcon();
        }else if(state == Constants.State.STOPPING.ordinal()){
        }else if(state == Constants.State.STOPPED.ordinal()){
            if(mConnectFragment != null){
                mConnectFragment.setConnectResult(false);
            }
            restoreServerConfig();
            changeProxyFlagIcon();
        }
    }

    private void showRateUsFragment() {
        if(!mSharedPreference.getBoolean(SharedPreferenceKey.IS_RATE_US_FRAGMENT_SHOWN, false)) {
            getWindow().getDecorView().postDelayed(new Runnable() {
                @Override
                public void run() {
                    getSupportFragmentManager().beginTransaction()
                            .add(R.id.rate_us_frame_layout, RateUsFragment.newInstance())
                            .commitAllowingStateLoss();
                }
            }, 2000);
        }
    }

    private ServiceConnection createShadowsocksServiceConnection(){
        return new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mShadowsocksService = IShadowsocksService.Stub.asInterface(service);
                registerShadowsocksCallback();
                checkVPNConnectivity();

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
        if(mShadowsocksService != null) {
            final int state;
            try {
                state = mShadowsocksService.getState();
                if (state == Constants.State.INIT.ordinal() || state == Constants.State.STOPPED.ordinal()
                        ) {
                    if (mSharedPreference != null) {
                        boolean autoConnect = mSharedPreference.getBoolean(SharedPreferenceKey.AUTO_CONNECT, false);
                        if (autoConnect) {
                            connectVpnServerAsync();
                        }
                    }
                }
            } catch (RemoteException e) {
                ShadowsocksApplication.handleException(e);
            }
        }
    }

    //todo: 这个方法的调用有问题
    private void checkVPNConnectivity() {
        try {
            if(mShadowsocksService != null) {
                updateConnectionState(mShadowsocksService.getState());
//                if(mShadowsocksService.getState() == Constants.State.CONNECTED.ordinal()) {
//                    if (mConnectOrDisconnectStartTime > 0) {
//                        long t = System.currentTimeMillis();
//                        GAHelper.sendTimingEvent(this, "VPN计时", "连接", t - mConnectOrDisconnectStartTime);
//                        mConnectOrDisconnectStartTime = 0;
//                    }
//                }else if(mShadowsocksService.getState() == Constants.State.STOPPED.ordinal()){
//                    if (mConnectOrDisconnectStartTime > 0) {
//                        long t = System.currentTimeMillis();
//                        GAHelper.sendTimingEvent(this, "VPN计时", "断开", t - mConnectOrDisconnectStartTime);
//                        mConnectOrDisconnectStartTime = 0;
//                    }
//                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
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
        drawer.setDrawerListener(toggle);
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
        changeProxyFlagIcon();
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
            if(mShadowsocksService != null){
//                int state = 0;
//                try {
//                    state = mShadowsocksService.getState();
//                } catch (RemoteException e) {
//                    ShadowsocksApplication.handleException(e);
//                }
//                if(state == Constants.State.INIT.ordinal() ||
//                        state == Constants.State.STOPPED.ordinal()){
//                    showVpnServerChangePopupWindow();
//                    GAHelper.sendEvent(this, "ProxyPopup", "正确时机", String.valueOf(state));
//                }else{
//                    Snackbar.make(findViewById(R.id.coordinator), R.string.change_proxy_tip, Snackbar.LENGTH_SHORT).show();
//                    GAHelper.sendEvent(this, "ProxyPopup", "错误时机", String.valueOf(state));
//
//                }
                startActivityForResult(new Intent(this, ServerListActivity.class), OPEN_SERVER_LIST);
            }
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

//    @Override
//    public void onClickConnectionButton() {
//        if(mShadowsocksService != null) {
//            try {
//                final int state = mShadowsocksService.getState();
//                if (mShadowsocksService == null || state == Constants.State.INIT.ordinal()
//                        || state == Constants.State.STOPPED.ordinal()) {
//                    connectVpnServerAsync();
//
//                } else {
//                    mShadowsocksService.stop();
//                    mConnectOrDisconnectStartTime = System.currentTimeMillis();
//                    if(mConnectingConfig != null && mConnectingConfig.name != null){
//                        GAHelper.sendEvent(this, "关闭连接", mConnectingConfig.name, String.valueOf(state));
//                    }else{
//                        GAHelper.sendEvent(this, "关闭连接", "错误", String.valueOf(state));
//                    }
//                }
//
//            } catch (RemoteException e) {
//                ShadowsocksApplication.handleException(e);
//            }
//        }
//
//    }

    private void connectVpnServerAsync() {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
////                loadServerList(false);
//                findVPNServer();
//                getWindow().getDecorView().post(new Runnable() {
//                    @Override
//                    public void run() {
//                        changeProxyFlagIcon();
//                    }
//                });
//                prepareStartService();
//                mConnectOrDisconnectStartTime = System.currentTimeMillis();
//                if(mConnectingConfig != null && mConnectingConfig.name != null) {
//                    GAHelper.sendEvent(ConnectivityActivity.this, "建立连接", mConnectingConfig.name);
//                }else{
//                    GAHelper.sendEvent(ConnectivityActivity.this, "建立连接", "错误");
//                }
//            }
//        }).start();

        new AsyncTask<Void, Void, ServerConfig>(){
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


    @Override
    protected void onStart() {
        super.onStart();
        registerShadowsocksCallback();
        checkVPNConnectivity();
        checkNetworkConnectivity();
        registerConnectivityReceiver();

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
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Action.CONNECTION_ACTIVITY_SHOW));
//        loadServerList(false);
        mConnectingConfig = loadConnectingServerConfig(mConnectingConfig);
        changeProxyFlagIcon();
        AppEventsLogger.activateApp(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        AppEventsLogger.deactivateApp(this);
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


    private void checkNetworkConnectivity(){
        if(mNoInternetDialog != null) {
            mNoInternetDialog.dismiss();
        }
        if(mSnackbar != null) {
            mSnackbar.dismiss();
        }
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager == null){
            new AlertDialog.Builder(this)
                    .setMessage(R.string.no_network)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();
            GAHelper.sendEvent(this, "网络连接","异常","没有网络服务");
        }else{
            final NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if(activeNetworkInfo == null){
                showNoInternetDialog(R.string.no_internet_message);
                GAHelper.sendEvent(this, "网络连接", "异常", "没有网络连接");
            }else if(!activeNetworkInfo.isAvailable()){
                showNoInternetDialog(R.string.not_available_internet);
                GAHelper.sendEvent(this, "网络连接", "异常", "当前网络连接不可用");
            }
        }
    }

    private void showNoInternetDialog(@StringRes int messageId) {
        final View rootView = getLayoutInflater().inflate(R.layout.dialog_no_internet, null);
        mNoInternetDialog = new AlertDialog.Builder(this, R.style.TANCStyle)
                .setView(rootView)
                .setCancelable(false)
                .create();
        TextView messageTextView = (TextView)rootView.findViewById(R.id.message);
        messageTextView.setText(messageId);
        rootView.findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNoInternetDialog.dismiss();
                try{
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                }catch (ActivityNotFoundException e){
                    ShadowsocksApplication.handleException(e);
                    showNoInternetSnackbar(R.string.failed_to_open_wifi_setting);
                }
            }
        });
        rootView.findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNoInternetDialog.dismiss();
                showNoInternetSnackbar(R.string.no_internet);
            }
        });
        mNoInternetDialog.show();
    }

    private void showNoInternetSnackbar(@StringRes int messageId) {
        mSnackbar = Snackbar.make(findViewById(R.id.coordinator), messageId, Snackbar.LENGTH_INDEFINITE);
        mSnackbar.setAction(R.string.close, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSnackbar.dismiss();
            }
        });
        mSnackbar.show();
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

//    public void saveVpnName(ServerConfig config){
//        String vpnName = config.name;
//        SharedPreferences.Editor editor = mSharedPreference.edit();
//        editor.putString(SharedPreferenceKey.VPN_NAME, vpnName).apply();
//        GAHelper.sendEvent(this, "选择VPN", vpnName);
//    }

//    private String findVPNServer(){
//        ArrayList<ServerConfig> serverConfigs = loadServerList();
////        if(mConnectingConfig == null || mConnectingConfig.name.equals(getString(R.string.vpn_name_opt))){
////
////            if(mSharedPreference.contains(SharedPreferenceKey.SERVER_LIST)){
////                mConnectingConfig = mServerConfigs.get(1);
////            }else{
////                int index = (int) (Math.random() * (mServerConfigs.size() -1) + 1);
////                mConnectingConfig = mServerConfigs.get(index);
////            }
////        }
//        int index = (int) (Math.random() * (serverConfigs.size() -1) + 1);
//        mConnectingConfig = serverConfigs.get(index);
//
//        long t1 = System.currentTimeMillis();
//        boolean r = ping(mConnectingConfig.server);
//        long t2 = System.currentTimeMillis();
//        boolean rr = isPortOpen(mConnectingConfig.server, 40010, 3000);
//        long t3 = System.currentTimeMillis();
//        Log.d("服务器状态", "ping: " + r + ", time: " + (t2-t1) );
//        Log.d("服务器状态", "port: " + rr + ", time: " + (t3-t2) );
//        return mConnectingConfig.server;
//    }

    private ServerConfig findVPNServer(){
        ArrayList<ServerConfig> serverConfigs = loadServerList();
//        if(mConnectingConfig == null || mConnectingConfig.name.equals(getString(R.string.vpn_name_opt))){
//
//            if(mSharedPreference.contains(SharedPreferenceKey.SERVER_LIST)){
//                mConnectingConfig = mServerConfigs.get(1);
//            }else{
//                int index = (int) (Math.random() * (mServerConfigs.size() -1) + 1);
//                mConnectingConfig = mServerConfigs.get(index);
//            }
//        }
//        int index = (int) (Math.random() * (serverConfigs.size() -1) + 1);
//        ServerConfig serverConfig = serverConfigs.get(index);
//
//        long t1 = System.currentTimeMillis();
//        boolean r = ping(serverConfig.server);
//        long t2 = System.currentTimeMillis();
//        boolean rr = isPortOpen(serverConfig.server, 40010, 3000);
//        long t3 = System.currentTimeMillis();
//        Log.d("服务器状态", "ping: " + r + ", time: " + (t2-t1) );
//        Log.d("服务器状态", "port: " + rr + ", time: " + (t3-t2) );
        ServerConfig serverConfig = new ServerConfig("fr", "107.191.46.208", "ic_flag_fr", "France");
        return serverConfig;
    }

    private void changeProxyFlagIcon(){
        if(mMenu != null) {
            if(mConnectingConfig != null && !mConnectingConfig.name.equals(getString(R.string.vpn_name_opt))) {
                try {
                    final SimpleTarget<GlideDrawable> target = new SimpleTarget<GlideDrawable>() {
                        @Override
                        public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                            mMenu.findItem(R.id.action_flag).setIcon(resource);
                        }
                    };
                    if (mConnectingConfig.flag.startsWith("http")) {
                        Glide.with(this).load(mConnectingConfig.flag).into(target);
                    } else {
                        Glide.with(this).load(mConnectingConfig.getResourceId(this)).into(target);
                    }
                } catch (Exception e) {
                    ShadowsocksApplication.handleException(e);
                }
            }else{
                mMenu.findItem(R.id.action_flag).setIcon(R.drawable.ic_flag_global_clicked);
            }
        }
    }

//    private void loadServerList(boolean force){
//        if(force || mServerConfigs == null){
//            String serverlist = mSharedPreference.getString(SharedPreferenceKey.SERVER_LIST, null);
//            if(serverlist != null){
//                ArrayList<ServerConfig> serverList = ServerConfig.createServerList(this, serverlist);
//                if(serverList != null && !serverList.isEmpty()){
//                    mServerConfigs = serverList;
//                    return;
//                }
//            }
//            mServerConfigs = ServerConfig.createDefaultServerList(getResources());
//        }
//    }
//
//    private void loadConnectingServerConfig(){
//        if(mServerConfigs!= null && !mServerConfigs.contains(mConnectingConfig)){
//            String vpnName = mSharedPreference.getString(SharedPreferenceKey.VPN_NAME, null);
//            if(vpnName != null && !vpnName.equals(getString(R.string.vpn_name_opt))){
//                for(ServerConfig config: mServerConfigs){
//                    if(config.name.equals(vpnName)){
//                        mConnectingConfig = config;
//                        break;
//                    }
//                }
//            }else if(vpnName != null && vpnName.equals(getString(R.string.vpn_name_opt))){
//                if(mShadowsocksService != null){
//                    try {
//                        if(mShadowsocksService.getState() == Constants.State.CONNECTED.ordinal()){
//                            vpnName = mSharedPreference.getString(SharedPreferenceKey.CONNECTING_VPN_NAME, null);
//                            String server = mSharedPreference.getString(SharedPreferenceKey.CONNECTING_VPN_SERVER, null);
//                            String flag = mSharedPreference.getString(SharedPreferenceKey.CONNECTING_VPN_FLAG, null);
//                            String nation = mSharedPreference.getString(SharedPreferenceKey.CONNECTING_VPN_NATION, null);
//                            mConnectingConfig = new ServerConfig(vpnName, server, flag, nation);
//                        }
//                    } catch (RemoteException e) {
//                        ShadowsocksApplication.handleException(e);
//                    }
//                }
//
//            }
//        }
//    }

    private ArrayList<ServerConfig> loadServerList(){
        ArrayList<ServerConfig> result = null;
        String serverlist = mSharedPreference.getString(SharedPreferenceKey.SERVER_LIST, null);
        if(serverlist != null){
            ArrayList<ServerConfig> serverList = ServerConfig.createServerList(this, serverlist);
            if(serverList != null && !serverList.isEmpty()){
                result = serverList;
            }
        }
        if(result == null){
            result = ServerConfig.createDefaultServerList(getResources());
        }
        return result;
    }

    private ServerConfig loadConnectingServerConfig(ServerConfig connectingConfig){
        ArrayList<ServerConfig> serverConfigs = loadServerList();
        if(serverConfigs!= null && !serverConfigs.contains(connectingConfig)){
            String vpnName = mSharedPreference.getString(SharedPreferenceKey.VPN_NAME, null);
            if(vpnName != null && !vpnName.equals(getString(R.string.vpn_name_opt))){
                for(ServerConfig config: serverConfigs){
                    if(config.name.equals(vpnName)){
                        connectingConfig = config;
                        break;
                    }
                }
            }else if(vpnName != null && vpnName.equals(getString(R.string.vpn_name_opt))){
                if(mShadowsocksService != null){
                    try {
                        if(mShadowsocksService.getState() == Constants.State.CONNECTED.ordinal()){
                            vpnName = mSharedPreference.getString(SharedPreferenceKey.CONNECTING_VPN_NAME, null);
                            String server = mSharedPreference.getString(SharedPreferenceKey.CONNECTING_VPN_SERVER, null);
                            String flag = mSharedPreference.getString(SharedPreferenceKey.CONNECTING_VPN_FLAG, null);
                            String nation = mSharedPreference.getString(SharedPreferenceKey.CONNECTING_VPN_NATION, null);
                            connectingConfig = new ServerConfig(vpnName, server, flag, nation);
                        }
                    } catch (RemoteException e) {
                        ShadowsocksApplication.handleException(e);
                    }
                }

            }
        }
        return connectingConfig;
    }



    private void restoreServerConfig(){
        String vpnName = mSharedPreference.getString(SharedPreferenceKey.VPN_NAME, null);
        if(vpnName == null || vpnName.equals(getString(R.string.vpn_name_opt))){
            mConnectingConfig = null;
        }
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
        if(mConnectFragment != null){
            mConnectFragment.startAnimation();
        }
        connectVpnServerAsync();

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
