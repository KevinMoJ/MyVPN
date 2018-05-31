package com.vm.shadowsocks.core;

import android.util.SparseArray;

import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.vm.shadowsocks.tcpip.CommonMethods;
import com.vm.shadowsocks.tunnel.shadowsocks.CryptFactory;
import com.vm.shadowsocks.tunnel.shadowsocks.ICrypt;
import com.vm.shadowsocks.tunnel.shadowsocks.ShadowsocksConfig;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

public class UdpProxyServer implements Runnable {


    public boolean Stopped;
    private Thread mReceivedThread;
    public short Port;
    private Selector mSelector;
    private DatagramChannel mDatagramChannel;
    private SparseArray<DatagramChannel> mClientRemoteChannelMap;

    public UdpProxyServer(int port) throws IOException {
        mSelector = Selector.open();
        mDatagramChannel = DatagramChannel.open();
        mDatagramChannel.configureBlocking(false);
        mDatagramChannel.socket().bind(new InetSocketAddress(port));
        mDatagramChannel.register(mSelector, SelectionKey.OP_READ);
        Port = (short)mDatagramChannel.socket().getLocalPort();
        mClientRemoteChannelMap = new SparseArray<>();
    }

    public void start() {
        mReceivedThread = new Thread(this);
        mReceivedThread.setName("UdpProxyThread");
        mReceivedThread.start();
    }

