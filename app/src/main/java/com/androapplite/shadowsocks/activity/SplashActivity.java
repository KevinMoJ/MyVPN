package com.androapplite.shadowsocks.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;

import com.androapplite.shadowsocks.R;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.androapplite.shadowsocks.broadcast.Action;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import yyf.shadowsocks.jni.*;
import yyf.shadowsocks.jni.System;
import yyf.shadowsocks.utils.Console;
import yyf.shadowsocks.utils.Constants;

public class SplashActivity extends BaseShadowsocksActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initBackgroundReceiver();
        initBackgroundReceiverIntentFilter();

/*        getWindow().getDecorView().postDelayed(new Runnable() {
            @Override
            public void run() {
                Activity activity = SplashActivity.this;
                startActivity(new Intent(activity,
                        DefaultSharedPrefeencesUtil.isNewUser(activity) ? NewUserGuideActivity.class : ConnectionActivity.class
                ));

            }
        }, TimeUnit.SECONDS.toMillis(2));*/

        new Thread() {
            @Override
            public void run() {
                long t2 = java.lang.System.currentTimeMillis();
                checkAndCopyAsset(yyf.shadowsocks.jni.System.getABI());
                long t3 = java.lang.System.currentTimeMillis();
                Log.d("ss-vpn", String.valueOf(t3-t2));
            }
        }.start();
    }

    private void copyAssets(String path) {
        AssetManager assetManager = getAssets();
        String[] files = null;
        try {
            files = assetManager.list(path);
        } catch(IOException e) {
            Log.e("ss-error", e.getMessage());
        }
        if (files != null) {
            for (int i = 0 ; i<files.length ; i++) {
                InputStream in = null;
                OutputStream out = null;
                try {
                    if (path.length() > 0) {
                        in = assetManager.open(path + "/" + files[i]);
                    } else {
                        in = assetManager.open(files[i]);
                    }
                    out = new FileOutputStream(Constants.Path.BASE + files[i]);
                    copyFile(in, out);
                    in.close();
                    in = null;
                    out.flush();
                    out.close();
                    out = null;
                } catch(Exception e){
                    Log.e("ss-srror", e.getMessage());
                }
            }
        }
    }
    private void copyFile(InputStream in,OutputStream out) throws IOException{
        byte buffer[] = new byte[1024];
        int read = in.read(buffer);
        while(read != -1){
            out.write(buffer, 0, read);
            read = in.read(buffer);
        }
    }

    private void initBackgroundReceiver(){
        mBackgroundReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if(action.equals(Action.CONNECTION_ACTIVITY_SHOW) || action.equals(Action.NEW_USER_GUIDE_ACTIVITY_SHOW)){
                    finish();
                }
            }
        };
    }

    private void initBackgroundReceiverIntentFilter(){
        mBackgroundReceiverIntentFilter = new IntentFilter();
        mBackgroundReceiverIntentFilter.addAction(Action.CONNECTION_ACTIVITY_SHOW);
        mBackgroundReceiverIntentFilter.addAction(Action.NEW_USER_GUIDE_ACTIVITY_SHOW);
    }

    private void checkAndCopyAsset(String path){
        AssetManager assetManager = getAssets();
        String[] filenames = null;
        try {
            filenames = assetManager.list(path);
        } catch(IOException e) {
            Log.e("ss-error", e.getMessage());
        }
        if (filenames != null) {
            for (String filename:filenames) {
                File outFile = new File(Constants.Path.BASE, filename);
                if(!outFile.exists()) {
                    InputStream in = null;
                    try {
                        if (!path.isEmpty()) {
                            in = assetManager.open(path + "/" + filename);
                        } else {
                            in = assetManager.open(filename);
                        }

                        OutputStream out = new FileOutputStream(outFile);
                        copyFile(in, out);
                        in.close();
                        in = null;
                        out.flush();
                        out.close();
                        out = null;

                    } catch (Exception e) {
                        Log.e("ss-error", e.getMessage());
                    }
                }
                if (!(outFile.canRead() && outFile.canExecute())) {
                    Console.runCommand("chmod 755 " + outFile.getAbsolutePath());
                }
            }
        }
    }
}
