package com.androapplite.shadowsocks.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateUtils;
import android.util.Log;

import com.androapplite.vpn3.R;
import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.PromotionTracking;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.activity.SplashActivity;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.bestgo.adsplugin.ads.AdAppHelper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.vm.shadowsocks.core.LocalVpnService;
import com.vm.shadowsocks.core.TcpTrafficMonitor;
import com.vm.shadowsocks.core.VpnNotification;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static android.content.Intent.ACTION_TIME_TICK;

public class VpnManageService extends Service implements Runnable,
        LocalVpnService.onStatusChangedListener, Handler.Callback, OnCompleteListener<Void> {
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
    private static final int NOTIFICATION_ID_GRAP_SPEED = 10;
    private static final int MSG_CANCEL_NOTIFICATION = 3;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private long mRemoteFetchStartTime;
    private boolean mIsRemoteFetchSuccess;


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
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
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
                if (!mIsRemoteFetchSuccess) {
                    fetchRemoteConfig();
                }
            } else {
                unregisterScreenActionReceiver();
                registerTimeTickReceiver();
                if (mConnectStartTime > 0) {
                    long usetime = (System.currentTimeMillis() - mConnectStartTime) / 1000;
                    Firebase.getInstance(this).logEvent("VPN计时", "使用", usetime);
                } else {
                    Firebase.getInstance(this).logEvent("VPN计时", "使用", 0);
                }
                mIsRemoteFetchSuccess = false;
            }
        }
    }

    @Override
    public void onLogReceived(String logString) {

    }

    @Override
    public void onTrafficUpdated(@Nullable TcpTrafficMonitor tcpTrafficMonitor) {
        if (tcpTrafficMonitor != null) {
            PromotionTracking.getInstance(this).reportUsageByte(tcpTrafficMonitor);
        }
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
        PromotionTracking promotionTracking = PromotionTracking.getInstance(this);
        switch (msg.what) {
            case MSG_1_MINUTE:
                promotionTracking.reportUninstallDayCount();
                promotionTracking.reportAppInstall();
                promotionTracking.reportPhoneModelAndAndroidOS();
                grabSppedCheck();
                break;
            case MSG_1_HOUR:
                promotionTracking.reportUninstallDayCount();
                promotionTracking.reportAppInstall();
                promotionTracking.reportPhoneModelAndAndroidOS();
                grabSppedCheck();
                mServiceHandler.sendEmptyMessageDelayed(MSG_1_HOUR, TimeUnit.HOURS.toMillis(1));
                break;
            case MSG_CANCEL_NOTIFICATION:
                NotificationManagerCompat.from(getApplicationContext()).cancel(NOTIFICATION_ID_GRAP_SPEED);
                showGrabSpeedNotification(false);
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

    private void grabSppedCheck() {
        SharedPreferences sp = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
        long lastFire = sp.getLong(SharedPreferenceKey.GRAB_SPEED_TIME, 0);
        if (!DateUtils.isToday(lastFire)) {
            String currentHourString = AdAppHelper.getInstance(this).getCustomCtrlValue("grab_speed", "-1");
            try {
                int currentHour = Integer.valueOf(currentHourString);
                if (currentHour > -1 && currentHour < 24) {
                    Calendar calendar = Calendar.getInstance();
                    if (calendar.get(Calendar.HOUR_OF_DAY) == currentHour) {
                        showGrabSpeedNotification(true);
                        sp.edit().putLong(SharedPreferenceKey.GRAB_SPEED_TIME, System.currentTimeMillis()).apply();
                        Firebase.getInstance(this).logEvent("抢网速", String.valueOf(currentHour));
                    }
                }
            } catch (Exception e) {
                ShadowsocksApplication.handleException(e);
            }
        }
    }

    private void showGrabSpeedNotification(boolean showFullScreenIntent){
        try {
            Intent intent = new Intent(this, SplashActivity.class);
            intent.putExtra("source", "notificaiton_grab_speed");
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            Bitmap largeIcon = BitmapFactory.decodeResource(this.getResources(), R.drawable.notification_icon_grap_speed);
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
            notificationBuilder.setSmallIcon(R.drawable.notification_icon)
                    .setLargeIcon(largeIcon)
                    .setContentIntent(pendingIntent)
                    .setShowWhen(false)
                    .setContentTitle(getString(R.string.grab_speed_notification_title))
                    .setContentText(getString(R.string.grab_speed_notification_content));
            if (showFullScreenIntent) {
                notificationBuilder.setFullScreenIntent(pendingIntent, true);
            }
            NotificationManagerCompat.from(getApplicationContext()).notify(NOTIFICATION_ID_GRAP_SPEED, notificationBuilder.build());
        } catch (Exception e) {
            ShadowsocksApplication.handleException(e);
        }
        if (showFullScreenIntent) {
            mServiceHandler.sendEmptyMessageDelayed(MSG_CANCEL_NOTIFICATION, TimeUnit.SECONDS.toMillis(5));
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        fetchRemoteConfig();
        return super.onStartCommand(intent, flags, startId);
    }

    private void fetchRemoteConfig() {
        mFirebaseRemoteConfig.fetch(300).addOnCompleteListener(this);
        mRemoteFetchStartTime = System.currentTimeMillis();
    }

    @Override
    public void onComplete(@NonNull Task<Void> task) {
        mIsRemoteFetchSuccess = task.isSuccessful();
        if (mIsRemoteFetchSuccess) {
            mFirebaseRemoteConfig.activateFetched();
        }
        Firebase.getInstance(this).logEvent("获取远程配置", String.valueOf(mIsRemoteFetchSuccess),
                System.currentTimeMillis() - mRemoteFetchStartTime);
    }
}
