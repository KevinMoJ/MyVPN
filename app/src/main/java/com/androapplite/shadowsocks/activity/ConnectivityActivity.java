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
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.VpnService;
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
import android.support.v4.app.FragmentManager;
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

import com.androapplite.shadowsocks.GAHelper;
import com.androapplite.shadowsocks.R;
import com.androapplite.shadowsocks.ShadowsockServiceHelper;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.fragment.ConnectionIndicatorFragment;
import com.androapplite.shadowsocks.fragment.ConnectivityFragment;
import com.androapplite.shadowsocks.fragment.TrafficRateFragment;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.smartads.Plugins;
import com.smartads.ads.AdBannerType;
import com.smartads.ads.AdType;
import com.smartads.plugin.PluginAdListener;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;

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
    private Snackbar mSnackbar;
    private AlertDialog mNoInternetDialog;
    private BroadcastReceiver mConnectivityReceiver;
    private ConnectionIndicatorFragment mConnectionIndicatorFragment;
    private Menu mMenu;
    private int mTemporaryVpnSelectIndex;
    private boolean mShowAdSwitch;
    private Handler mAdSwitchHandler;
    private Runnable mAdSwitchCallback;

    boolean showFirst = false;

    public static final String NAME = "ConnectivityActivity";

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
        initConnectivityReceiver();
        toolbar.post(new Runnable() {
            @Override
            public void run() {
                final SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(ConnectivityActivity.this);
                if (sharedPreferences.getBoolean(SharedPreferenceKey.NEED_TO_SHOW_PROXY_POPUP, true)) {
                    try {
                        showVpnServerChangePopupWindow();
                        sharedPreferences.edit().putBoolean(SharedPreferenceKey.NEED_TO_SHOW_PROXY_POPUP, false).apply();
                    } catch (RuntimeException e) {
                        ShadowsocksApplication.handleException(e);
                    }
                }
            }
        });
        mTemporaryVpnSelectIndex = indexOfSelectedVPN();


        Plugins.onEnter(ConnectivityActivity.NAME, this.getApplicationContext());
        Plugins.initBannerAd(ConnectivityActivity.NAME);
        Plugins.initNgsAd(ConnectivityActivity.NAME);

        Plugins.setPluginAdListener(new PluginAdListener() {
            @Override
            public void onReceiveAd(String s, AdType adType) {
                if (adType == AdType.Ngs) {
                    ngsLoaded = true;
                }
            }

            @Override
            public void onAdClosed(String s, AdType adType) {
                if (adType == AdType.Ngs) {
                    try {
                        Plugins.loadNewNgsAd(NAME);
                    } catch (Exception ex) {
                    }
                }
            }
        });

        ngsLoaded = false;

        try {
            Plugins.loadNewBannerAd(NAME);
            Plugins.loadNewNgsAd(NAME);
        } catch (Exception ex) {
        }

        FrameLayout container = (FrameLayout)findViewById(R.id.ad_view_container);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER);
        try {
            container.addView(Plugins.adBanner(NAME), params);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        handler1.sendEmptyMessageDelayed(1000, 1000);
    }

    private boolean startUp = false;
    private boolean ngsLoaded = false;
    private boolean showResumeAd = false;
    private Handler handler1 = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1000) {
                if (ngsLoaded) {
                    try {
                        Plugins.adNgs(NAME, -1);
                    } catch (Exception ex) {}
                } else {
                    handler1.sendEmptyMessageDelayed(1000, 200);
                }
            }
        }
    };

    private IShadowsocksServiceCallback.Stub createShadowsocksServiceCallbackBinder(){
        return new IShadowsocksServiceCallback.Stub(){
            @Override
            public void stateChanged(final int state, String msg) throws RemoteException {
                restoreVpnSelectIndex();
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
            if(state == Constants.State.CONNECTING.ordinal()){

            }else if(state == Constants.State.CONNECTED.ordinal()){
                try {
                    Plugins.adNgs(NAME, -1);
                } catch (Exception ex) {
                }

                if (!startUp) {
                    try {
                        Plugins.loadNewBannerAd(NAME);
                        Plugins.loadNewNgsAd(NAME);
                    } catch (Exception ex) {
                    }
                    startUp = true;
                }
            }else if(state == Constants.State.ERROR.ordinal()){
                restoreVpnSelectIndex();
                changeProxyFlagIcon();
            }else if(state == Constants.State.INIT.ordinal()){
                restoreVpnSelectIndex();
                changeProxyFlagIcon();
            }else if(state == Constants.State.STOPPING.ordinal()){
            }else if(state == Constants.State.STOPPED.ordinal()){
                restoreVpnSelectIndex();
                changeProxyFlagIcon();
            }
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
                int state = 0;
                try {
                    state = mShadowsocksService.getState();
                } catch (RemoteException e) {
                    ShadowsocksApplication.handleException(e);
                }
                if(state == Constants.State.INIT.ordinal() ||
                        state == Constants.State.STOPPED.ordinal()){
                    showVpnServerChangePopupWindow();
                    GAHelper.sendEvent(this, "ProxyPopup", "正确时机", String.valueOf(state));
                }else{
                    Snackbar.make(findViewById(R.id.coordinator), R.string.change_proxy_tip, Snackbar.LENGTH_SHORT).show();
                    GAHelper.sendEvent(this, "ProxyPopup", "错误时机", String.valueOf(state));

                }
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showVpnServerChangePopupWindow() {
        final View popupView = getLayoutInflater().inflate(R.layout.popup_vpn_server, null);

        ListView vpnServerListView = (ListView) popupView.findViewById(R.id.vpn_server_list);
        vpnServerListView.setAdapter(new VpnServerItemAdapter());

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        final PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setBackgroundDrawable(new BitmapDrawable());
        vpnServerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                saveVpnName(position);
                mTemporaryVpnSelectIndex = indexOfSelectedVPN();
                changeProxyFlagIcon();
                popupWindow.dismiss();
            }
        });

        View toobar = findViewById(R.id.toolbar);
        int offset = (int) getResources().getDimension(R.dimen.standard_margin);
        try {
            popupWindow.showAsDropDown(toobar, toobar.getWidth() - offset - popupView.getMeasuredWidth(), -offset);
        }catch (Exception e){
            ShadowsocksApplication.handleException(e);
            Snackbar.make(findViewById(R.id.coordinator), R.string.not_open_vpn_popup, Snackbar.LENGTH_SHORT).show();
        }
    }

    public class VpnServerItemAdapter extends BaseAdapter{
        private TypedArray mServerNames;
        private TypedArray mServerIcons;

        public VpnServerItemAdapter(){
            Resources resources = getResources();
            mServerNames = resources.obtainTypedArray(R.array.vpn_names);
            mServerIcons = resources.obtainTypedArray(R.array.vpn_icons);
        }

        @Override
        public int getCount() {
            return mServerNames.length();
        }

        @Override
        public Object getItem(int position) {
            return mServerNames.getString(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = null;
            ViewHolder viewHolder = null;
            if(convertView == null) {
                view = getLayoutInflater().inflate(R.layout.item_popup_vpn_server, null);
                viewHolder = new ViewHolder();
                viewHolder.vpnIconImageView = (ImageView)view.findViewById(R.id.vpn_icon);
                viewHolder.vpnNameTextView = (TextView)view.findViewById(R.id.vpn_name);
                view.setTag(viewHolder);
            }else{
                view = convertView;
                viewHolder = (ViewHolder)view.getTag();
            }
            viewHolder.vpnIconImageView.setImageResource(
                    mServerIcons.getResourceId(position, R.drawable.ic_close_24dp));
            viewHolder.vpnNameTextView.setText(
                    mServerNames.getResourceId(position, R.string.vpn_name_opt)
            );
            return view;
        }

        public class ViewHolder{
            public ImageView vpnIconImageView;
            public TextView vpnNameTextView;
        }

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

    @Override
    public void onClickConnectionButton() {
        if(mShadowsocksService != null) {
            try {
                final int state = mShadowsocksService.getState();
                if (mShadowsocksService == null || state == Constants.State.INIT.ordinal()
                        || state == Constants.State.STOPPED.ordinal()) {
                    findVPNServer();
                    ShadowsocksApplication.debug("select vpn", mTemporaryVpnSelectIndex + "");
                    changeProxyFlagIcon();
                    prepareStartService();
                    mConnectOrDisconnectStartTime = System.currentTimeMillis();
                    GAHelper.sendEvent(this, "连接VPN", "打开", String.valueOf(state));

                } else {
                    mShadowsocksService.stop();
                    mConnectOrDisconnectStartTime = System.currentTimeMillis();
                    GAHelper.sendEvent(this, "连接VPN", "关闭", String.valueOf(state));
                    try {
                        Plugins.adNgs(NAME, -1);
                    } catch (Exception ex) {
                    }
//                    AdHelper.getInstance(this).showByTag(getString(R.string.tag_connect));
                }

            } catch (RemoteException e) {
                ShadowsocksApplication.handleException(e);
            }
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
        Plugins.onDestroy(NAME);
        super.onDestroy();
        if (mShadowsocksServiceConnection != null) {
            unbindService(mShadowsocksServiceConnection);
        }
    }

    @Override
    protected void onResume() {
        Plugins.onResume(NAME, this.getApplicationContext());
        super.onResume();
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Action.CONNECTION_ACTIVITY_SHOW));
        //todo 显示插页广告,如果有可能,两分钟内,再次打开不要显示广告
//        ShadowsocksApplication.debug("广告开关", "显示 " + mShowAdSwitch);
        AppEventsLogger.activateApp(this);

        if (!showResumeAd) {
            try {
                Plugins.adNgs(NAME, -1);
            } catch (Exception ex) {
            }
            showResumeAd = true;
        } else {
            showResumeAd = false;
        }
    }

    @Override
    protected void onPause() {
        Plugins.onPause(NAME, this.getApplicationContext());
        super.onPause();
        AppEventsLogger.deactivateApp(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            //Intent intent = new Intent(this, ShadowsocksVpnService.class);
            //bindService(intent,connection,BIND_AUTO_CREATE);
            if(mShadowsocksService != null){
                try {
                    Config config = new Config(findVPNServer());
                    mShadowsocksService.start(config);
                    ShadowsocksApplication.debug("ss-vpn", "bgService.StartVpn");
//                    AdHelper.getInstance(this).showByTag(getString(R.string.tag_connect));
                }catch(RemoteException e){
                    ShadowsocksApplication.handleException(e);
                }
            }else{
                ShadowsocksApplication.debug("ss-vpn", "bgServiceIsNull");
            }
            Plugins.adNgs(NAME, -1);

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
            SharedPreferences sharedPreference = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
            final String vpnName = button.getText().toString();
            sharedPreference.edit().putString(SharedPreferenceKey.VPN_NAME, vpnName).apply();
            GAHelper.sendEvent(this, "选择VPN", vpnName);

        }
    }

    private void saveVpnName(int position){
        Resources resources = getResources();
        TypedArray names = resources.obtainTypedArray(R.array.vpn_names);
        final String vpnName = names.getString(position);
        SharedPreferences sharedPreference = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
        sharedPreference.edit().putString(SharedPreferenceKey.VPN_NAME, vpnName).apply();
        GAHelper.sendEvent(this, "选择VPN", vpnName);
    }

    private String findVPNServer(){
        mTemporaryVpnSelectIndex = indexOfSelectedVPNWithParsingOptimizedServer();
        TypedArray b = getResources().obtainTypedArray(R.array.vpn_servers);
        return b.getString(mTemporaryVpnSelectIndex);
    }

    private int indexOfSelectedVPN() {
        SharedPreferences sharedPreference = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
        String vpnName = sharedPreference.getString(SharedPreferenceKey.VPN_NAME, null);
        TypedArray a = getResources().obtainTypedArray(R.array.vpn_names);
        int i = 0;
        if(vpnName != null){
            for(i = 0; i < a.length(); i++){
                if(vpnName.equals(a.getString(i))){
                    break;
                }
            }
            if(i >= a.length()){
                i = 0;
            }
        }
        return i;
    }

    private int indexOfSelectedVPNWithParsingOptimizedServer() {
        int i = indexOfSelectedVPN();
        if(i == 0){
            TypedArray a = getResources().obtainTypedArray(R.array.vpn_names);
            i = (int) (Math.random() * (a.length() - 1) + 1);
        }
        return i;
    }

    private void changeProxyFlagIcon(){
        TypedArray icons = getResources().obtainTypedArray(R.array.vpn_icons);
        if(mMenu != null) {
            if(mTemporaryVpnSelectIndex == 0){
                mMenu.findItem(R.id.action_flag).setIcon(R.drawable.ic_flag_global_clicked);
            }else {
                mMenu.findItem(R.id.action_flag).setIcon(icons.getDrawable(mTemporaryVpnSelectIndex));
            }
//            mMenu.findItem(R.id.action_flag).setIcon(icons.getDrawable(mTemporaryVpnSelectIndex));
        }
    }

    private void restoreVpnSelectIndex(){
        mTemporaryVpnSelectIndex = indexOfSelectedVPN();
    }
}
