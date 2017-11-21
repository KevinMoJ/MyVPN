package com.androapplite.shadowsocks.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.net.VpnService;
import android.os.SystemClock;
import android.util.Log;

import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.model.VpnState;
import com.vm.shadowsocks.core.LocalVpnService;
import com.vm.shadowsocks.core.ProxyConfig;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Random;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class InterruptVpnIntentService extends IntentService {
    private static final String TAG = "InterruptVpnSrv";

    public InterruptVpnIntentService() {
        super("InterruptVpnIntentService");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            for (int i = 0; i < 60; i++) {
                NetworkInterface tun = getTun();
                Log.d(TAG, "has tun: " + (tun != null));
                if (tun != null) {
                    boolean self = isSelf(tun);
                    Log.d(TAG, "tun is self: " + self);
                    if (!self) {
                        int random = (int)Math.random() * 10000;
                        SystemClock.sleep(random);
                        VpnService.prepare(this);
//                        LocalVpnService.IsRunning = true;
//                        startService(new Intent(this, LocalVpnService.class));
                    }
                    break;
                }
                //一直没有tun表示断开
                SystemClock.sleep(1000);
            }

        }
    }


    private NetworkInterface getTun() {
        NetworkInterface tun = null;
        try {
            Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
            while (enumeration.hasMoreElements()) {
                NetworkInterface networkInterface = enumeration.nextElement();
                if ("tun0".equals(networkInterface.getDisplayName())) {
                    tun = networkInterface;
                    break;
                }
            }
        } catch (Exception e) {
            ShadowsocksApplication.handleException(e);
        }
        return tun;
    }

    private  boolean isSelf(NetworkInterface tun) {
        boolean r = false;
        ProxyConfig.IPAddress ipAddress = ProxyConfig.Instance.getDefaultLocalIP();
        String localIp = ipAddress.Address;
        Enumeration<InetAddress> enumeration = tun.getInetAddresses();
        while (enumeration.hasMoreElements()) {
            InetAddress inetAddress = enumeration.nextElement();
            if (localIp.equals(inetAddress.getHostName())) {
                r = true;
                break;
            }
        }
        return r;
    }

}
