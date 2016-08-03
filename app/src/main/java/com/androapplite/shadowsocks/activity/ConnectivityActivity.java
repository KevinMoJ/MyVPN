package com.androapplite.shadowsocks.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.support.v4.app.FragmentManager;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.androapplite.shadowsocks.GAHelper;
import com.androapplite.shadowsocks.R;
import com.androapplite.shadowsocks.ShadowsockServiceHelper;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.fragment.ConnectivityFragment;
import com.androapplite.shadowsocks.fragment.TrafficRateFragment;

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
            public void trafficUpdated(long txRate, long rxRate, final long txTotal, final long rxTotal) throws RemoteException {
                ShadowsocksApplication.debug("traffic", "txRate: " + txRate);
                ShadowsocksApplication.debug("traffic", "rxRate: " + rxRate);
                ShadowsocksApplication.debug("traffic", "txTotal: " + txTotal);
                ShadowsocksApplication.debug("traffic", "rxTotal: " + rxTotal);
                ShadowsocksApplication.debug("traffic", "txTotal old : " + TrafficMonitor.formatTraffic(ConnectivityActivity.this, mShadowsocksService.getTxTotalMonthly()));
                ShadowsocksApplication.debug("traffic", "rxTotal old : " + TrafficMonitor.formatTraffic(ConnectivityActivity.this, mShadowsocksService.getRxTotalMonthly()));
                if(mTrafficRateFragment != null){
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
//                            try {
//                                mTrafficFragment.updateTrafficUse(txTotal, rxTotal,
//                                        mShadowsocksService.getTxTotalMonthly(), mShadowsocksService.getRxTotalMonthly());
//                            } catch (RemoteException e) {
//                                ShadowsocksApplication.handleException(e);
//                            }
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
        }
    }

    private ServiceConnection createShadowsocksServiceConnection(){
        return new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mShadowsocksService = IShadowsocksService.Stub.asInterface(service);
                registerShadowsocksCallback();
                checkConnectivity();
//                try {
//                    updateConnectionState(mShadowsocksService.getState());
//                    if(mTrafficFragment != null){
//                        mTrafficFragment.updateTrafficUse(0, 0,
//                                mShadowsocksService.getTxTotalMonthly(), mShadowsocksService.getRxTotalMonthly());
//                    }
//                } catch (RemoteException e) {
//                    ShadowsocksApplication.handleException(e);
//                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                unregisterShadowsocksCallback();
                mShadowsocksService = null;
            }
        };
    }

    private void checkConnectivity() {
        try {
            if(mShadowsocksService != null) {
                updateConnectionState(mShadowsocksService.getState());
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
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

//        if (id == R.id.nav_camera) {
//            // Handle the camera action
//        } else if (id == R.id.nav_gallery) {
//
//        } else if (id == R.id.nav_slideshow) {
//
//        } else if (id == R.id.nav_manage) {
//
//        } else if (id == R.id.nav_share) {
//
//        } else if (id == R.id.nav_send) {
//
//        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
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
        if (mShadowsocksServiceConnection != null) {
            unbindService(mShadowsocksServiceConnection);
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

}
