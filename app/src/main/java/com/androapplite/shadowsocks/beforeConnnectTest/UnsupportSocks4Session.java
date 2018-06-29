package com.androapplite.shadowsocks.beforeConnnectTest;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Created by huangjian on 2018/6/28.
 */

public class UnsupportSocks4Session extends Session {
    private static final byte VER = 0X00;
    private static final byte REP_SERVER_FAILURE = 0X5B;
    UnsupportSocks4Session(ShadowSocksProxyRunnable proxyServer, SelectionKey key) throws Exception {
        super(proxyServer, key);
    }

    @Override
    public void handleRead(SelectionKey key) throws Exception {
        SocketChannel channel = (SocketChannel) key.channel();
        if (channel.equals(mInnerChannel)) {
            ((ByteBuffer) mProxyServer.buffer().clear()).put(VER).put(REP_SERVER_FAILURE);
            writeBuffer(channel);
            channel.close();
            key.cancel();
        }
    }

    @Override
    public void handleConnect(SelectionKey key) throws Exception {

    }
}
