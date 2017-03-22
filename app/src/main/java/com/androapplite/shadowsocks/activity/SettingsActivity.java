package com.androapplite.shadowsocks.activity;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.R;
import com.androapplite.shadowsocks.ShadowsockServiceHelper;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.fragment.SettingsFragment;

import yyf.shadowsocks.IShadowsocksService;

public class SettingsActivity extends AppCompatActivity implements SettingsFragment.OnSettingsActionListener {

    private IShadowsocksService mShadowsocksService;
    private ServiceConnection mShadowsocksServiceConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        mShadowsocksServiceConnection = createShadowsocksServiceConnection();
        ShadowsockServiceHelper.bindService(this, mShadowsocksServiceConnection);
    }

    private ServiceConnection createShadowsocksServiceConnection(){
        return new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mShadowsocksService = IShadowsocksService.Stub.asInterface(service);

            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mShadowsocksService = null;
            }
        };
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void about() {
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
        Firebase.getInstance(this).logEvent( "设置", "关于");
    }

    @Override
    public void enableNotification(boolean enable) {
        if(mShadowsocksService != null){
            try {
                mShadowsocksService.enableNotification(enable);
            } catch (RemoteException e) {
                ShadowsocksApplication.handleException(e);
            }
        }
        Firebase.getInstance(this).logEvent( "设置", "通知", String.valueOf(enable));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mShadowsocksServiceConnection != null) {
            unbindService(mShadowsocksServiceConnection);
        }
    }

    @Override
    public void autoConect(boolean enable) {
        Firebase.getInstance(this).logEvent( "设置", "自动连接", String.valueOf(enable));
    }
}
