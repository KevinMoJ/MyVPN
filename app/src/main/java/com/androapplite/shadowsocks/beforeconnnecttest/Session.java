package com.androapplite.shadowsocks.beforeconnnecttest;

import android.os.Build;

import com.vm.shadowsocks.tunnel.shadowsocks.ICrypt;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;


/**
 * Created by huangjian on 2018/6/27.
 */

public abstract  class Session {
    private static final byte TYPE_IPV4 = 0x01;
    private static final byte TYPE_HOST = 0X03;
    private static final byte TYPE_IPV6 = 0X04;
    protected ShadowSocksProxyRunnable mProxyServer;
    protected SocketChannel mInnerChannel;
    protected SocketChannel mOuterChannel;
    protected ShadowsocksConfig mConfig;
    private ICrypt mCrypt;


    Session(ShadowSocksProxyRunnable proxyServer, SelectionKey key) throws Exception {
        mProxyServer = proxyServer;
        mConfig = (ShadowsocksConfig) key.attachment();
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        mInnerChannel = serverSocketChannel.accept();
        mInnerChannel.configureBlocking(false);
        mInnerChannel.register(mProxyServer.selector(), SelectionKey.OP_READ, this);
    }

    public static Session newSession(ShadowSocksProxyRunnable proxyServer, SelectionKey key) throws Exception{
        if (proxyServer.isConnectionTest(key)) {
            return new ConnectionTestSession(proxyServer, key);
        } else {
            ShadowsocksConfig config = (ShadowsocksConfig) key.attachment();
            if (config instanceof ShadowsocksConfig) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                    return new Socks5Session(proxyServer, key);
                } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    return new UnsupportSocks4Session(proxyServer, key);
                } else {
                    return new Socks4Session(proxyServer, key);
                }
            }
        }
        return null;
    }

    public abstract void handleRead(SelectionKey key) throws Exception;
    protected int writeBuffer(SocketChannel channel) throws Exception{
        ByteBuffer buffer = mProxyServer.buffer();
        if (buffer.position() > 0) {
            buffer.flip();
        }

        try {
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
        } catch (Exception e) {
            channel.close();
            throw e;
        }
        return buffer.limit();
    }

    protected void createOuterSocketChannel() throws Exception{
        mOuterChannel = SocketChannel.open();
        mOuterChannel.configureBlocking(false);
        mOuterChannel.connect(mConfig.address());
        mOuterChannel.register(mProxyServer.selector(), SelectionKey.OP_CONNECT, this);
    }

    public abstract void handleConnect(SelectionKey key) throws Exception;
    protected void encryptBuffer() {
        byte[] raw = mProxyServer.copyFromBuffer();
        byte[] encrypt = crypt().encrypt(raw);
        mProxyServer.copyToBuffer(encrypt);
    }

    protected void decryptBuffer() throws Exception{
        byte[] raw = mProxyServer.copyFromBuffer();
        byte[] decrypt = crypt().decrypt(raw);
        mProxyServer.copyToBuffer(decrypt);
    }

    private ICrypt crypt() {
        if (mCrypt == null) {
            mCrypt = mConfig.newCrypt();
        }
        return mCrypt;
    }

    protected void prepareAddressToBuffer(InetSocketAddress remoteAddress) {
        ByteBuffer buffer = mProxyServer.buffer();
        buffer.clear();
        if (remoteAddress.isUnresolved()) {
            String host = remoteAddress.getHostName();
            buffer.put(TYPE_HOST).put((byte) host.length()).put(host.getBytes());
        } else if (remoteAddress.getAddress() instanceof Inet4Address) {
            buffer.put(TYPE_IPV4).put(remoteAddress.getAddress().getAddress());
        } else if (remoteAddress.getAddress() instanceof Inet6Address) {
            buffer.put(TYPE_IPV6).put(remoteAddress.getAddress().getAddress());
        }
        buffer.putShort((short)(remoteAddress.getPort() & 0xffff));
    }
}
