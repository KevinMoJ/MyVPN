package com.vm.shadowsocks.core;

import android.util.Log;
import android.util.SparseArray;

import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.vm.shadowsocks.tcpip.CommonMethods;
import com.vm.shadowsocks.tunnel.shadowsocks.CryptFactory;
import com.vm.shadowsocks.tunnel.shadowsocks.ICrypt;
import com.vm.shadowsocks.tunnel.shadowsocks.ShadowsocksConfig;

import java.io.IOException;
import java.net.DatagramSocket;
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
    private SparseArray<ChannelProperty> mClientRemoteChannelMap;

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
                ChannelProperty channelProperty = mClientRemoteChannelMap.valueAt(i);
                try {
                    channelProperty.channel.close();
                } catch (IOException e) {
                    ShadowsocksApplication.handleException(e);
                }
            }
            mClientRemoteChannelMap.clear();
        }
        mReceivedThread.interrupt();
    }

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
                                                ChannelProperty channelProperty = mClientRemoteChannelMap.get(sessionPort);
                                                if (channelProperty == null) {
                                                    channelProperty = createRemoteChannel(sessionPort);
                                                }
                                                if (channelProperty != null) {
                                                    InetSocketAddress clientRemoteChannelAddress = (InetSocketAddress) channelProperty.channel.socket().getLocalSocketAddress();
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
                                                ChannelProperty channelProperty = mClientRemoteChannelMap.get(sessionPort);
                                                if (session != null && channelProperty != null) {
                                                    Log.d("udpproxy->", (sessionPort & 0xffff) + " session.SendTactics: " + channelProperty.sendTactics);
//                                                    if (channelProperty.sendTactics == 0 || channelProperty.sendTactics == 1) {
//                                                        InetSocketAddress clientRemoteSocketAddress =
//                                                                new InetSocketAddress(CommonMethods.ipIntToString(session.RemoteIP), session.RemotePort & 0xffff);
//                                                        payloadBuffer.flip();
//                                                        serverChannel.send(payloadBuffer, clientRemoteSocketAddress);
//                                                        Log.d("udpproxy->", (sessionPort & 0xffff) + "->" + clientRemoteSocketAddress.toString());
//                                                    }
                                                    if (channelProperty.sendTactics == 0 || channelProperty.sendTactics == 2) {
                                                        ShadowsocksConfig shadowsocksConfig = (ShadowsocksConfig) ProxyConfig.Instance.getDefaultProxy();
                                                        InetSocketAddress shadowsocksSocketAddress = shadowsocksConfig.ServerAddress;
                                                        InetSocketAddress clientRemoteSocketAddress = shadowsocksSocketAddress;
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
                                                        payloadBuffer.flip();
                                                        serverChannel.send(payloadBuffer, clientRemoteSocketAddress);
                                                        Log.d("udpproxy->", (sessionPort & 0xffff) + "->" + CommonMethods.ipIntToString(session.RemoteIP) + ":" + (session.RemotePort & 0xffff) + " proxy(" + shadowsocksSocketAddress + ")");
                                                    }
                                                }
                                            }
                                        } else{
                                            Integer sessionPortInt = (Integer) key.attachment();
                                            if (sessionPortInt != null) {
                                                int sessionPort = (short)(sessionPortInt & 0xffff);
                                                NatSession session = NatSessionManager.getSession(sessionPort);
                                                ChannelProperty channelProperty = mClientRemoteChannelMap.get(sessionPort);
                                                if (session != null && channelProperty != null) {
                                                    ShadowsocksConfig shadowsocksConfig = (ShadowsocksConfig) ProxyConfig.Instance.getDefaultProxy();
                                                    InetSocketAddress shadowsocksSocketAddress = shadowsocksConfig.ServerAddress;
                                                    Log.d("udpproxy<-", (sessionPort & 0xffff) + " session.SendTactics: " + channelProperty.sendTactics);
                                                    if (serverRemoteAddress.equals(shadowsocksSocketAddress)) {
                                                        if (channelProperty.sendTactics == 0) {
                                                            channelProperty.sendTactics = 2;
                                                        }
                                                        byte[] payload = new byte[size];
                                                        payloadBuffer.flip();
                                                        payloadBuffer.get(payload);
                                                        ICrypt iCrypt = CryptFactory.get(shadowsocksConfig.EncryptMethod, shadowsocksConfig.Password);
                                                        payload = iCrypt.decrypt(payload);
                                                        payloadBuffer.clear();
                                                        payloadBuffer.put(payload, 7, payload.length -7);
                                                    } else {
//                                                        channelProperty.sendTactics = 1;
                                                    }
                                                    InetSocketAddress localRemoteSocketAddress = new InetSocketAddress(CommonMethods.ipIntToString(session.RemoteIP), sessionPort & 0xffff);
                                                    payloadBuffer.flip();
                                                    mDatagramChannel.send(payloadBuffer, localRemoteSocketAddress);
                                                    if (serverRemoteAddress.equals(shadowsocksSocketAddress)) {
                                                        Log.d("udpproxy<-", (sessionPort & 0xffff) + "<-" + CommonMethods.ipIntToString(session.RemoteIP) + ":" + (session.RemotePort & 0xffff) + " proxy(" + shadowsocksSocketAddress + ")");
                                                    } else {
                                                        Log.d("udpproxy<-", (sessionPort & 0xffff) + "<-" + localRemoteSocketAddress.toString());
                                                    }
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

    private ChannelProperty createRemoteChannel(int sessionPort) throws IOException {
        ChannelProperty channelProperty = null;
        DatagramChannel remoteChannel = DatagramChannel.open();
        remoteChannel.configureBlocking(false);
        DatagramSocket remoteSocket = remoteChannel.socket();
        remoteSocket.bind(new InetSocketAddress(0));
        remoteChannel.register(mSelector, SelectionKey.OP_READ, Integer.valueOf(sessionPort));
        if (LocalVpnService.Instance.protect(remoteSocket)) {
            channelProperty = new ChannelProperty(remoteChannel);
            mClientRemoteChannelMap.put(sessionPort, channelProperty);
        } else {
            remoteChannel.close();
            channelProperty = null;
        }
        return channelProperty;
    }

    public Thread getThread(){
        return mReceivedThread;
    }

    private static class ChannelProperty {
        DatagramChannel channel;
        byte sendTactics;
        //    public byte SendTactics; //=0：初始化；=1：直连；=2：vpn连


        ChannelProperty(DatagramChannel channel) {
            this.channel = channel;
        }
    }

}
