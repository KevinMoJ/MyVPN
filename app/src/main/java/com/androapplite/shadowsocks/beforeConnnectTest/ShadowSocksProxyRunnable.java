package com.androapplite.shadowsocks.beforeConnnectTest;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;


/**
 * Created by huangjian on 2018/6/19.
 * 使用方法参照MainActivity
 */
public class ShadowSocksProxyRunnable implements Runnable {
    private static final int DEFAULT_BUFFER_SIZE = 1460;
    private static final byte TYPE_IPV4 = 0x01;


    private Selector mSelector;
    private boolean mRunning;
    private Thread mThread;
    private ArrayList<ServerSocketChannel> mProxyServerSocketChannels;
    private byte[] mBytes;
    private ByteBuffer mBuffer;
    private ServerSocketChannel mConnectionTestServerSocketChannel;

    public ShadowSocksProxyRunnable() {
        mProxyServerSocketChannels = new ArrayList<>();
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

        if (mBytes == null) {
            mBytes = new byte[DEFAULT_BUFFER_SIZE];
            mBuffer = ByteBuffer.wrap(mBytes);
        }
    }

    public void stop() {
        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
            mRunning = false;
        }
        destroyLocalServer();
        mConnectionTestServerSocketChannel = null;

        if(mBytes != null) {
            mBytes = null;
            mBuffer = null;
        }
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

        for(ServerSocketChannel channel: mProxyServerSocketChannels) {
            try {
                channel.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mProxyServerSocketChannels.clear();
    }

    public synchronized Proxy createShadowsocksProxy(String server, int port, String method, String password) {
        ShadowsocksConfig config = new ShadowsocksConfig(server, port, method, password);
        return createShadowsocksProxy(config);
    }

    public Proxy createShadowsocksProxy(String server, int port) {
        ShadowsocksConfig config = new ShadowsocksConfig(server, port);
        return createShadowsocksProxy(config);
    }

    public Proxy createShadowsocksProxy(ShadowsocksConfig config) {
        try {
            ServerSocketChannel localServerSocketChannel = ServerSocketChannel.open();
            localServerSocketChannel.configureBlocking(false);
            localServerSocketChannel.socket().bind(new InetSocketAddress(0));
            int localPort = localServerSocketChannel.socket().getLocalPort();
            InetSocketAddress localServerSocketAddress = new InetSocketAddress(InetAddress.getLocalHost(), localPort);
            Proxy proxy = new Proxy(Proxy.Type.SOCKS, localServerSocketAddress);
            mSelector.wakeup();
            synchronized (this) {
                mProxyServerSocketChannels.add(localServerSocketChannel);
                localServerSocketChannel.register(mSelector, SelectionKey.OP_ACCEPT, config);
            }
            System.out.println(proxy.toString() + " | " + localServerSocketAddress);
            return proxy;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void run() {
        synchronized (this) {
            try {
                mConnectionTestServerSocketChannel = ServerSocketChannel.open();
                mConnectionTestServerSocketChannel.configureBlocking(false);
                mConnectionTestServerSocketChannel.socket().bind(new InetSocketAddress(0));
                mConnectionTestServerSocketChannel.register(mSelector, SelectionKey.OP_ACCEPT);
                mProxyServerSocketChannels.add(mConnectionTestServerSocketChannel);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Iterator<SelectionKey> iterator = null;
        while (mRunning) {
            try{mSelector.select();
            }catch (Exception e) {
                e.printStackTrace();
            }
            if (!mRunning || Thread.currentThread().isInterrupted()) {
                break;
            }
            synchronized (this) {
                if (mSelector != null && mSelector.isOpen())
                    iterator = mSelector.selectedKeys().iterator();
            }
            if (iterator != null) {
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isValid()) {
                        if (key.isAcceptable()) {
                            handleAccept(key);
                        } else if (key.isReadable()) {
                            handleRead(key);
                        } else if (key.isConnectable()) {
                            handleConnect(key);
                        }
                    } else {
                        key.cancel();
                    }
                }
            }
        }
    }

    private void handleAccept(SelectionKey key) {
        try {
            Session.newSession(this, key);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleRead(SelectionKey key) {
        Session session  = (Session)key.attachment();
        if (session instanceof Session) {
            try {
                session.handleRead(key);
            } catch (Exception e) {
                e.printStackTrace();
                closeChannel(key);
            }
        } else {
            closeChannel(key);
            key.cancel();
        }
    }

    private void handleConnect(SelectionKey key) {
        Session session  = (Session)key.attachment();
        if (session instanceof  Session) {
            try {
                session.handleConnect(key);
            } catch (Exception e) {
                e.printStackTrace();
                closeChannel(key);
                key.cancel();
            }
        } else {
            closeChannel(key);
            key.cancel();
        }
    }

    public Selector selector() {
        return  mSelector;
    }

    public ByteBuffer buffer() {
        return mBuffer;
    }

    public ByteBuffer copyToBuffer(byte[] src) {
       return ((ByteBuffer)mBuffer.clear()).put(src);
    }

    public byte[] copyFromBuffer() {
        if (mBuffer.position() != 0) {
            mBuffer.flip();
        }
        int size = mBuffer.limit();
        if (size > 0) {
            byte[] dst = new byte[size];
            mBuffer.get(dst);
            return dst;
        } else {
            return null;
        }
    }

    private void closeChannel(SelectionKey key) {
        try {
            key.channel().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean connectionTest(String server, int port) {
        while(true) {
            synchronized (this) {
                if (mConnectionTestServerSocketChannel != null) {
                    break;
                }
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        ShadowsocksConfig config = new ShadowsocksConfig(server, port);
        try {
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(true);
            socketChannel.connect(new InetSocketAddress(InetAddress.getLocalHost().getHostName(), mConnectionTestServerSocketChannel.socket().getLocalPort()));
            ByteBuffer buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
            buffer.put(TYPE_IPV4)
                    .put(config.address().getAddress().getAddress())
                    .putShort((short) (config.address().getPort() & 0xffff))
                    .flip();
            socketChannel.write(buffer);
            buffer.clear();
            socketChannel.read(buffer);
            return ConnectionTestSession.isResponseOk(buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isConnectionTest(SelectionKey key) {
        return key.channel().equals(mConnectionTestServerSocketChannel);
    }
}
