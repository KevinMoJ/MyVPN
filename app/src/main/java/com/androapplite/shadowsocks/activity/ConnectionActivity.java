
package com.androapplite.shadowsocks.activity;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.VpnService;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.androapplite.shadowsocks.R;
import com.androapplite.shadowsocks.ShadowsockServiceHelper;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.fragment.ConnectionFragment;
import com.androapplite.shadowsocks.fragment.RateUsFragment;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;

import yyf.shadowsocks.Config;
import yyf.shadowsocks.IShadowsocksService;
import yyf.shadowsocks.IShadowsocksServiceCallback;
import yyf.shadowsocks.utils.Constants;

public class ConnectionActivity extends BaseShadowsocksActivity implements
        RateUsFragment.OnFragmentInteractionListener, ConnectionFragment.OnFragmentInteractionListener,
        IShadowsocksServiceCallback{
    private RateUsFragment mRateUsFragment;
    private static int REQUEST_CONNECT = 1;
    private IShadowsocksService mShadowsocksService;
    private ServiceConnection mShadowsocksServiceConnection;
    private ConnectionFragment mConnectionFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);
        mConnectionFragment = findConectionFragment();
        initToobar();
        showRateUsFragmentWhenFirstOpen();
        mShadowsocksServiceConnection = createShadowsocksServiceConnection();
        ShadowsockServiceHelper.bindService(this, mShadowsocksServiceConnection);

    }

    private void showRateUsFragmentWhenFirstOpen() {
        if(!DefaultSharedPrefeencesUtil.isRateUsFragmentShown(this)) {
            mRateUsFragment = RateUsFragment.newInstance();
            initRateUsFragment();
            DefaultSharedPrefeencesUtil.markRateUsFragmentAsShowed(this);
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.share_icon:
            case R.id.share:
                share();
                break;
            case R.id.rate_us:
                rateUs();
                break;
            case R.id.contact_us:
                contactUs();
                break;
            case R.id.about:
                about();
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
            e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    @Override
    public void onCloseRateUs() {
        if(mRateUsFragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().remove(mRateUsFragment).commit();
            mRateUsFragment = null;
        }
    }

    @Override
    public void onRateUs() {
        rateUs();
    }

    private void prepareStartService(){
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            startActivityForResult(intent, REQUEST_CONNECT);
            Log.v("ss-vpn", "startActivityForResult");
        } else {
            onActivityResult(REQUEST_CONNECT, Activity.RESULT_OK, null);
            Log.v("ss-vpn", "onActivityResult");
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
                    Log.v("ss-vpn", "bgService.StartVpn");
                }catch(RemoteException e){
                    e.getStackTrace();
                }
            }else{
                Log.v("ss-vpn","bgServiceIsNull");
            }
        }
    }

    @Override
    public void onClickConnectionButton() {
        try {
            if (mShadowsocksService == null || mShadowsocksService.getState() == Constants.State.INIT.ordinal()
                    || mShadowsocksService.getState() == Constants.State.STOPPED.ordinal()) {
                prepareStartService();
            }else{
                mShadowsocksService.stop();
            }
        }catch (RemoteException e){
            e.printStackTrace();
        }

    }

    @Override
    public void stateChanged(int state, String msg) throws RemoteException {
        if(state == Constants.State.CONNECTED.ordinal()){
            mConnectionFragment.connected();
        }else if(state == Constants.State.STOPPED.ordinal()){
            mConnectionFragment.stop();
        }
    }

    @Override
    public void trafficUpdated(long txRate, long rxRate, long txTotal, long rxTotal) throws RemoteException {

    }

    @Override
    public IBinder asBinder() {
        return null;
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerShadowsocksCallback();
    }

    private void registerShadowsocksCallback() {
        if(mShadowsocksService != null){
            try {
                mShadowsocksService.registerCallback(this);
            } catch (RemoteException e) {
                e.printStackTrace();
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
                mShadowsocksService.unregisterCallback(this);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mShadowsocksServiceConnection != null){
            unbindService(mShadowsocksServiceConnection);
        }
    }
}
