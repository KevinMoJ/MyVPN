package com.androapplite.shadowsocks.utils;

import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;

import com.vm.shadowsocks.tunnel.shadowsocks.CryptFactory;
import com.vm.shadowsocks.tunnel.shadowsocks.ICrypt;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;


/**
 * Created by huangjian on 2018/6/19.
 * 使用方法参照MainActivity
 */
public class ShadowSocksProxyRunnable implements Runnable {
    private static final byte SOCKS_PROTOCOL_4 = 0X04;
    private static final byte SOCKS_PROTOCOL_5 = 0X05;
    private static final int DEFAULT_BUFFER_SIZE = 1460;
    private static final byte CMD_CONNECT = 0x01;
    private static final byte CMD_BIND = 0x02;
    private static final byte CMD_UDP = 0x03;
    private static final byte TYPE_IPV4 = 0x01;
    private static final byte TYPE_HOST = 0X03;
    private static final byte TYPE_IPV6 = 0X04;
    private static final byte ALLOW_PROXY = 0X5A;
    private static final byte DENY_PROXY = 0X5B;

    private static final byte REP_SUCCESS = 0X00;
    private static final byte REP_SERVER_FAILURE = 0X01;
    private static final byte REP_CONNECTION_NOT_ALLOWED = 0X02;
    private static final byte REP_NETWORK_UNREACHABLE = 0X03;
    private static final byte REP_HOST_UNREACHABLE = 0X04;
    private static final byte REP_CONNECTION_REFUSED = 0X05;
    private static final byte REP_TTL_EXPIRED = 0X06;
    private static final byte REP_COMMAND_NOT_SUPPORT = 0X07;
    private static final byte REP_ADDRESS_TYPE_NOT_SUPPORTED = 0X08;
    private static final byte REP_UNASSIGNED = 0X09;
    private static final byte RESERVE = 0X00;
    private static final byte NO_AUTHENTICATION_REQUIRED = 0x00;
    private static final byte SOCKS_4_REPLY_VN = 0X00;
    private static final byte SOCKS_4_REP_SUCCESS = 90;

    private Selector mSelector;
//            private ServerSocketChannel mLocalServerSocketChannel;
    private boolean mRunning;
    private Thread mThread;
    private ArrayMap<Proxy, ServerSocketChannel> mProxyServerSocketChannelMap;

    public ShadowSocksProxyRunnable() {
        mProxyServerSocketChannelMap = new ArrayMap<>();
    }


