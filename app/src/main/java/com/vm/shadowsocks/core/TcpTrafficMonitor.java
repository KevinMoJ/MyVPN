package com.vm.shadowsocks.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.vm.shadowsocks.tcpip.IPHeader;
import com.vm.shadowsocks.tcpip.TCPHeader;
import com.vm.shadowsocks.tcpip.UDPHeader;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by huangjian on 2017/10/18.
 */

public class TcpTrafficMonitor extends BroadcastReceiver implements Runnable{
    public volatile long pSentByteCount;
    public volatile long pReceivedByteCount;
    public volatile long pProxySentByteCount;
    public volatile long pProxyReceivedByteCount;
    public volatile long pSentCount;
    public volatile long pReceivedCount;
    public volatile long pProxySentCount;
    public volatile long pProxyReceivedCount;
    public volatile long pSentSpeed;
    public volatile long pReceivedSpeed;
    public volatile long pProxySentSpeed;
    public volatile long pProxyReceivedSpeed;
    public volatile long pSentByteCountLast;
    public volatile long pReceivedByteCountLast;
    public volatile long pProxySentByteCountLast;
    public volatile long pProxyReceivedByteCountLast;
    public volatile long pProxyPayloadSentByteCount;
    public volatile long pProxyPayloadReceivedByteCount;
    public volatile long pPayloadSentByteCount;
    public volatile long pPayloadReceivedByteCount;
    public volatile long pPayloadSentSpeed;
    public volatile long pPayloadReceivedSpeed;
    public volatile long pProxyPayloadSentSpeed;
    public volatile long pProxyPayloadReceivedSpeed;
    public volatile long pPayloadSentByteCountLast;
    public volatile long pPayloadReceivedByteCountLast;
    public volatile long pProxyPayloadSentByteCountLast;
    public volatile long pProxyPayloadReceivedByteCountLast;
    private volatile boolean mIsStopped;
    private volatile ScheduledFuture mScheduledFuture;
    private ScheduledExecutorService mScheduledExecutorService;
    private volatile IntentFilter mIntentFilter;
    private long mSentSpeedLast;
    private long mReceivedSpeedLast;
    private long mProxySentSpeedLast;
    private long mProxyReceivedSpeedLast;
    private long mPayloadSentSpeedLast;
    private long mPayloadReceivedSpeedLast;
    private long mProxyPayloadSentSpeedLast;
    private long mProxyPayloadReceivedSpeedLast;
    public int pNetworkError;
    private int mNetworkErrorCount;


    public TcpTrafficMonitor(ScheduledExecutorService service) {
        mScheduledExecutorService = service;
        pNetworkError = -1;
        mNetworkErrorCount = 0;
    }

    private void stop() {
        mIsStopped = true;
        if (mScheduledFuture != null) {
            mScheduledFuture.cancel(true);
            mScheduledFuture = null;
        }
    }

    private void start() {
        mIsStopped = false;
        if (mScheduledFuture == null) {
            try {
                mScheduledFuture = mScheduledExecutorService.scheduleAtFixedRate(this, 1, 1, TimeUnit.SECONDS);
            } catch (Exception e) {
                ShadowsocksApplication.handleException(e);
            }
        }
    }

