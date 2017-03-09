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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
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
import android.util.TimeUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.androapplite.shadowsocks.GAHelper;
import com.androapplite.shadowsocks.R;
import com.androapplite.shadowsocks.Rotate3dAnimation;
import com.androapplite.shadowsocks.ShadowsockServiceHelper;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.ads.AdAppHelper;
import com.androapplite.shadowsocks.ads.AdStateListener;
import com.androapplite.shadowsocks.ads.AdType;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.fragment.ConnectFragment;
import com.androapplite.shadowsocks.fragment.DisconnectFragment;
import com.androapplite.shadowsocks.fragment.RateUsFragment;
import com.androapplite.shadowsocks.model.ServerConfig;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.facebook.FacebookSdk;
import com.androapplite.shadowsocks.service.ConnectionTestService;
//import com.bumptech.glide.Glide;
//import com.bumptech.glide.load.resource.drawable.GlideDrawable;
//import com.bumptech.glide.request.animation.GlideAnimation;
//import com.bumptech.glide.request.target.SimpleTarget;
import com.androapplite.shadowsocks.service.ServerListFetcherService;
import com.facebook.appevents.AppEventsLogger;
import com.umeng.analytics.game.UMGameAgent;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.System;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import yyf.shadowsocks.Config;
import yyf.shadowsocks.IShadowsocksService;
import yyf.shadowsocks.IShadowsocksServiceCallback;
import yyf.shadowsocks.utils.Constants;

