
package com.androapplite.shadowsocks.activity;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.VpnService;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import com.androapplite.shadowsocks.GAHelper;
import com.androapplite.shadowsocks.R;
import com.androapplite.shadowsocks.ShadowsockServiceHelper;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.fragment.ConnectionFragment;
import com.androapplite.shadowsocks.fragment.RateUsFragment;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;

import java.lang.System;

import yyf.shadowsocks.Config;
import yyf.shadowsocks.IShadowsocksService;
import yyf.shadowsocks.IShadowsocksServiceCallback;
import yyf.shadowsocks.utils.Constants;

public class ConnectionActivity extends BaseShadowsocksActivity implements
        RateUsFragment.OnFragmentInteractionListener, ConnectionFragment.OnFragmentInteractionListener{
    private RateUsFragment mRateUsFragment;
    private static int REQUEST_CONNECT = 1;
    private IShadowsocksService mShadowsocksService;
    private ServiceConnection mShadowsocksServiceConnection;
    private ConnectionFragment mConnectionFragment;
    private IShadowsocksServiceCallback.Stub mShadowsocksServiceCallbackBinder;
    private long mConnectOrDisconnectStartTime;
    private BroadcastReceiver mConnectivityBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);
        mConnectionFragment = findConectionFragment();
        initToobar();
        showRateUsFragmentWhenFirstOpen();
        mShadowsocksServiceConnection = createShadowsocksServiceConnection();
        mShadowsocksServiceCallbackBinder = createShadowsocksServiceCallbackBinder();
        ShadowsockServiceHelper.bindService(this, mShadowsocksServiceConnection);

        mConnectivityBroadcastReceiver = createConnectivityReceiver();
        registerConnectivityReceiver();
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
            public void trafficUpdated(long txRate, long rxRate, long txTotal, long rxTotal) throws RemoteException {
                ShadowsocksApplication.debug("traffic", "txRate: " + txRate);
                ShadowsocksApplication.debug("traffic", "rxRate: " + rxRate);
                ShadowsocksApplication.debug("traffic", "txTotal: " + txTotal);
                ShadowsocksApplication.debug("traffic", "rxTotal: " + rxTotal);
            }
        };
    }

    private void updateConnectionState(int state) {
        if(mConnectionFragment != null) {
            if (state == Constants.State.CONNECTED.ordinal()) {
                mConnectionFragment.connected();
                if(mConnectOrDisconnectStartTime > 0) {
                    long t = System.currentTimeMillis();
                    GAHelper.sendTimingEvent(ConnectionActivity.this, "VPN计时", "连接", t-mConnectOrDisconnectStartTime);
                    mConnectOrDisconnectStartTime = 0;
                }
            }else if(state == Constants.State.STOPPED.ordinal()) {
                mConnectionFragment.stop();
                if(mConnectOrDisconnectStartTime > 0){
                    long t = System.currentTimeMillis();
                    GAHelper.sendTimingEvent(ConnectionActivity.this, "VPN计时", "断开", t-mConnectOrDisconnectStartTime);
                    mConnectOrDisconnectStartTime = 0;
                }
            }else if(state == Constants.State.CONNECTING.ordinal()){
                mConnectionFragment.connecting();
            }
        }
    }

    private void showRateUsFragmentWhenFirstOpen() {
        if(!DefaultSharedPrefeencesUtil.doesNeedRateUsFragmentShow(this)) {
            mRateUsFragment = RateUsFragment.newInstance();
            initRateUsFragment();
        }
    }

    private ConnectionFragment findConectionFragment(){
        FragmentManager fragmentManager = getSupportFragmentManager();
        return (ConnectionFragment)fragmentManager.findFragmentById(R.id.connection_fragment);
    }

    private ServiceConnection createShadowsocksServiceConnection(){
        return new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mShadowsocksService = IShadowsocksService.Stub.asInterface(service);
                registerShadowsocksCallback();
                try {
                    updateConnectionState(mShadowsocksService.getState());
                } catch (RemoteException e) {
                    ShadowsocksApplication.handleException(e);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mShadowsocksService = null;
            }
        };
    }

    private void initRateUsFragment(){
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.rate_us_fragment_container, mRateUsFragment).commit();

    }

    private void initToobar(){
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Action.CONNECTION_ACTIVITY_SHOW));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_connection_activity, menu);
/*        MenuItem item = menu.findItem(R.id.share_icon);
        Drawable drawable = item.getIcon();
        drawable.setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_ATOP);
        item.setIcon(drawable);*/
        final int whiteColor = getResources().getColor(android.R.color.white);
        final int primaryColor = getResources().getColor(R.color.colorPrimary);
        for(int i=0; i<menu.size(); i++){
            MenuItem menuItem = menu.getItem(i);
            paintColorOnMenuItem(menuItem, whiteColor);
            SubMenu subMenu = menuItem.getSubMenu();
            if(subMenu != null){
                for(int j=0; j<subMenu.size(); j++){

                    paintColorOnMenuItem(subMenu.getItem(j), primaryColor);
                }
            }
        }
        return true;
    }

    private static void paintColorOnMenuItem(@NonNull MenuItem menuItem, @ColorInt int color) {
        Drawable drawable = menuItem.getIcon();
        if(drawable != null) {
            drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            menuItem.setIcon(drawable);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.share_icon:
                share();
                GAHelper.sendEvent(this, "菜单", "分享", "图标");
                break;
            case R.id.share:
                share();
                GAHelper.sendEvent(this, "菜单", "分享", "文字");
                break;
            case R.id.rate_us:
                rateUs();
                GAHelper.sendEvent(this, "菜单", "给我们打分");
                break;
            case R.id.contact_us:
                contactUs();
                GAHelper.sendEvent(this, "菜单", "联系我们");
                break;
            case R.id.about:
                about();
                GAHelper.sendEvent(this, "菜单", "关于");
                break;
        }
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

    @Override
    public void onCloseRateUs() {
        if(mRateUsFragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().remove(mRateUsFragment).commit();
            mRateUsFragment = null;
            DefaultSharedPrefeencesUtil.markRateUsFragmentNotNeedToShow(this);
            GAHelper.sendEvent(this, "给我们打分", "关闭");
        }
    }

    @Override
    public void onRateUs() {
        rateUs();
        DefaultSharedPrefeencesUtil.markRateUsFragmentNotNeedToShow(this);
        GAHelper.sendEvent(this, "给我们打分", "打开");
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
            }

        }catch (RemoteException e){
            ShadowsocksApplication.handleException(e);
        }

    }


    @Override
    protected void onStart() {
        super.onStart();
        registerShadowsocksCallback();
        checkConnectivity();
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
        if(mShadowsocksServiceConnection != null){
            unbindService(mShadowsocksServiceConnection);
        }

        if(mConnectivityBroadcastReceiver != null){
            unregisterReceiver(mConnectivityBroadcastReceiver);
        }
    }

    private BroadcastReceiver createConnectivityReceiver(){
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent != null
                        && ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())
                        && intent.getIntExtra(ConnectivityManager.EXTRA_NETWORK_TYPE, -1) == ConnectivityManager.TYPE_VPN) {
                    checkConnectivity();
                }
            }
        };
    }

    private void registerConnectivityReceiver(){
        //android4的vpn变化没有发送广播
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mConnectivityBroadcastReceiver, intentFilter);
    }

    private void checkConnectivity(){
        if(mConnectionFragment != null){
            ConnectivityManager cm =
                    (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_VPN);
            if(activeNetwork != null ){
                if(activeNetwork.isConnectedOrConnecting()){
                    if(activeNetwork.isConnected()){
                        mConnectionFragment.connected();
                    }else{
                        mConnectionFragment.connecting();
                    }
                }else{
                    mConnectionFragment.stop();
                }
            }
        }
    }
}
