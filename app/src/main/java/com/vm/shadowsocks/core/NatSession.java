package com.vm.shadowsocks.core;

public class NatSession {
    public int RemoteIP;
    public short RemotePort;
    public String RemoteHost;
    public int BytesSent;
    public int PacketSent;
    public long LastNanoTime;
    public int RemoteRealIP;
    public byte IsSelfPort;
//    public byte SendTactics; //=0：初始化；=1：直连；=2：vpn连
}
