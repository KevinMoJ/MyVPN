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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.design.widget.NavigationView;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.androapplite.shadowsocks.ad.AdHelper;
import com.androapplite.shadowsocks.GAHelper;
import com.androapplite.shadowsocks.R;
import com.androapplite.shadowsocks.ShadowsockServiceHelper;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.ad.Banner;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.fragment.ConnectionIndicatorFragment;
import com.androapplite.shadowsocks.fragment.ConnectivityFragment;
import com.androapplite.shadowsocks.fragment.TrafficRateFragment;

import java.lang.System;

import yyf.shadowsocks.Config;
import yyf.shadowsocks.IShadowsocksService;
import yyf.shadowsocks.IShadowsocksServiceCallback;
import yyf.shadowsocks.utils.Constants;
import yyf.shadowsocks.utils.TrafficMonitor;

public class ConnectivityActivity extends BaseShadowsocksActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        ConnectivityFragment.OnFragmentInteractionListener{

    private IShadowsocksService mShadowsocksService;
    private ServiceConnection mShadowsocksServiceConnection;
    private IShadowsocksServiceCallback.Stub mShadowsocksServiceCallbackBinder;
    private ConnectivityFragment mConnectivityFragment;
    private static int REQUEST_CONNECT = 1;
    private long mConnectOrDisconnectStartTime;
    private TrafficRateFragment mTrafficRateFragment;
//    private AdView mAdView;
    private Banner mBanner;
    private Snackbar mSnackbar;
    private AlertDialog mNoInternetDialog;
    private BroadcastReceiver mConnectivityReceiver;
    private ConnectionIndicatorFragment mConnectionIndicatorFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connectivity);
        Toolbar toolbar = initToolbar();
        initDrawer(toolbar);
        initNavigationView();
        mShadowsocksServiceConnection = createShadowsocksServiceConnection();
        ShadowsockServiceHelper.bindService(this, mShadowsocksServiceConnection);
        mShadowsocksServiceCallbackBinder = createShadowsocksServiceCallbackBinder();
        FragmentManager fragmentManager = getSupportFragmentManager();
        mConnectivityFragment = (ConnectivityFragment)fragmentManager.findFragmentById(R.id.connection_fragment);
        mTrafficRateFragment = (TrafficRateFragment)fragmentManager.findFragmentById(R.id.traffic_fragment);
        mConnectionIndicatorFragment = (ConnectionIndicatorFragment)fragmentManager.findFragmentById(R.id.connection_indicator);
        GAHelper.sendScreenView(this, "VPN连接屏幕");
        mBanner = AdHelper.getInstance(this).addToViewGroup(getString(R.string.tag_banner),
                (ViewGroup)findViewById(R.id.ad_view_container));
        initConnectivityReceiver();
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
                if(mTrafficRateFragment != null){
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            mTrafficRateFragment.updateRate(TrafficMonitor.formatTrafficRate(ConnectivityActivity.this, txRate),
                                    TrafficMonitor.formatTrafficRate(ConnectivityActivity.this, rxRate));
                        }
                    });
                }
            }
        };
    }

    private void updateConnectionState(int state){
        if(mConnectivityFragment != null){
//            if(state == Constants.State.CONNECTING.ordinal()){
//
//            }else if(state == Constants.State.CONNECTED.ordinal()){
//            }else if(state == Constants.State.ERROR.ordinal()){
//            }else if(state == Constants.State.INIT.ordinal()){
//            }else if(state == Constants.State.STOPPING.ordinal()){
//            }else if(state == Constants.State.STOPPED.ordinal()){
//
//            }
            mConnectivityFragment.updateConnectionState(state);
            mConnectionIndicatorFragment.updateConnectionState(state);
        }
    }

    private ServiceConnection createShadowsocksServiceConnection(){
        return new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mShadowsocksService = IShadowsocksService.Stub.asInterface(service);
                registerShadowsocksCallback();
                checkVPNConnectivity();

            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                unregisterShadowsocksCallback();
                mShadowsocksService = null;
            }
        };
    }

    private void checkVPNConnectivity() {
        try {
            if(mShadowsocksService != null) {
                updateConnectionState(mShadowsocksService.getState());
                if(mShadowsocksService.getState() == Constants.State.CONNECTED.ordinal()) {
                    if (mConnectOrDisconnectStartTime > 0) {
                        long t = System.currentTimeMillis();
                        GAHelper.sendTimingEvent(this, "VPN计时", "连接", t - mConnectOrDisconnectStartTime);
                        mConnectOrDisconnectStartTime = 0;
                    }
                }else if(mShadowsocksService.getState() == Constants.State.STOPPED.ordinal()){
                    if (mConnectOrDisconnectStartTime > 0) {
                        long t = System.currentTimeMillis();
                        GAHelper.sendTimingEvent(this, "VPN计时", "断开", t - mConnectOrDisconnectStartTime);
                        mConnectOrDisconnectStartTime = 0;
                    }
                }
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
                int state = mShadowsocksService.getState();
                if(state == Constants.State.INIT.ordinal() ||
                        state == Constants.State.STOPPED.ordinal()){
                    View popupView = getLayoutInflater().inflate(R.layout.popup_proxy, null);
                    popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                    PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
                    popupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.overlay_popup_menu_bg));
                    View toobar = findViewById(R.id.toolbar);
                    popupWindow.showAsDropDown(toobar, toobar.getWidth()-popupView.getWidth(), 0);
                }
            })
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
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void share(){
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, getPlayStoreUrlString());
        shareIntent.setType("text/plain");
        try {
            startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.share)));
        }catch(ActivityNotFoundException e){
            ShadowsocksApplication.handleException(e);
        }
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
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(getPlayStoreUrlString())));
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
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            startActivityForResult(intent, REQUEST_CONNECT);
            ShadowsocksApplication.debug("ss-vpn", "startActivityForResult");
        } else {
            onActivityResult(REQUEST_CONNECT, Activity.RESULT_OK, null);
            ShadowsocksApplication.debug("ss-vpn", "onActivityResult");
        }
    }

    @Override
    public void onClickConnectionButton() {
        try {
            if (mShadowsocksService == null || mShadowsocksService.getState() == Constants.State.INIT.ordinal()
                    || mShadowsocksService.getState() == Constants.State.STOPPED.ordinal()) {
                prepareStartService();
                mConnectOrDisconnectStartTime = System.currentTimeMillis();
                GAHelper.sendEvent(this, "连接VPN", "打开");
            }else{
                mShadowsocksService.stop();
                mConnectOrDisconnectStartTime = System.currentTimeMillis();
                GAHelper.sendEvent(this, "连接VPN", "关闭");
                AdHelper.getInstance(this).showByTag(getString(R.string.tag_connect));
            }

        }catch (RemoteException e){
            ShadowsocksApplication.handleException(e);
        }

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
        if(mShadowsocksService != null){
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
        if(mBanner != null) {
            mBanner.destory();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Action.CONNECTION_ACTIVITY_SHOW));
        if(mBanner != null) {
            mBanner.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mBanner != null) {
            mBanner.pause();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            //Intent intent = new Intent(this, ShadowsocksVpnService.class);
            //bindService(intent,connection,BIND_AUTO_CREATE);
            if(mShadowsocksService != null){
                try {
                    Config config = new Config();
                    mShadowsocksService.start(config);
                    ShadowsocksApplication.debug("ss-vpn", "bgService.StartVpn");
                }catch(RemoteException e){
                    ShadowsocksApplication.handleException(e);
                }
            }else{
                ShadowsocksApplication.debug("ss-vpn", "bgServiceIsNull");
            }
            AdHelper.getInstance(this).showByTag(getString(R.string.tag_connect));

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

    public void chooseVPN(View view){
        if(view instanceof Button){
            Button button = (Button)view;
            SharedPreference sharedPreference = DefaultSharedPreferenceUtil.getDefaultSharedPreferences(this);
        }
    }

}
