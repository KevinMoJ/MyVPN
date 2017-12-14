package com.vm.shadowsocks.core;

import com.vm.shadowsocks.tcpip.CommonMethods;

import java.io.IOException;

/**
 * Created by huangjian on 2017/12/13.
 */

public class TcpProxyServerBypassSelf extends TcpProxyServer {
    public TcpProxyServerBypassSelf(int port) throws IOException {
        super(port);
    }

    @Override
    public void start() {
        super.start("TcpProxyServerBypassSelf");
    }

    protected boolean needProxy(NatSession session) {
        boolean r = session.IsSelfPort != 1 && super.needProxy(session);

        if (ProxyConfig.IS_DEBUG)
            System.out.printf("TcpProxyServerBypassSelf %s=>%s:%d, RemoteRealIP: %s, result: %b\n",
                    session.RemoteHost, CommonMethods.ipIntToString(session.RemoteIP),
                    session.RemotePort & 0xFFFF,
                    CommonMethods.ipIntToString(session.RemoteRealIP), r);
        return r;
    }
}
