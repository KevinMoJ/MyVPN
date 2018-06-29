package com.androapplite.shadowsocks.beforeConnnectTest;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Created by huangjian on 2018/6/28.
 */

public class Socks4Session extends Session {
    private static final byte VN = 0X00;
    private static final byte REP_SERVER_SUCCESS = 0X5A;
    private static final byte REP_SERVER_FAILURE = 0X5B;


    private InetSocketAddress mRemoteAddress;

    Socks4Session(ShadowSocksProxyRunnable proxyServer, SelectionKey key) throws Exception {
        super(proxyServer, key);
    }

    @Override
    public void handleRead(SelectionKey key) throws Exception {
        SocketChannel channel = (SocketChannel) key.channel();
        int size = channel.read((ByteBuffer) mProxyServer.buffer().clear());
        if (size > 0) {
            if (channel.equals(mInnerChannel)) {
                if (mRemoteAddress == null) {
                    mRemoteAddress = extractRemoteAddress();
                    createOuterSocketChannel();
                } else {
                    encryptBuffer();
                    writeBuffer(mOuterChannel);
                }
            } else if (channel.equals(mOuterChannel)) {
                decryptBuffer();
                writeBuffer(mInnerChannel);
            }
        } else {
            channel.close();
            key.cancel();
        }
    }

    private InetSocketAddress extractRemoteAddress() throws Exception{
        ByteBuffer buffer = mProxyServer.buffer();
        buffer.position(2);
        int port = buffer.getShort() & 0xffff;
        byte[] ip = new byte[4];
        buffer.get(ip);
        return new InetSocketAddress(InetAddress.getByAddress(ip), port);
    }

    @Override
    public void handleConnect(SelectionKey key) throws Exception {
        SocketChannel channel = (SocketChannel) key.channel();
        if (channel.equals(mOuterChannel)) {
            if (channel.finishConnect()) {
                if (channel.isConnected()) {
                    prepareAddressToBuffer(mRemoteAddress);
                    encryptBuffer();
                    writeBuffer(channel);

                    prepareRemoteStatusToBuffer(REP_SERVER_SUCCESS);
                    writeBuffer(mInnerChannel);
                    key.interestOps(SelectionKey.OP_READ);
                } else {
                    channel.close();
                    key.cancel();

                    prepareRemoteStatusToBuffer(REP_SERVER_FAILURE);
                    writeBuffer(mInnerChannel);
                    mInnerChannel.close();
                }
            }
        }
    }

    private void prepareRemoteStatusToBuffer(byte reply) {
        ((ByteBuffer)mProxyServer.buffer().clear())
                .put(VN)
                .put(reply)
                .putShort((short) (mRemoteAddress.getPort() & 0xffff))
                .put(mRemoteAddress.getAddress().getAddress());
    }
}
