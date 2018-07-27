package com.androapplite.shadowsocks.beforeconnnecttest;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Created by huangjian on 2018/6/28.
 */

public class ConnectionTestSession extends Session {
    private static final byte[] REQ_CT = "CT".getBytes();
    private static final byte[] RES_OK = "OK".getBytes();
    private static final byte[] RES_BAD = "BAD".getBytes();
    private static final byte[] CONNECT_TEST_DOMAIN = "ct.vpnnest.com".getBytes();
    private static final short CONNECT_TEST_PORT = 80;
    private static final byte TYPE_HOST = 0X03;



    ConnectionTestSession(ShadowSocksProxyRunnable proxyServer, SelectionKey key) throws Exception {
        super(proxyServer, key);
    }

    @Override
    public void handleRead(SelectionKey key) throws Exception {
        SocketChannel channel = (SocketChannel) key.channel();
        int size = channel.read((ByteBuffer) mProxyServer.buffer().clear());
        if (size > 0) {
            if (channel.equals(mInnerChannel)) {
                if (mConfig == null) {
                    ByteBuffer buffer = mProxyServer.buffer();
                    buffer.flip();
                    byte type = buffer.get();
                    byte[] ip = new byte[4];
                    buffer.get(ip);
                    int port = buffer.getShort() & 0xffff;
                    mConfig = new ShadowsocksConfig(ip, port);
                    createOuterSocketChannel();
                }
            } else {
                decryptBuffer();
                writeBuffer(mInnerChannel);
            }
        } else {
            channel.close();
            key.cancel();
        }

    }

    @Override
    public void handleConnect(SelectionKey key) throws Exception {
        SocketChannel channel = (SocketChannel) key.channel();
        if (channel.equals(mOuterChannel)) {
            if (channel.finishConnect()) {
                ByteBuffer buffer = mProxyServer.buffer();
                ((ByteBuffer)buffer.clear())
                        .put(TYPE_HOST)
                        .put((byte) CONNECT_TEST_DOMAIN.length)
                        .put(CONNECT_TEST_DOMAIN)
                        .putShort((short) (CONNECT_TEST_PORT & 0xffff));
                encryptBuffer();
                writeBuffer(channel);

                mProxyServer.copyToBuffer(REQ_CT);
                encryptBuffer();
                writeBuffer(channel);
                key.interestOps(SelectionKey.OP_READ);
            } else {
                mProxyServer.copyToBuffer(RES_BAD);
                writeBuffer(mInnerChannel);
            }
        }
    }

    public static boolean isResponseOk(ByteBuffer buffer) {
        if (buffer.position() > 0) {
            buffer.flip();
        }
        if (buffer.limit() == 0) {
            return false;
        }
        byte[] raw = new byte[buffer.limit()];
        buffer.get(raw);
        return raw.length == RES_OK.length && raw[0] == RES_OK[0] && raw[1] == RES_OK[1];
    }
}