    private void destroyLocalServer() {
        try {
            if (mSelector != null) {
                mSelector.close();
                mSelector = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < mProxyServerSocketChannelMap.size(); i++) {
            ServerSocketChannel serverSocketChannel = mProxyServerSocketChannelMap.valueAt(i);
            if (serverSocketChannel != null) {
                try {
                    serverSocketChannel.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        mProxyServerSocketChannelMap.clear();
    }


    public void start() {
        if (mSelector == null) {
            try {
                mSelector = Selector.open();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (mThread == null) {
            mThread = new Thread(this);
            mThread.start();
            mRunning = true;
        }
    }

    public void stop() {
        if (mThread != null) {
            destroyLocalServer();
            mThread.interrupt();
            mThread = null;
            mRunning = false;
        }
    }

    @Override
    public void run() {
        byte[] bufferBytes = new byte[DEFAULT_BUFFER_SIZE];
        ByteBuffer byteBuffer = ByteBuffer.wrap(bufferBytes);
        while (mRunning) {
            try {
                mSelector.select();
                synchronized (this) {
                    Iterator<SelectionKey> iterator = mSelector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        if (key.isValid()) {
                            try {
                                if (key.isAcceptable()) { //第一步，内连接创建
                                    ShadowsocksConfig config = (ShadowsocksConfig) key.attachment();
                                    if (config instanceof ShadowsocksConfig) {
                                        SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
                                        socketChannel.configureBlocking(false);
                                        Tunnel tunnel = new Tunnel(config, socketChannel);
                                        socketChannel.register(mSelector, SelectionKey.OP_READ, tunnel);
                                    }

                                } else if (key.isConnectable()) {
                                    SocketChannel socketChannel = ((SocketChannel) key.channel());
                                    socketChannel.finishConnect();
                                    Tunnel tunnel = (Tunnel) key.attachment();
                                    if (tunnel != null) {
                                        if (socketChannel.equals(tunnel.outerChannel)) {
                                            switch (tunnel.nextStep) {
                                                case Tunnel.STEP_3_REMOTE_RESPONSE:
                                                    tunnel.handleStep3(byteBuffer);
                                                    break;
                                                case Tunnel.STEP_3_REMOTE_RESPONSE_SOCKS_4:
                                                    tunnel.handleStep3Socks4(byteBuffer);
                                                    break;
                                            }
                                        } else if (socketChannel.equals(tunnel.innerChannel)) {

                                        }
                                        socketChannel.register(mSelector, SelectionKey.OP_READ, tunnel);
                                    }
                                } else if (key.isReadable()) {
                                    SocketChannel socketChannel = (SocketChannel) key.channel();
                                    byteBuffer.clear();
                                    int size = socketChannel.read(byteBuffer);
//                                        System.out.println("isReadable size: " + size);
                                    if (size > -1) {
                                        Tunnel tunnel = (Tunnel) key.attachment();
                                        if (tunnel instanceof Tunnel) {
                                            if (socketChannel.equals(tunnel.innerChannel)) {
                                                switch (tunnel.nextStep) {
                                                    case Tunnel.STEP_1_HANDSHAKE:
//                                                        System.out.println(">>>>>>>>handshake>>>>>>>>>>>>");
//                                                        for(byte b:bufferBytes) {
//                                                            System.out.println(b + "\t->\t" + Character.toString((char)b));
//                                                        }
//                                                        System.out.println("<<<<<<<<<<<<<<<<<<<<");
                                                        byte vn = bufferBytes[0];
                                                        switch (vn) {
                                                            case SOCKS_PROTOCOL_4:
                                                                tunnel.handleStep1Socks4(mSelector, bufferBytes, byteBuffer);
                                                                break;
                                                            case SOCKS_PROTOCOL_5:
                                                                tunnel.handStep1(byteBuffer);
                                                                break;
                                                        }
                                                        break;
                                                    case Tunnel.STEP_2_REMOTE_HOST:
                                                        tunnel.handleStep2(mSelector, bufferBytes, byteBuffer);
                                                        break;
                                                    case Tunnel.STEP_4_SEND_DATA:
                                                        tunnel.handleStep4SendOut(byteBuffer);
                                                        break;
                                                }
                                            } else if (socketChannel.equals(tunnel.outerChannel)) {
                                                tunnel.handleStep4Receive(byteBuffer);
                                            }
                                        }
                                    } else {
                                        key.cancel();
                                        key.channel().close();
                                    }
                                } else if (key.isWritable()) {
//                                    System.out.println("isWritable");
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                key.cancel();
                                key.channel().close();
                            }
                        }
                        iterator.remove();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class Tunnel {
        byte nextStep;
        SocketChannel outerChannel;
        SocketChannel innerChannel;
        static final byte STEP_1_HANDSHAKE = 1;
        static final byte STEP_2_REMOTE_HOST = 2;
        static final byte STEP_3_REMOTE_RESPONSE = 3;
        static final byte STEP_4_SEND_DATA = 4;
        InetSocketAddress remoteAddress;
        SocketAddress ssRemoteAddress;
        ICrypt crypt;
        ShadowsocksConfig ssConfig;
        byte aType;
        static final byte STEP_3_REMOTE_RESPONSE_SOCKS_4 = 100;


        Tunnel(ShadowsocksConfig config, SocketChannel socketChannel) {
            ssConfig = config;
            innerChannel = socketChannel;
            nextStep = STEP_1_HANDSHAKE;
        }

        private void handStep1(ByteBuffer byteBuffer) throws IOException {
            ((ByteBuffer) byteBuffer.clear())
                    .put(SOCKS_PROTOCOL_5)
                    .put(NO_AUTHENTICATION_REQUIRED);
            send(byteBuffer, innerChannel);
            nextStep = STEP_2_REMOTE_HOST;
        }

        private void send(ByteBuffer buffer, SocketChannel channel) throws IOException {
            if (buffer.position() > 0) {
                buffer.flip();
            }
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
        }

        private void handleStep2(Selector selector, byte[] bufferBytes, ByteBuffer byteBuffer) throws IOException {
            byte reqCmd = bufferBytes[1];
            byte reqAtyp = bufferBytes[3];
            remoteAddress = createRemoteSocketAddress(bufferBytes, byteBuffer, reqAtyp);

            switch (reqCmd) {
                case CMD_CONNECT:
                    ssRemoteAddress = new InetSocketAddress(ssConfig.server, ssConfig.port);
                    SocketChannel remoteSocketChannel = SocketChannel.open();
                    remoteSocketChannel.configureBlocking(false);
                    remoteSocketChannel.connect(ssRemoteAddress);
                    remoteSocketChannel.register(selector, SelectionKey.OP_CONNECT, this);
                    outerChannel = remoteSocketChannel;
                    nextStep = Tunnel.STEP_3_REMOTE_RESPONSE;
                    break;
                case CMD_BIND:
                    break;
                case CMD_UDP:
                    break;
            }
            aType = reqAtyp;
        }

        @Nullable
        private InetSocketAddress createRemoteSocketAddress(byte[] bufferBytes, ByteBuffer byteBuffer, byte reqAtyp) throws UnknownHostException {
            InetSocketAddress address = null;
            int port = 0;
            byteBuffer.flip().position(4);
            switch (reqAtyp) {
                case TYPE_IPV4:
                    byte[] ipv4Address = new byte[4];
                    byteBuffer.get(ipv4Address);
                    InetAddress ipv4 = InetAddress.getByAddress(ipv4Address);
                    port = byteBuffer.getShort() & 0xffff;
                    address = new InetSocketAddress(ipv4, port);
                    break;
                case TYPE_HOST:
                    byte hostLen = bufferBytes[4];
                    byte[] hostBytes = new byte[hostLen];
                    byteBuffer.position(5);
                    byteBuffer.get(hostBytes);
                    String host = new String(hostBytes);
                    port = byteBuffer.getShort() & 0xffff;
//                address = new InetSocketAddress(host, port);
                    address = InetSocketAddress.createUnresolved(host, port);
                    break;
                case TYPE_IPV6:
                    byte[] ipv6Address = new byte[16];
                    byteBuffer.get(ipv6Address);
                    InetAddress ipv6 = InetAddress.getByAddress(ipv6Address);
                    port = byteBuffer.getShort() & 0xffff;
                    address = new InetSocketAddress(ipv6, port);
                    break;
            }
            return address;
        }


        private void handleStep3(ByteBuffer byteBuffer) throws IOException {
            //向vpn server发送连接数据，告诉需要连接的网址
            ((ByteBuffer) byteBuffer.clear())
                    .put(aType);
            switch (aType) {
                case TYPE_IPV4:
                    byteBuffer.put(remoteAddress.getAddress().getAddress());
                    break;
                case TYPE_HOST:
                    String domain = remoteAddress.getHostName();
                    byteBuffer.put((byte) domain.length())
                            .put(domain.getBytes());
                    break;
                case TYPE_IPV6:
                    byteBuffer.put(remoteAddress.getAddress().getAddress());
                    break;
            }
            byteBuffer.putShort((short) (remoteAddress.getPort() & 0xffff));
            byteBuffer.flip();
            byte[] raw = new byte[byteBuffer.limit()];
            byteBuffer.get(raw);
//                crypt = CryptFactory.get("aes-256-cfb", "vpnnest!@#123d");
            crypt = CryptFactory.get(ssConfig.method, ssConfig.password);
            byte[] encrypt = crypt.encrypt(raw);
            byteBuffer.clear();
            byteBuffer.put(encrypt);
            send(byteBuffer, outerChannel);

            //向内部连接发送数据，告诉连接成功
            ((ByteBuffer) byteBuffer.clear())
                    .put(SOCKS_PROTOCOL_5)
                    .put(REP_SUCCESS)
                    .put(RESERVE)
                    .put(TYPE_IPV4)
                    .put(outerChannel.socket().getLocalAddress().getAddress())
                    .putShort((short) (outerChannel.socket().getLocalPort() & 0xFFFF));
            send(byteBuffer, innerChannel);
            nextStep = Tunnel.STEP_4_SEND_DATA;
        }

        private void handleStep4SendOut(ByteBuffer byteBuffer) throws IOException {
            byteBuffer.flip();
            byte[] raw = new byte[byteBuffer.limit()];
            byteBuffer.get(raw);
//                System.out.println(">>>>>>>>>request>>>>>>>>>>>");
//                for(byte b:raw) {
//                    System.out.println(b + "\t->\t" + Character.toString((char)b));
//                }
//                System.out.println("<<<<<<<<<<<<<<<<<<<<");
            byte[] encrypt = crypt.encrypt(raw);
            byteBuffer.clear();
            byteBuffer.put(encrypt);
            send(byteBuffer, outerChannel);
        }

        private void handleStep4Receive(ByteBuffer byteBuffer) throws IOException {
            byteBuffer.flip();
            byte[] raw = new byte[byteBuffer.limit()];
            byteBuffer.get(raw);
            byte[] decrypt = crypt.decrypt(raw);
//                System.out.println(">>>>>>>>response>>>>>>>>>>>>");
//                for(byte b:decrypt) {
//                    System.out.println(b + "\t->\t" + Character.toString((char)b));
//                }
//                System.out.println("<<<<<<<<<<<<<<<<<<<<");
            byteBuffer.clear();
            byteBuffer.put(decrypt);
            send(byteBuffer, innerChannel);
        }

        private void handleStep1Socks4(Selector selector, byte[] bufferBytes, ByteBuffer byteBuffer) throws Exception {
            byte reqCmd = bufferBytes[1];
            switch (reqCmd) {
                case CMD_CONNECT:
                    remoteAddress = createRemoteSocketAddress(byteBuffer);
                    ssRemoteAddress = new InetSocketAddress(ssConfig.server, ssConfig.port);
                    SocketChannel remoteSocketChannel = SocketChannel.open();
                    remoteSocketChannel.configureBlocking(false);
                    remoteSocketChannel.connect(ssRemoteAddress);
                    remoteSocketChannel.register(selector, SelectionKey.OP_CONNECT, this);
                    outerChannel = remoteSocketChannel;
                    nextStep = Tunnel.STEP_3_REMOTE_RESPONSE_SOCKS_4;
                    break;
                case CMD_BIND:
                    break;
                case CMD_UDP:
                    break;
            }
            aType = TYPE_IPV4;
        }

        private InetSocketAddress createRemoteSocketAddress(ByteBuffer byteBuffer) throws UnknownHostException {
            byteBuffer.flip().position(2);
            int port = byteBuffer.getShort() & 0xffff;
            byte[] ipv4Address = new byte[4];
            byteBuffer.get(ipv4Address);
            InetAddress ipv4 = InetAddress.getByAddress(ipv4Address);
            return new InetSocketAddress(ipv4, port);
        }

        private void handleStep3Socks4(ByteBuffer byteBuffer) throws IOException {
            //向vpn server发送连接数据，告诉需要连接的网址
            ((ByteBuffer) byteBuffer.clear())
                    .put(aType);
            switch (aType) {
                case TYPE_IPV4:
                    byteBuffer.put(remoteAddress.getAddress().getAddress());
                    break;
                case TYPE_HOST:
                    String domain = remoteAddress.getHostName();
                    byteBuffer.put((byte) domain.length())
                            .put(domain.getBytes());
                    break;
                case TYPE_IPV6:
                    byteBuffer.put(remoteAddress.getAddress().getAddress());
                    break;
            }
            byteBuffer.putShort((short) (remoteAddress.getPort() & 0xffff));
            byteBuffer.flip();
            byte[] raw = new byte[byteBuffer.limit()];
            byteBuffer.get(raw);
//                crypt = CryptFactory.get("aes-256-cfb", "vpnnest!@#123d");
            crypt = CryptFactory.get(ssConfig.method, ssConfig.password);
            byte[] encrypt = crypt.encrypt(raw);
            byteBuffer.clear();
            byteBuffer.put(encrypt);
            send(byteBuffer, outerChannel);

            //向内部连接发送数据，告诉连接成功
            ((ByteBuffer) byteBuffer.clear())
                    .put(SOCKS_4_REPLY_VN)
                    .put(SOCKS_4_REP_SUCCESS)
                    .putShort((short) (outerChannel.socket().getLocalPort() & 0xFFFF))
                    .put(outerChannel.socket().getLocalAddress().getAddress());
            send(byteBuffer, innerChannel);
            nextStep = Tunnel.STEP_4_SEND_DATA;
        }
    }


    public synchronized Proxy createShadowsocksProxy(String server, int port, String method, String password) {
        try {
            ServerSocketChannel localServerSocketChannel = ServerSocketChannel.open();
            localServerSocketChannel.configureBlocking(false);
            localServerSocketChannel.socket().bind(new InetSocketAddress(0));
            int localPort = localServerSocketChannel.socket().getLocalPort();
            InetSocketAddress localServerSocketAddress = new InetSocketAddress(InetAddress.getLocalHost(), localPort);
            Proxy proxy = new Proxy(Proxy.Type.SOCKS, localServerSocketAddress);
            mProxyServerSocketChannelMap.put(proxy, localServerSocketChannel);
            ShadowsocksConfig config = new ShadowsocksConfig(server, port, method, password);
            mSelector.wakeup();
            synchronized (this) {
                localServerSocketChannel.register(mSelector, SelectionKey.OP_ACCEPT, config);
            }
            return proxy;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public synchronized Proxy createShadowsocksProxy(String server, int port) {
        try {
            ServerSocketChannel localServerSocketChannel = ServerSocketChannel.open();
            localServerSocketChannel.configureBlocking(false);
            localServerSocketChannel.socket().bind(new InetSocketAddress(0));
            int localPort = localServerSocketChannel.socket().getLocalPort();
            InetSocketAddress localServerSocketAddress = new InetSocketAddress(InetAddress.getLocalHost(), localPort);
            Proxy proxy = new Proxy(Proxy.Type.SOCKS, localServerSocketAddress);
            mProxyServerSocketChannelMap.put(proxy, localServerSocketChannel);
            ShadowsocksConfig config = new ShadowsocksConfig(server, port);
            mSelector.wakeup();
            synchronized (this) {
                localServerSocketChannel.register(mSelector, SelectionKey.OP_ACCEPT, config);
            }
            return proxy;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static class ShadowsocksConfig {
        //            InetSocketAddress address;
        String server;
        int port;
        String method;
        String password;

        ShadowsocksConfig(String server, int port, String method, String password) {
            this.server = server;
            this.port = port;
            this.method = method;
            this.password = password;
        }

        ShadowsocksConfig(String server, int port) {
            this.server = server;
            this.port = port;
            this.method = defaultMethod();
            this.password = defaultPassword();
        }

        private String defaultMethod() {
            return new StringBuilder("aes").append("-").append(256).append("-cfb").toString();
        }

        private String defaultPassword() {
            return new StringBuilder("vpn").append("nest").append("!@#").append("123d").toString();
        }
    }
}