    @Override
    public void run() {
        if (!mIsStopped) {
            pSentSpeed = pSentByteCount - pSentByteCountLast;
            pSentByteCountLast = pSentByteCount;
            pReceivedSpeed = pReceivedByteCount - pReceivedByteCountLast;
            pReceivedByteCountLast = pReceivedByteCount;

            pProxySentSpeed = pProxySentByteCount - pProxySentByteCountLast;
            pProxySentByteCountLast = pProxySentByteCount;
            pProxyReceivedSpeed = pProxyReceivedByteCount - pProxyReceivedByteCountLast;
            pProxyReceivedByteCountLast = pProxyReceivedByteCount;

            pPayloadSentSpeed = pPayloadSentByteCount - pPayloadSentByteCountLast;
            pPayloadSentByteCountLast = pPayloadSentByteCount;
            pPayloadReceivedSpeed = pPayloadReceivedByteCount - pPayloadReceivedByteCountLast;
            pPayloadReceivedByteCountLast = pPayloadReceivedByteCount;

            pProxyPayloadSentSpeed = pProxyPayloadSentByteCount - pProxyPayloadSentByteCountLast;
            pProxyPayloadSentByteCountLast = pProxyPayloadSentByteCount;
            pProxyPayloadReceivedSpeed = pProxyPayloadReceivedByteCount - pProxyPayloadReceivedByteCountLast;
            pProxyPayloadReceivedByteCountLast = pProxyPayloadReceivedByteCount;

            if (ProxyConfig.IS_DEBUG) {
                System.out.printf("speed total: %d/%d, proxy: %d/%d; total payload: %d/%d, proxy payload: %d/%d\n",
                        pSentSpeed, pReceivedSpeed, pProxySentSpeed, pProxyReceivedSpeed,
                        pPayloadSentSpeed, pPayloadReceivedSpeed, pProxyPayloadSentSpeed, pProxyPayloadReceivedSpeed);
            }

            if (pSentSpeed != mSentSpeedLast || pReceivedSpeed != mReceivedSpeedLast ||
                    pProxySentSpeed != mProxySentSpeedLast || pProxyReceivedSpeed != mProxyReceivedSpeedLast ||
                    pPayloadSentSpeed != mPayloadSentSpeedLast || pPayloadReceivedSpeed != mPayloadReceivedSpeedLast ||
                    pProxyPayloadSentSpeed != mProxyPayloadSentSpeedLast || pProxyPayloadReceivedSpeed != mProxyPayloadReceivedSpeedLast) {
                mSentSpeedLast = pSentSpeed;
                mReceivedSpeedLast = pReceivedSpeed;
                mProxySentSpeedLast = pProxySentSpeed;
                mProxyReceivedSpeedLast = pProxyReceivedSpeed;
                mPayloadSentSpeedLast = pPayloadSentSpeed;
                mPayloadReceivedSpeedLast = pPayloadReceivedSpeed;
                mProxyPayloadSentSpeedLast = pProxyPayloadSentSpeed;
                mProxyPayloadReceivedSpeedLast = pProxyPayloadReceivedSpeed;
                if (pPayloadReceivedSpeed <= 0) {
                    if (checkActiveNetwork()){
                        if (pPayloadSentSpeed > 0) {
                            mNetworkErrorCount++;
                        }
                    } else {
                        pNetworkError = 0;
                        mNetworkErrorCount = 0;
                    }
                } else {
                    pNetworkError = -1;
                    mNetworkErrorCount = 0;
                }

                if (mNetworkErrorCount > 5) {
                    pNetworkError = 1;
                }

                LocalVpnService.Instance.updateTraffic();
                if (ProxyConfig.IS_DEBUG) {
                    System.out.println("speed updateTraffic");
                }
            }
        }
    }

    private boolean checkActiveNetwork() {
        ConnectivityManager connectivityManager = (ConnectivityManager) LocalVpnService.Instance.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetInfo != null && activeNetInfo.isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    public void updateTrafficSend(IPHeader ipHeader, TCPHeader tcpHeader, int size) {
        pSentByteCount += size;
        pSentCount++;
        int payload = ipHeader.getDataLength() - tcpHeader.getHeaderLength();
        pPayloadSentByteCount += payload;
        if (ProxyConfig.isFakeIP(ipHeader.getSourceIP())) {
            pProxySentByteCount += size;
            pProxySentCount++;
            pProxyPayloadSentByteCount += payload;
        }
    }

    public void updateTrafficReceive(IPHeader ipHeader, TCPHeader tcpHeader, int size) {
        pReceivedByteCount += size;
        pReceivedCount++;
        int payload = ipHeader.getDataLength() - tcpHeader.getHeaderLength();
        pPayloadReceivedByteCount += payload;
        if (ProxyConfig.isFakeIP(ipHeader.getSourceIP())) {
            pProxyReceivedByteCount += size;
            pProxyReceivedCount++;
            pProxyPayloadReceivedByteCount += payload;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        switch (action) {
            case Intent.ACTION_SCREEN_ON:
                start();
                break;
            case Intent.ACTION_SCREEN_OFF:
                stop();
                break;
        }
    }

    public void scheduleAndRegisterReceiver(Context context) {
        if (mIntentFilter == null) {
            mIntentFilter = new IntentFilter();
            mIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);
            mIntentFilter.addAction(Intent.ACTION_SCREEN_ON);
            try {
                context.registerReceiver(this, mIntentFilter);
            } catch (Exception e) {
                ShadowsocksApplication.handleException(e);
            }
            start();
        }
    }

    public void unscheduleAndUnregisterReceiver(Context context) {
        if (mIntentFilter != null) {
            mIntentFilter = null;
            stop();
            try {
                context.unregisterReceiver(this);
            } catch (Exception e) {
                ShadowsocksApplication.handleException(e);
            }
        }
    }

    public void updateTrafficSend(IPHeader ipHeader, UDPHeader udpHeader, int size) {
        pSentByteCount += size;
        pSentCount++;
        int payload = ipHeader.getDataLength() - 8;
        pPayloadSentByteCount += payload;
        if (ProxyConfig.isFakeIP(ipHeader.getSourceIP())) {
            pProxySentByteCount += size;
            pProxySentCount++;
            pProxyPayloadSentByteCount += payload;
        }
    }

    public void updateTrafficReceive(IPHeader ipHeader, UDPHeader udpHeader, int size) {
        pReceivedByteCount += size;
        pReceivedCount++;
        int payload = ipHeader.getDataLength() - 8;
        pPayloadReceivedByteCount += payload;
        if (ProxyConfig.isFakeIP(ipHeader.getSourceIP())) {
            pProxyReceivedByteCount += size;
            pProxyReceivedCount++;
            pProxyPayloadReceivedByteCount += payload;
        }
    }
}
