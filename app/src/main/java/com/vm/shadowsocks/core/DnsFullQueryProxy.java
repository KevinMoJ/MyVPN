package com.vm.shadowsocks.core;

import android.util.SparseIntArray;

import com.vm.shadowsocks.dns.DnsPacket;
import com.vm.shadowsocks.tcpip.CommonMethods;
import com.vm.shadowsocks.tcpip.IPHeader;
import com.vm.shadowsocks.tcpip.UDPHeader;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;

/**
 * Created by huangjian on 2017/12/13.
 */

public class DnsFullQueryProxy extends DnsProxy {
    private SparseIntArray mFakeRealMapArray;
    public DnsFullQueryProxy() throws IOException {
        super();
        mFakeRealMapArray = new SparseIntArray();
    }

    @Override
    public void onDnsRequestReceived(IPHeader ipHeader, UDPHeader udpHeader, DnsPacket dnsPacket) {
        super.proxyDnsRequest(ipHeader, udpHeader, dnsPacket);
    }

    @Override
    protected void mapFakeAndRealIp(int fakeIp, int realIp) {
        synchronized (this) {
            mFakeRealMapArray.put(fakeIp, realIp);
        }
    }

    public int translateToRealIp(int fakeIp) {
        synchronized (this) {
            return mFakeRealMapArray.get(fakeIp);
        }
    }

    @Override
    public void start() {
        super.start("DnsFullQueryProxy");
    }
}