    public void stop() {
        Stopped = true;
        if (mSelector != null) {
            try {
                mSelector.close();
                mSelector = null;
            } catch (Exception e) {
                ShadowsocksApplication.handleException(e);
            }

            if (mDatagramChannel != null) {
                try {
                    mDatagramChannel.close();
                    mDatagramChannel = null;
                } catch (Exception e) {
                    ShadowsocksApplication.handleException(e);
                }
            }
        }
        if (mClientRemoteChannelMap != null) {
            for(int i=0; i<mClientRemoteChannelMap.size(); i++) {
                DatagramChannel channel = mClientRemoteChannelMap.valueAt(i);
                try {
                    channel.close();
                } catch (IOException e) {
                    ShadowsocksApplication.handleException(e);
                }
            }
            mClientRemoteChannelMap.clear();
        }
        mReceivedThread.interrupt();
    }

//    @Override
//    public void run() {
//        ByteBuffer payload = ByteBuffer.allocate(2000);
//        while (!Stopped) {
//            try {
//                mSelector.select();
//                Iterator<SelectionKey> keyIterator = mSelector.selectedKeys().iterator();
//                while (keyIterator.hasNext()) {
//                    SelectionKey key = keyIterator.next();
//                    if (key.isValid()) {
//                        if (key.isReadable()) {
//                            DatagramChannel channel = (DatagramChannel) key.channel();
//                            channel.receive(payload);
//                        }
////                        key.cancel();
//                    }
//                    keyIterator.remove();
//
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }

    @Override
    public void run() {
        try{
            ByteBuffer payloadBuffer = ByteBuffer.allocate(65507);
            while (!Stopped && mSelector.isOpen()) {
                mSelector.select();
                Iterator<SelectionKey> keyIterator = mSelector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    if (key.isValid()) {
                        try {
                            if (key.isReadable()) {
                                DatagramChannel serverChannel = (DatagramChannel)key.channel();
                                DatagramSocket serverSocket = serverChannel.socket();
                                payloadBuffer.clear();
                                InetSocketAddress serverRemoteAddress = (InetSocketAddress) serverChannel.receive(payloadBuffer);
                                int size = payloadBuffer.position();
                                if (size > 0) {
                                    InetSocketAddress serverLocalAddress = (InetSocketAddress) serverSocket.getLocalSocketAddress();
                                    if (serverRemoteAddress != null && serverLocalAddress != null){
                                        if (serverChannel == mDatagramChannel) {
                                            int sessionPort = (short)(serverRemoteAddress.getPort() & 0xffff);
                                            NatSession session = NatSessionManager.getSession(sessionPort);
                                            if (session != null) {
                                                DatagramChannel clientRemoteChannel = mClientRemoteChannelMap.get(sessionPort);
                                                if (clientRemoteChannel == null) {
                                                    clientRemoteChannel = createRemoteChannel(sessionPort);
                                                }
                                                if (clientRemoteChannel != null) {
                                                    InetSocketAddress clientRemoteChannelAddress = (InetSocketAddress) clientRemoteChannel.socket().getLocalSocketAddress();
                                                    payloadBuffer.flip();
                                                    serverChannel.send(payloadBuffer, clientRemoteChannelAddress);
                                                }
                                            }
                                        } else if (
                                                serverRemoteAddress.getHostName().contains("localhost")
                                                && serverRemoteAddress.getPort() == mDatagramChannel.socket().getLocalPort()){
                                            Integer sessionPortInt = (Integer) key.attachment();
                                            if (sessionPortInt != null) {
                                                int sessionPort = sessionPortInt;
                                                NatSession session = NatSessionManager.getSession(sessionPort);
                                                if (session != null) {
                                                    ShadowsocksConfig shadowsocksConfig = (ShadowsocksConfig) ProxyConfig.Instance.getDefaultProxy();
                                                    InetSocketAddress shadowsocksSocketAddress = shadowsocksConfig.ServerAddress;
                                                    InetSocketAddress clientRemoteSocketAddress = createClientRemoteSocketAddress(shadowsocksSocketAddress, session);
                                                    if (clientRemoteSocketAddress.equals(shadowsocksSocketAddress)) {
                                                        byte[] payload = new byte[size];
                                                        payloadBuffer.flip();
                                                        payloadBuffer.get(payload);
                                                        payloadBuffer.clear();
                                                        payloadBuffer.put((byte) 0x03);//domain
                                                        byte[] domainBytes = session.RemoteHost.getBytes();
                                                        payloadBuffer.put((byte) domainBytes.length);//domain length;
                                                        payloadBuffer.put(domainBytes);
                                                        payloadBuffer.putShort(session.RemotePort);
                                                        payloadBuffer.put(payload);
                                                        payload = new byte[payloadBuffer.position()];
                                                        payloadBuffer.flip();
                                                        payloadBuffer.get(payload);
                                                        ICrypt iCrypt = CryptFactory.get(shadowsocksConfig.EncryptMethod, shadowsocksConfig.Password);
                                                        payload = iCrypt.encrypt(payload);
                                                        payloadBuffer.clear();
                                                        payloadBuffer.put(payload);
                                                    }
                                                    payloadBuffer.flip();
                                                    serverChannel.send(payloadBuffer, clientRemoteSocketAddress);
                                                }
                                            }
                                        } else{
                                            Integer sessionPortInt = (Integer) key.attachment();
                                            if (sessionPortInt != null) {
                                                int sessionPort = (short)(sessionPortInt & 0xffff);
                                                NatSession session = NatSessionManager.getSession(sessionPort);
                                                if (session != null) {
                                                    ShadowsocksConfig shadowsocksConfig = (ShadowsocksConfig) ProxyConfig.Instance.getDefaultProxy();
                                                    InetSocketAddress shadowsocksSocketAddress = shadowsocksConfig.ServerAddress;
                                                    if (serverRemoteAddress.equals(shadowsocksSocketAddress)) {
                                                        byte[] payload = new byte[size];
                                                        payloadBuffer.flip();
                                                        payloadBuffer.get(payload);
                                                        ICrypt iCrypt = CryptFactory.get(shadowsocksConfig.EncryptMethod, shadowsocksConfig.Password);
                                                        payload = iCrypt.decrypt(payload);
                                                        payloadBuffer.clear();
                                                        payloadBuffer.put(payload, 7, payload.length -7);
                                                    }
                                                    InetSocketAddress localRemoteSocketAddress = new InetSocketAddress(CommonMethods.ipIntToString(session.RemoteIP), sessionPort & 0xffff);
                                                    payloadBuffer.flip();
                                                    mDatagramChannel.send(payloadBuffer, localRemoteSocketAddress);
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (key.isWritable()) {
                            }
                        } catch (Exception e) {
                            ShadowsocksApplication.handleException(e);
                        }
                    }
                    keyIterator.remove();
                }
            }
        } catch (Exception e) {
            ShadowsocksApplication.handleException(e);
        } finally {
            this.stop();
        }

    }

    private InetSocketAddress createClientRemoteSocketAddress(InetSocketAddress shadowsocksSocketAddress, NatSession session) {
        InetSocketAddress clientRemoteSocketAddress;
        if (ProxyConfig.isFakeIP(session.RemoteIP)) {
            clientRemoteSocketAddress = shadowsocksSocketAddress;
        } else {
            clientRemoteSocketAddress = new InetSocketAddress(CommonMethods.ipIntToString(session.RemoteIP), session.RemotePort & 0xffff);
        }
        return clientRemoteSocketAddress;
    }

    private DatagramChannel createRemoteChannel(int sessionPort) throws IOException {
        DatagramChannel remoteChannel = null;
        remoteChannel = DatagramChannel.open();
        remoteChannel.configureBlocking(false);
        DatagramSocket remoteSocket = remoteChannel.socket();
        remoteSocket.bind(new InetSocketAddress(0));
        remoteChannel.register(mSelector, SelectionKey.OP_READ, Integer.valueOf(sessionPort));
        if (LocalVpnService.Instance.protect(remoteSocket)) {
            mClientRemoteChannelMap.put(sessionPort, remoteChannel);
        } else {
            remoteChannel.close();
            remoteChannel = null;
        }
        return remoteChannel;
    }

    public Thread getThread(){
        return mReceivedThread;
    }


}
