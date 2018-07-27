package com.androapplite.shadowsocks.beforeconnnecttest;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Created by huangjian on 2018/6/27.
 */

public class Socks5Session extends Session {
    private static final byte VER = 0X05;
    private static final byte NO_AUTHENTICATION_REQUIRED = 0x00;
    private static final int ATYPE_POSITION = 3;
    private static final byte TYPE_IPV4 = 0x01;
    private static final byte TYPE_HOST = 0X03;
    private static final byte TYPE_IPV6 = 0X04;
    private static final byte REP_SUCCESS = 0X00;
    private static final byte REP_SERVER_FAILURE = 0X01;
    private static final byte RESERVE = 0X00;


    private static final int STEP_1_AUTHENTICATION = 1;
    private static final int STEP_2_REMOTE_ADDRESS = 2;
    private static final int STEP_3_TRANSFER_DATA = 3;
    private static final int STEP_ERROR = -1;

    @IntDef({STEP_1_AUTHENTICATION, STEP_2_REMOTE_ADDRESS,STEP_3_TRANSFER_DATA, STEP_ERROR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Step {}

    @Step private int mNextStep;
    private InetSocketAddress mRemoteAddress;


    Socks5Session(ShadowSocksProxyRunnable proxyServer, SelectionKey key) throws Exception {
        super(proxyServer, key);
        mNextStep = STEP_1_AUTHENTICATION;
    }

    @Override
    public void handleRead(SelectionKey key) throws Exception {
        SocketChannel channel = (SocketChannel) key.channel();
        int size = -1;
        if (channel.isConnected()) {
            size = channel.read((ByteBuffer) mProxyServer.buffer().clear());
        }
        if (size > 0) {
            if (channel.equals(mInnerChannel)) {
                switch (mNextStep) {
                    case STEP_1_AUTHENTICATION:
                        ((ByteBuffer) mProxyServer.buffer().clear())
                                .put(VER)
                                .put(NO_AUTHENTICATION_REQUIRED);
                        writeBuffer(mInnerChannel);
                        mNextStep = STEP_2_REMOTE_ADDRESS;
                        break;
                    case STEP_2_REMOTE_ADDRESS:
                        mRemoteAddress = extractRemoteAddress();
                        createOuterSocketChannel();
                        break;
                    case STEP_3_TRANSFER_DATA:
                        encryptBuffer();
                        writeBuffer(mOuterChannel);
                        break;
                    case STEP_ERROR:
                        channel.close();
                        key.cancel();
                        mOuterChannel.close();
                        break;
                }
            } else if (channel.equals(mOuterChannel)) {
                switch (mNextStep) {
                    case STEP_3_TRANSFER_DATA:
                        decryptBuffer();
                        writeBuffer(mInnerChannel);
                        break;
                    case STEP_ERROR:
                        channel.close();
                        key.cancel();
                        mInnerChannel.close();
                        break;
                }
            }
        } else {
            channel.close();
            key.cancel();
        }
    }

    private InetSocketAddress extractRemoteAddress() throws Exception{
        InetSocketAddress address = null;
        int port = 0;
        ByteBuffer buffer = mProxyServer.buffer();
        buffer.flip().position(ATYPE_POSITION);
        byte atype = buffer.get();
        switch (atype) {
            case TYPE_IPV4:
                byte[] ipv4Address = new byte[4];
                buffer.get(ipv4Address);
                InetAddress ipv4 = InetAddress.getByAddress(ipv4Address);
                port = buffer.getShort() & 0xffff;
                address = new InetSocketAddress(ipv4, port);
                break;
            case TYPE_HOST:
                byte hostLen = buffer.get();
                byte[] hostBytes = new byte[hostLen];
                buffer.get(hostBytes);
                String host = new String(hostBytes);
                port = buffer.getShort() & 0xffff;
                address = InetSocketAddress.createUnresolved(host, port);
                break;
            case TYPE_IPV6:
                byte[] ipv6Address = new byte[16];
                buffer.get(ipv6Address);
                InetAddress ipv6 = InetAddress.getByAddress(ipv6Address);
                port = buffer.getShort() & 0xffff;
                address = new InetSocketAddress(ipv6, port);
                break;
        }
        return address;
    }

    @Override
    public void handleConnect(SelectionKey key) throws Exception {
        SocketChannel channel = (SocketChannel) key.channel();
        if (channel.equals(mOuterChannel)) {
            if(channel.finishConnect()) {
                if (channel.isConnected()) {
                    prepareAddressToBuffer(mRemoteAddress);
                    encryptBuffer();
                    writeBuffer(mOuterChannel);

                    prepareRemoteStatusToBuffer(REP_SUCCESS);
                    writeBuffer(mInnerChannel);
                    key.interestOps(SelectionKey.OP_READ);
                    mNextStep = STEP_3_TRANSFER_DATA;
                } else {
                    prepareRemoteStatusToBuffer(REP_SERVER_FAILURE);
                    writeBuffer(mInnerChannel);
                    channel.close();
                    key.cancel();
                    mInnerChannel.close();
                }
            }
        }
    }

    private void prepareRemoteStatusToBuffer(byte reply) {
        ((ByteBuffer)mProxyServer.buffer().clear())
                .put(VER)
                .put(reply)
                .put(RESERVE)
                .put(TYPE_IPV4)
                .put(mOuterChannel.socket().getLocalAddress().getAddress())
                .putShort((short) (mOuterChannel.socket().getLocalPort() & 0xFFFF));
    }

}
