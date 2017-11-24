package com.vm.shadowsocks.core;

import android.support.v4.util.ArrayMap;

import com.vm.shadowsocks.tcpip.CommonMethods;
import com.vm.shadowsocks.tcpip.IPHeader;
import com.vm.shadowsocks.tcpip.UDPHeader;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

public class UdpProxy implements Runnable {

    private static class UdpSrcDesMapping {
        public int ClientIP;
        public short ClientPort;
        public int RemoteIP;
        public short RemotePort;
        public long pCreateTime;
    }

    public boolean Stopped;
    private DatagramSocket m_Client;
    private Thread m_ReceivedThread;
    private ArrayMap<? super SocketAddress, UdpSrcDesMapping> mSrcDesMap;
    public int Port;
    private long mStartTime;
    private long mTimeOut;

    public UdpProxy() throws IOException {
        mSrcDesMap = new ArrayMap<>();
        m_Client = new DatagramSocket(0);
        Port= m_Client.getLocalPort();
        mTimeOut = TimeUnit.SECONDS.toMillis(600);
    }

    public void start() {
        m_ReceivedThread = new Thread(this);
        m_ReceivedThread.setName("UdpProxyThread");
        m_ReceivedThread.start();
        mStartTime = System.currentTimeMillis();
    }

    public void stop() {
        Stopped = true;
        if (m_Client != null) {
            m_Client.close();
            m_Client = null;
        }
        synchronized (mSrcDesMap) {
            mSrcDesMap.clear();
        }
    }

    @Override
    public void run() {
        try {
            byte[] RECEIVE_BUFFER = new byte[2000];
            IPHeader ipHeader = new IPHeader(RECEIVE_BUFFER, 0);
            ipHeader.Default();
            UDPHeader udpHeader = new UDPHeader(RECEIVE_BUFFER, 20);
            DatagramPacket packet = new DatagramPacket(RECEIVE_BUFFER, 28, RECEIVE_BUFFER.length - 28);

            while (m_Client != null && !m_Client.isClosed()) {
                packet.setLength(RECEIVE_BUFFER.length - 28);
                m_Client.receive(packet);

                if (ProxyConfig.IS_DEBUG){
                    System.out.printf("Udp receive %s:%d=>%s:%d\n",
                            CommonMethods.ipIntToInet4Address(ipHeader.getSourceIP()), udpHeader.getSourcePort() & 0xffff,
                            CommonMethods.ipIntToInet4Address(ipHeader.getDestinationIP()), udpHeader.getDestinationPort() & 0xffff);
                }
                try {
                    onUdpResponseReceived(ipHeader, udpHeader, packet);
                } catch (Exception e) {
                    e.printStackTrace();
                    LocalVpnService.Instance.writeLog("Error: Udp receive error: %s", e);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("UdpProxy Thread Exited.");
            this.stop();
        }
    }

    private void onUdpResponseReceived(IPHeader ipHeader, UDPHeader udpHeader, DatagramPacket packet) {
        UdpSrcDesMapping mapping = null;
        synchronized (mSrcDesMap) {
            mapping = mSrcDesMap.get(packet.getSocketAddress());
        }

        if (mapping != null) {
            ipHeader.setSourceIP(mapping.RemoteIP);
            ipHeader.setDestinationIP(mapping.ClientIP);
            ipHeader.setProtocol(IPHeader.UDP);
            ipHeader.setTotalLength(20 + 8 + packet.getLength());
            udpHeader.setSourcePort(mapping.RemotePort);
            udpHeader.setDestinationPort(mapping.ClientPort);
            udpHeader.setTotalLength(8 + packet.getLength());
            LocalVpnService.Instance.sendUDPPacket(ipHeader, udpHeader);
        }
    }

    public void onUdpRequestReceived(IPHeader ipHeader, UDPHeader udpHeader) {
        int port = udpHeader.getDestinationPort() & 0xffff;
        InetSocketAddress remoteAddress = null;
        try{
            remoteAddress = new InetSocketAddress(CommonMethods.ipIntToInet4Address(ipHeader.getDestinationIP()), port);
        } catch (Exception e) {
            LocalVpnService.Instance.writeLog("Error: udp InetSocketAddress failed: %s", e);
        }
        if (remoteAddress != null) {
            UdpSrcDesMapping mapping = mSrcDesMap.get(remoteAddress);
            if (mapping == null) {
                mapping = new UdpSrcDesMapping();
            }
            mapping.ClientIP = ipHeader.getSourceIP();
            mapping.ClientPort = udpHeader.getSourcePort();
            mapping.RemoteIP = ipHeader.getDestinationIP();
            mapping.RemotePort = udpHeader.getDestinationPort();
            mapping.pCreateTime = System.currentTimeMillis();

            long expiredTime = System.currentTimeMillis() - mTimeOut;
            synchronized (mSrcDesMap) {
                mSrcDesMap.put(remoteAddress, mapping);// 关联数据
            }
            if (mSrcDesMap.size() > 100 &&  expiredTime > mStartTime) {
                clearExpiredMappings(expiredTime);
                mStartTime = System.currentTimeMillis();
            }

            if (ProxyConfig.IS_DEBUG){
                System.out.printf("Udp send %s:%d=>%s:%d\n",
                        CommonMethods.ipIntToInet4Address(mapping.ClientIP), mapping.ClientPort & 0xffff,
                        CommonMethods.ipIntToInet4Address(mapping.RemoteIP), mapping.RemotePort & 0xffff);
            }
            DatagramPacket packet = new DatagramPacket(udpHeader.m_Data, udpHeader.m_Offset + 8, udpHeader.getTotalLength());
            packet.setSocketAddress(remoteAddress);

            try {
                if (LocalVpnService.Instance.protect(m_Client)) {
                    m_Client.send(packet);
                    if (ProxyConfig.IS_DEBUG) {
                        System.out.printf("UdpProxy client %d\n", Port);
                    }
                } else {
                    System.err.println("VPN protect udp socket failed.");
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                LocalVpnService.Instance.writeLog("Error: udp send failed: %s", e);
            }
        }
    }

    public Thread getThread(){
        return m_ReceivedThread;
    }

    private void clearExpiredMappings(long expiredTime) {
        synchronized (mSrcDesMap) {
            for(int i = mSrcDesMap.size() - 1; i >= 0; i--) {
                UdpSrcDesMapping mapping = mSrcDesMap.valueAt(i);
                if (mapping != null && mapping.pCreateTime < expiredTime) {
                    mSrcDesMap.removeAt(i);
                }
            }
        }
    }
}
