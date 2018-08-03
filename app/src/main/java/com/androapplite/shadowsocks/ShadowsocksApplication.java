package com.androapplite.shadowsocks;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDex;
import android.util.Log;

import com.androapplite.shadowsocks.activity.MainActivity;
import com.androapplite.shadowsocks.model.VpnState;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.androapplite.shadowsocks.service.IpCountryIntentService;
import com.androapplite.shadowsocks.utils.RuntimeSettings;
import com.androapplite.vpn3.BuildConfig;
import com.vm.shadowsocks.core.VpnNotification;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Locale;


/**
 * Created by jim on 16/5/2.
 */
public class ShadowsocksApplication extends Application implements Application.ActivityLifecycleCallbacks {
    private int mOpenActivities;
    private boolean mIsFirstOpen;

    private static Context gContext;

    @Override
    public void onCreate() {
        super.onCreate();
        gContext = getApplicationContext();
        checkVpnState();
        IpCountryIntentService.startService(this);
        VpnNotification.showVpnStoppedNotificationGlobe(this, false);
        mIsFirstOpen = true;
        registerActivityLifecycleCallbacks(this);
    }

    private void checkVpnState() {
        int stateValue = RuntimeSettings.getVPNState();
        Log.d("ShadowsocksApplication", "vpn state: " + stateValue);
        if (stateValue >= 0 && stateValue < VpnState.values().length) {
            VpnState state = VpnState.values()[stateValue];
        }
    }

    public static final void debug(@NonNull String tag, @NonNull String msg) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, msg);
        }
    }

    public static final void handleException(@NonNull Throwable throwable) {
        if (BuildConfig.DEBUG) {
            throwable.printStackTrace();
        }
    }


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }


    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {
        mOpenActivities++;
        if (activity instanceof MainActivity) {
            if (mIsFirstOpen) {
                mIsFirstOpen = false;
//                PromotionTracking.getInstance(this).reportOpenMainPageCount();
                reportTcpRecord();
            }
//            PromotionTracking.getInstance(this).reportContinuousDayCount();
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {
        mOpenActivities--;
        if (mOpenActivities == 0) {
            mIsFirstOpen = true;
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

    public int getOpenActivityNumber() {
        return mOpenActivities;
    }

    public static Context getGlobalContext() {
        return gContext;
    }

    private void reportTcpRecord() {
        SharedPreferences sp = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
        boolean shouldReportTcpRecord = sp.getBoolean(SharedPreferenceKey.REPORT_TCP_RECORD, true);
        if (shouldReportTcpRecord) {
            int pid = android.os.Process.myPid();
            String tcpFilename = String.format(Locale.ENGLISH, "/proc/%d/net/tcp", pid);
            File tcpFile = new File(tcpFilename);

            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(tcpFile));
                String line = null;
                int lineNumber = 0;
                while (lineNumber++ < 2) {
                    try {
                        line = bufferedReader.readLine();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                bufferedReader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
