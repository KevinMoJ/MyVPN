package com.androapplite.shadowsocks.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.PromotionTracking;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.vm.shadowsocks.core.LocalVpnService;
import com.vm.shadowsocks.core.TcpTrafficMonitor;
import com.vm.shadowsocks.core.VpnNotification;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static android.content.Intent.ACTION_TIME_TICK;

public class VpnManageService extends Service implements Runnable,
        LocalVpnService.onStatusChangedListener, Handler.Callback{
    private ScheduledExecutorService mService;
    private ScheduledFuture mFuture;
    private volatile long mTimeStart;
    private SharedPreferences mSharedPreference;
    private Intent mUseTimeIntent;
    private long mConnectStartTime;
    private volatile Looper mServiceLooper;
    private volatile Handler mServiceHandler;
    private static final int MSG_1_MINUTE = 1;
    private static final int MSG_1_HOUR = 2;
    private ScreenActionReceiver mScreenActionReceiver;
    private TimeTickReceiver mTimeTickReceiver;

    public VpnManageService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mUseTimeIntent = new Intent(Action.ACTION_TIME_USE);
        mSharedPreference = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
        mService = Executors.newSingleThreadScheduledExecutor();
        if (LocalVpnService.IsRunning) {
            startScheduler(TimeUnit.SECONDS);
            registerScreenActionReceiver();
        } else {
            registerTimeTickReceiver();
        }
        LocalVpnService.addOnStatusChangedListener(this);
        HandlerThread thread = new HandlerThread("VpnManageService");
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new Handler(mServiceLooper, this);
        mServiceHandler.sendEmptyMessageDelayed(MSG_1_MINUTE, TimeUnit.MINUTES.toMillis(1));
        mServiceHandler.sendEmptyMessageDelayed(MSG_1_HOUR, TimeUnit.HOURS.toMillis(1));
    }

    private void registerScreenActionReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mScreenActionReceiver = new ScreenActionReceiver(this);
        registerReceiver(mScreenActionReceiver, intentFilter, null, mServiceHandler);
    }

    private void unregisterScreenActionReceiver(){
        if (mScreenActionReceiver != null) {
            unregisterReceiver(mScreenActionReceiver);
            mScreenActionReceiver = null;
        }
    }

    @Override
    public void onDestroy() {
        LocalVpnService.removeOnStatusChangedListener(this);
        unregisterScreenActionReceiver();
        unregisterTimeTickReceiver();
        if (mFuture != null) {
            mFuture.cancel(true);
            mFuture = null;
        }
        mService.shutdown();
        mServiceLooper.quit();
        super.onDestroy();
    }

    private static class ScreenActionReceiver extends BroadcastReceiver {
        private WeakReference<VpnManageService> mReference;

        ScreenActionReceiver(VpnManageService service) {
            mReference = new WeakReference<>(service);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            VpnManageService service = mReference.get();
            if (service != null) {
                String action = intent.getAction();
                switch (action) {
                    case Intent.ACTION_SCREEN_ON:
                        service.stopScheduler();
                        service.startScheduler(TimeUnit.SECONDS);
                        break;
                    case Intent.ACTION_SCREEN_OFF:
                        service.stopScheduler();
                        service.startScheduler(TimeUnit.MINUTES);
                        break;
                }
            }

        }
    }

    private void startScheduler(TimeUnit timeUnit) {
        if (mTimeStart == 0) {
            mTimeStart = System.currentTimeMillis();
        }
        try {
            mFuture = mService.scheduleAtFixedRate(this, 1, 1, timeUnit);
        } catch (Exception e) {
            ShadowsocksApplication.handleException(e);
        }
    }

    private void stopScheduler() {
        if (mFuture != null) {
            mFuture.cancel(true);
            mFuture = null;
        }
    }

    @Override
    public void onStatusChanged(String status, Boolean isRunning) {
        if (mSharedPreference != null) {
            stopScheduler();
            if (isRunning) {
                startScheduler(TimeUnit.SECONDS);
                registerScreenActionReceiver();
                unregisterTimeTickReceiver();
                long success = mSharedPreference.getLong(SharedPreferenceKey.SUCCESS_CONNECT_COUNT, 0);
                mSharedPreference.edit().putLong(SharedPreferenceKey.SUCCESS_CONNECT_COUNT, success + 1).apply();
                mConnectStartTime = System.currentTimeMillis();
            } else {
                unregisterScreenActionReceiver();
                registerTimeTickReceiver();
                if (mConnectStartTime > 0) {
                    long usetime = (System.currentTimeMillis() - mConnectStartTime) / 1000;
                    Firebase.getInstance(this).logEvent("VPN计时", "使用", usetime);
                } else {
                    Firebase.getInstance(this).logEvent("VPN计时", "使用", 0);
                }
            }
        }
    }

    @Override
    public void onLogReceived(String logString) {

    }

    @Override
    public void onTrafficUpdated(@Nullable TcpTrafficMonitor tcpTrafficMonitor) {
        PromotionTracking.getInstance(this).reportUsageByte(tcpTrafficMonitor);
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        long useTime = mSharedPreference.getLong(SharedPreferenceKey.USE_TIME, 0);
        useTime += (start - mTimeStart) / 1000;
        mSharedPreference.edit().putLong(SharedPreferenceKey.USE_TIME, useTime).apply();
        mTimeStart = start;
        LocalBroadcastManager.getInstance(this).sendBroadcast(mUseTimeIntent);
        Log.d("VpnManageService", "use time");
    }

    public static void start(Context context) {
        context.startService(new Intent(context, VpnManageService.class));
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_1_MINUTE:
                PromotionTracking.getInstance(this).reportUninstallDayCount();
                PromotionTracking.getInstance(this).reportAppInstall();
                PromotionTracking.getInstance(this).reportPhoneModelAndAndroidOS();
                break;
            case MSG_1_HOUR:
                PromotionTracking.getInstance(this).reportUninstallDayCount();
                PromotionTracking.getInstance(this).reportAppInstall();
                PromotionTracking.getInstance(this).reportPhoneModelAndAndroidOS();
                mServiceHandler.sendEmptyMessageDelayed(MSG_1_HOUR, TimeUnit.HOURS.toMillis(1));
                break;
        }
        return true;
    }

    private static class TimeTickReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_TIME_TICK:
                    if (!LocalVpnService.IsRunning) {
                        VpnNotification.showVpnStoppedNotificationGlobe(context, false);
                    }
                    break;
            }
        }
    }

    private void registerTimeTickReceiver() {
        mTimeTickReceiver = new TimeTickReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_TIME_TICK);
        registerReceiver(mTimeTickReceiver, intentFilter, null, mServiceHandler);
    }

    private void unregisterTimeTickReceiver() {
        if (mTimeTickReceiver != null) {
            unregisterReceiver(mTimeTickReceiver);
            mTimeTickReceiver = null;
        }
    }
}
