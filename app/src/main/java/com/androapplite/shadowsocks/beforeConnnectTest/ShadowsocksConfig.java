package com.androapplite.shadowsocks.beforeConnnectTest;

import com.vm.shadowsocks.tunnel.shadowsocks.CryptFactory;
import com.vm.shadowsocks.tunnel.shadowsocks.ICrypt;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * Created by huangjian on 2018/6/27.
 */
public class ShadowsocksConfig {
    InetSocketAddress mAddress;
//    ICrypt mCrypt;
    private String mServer;
    private int mPort;
    private String mMethod;
    private String mPassword;

    public ShadowsocksConfig(String server, int port, String method, String password) {
        mServer = server;
        mPort = port;
        mMethod = method;
        mPassword = password;
    }

    public ShadowsocksConfig(String server, int port) {
        this(server, port, defaultMethod(), defaultPassword());
    }

    public ShadowsocksConfig(byte[] ip, int port) {
        try {
            mAddress = new InetSocketAddress(InetAddress.getByAddress(ip), port);
            mMethod = defaultMethod();
            mPassword = defaultPassword();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private static String defaultMethod() {
        return new StringBuilder("aes").append("-").append(256).append("-cfb").toString();
    }

    private static String defaultPassword() {
        return new StringBuilder("vpn").append("nest").append("!@#").append("123d").toString();
    }

    public InetSocketAddress address() {
        if (mAddress == null) {
            mAddress = new InetSocketAddress(mServer, mPort);
        }
        return mAddress;
    }

    public ICrypt newCrypt() {
        return CryptFactory.get(mMethod, mPassword);
    }
}