public class ConnectivityActivity extends BaseShadowsocksActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        ConnectFragment.OnConnectActionListener, RateUsFragment.OnFragmentInteractionListener,
        DisconnectFragment.OnDisconnectActionListener{

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
    private Runnable mShowRateUsRunnable;
    private ProgressDialog mFetchServerListProgressDialog;
    private Handler mConnectingTimeoutHandler;
    private Runnable mConnectingTimeoutRunnable;
    private HashSet<ServerConfig> mErrorServers;
    private AlertDialog mExitAlert;


    public static final String NAME = "ConnectivityActivity";

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

        GAHelper.sendEvent(this, "广告", "屏幕浏览", "首页");
        UMGameAgent.onEvent(getApplicationContext(), "shouye");
        initForegroundBroadcastIntentFilter();
        initForegroundBroadcastReceiver();
        mErrorServers = new HashSet<>();

        final AdAppHelper adAppHelper = AdAppHelper.getInstance(getApplicationContext());
        FrameLayout container = (FrameLayout)findViewById(R.id.ad_view_container);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER);
        try {
            container.addView(adAppHelper.getBanner(), params);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if(adAppHelper.isFullAdLoaded()) {
            adAppHelper.showFullAd();
        }
        adAppHelper.setListener(new AdStateListener() {
            @Override
            public void onAdClosed(AdType adType) {
                switch (adType.getType()){
                    case AdType.ADMOB_FULL:
                    case AdType.FACEBOOK_FBN:
                    case AdType.FACEBOOK_FULL:
                        if(mCurrentState == Constants.State.CONNECTED && mExitAlert == null) {
                            rotateAd();
                        }
                        break;
                }
            }
        });

    }

    private void initForegroundBroadcastIntentFilter(){
        mForgroundReceiverIntentFilter = new IntentFilter();
        mForgroundReceiverIntentFilter.addAction(Action.SERVER_LIST_FETCH_FINISH);
    }

    private void initForegroundBroadcastReceiver(){
        mForgroundReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch(action){
                    case Action.SERVER_LIST_FETCH_FINISH:
                        if(mFetchServerListProgressDialog != null){
                            if(!mSharedPreference.contains(SharedPreferenceKey.SERVER_LIST)){
                                mFetchServerListProgressDialog.setOnDismissListener(null);
                                Snackbar.make(findViewById(R.id.coordinator), R.string.fetch_server_list_failed, Snackbar.LENGTH_SHORT).show();
                            }
                            mFetchServerListProgressDialog.dismiss();
                            mFetchServerListProgressDialog = null;
                        }
                        break;
                }
            }
        };
    }

    private boolean startUp = false;
    private boolean showResumeAd = false;
    private boolean hasShowResumeAd = false;

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if(fragment instanceof ConnectFragment){
            mConnectFragment = (ConnectFragment)fragment;
        }
    }

    private void initVpnFlagAndNation() {
        String vpnNation = mSharedPreference.getString(SharedPreferenceKey.VPN_NATION, null);
        if (vpnNation == null) {
            mSharedPreference.edit()
                    .putString(SharedPreferenceKey.VPN_NATION, getString(R.string.vpn_nation_opt))
                    .putString(SharedPreferenceKey.VPN_FLAG, getResources().getResourceEntryName(R.drawable.ic_flag_global))
                    .commit();
        }
    }

    private IShadowsocksServiceCallback.Stub createShadowsocksServiceCallbackBinder(){
        return new IShadowsocksServiceCallback.Stub(){
            @Override
            public void stateChanged(final int state, String msg) throws RemoteException {
                mUpdateVpnStateRunable = new Runnable() {
                    @Override
                    public void run() {
                        mNewState = Constants.State.values()[state];
                        updateConnectionState();
                        Log.d("状态", mNewState.name());
                    }
                };
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
                    try {
                        if (DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this).getBoolean(SharedPreferenceKey.FIRST_CONNECT_SUCCESS, false)) {
                            if(adAppHelper.isFullAdLoaded()) {
                                GAHelper.sendEvent(this, "广告", "点击功能按钮");
                                UMGameAgent.onEvent(getApplicationContext(), "gnan");
                                adAppHelper.showFullAd();
                                showResumeAd = true;
                            }else{
                                rotateAd();
                            }
                        }
                    } catch (Exception ex) {
                    }

                    if (!startUp) {
                        startUp = true;
                        adAppHelper.loadNewInterstitial();
                        adAppHelper.loadNewNative();
                        adAppHelper.loadNewBanner();
                    }
                    if (mConnectFragment != null) {
                        mConnectFragment.setConnectResult(mNewState);
                    }

                    if (mConnectingConfig == null) {
                        mConnectingConfig = ServerConfig.loadFromSharedPreference(mSharedPreference);
                    } else {
                        if (mSharedPreference != null) {
                            mSharedPreference.edit()
                                    .putLong(com.androapplite.shadowsocks.preference.SharedPreferenceKey.CONNECT_TIME, System.currentTimeMillis())
                                    .commit();
                        }
                    }
                    clearConnectingTimeout();
                    if(mConnectFragment != null && mConnectingConfig != null ) {
                        ConnectionTestService.testConnection(this, mConnectingConfig.name);
                        showRateUsFragment();
                    }
                    mIsConnecting = false;
                    mErrorServers.clear();
                    break;
                case STOPPING:
                    clearConnectingTimeout();
                    break;
                case STOPPED:
                    mIsConnecting = false;
                    if (mConnectFragment != null) {
                        mConnectFragment.setConnectResult(mNewState);
                    }
                    clearConnectingTimeout();
                    break;
                case ERROR:
                    if (mConnectFragment != null) {
                        mConnectFragment.setConnectResult(mNewState);
                    }
                    clearConnectingTimeout();
                    mIsConnecting = false;
                    mErrorServers.add(mConnectingConfig);
                    break;
            }
        }
        changeProxyFlagIcon();
    }

    private void showRateUsFragment() {
        if(!mSharedPreference.getBoolean(SharedPreferenceKey.IS_RATE_US_FRAGMENT_SHOWN, false)) {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.rate_us_frame_layout);
            if(fragment == null) {
                mShowRateUsRunnable = new Runnable() {
                    @Override
                    public void run() {
                        try {

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                                if (!ConnectivityActivity.this.isDestroyed()) {
                                    getSupportFragmentManager().beginTransaction()
                                            .replace(R.id.rate_us_frame_layout, RateUsFragment.newInstance())
                                            .commitAllowingStateLoss();
                                }
                            } else {
                                getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.rate_us_frame_layout, RateUsFragment.newInstance())
                                        .commitAllowingStateLoss();
                            }
                        }catch (Exception e){
                            ShadowsocksApplication.handleException(e);
                        }

                    }
                };
                getWindow().getDecorView().postDelayed(mShowRateUsRunnable, 2000);
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

            AdAppHelper.getInstance(this).showFullAd();
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
        mConnectingTimeoutRunnable = new Runnable() {
            @Override
            public void run() {
                if(mShadowsocksService != null){
                    try {
                        mShadowsocksService.stop();
                    } catch (RemoteException e) {
                        ShadowsocksApplication.handleException(e);
                    }
                }
                mConnectingTimeoutHandler = null;
                mConnectingTimeoutRunnable = null;
                Snackbar.make(findViewById(R.id.coordinator), R.string.timeout_tip, Snackbar.LENGTH_SHORT).show();
            }
        };
        mConnectingTimeoutHandler.postDelayed(mConnectingTimeoutRunnable, TimeUnit.SECONDS.toMillis(20));
        new Thread(new Runnable() {
            @Override
            public void run() {
                final ServerConfig serverConfig = findVPNServer();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(serverConfig != null) {
                            mConnectingConfig = serverConfig;
                            prepareStartService();
                        }else {
                            Snackbar.make(findViewById(R.id.coordinator), R.string.server_not_available, Snackbar.LENGTH_LONG).show();
                            if(mConnectFragment != null){
                                mConnectFragment.setConnectResult(Constants.State.ERROR);
                            }
                            mIsConnecting = false;
                        }
                    }
                });
            }
        }).start();
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
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Action.CONNECTION_ACTIVITY_SHOW));
        if(mShadowsocksService != null){
            try {
                mNewState = Constants.State.values()[mShadowsocksService.getState()];
            } catch (RemoteException e) {
                ShadowsocksApplication.handleException(e);
            }
        }
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
    protected void onResume() {
        AdAppHelper.getInstance(getApplicationContext()).onResume();
        super.onResume();

    }

    @Override
    protected void onPause() {
        AdAppHelper.getInstance(getApplicationContext()).onPause();
        super.onPause();
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
                try {
                    if (DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this).getBoolean(SharedPreferenceKey.FIRST_CONNECT_SUCCESS, false)) {
                        GAHelper.sendEvent(this, "广告", "点击功能按钮");
                        UMGameAgent.onEvent(getApplicationContext(), "gnan");
                        AdAppHelper.getInstance(getApplicationContext()).showFullAd();
                        showResumeAd = true;
                    }
                } catch (Exception ex) {
                }
            } else if (requestCode == OPEN_SERVER_LIST) {
                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                if (connectivityManager != null) {
                    NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                    if (networkInfo != null && networkInfo.isAvailable()) {
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
                try {
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                } catch (ActivityNotFoundException e) {
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

        ArrayList<ServerConfig> serverConfigs = loadServerList();
        if(serverConfigs !=null && !serverConfigs.isEmpty()) {
            if (mNewState == Constants.State.INIT || mNewState == Constants.State.STOPPED) {
                serverConfig = ServerConfig.loadFromSharedPreference(mSharedPreference);
            }
            if (serverConfig != null) {
                if (!serverConfigs.contains(serverConfig) ||
                        mErrorServers.contains(serverConfig)) {
                    serverConfig = null;
                } else {
                    Pair<Boolean, Long> pair = isPortOpen(serverConfig.server, serverConfig.port, 15000);
                    if (pair.first) {
                        GAHelper.sendTimingEvent(this, "连接测试成功", serverConfig.name, pair.second);
                    } else {
                        GAHelper.sendTimingEvent(this, "连接测试失败", serverConfig.name, pair.second);
                        serverConfig = null;
                    }

                }
            }
            if (serverConfig == null) {
                final String global = getString(R.string.vpn_nation_opt);
                final String nation = mSharedPreference.getString(SharedPreferenceKey.VPN_NATION, global);
                final boolean hasServerListJson = mSharedPreference.contains(SharedPreferenceKey.SERVER_LIST);
                final boolean isGlobalOption = nation.equals(global);
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
                if (!hasServerListJson) {
                    Collections.shuffle(filteredConfigs);
                }
                int i;
                for (i = 0; i < filteredConfigs.size(); i++) {
                    serverConfig = filteredConfigs.get(i);
                    if (mErrorServers.contains(serverConfig)) continue;
                    Pair<Boolean, Long> pair = isPortOpen(serverConfig.server, serverConfig.port, 15000);
                    if (pair.first) {
                        GAHelper.sendTimingEvent(this, "连接测试成功", serverConfig.name, pair.second);
                        break;
                    } else {
                        GAHelper.sendTimingEvent(this, "连接测试失败", serverConfig.name, pair.second);
                    }
                }
                if (i >= filteredConfigs.size()) {
                    serverConfig = null;
                }
            }
        }
        return serverConfig;
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
        if(serverlist != null){
            ArrayList<ServerConfig> serverList = ServerConfig.createServerList(this, serverlist);
            if(serverList != null && serverList.size() > 1){
                result = serverList;
            }
        }
//        if(result == null){
//            result = ServerConfig.createDefaultServerList(getResources());
//        }
        return result;
    }

    public static Pair<Boolean, Long> isPortOpen(final String ip, final int port, final int timeout) {
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
//        return new Pair<>(true, 150L);
    }

    @Override
    public void onConnectButtonClick() {
        if(mShadowsocksService != null) {
            if (!mIsConnecting && (mNewState == Constants.State.INIT || mNewState == Constants.State.STOPPED || mNewState == Constants.State.ERROR)) {
                if(checkNetworkConnectivity()) {
                    connectVpnServerAsync();
                }
                GAHelper.sendEvent(this, "连接VPN", "连接", mNewState.name());
                if(mSharedPreference != null) {
                    String nation = mSharedPreference.getString(SharedPreferenceKey.VPN_NATION, "空");
                    GAHelper.sendEvent(this, "选择国家", nation);
                }
            } else {
                DisconnectFragment disconnectFragment = new DisconnectFragment();
                disconnectFragment.show(getSupportFragmentManager(), "disconnect");
                GAHelper.sendEvent(this, "连接VPN", "断开", mNewState.name());
            }
        }

    }

    @Override
    public void onCloseRateUs(final RateUsFragment fragment) {
        remoeRateUsFragment(fragment);
    }

    private void remoeRateUsFragment(RateUsFragment fragment) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                if (!isDestroyed()) {
                    getSupportFragmentManager().beginTransaction().remove(fragment).commitAllowingStateLoss();
                }
            } else {
                getSupportFragmentManager().beginTransaction().remove(fragment).commitAllowingStateLoss();
            }
        }catch (Exception e){
            ShadowsocksApplication.handleException(e);
        }
        mSharedPreference.edit().putBoolean(SharedPreferenceKey.IS_RATE_US_FRAGMENT_SHOWN, true).commit();
    }

    @Override
    public void onRateUs(final RateUsFragment fragment) {
        remoeRateUsFragment(fragment);
        rateUs();
    }

    @Override
    public void onCancel(DisconnectFragment disconnectFragment) {
        GAHelper.sendEvent(this, "连接VPN", "断开", "取消断开");
    }

    @Override
    public void onDisconnect(DisconnectFragment disconnectFragment) {
        disconnectVpnServiceAsync();
        GAHelper.sendEvent(this, "连接VPN", "断开", "确认断开");
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
}
