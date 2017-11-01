package com.vm.shadowsocks.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.vm.shadowsocks.tcpip.IPHeader;
import com.vm.shadowsocks.tcpip.TCPHeader;

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
    private volatile boolean mIsStoped;
    private ScheduledFuture mScheduledFuture;
    private ScheduledExecutorService mScheduledExecutorService;
    private IntentFilter mIntentFilter;
    private long mSentSpeedLast;
    private long mReceivedSpeedLast;
    private long mProxySentSpeedLast;
    private long mProxyReceivedSpeedLast;
    private long mPayloadSentSpeedLast;
    private long mPayloadReceivedSpeedLast;
    private long mProxyPayloadSentSpeedLast;
    private long mProxyPayloadReceivedSpeedLast;


    public TcpTrafficMonitor(ScheduledExecutorService service) {
        mScheduledExecutorService = service;
    }

    private void stop() {
        mIsStoped = true;
        if (mScheduledFuture != null) {
            mScheduledFuture.cancel(true);
            mScheduledFuture = null;
        }
    }

    private void start() {
        mIsStoped = false;
        if (mScheduledFuture == null) {
            mScheduledFuture = mScheduledExecutorService.scheduleAtFixedRate(this, 1, 1, TimeUnit.SECONDS);
        }
    }

    @Override
    public void run() {
        if (!mIsStoped) {
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
                LocalVpnService.Instance.updateTraffic();
                if (ProxyConfig.IS_DEBUG) {
                    System.out.println("speed updateTraffic");
                }
            }
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
            context.registerReceiver(this, mIntentFilter);
            start();
        }
    }

    public void unscheduleAndUnregisterReceiver(Context context) {
        if (mIntentFilter != null) {
            stop();
            context.unregisterReceiver(this);
            mIntentFilter = null;
        }
    }
}
