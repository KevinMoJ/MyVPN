package com.vm.shadowsocks.core;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.activity.MainActivity;
import com.androapplite.shadowsocks.utils.InternetUtil;
import com.androapplite.shadowsocks.utils.RealTimeLogger;
import com.androapplite.vpn3.R;
import com.crashlytics.android.Crashlytics;
import com.vm.shadowsocks.core.ProxyConfig.IPAddress;
import com.vm.shadowsocks.dns.DnsPacket;
import com.vm.shadowsocks.tcpip.CommonMethods;
import com.vm.shadowsocks.tcpip.IPHeader;
import com.vm.shadowsocks.tcpip.TCPHeader;
import com.vm.shadowsocks.tcpip.UDPHeader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class LocalVpnService extends VpnService implements Runnable {

    public static LocalVpnService Instance;
    public static String ProxyUrl;
    public static volatile boolean IsRunning = false;

    private static int ID;
    private static int LOCAL_IP;
    private static ConcurrentHashMap<onStatusChangedListener, Object> m_OnStatusChangedListeners = new ConcurrentHashMap<onStatusChangedListener, Object>();

    private volatile Thread m_VPNThread;
    private ParcelFileDescriptor m_VPNInterface;
    private volatile TcpProxyServer m_TcpProxyServer;
    private volatile DnsProxy m_DnsProxy;
    private FileOutputStream m_VPNOutputStream;

    private byte[] m_Packet;
    private IPHeader m_IPHeader;
    private TCPHeader m_TCPHeader;
    private UDPHeader m_UDPHeader;
    private ByteBuffer m_DNSBuffer;
    private Handler m_Handler;
    private volatile TcpTrafficMonitor mTcpTrafficMonitor;
    private volatile StatusGuard mStatusGuard;
    private volatile ScheduledExecutorService mScheduleExecutorService;
    private VpnNotification mNotification;
    private volatile UdpProxy mUdpProxy;
    public long gDelay;

    public LocalVpnService() {
        ID++;
        m_Handler = new Handler();
        m_Packet = new byte[20000];
        m_IPHeader = new IPHeader(m_Packet, 0);
        m_TCPHeader = new TCPHeader(m_Packet, 20);
        m_UDPHeader = new UDPHeader(m_Packet, 20);
        m_DNSBuffer = ((ByteBuffer) ByteBuffer.wrap(m_Packet).position(28)).slice();
        Instance = this;

        System.out.printf("New VPNService(%d)\n", ID);
    }

    @Override
    public void onCreate() {
        System.out.printf("VPNService(%s) created.\n", ID);
        // Start a new session by creating a new thread.
        m_VPNThread = new Thread(this, "VPNServiceThread");
        m_VPNThread.start();
        super.onCreate();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        IsRunning = true;
        return super.onStartCommand(intent, flags, startId);
    }

    public interface onStatusChangedListener {
        void onStatusChanged(String status, Boolean isRunning);
        void onLogReceived(String logString);
        void onTrafficUpdated(@Nullable TcpTrafficMonitor tcpTrafficMonitor);
    }

    public static void addOnStatusChangedListener(final onStatusChangedListener listener) {
        if (!m_OnStatusChangedListeners.containsKey(listener)) {
            m_OnStatusChangedListeners.put(listener, 1);
        }
    }

    public static void removeOnStatusChangedListener(onStatusChangedListener listener) {
        if (m_OnStatusChangedListeners.containsKey(listener)) {
            m_OnStatusChangedListeners.remove(listener);
        }
    }

    private void onStatusChanged(final String status, final boolean isRunning) {
        m_Handler.post(new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<onStatusChangedListener, Object> entry : m_OnStatusChangedListeners.entrySet()) {
                    entry.getKey().onStatusChanged(status, isRunning);
                }
            }
        });
    }

    public void writeLog(final String format, Object... args) {
        final String logString = String.format(format, args);
        if (ProxyConfig.IS_DEBUG) {
            System.out.println(logString);
        }

        try {
            for (Object o : args) {
                if (o instanceof Throwable && IsRunning) {
                    Throwable throwable = (Throwable) o;
                    ShadowsocksApplication.handleException(throwable);
                    Crashlytics.logException(throwable);
                    RealTimeLogger.getInstance(this).logEventAsync("error", "error_type", ((Exception) throwable).getMessage()
                            , "net_type", InternetUtil.getNetworkState(this));
                    Firebase.getInstance(Instance).logEvent("Error", throwable.getMessage());
                }
            }
        } catch (Exception e) {}
        if (mStatusGuard != null) {
            Map<String, Integer> errors = mStatusGuard.getErrors();
            if (format.startsWith("Error:")) {
                if (format.startsWith("Error: read buffer")) {
                    args[0] = "";
                    String logString2 = String.format(format, args);
                    Integer count = errors.get(logString2);
                    if (count == null) {
                        errors.put(logString2, 1);
                    } else {
                        errors.put(logString2, ++count);
                    }
                } else {
                    Integer count = errors.get(logString);
                    if (count == null) {
                        errors.put(logString, 1);
                    } else {
                        errors.put(logString, ++count);
                    }
                }
            }
        }

        m_Handler.post(new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<onStatusChangedListener, Object> entry : m_OnStatusChangedListeners.entrySet()) {
                    entry.getKey().onLogReceived(logString);
                }
            }
        });
    }

    public void sendUDPPacket(IPHeader ipHeader, UDPHeader udpHeader) {
        try {
            CommonMethods.ComputeUDPChecksum(ipHeader, udpHeader);
            this.m_VPNOutputStream.write(ipHeader.m_Data, ipHeader.m_Offset, ipHeader.getTotalLength());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    String getAppInstallID() {
        SharedPreferences preferences = getSharedPreferences("SmartProxy", MODE_PRIVATE);
        String appInstallID = preferences.getString("AppInstallID", null);
        if (appInstallID == null || appInstallID.isEmpty()) {
            appInstallID = UUID.randomUUID().toString();
            Editor editor = preferences.edit();
            editor.putString("AppInstallID", appInstallID);
            editor.apply();
        }
        return appInstallID;
    }

    String getVersionName() {
        try {
            PackageManager packageManager = getPackageManager();
            // getPackageName()是你当前类的包名，0代表是获取版本信息
            PackageInfo packInfo = packageManager.getPackageInfo(getPackageName(), 0);
            String version = packInfo.versionName;
            return version;
        } catch (Exception e) {
            return "0.0";
        }
    }

    @Override
    public synchronized void run() {
        try {
            System.out.printf("VPNService(%s) work thread is runing...\n", ID);

            ProxyConfig.AppInstallID = getAppInstallID();//获取安装ID
            ProxyConfig.AppVersion = getVersionName();//获取版本号
            System.out.printf("AppInstallID: %s\n", ProxyConfig.AppInstallID);
            writeLog("Android version: %s", Build.VERSION.RELEASE);
            writeLog("App version: %s", ProxyConfig.AppVersion);


            ChinaIpMaskManager.loadFromFile(getResources().openRawResource(R.raw.ipmask));//加载中国的IP段，用于IP分流。
            waitUntilPreapred();//检查是否准备完毕。

            writeLog("Load config from file ...");
            try {
                ProxyConfig.Instance.loadFromFile(getResources().openRawResource(R.raw.config));
                writeLog("Load done");
            } catch (Exception e) {
                String errString = e.getMessage();
                if (errString == null || errString.isEmpty()) {
                    errString = e.toString();
                }
                writeLog("Error: Load failed with error: %s", errString);
            }

            if (ProxyConfig.IS_DEBUG) {
                ProxyConfig.Instance.addDomainToHashMap("iplocation.net", true);
            }

            m_TcpProxyServer = new TcpProxyServerBypassSelf(0);
            m_TcpProxyServer.start();
            writeLog("LocalTcpServer started.");

//            m_DnsProxy = new DnsProxy();
            m_DnsProxy = new DnsFullQueryProxy();
            m_DnsProxy.start();
            writeLog("LocalDnsProxy started.");

            mUdpProxy = new UdpProxy();
            mUdpProxy.start();
            writeLog("LocalUdpProxy started.");

            startNotification();

            while (true) {
                if (IsRunning) {
                    //加载配置文件
                    writeLog("set shadowsocks/(http proxy)");
                    try {
                        ProxyConfig.Instance.m_ProxyList.clear();
//                        ProxyConfig.Instance.addProxyToList(ProxyUrl);
                        ProxyConfig.Instance.addDefaultProxy(ProxyUrl);
                        writeLog("Proxy is: %s", ProxyConfig.Instance.getDefaultProxy());
                    } catch (Exception e) {
                        String errString = e.getMessage();
                        if (errString == null || errString.isEmpty()) {
                            errString = e.toString();
                        }
                        IsRunning = false;
                        onStatusChanged(errString, false);
                        continue;
                    }
                    String welcomeInfoString = ProxyConfig.Instance.getWelcomeInfo();
                    if (welcomeInfoString != null && !welcomeInfoString.isEmpty()) {
                        writeLog("%s", ProxyConfig.Instance.getWelcomeInfo());
                    }
                    writeLog("Global mode is " + (ProxyConfig.Instance.globalMode ? "on" : "off"));

                    //启动TcpTrafficMonitor和StatusGuard
                    startSchedule();
                    mNotification.showVpnStartedNotification();
                    runVPN();
                    //停止定时服务和通知
                    stopSchedule();
                } else {
                    Thread.sleep(100);
                }
            }
        } catch (InterruptedException e) {
            writeLog("Error: Interrupt error: %s", e);
        } catch (Exception e) {
            writeLog("Error: Fatal error: %s", e);
        } finally {
            writeLog("App terminated.");
            dispose();
        }
    }

    private void stopNotification() {
        if (mNotification != null) {
//            mNotification.dismissNotification();
            removeOnStatusChangedListener(mNotification);
            mNotification = null;
        }
    }

    private void stopSchedule() {
        if (mScheduleExecutorService != null) {
            mScheduleExecutorService.shutdown();
            mScheduleExecutorService = null;
            mStatusGuard = null;
            mTcpTrafficMonitor.unscheduleAndUnregisterReceiver(this);
            mTcpTrafficMonitor = null;
        }
    }

    private void startNotification() {
        if (mNotification == null) {
            mNotification = new VpnNotification(this);
        }
        addOnStatusChangedListener(mNotification);
    }

    private void startSchedule() {
        if (mScheduleExecutorService == null) {
            mScheduleExecutorService = Executors.newSingleThreadScheduledExecutor();
            mTcpTrafficMonitor = new TcpTrafficMonitor(mScheduleExecutorService);
            mTcpTrafficMonitor.scheduleAndRegisterReceiver(this);
            mStatusGuard = new StatusGuard(this, mScheduleExecutorService);
        }
    }

    private void runVPN() throws Exception {
        this.m_VPNInterface = establishVPN();
        this.m_VPNOutputStream = new FileOutputStream(m_VPNInterface.getFileDescriptor());
        FileInputStream in = new FileInputStream(m_VPNInterface.getFileDescriptor());
        int size = 0;
        while (size != -1 && IsRunning) {
            while ((size = in.read(m_Packet)) > 0 && IsRunning) {
                if (m_DnsProxy.Stopped || m_TcpProxyServer.Stopped || mUdpProxy.Stopped) {
                    in.close();
                    throw new Exception("LocalServer stopped.");
                }
                onIPPacketReceived(m_IPHeader, size);
            }
            Thread.sleep(20);
        }
        in.close();
        disconnectVPN();
    }

    void onIPPacketReceived(IPHeader ipHeader, int size) throws IOException {
        int sourceIp = ipHeader.getSourceIP();
        int destinationIP = ipHeader.getDestinationIP();
        if (ProxyConfig.IS_DEBUG){
            System.out.printf("Ip %s=>%s protocol %d\n",
                    CommonMethods.ipIntToInet4Address(sourceIp),
                    CommonMethods.ipIntToInet4Address(destinationIP),
                    ipHeader.getProtocol());
        }
        switch (ipHeader.getProtocol()) {
            case IPHeader.TCP:
                if (sourceIp == LOCAL_IP) {
                    TCPHeader tcpHeader = m_TCPHeader;
                    tcpHeader.m_Offset = ipHeader.getHeaderLength();
                    short sourcePort = tcpHeader.getSourcePort();
                    short destinationPort = tcpHeader.getDestinationPort();
                    if (sourcePort == m_TcpProxyServer.Port) {// 收到本地TCP服务器数据
                        NatSession session = NatSessionManager.getSession(destinationPort);
                        if (session != null) {
                            if (ProxyConfig.IS_DEBUG) {
                                System.out.printf("onIPPacketReceived 3 %s:%d=>%s:%d, host: %s\n",
                                        CommonMethods.ipIntToString(ipHeader.getSourceIP()), tcpHeader.getSourcePort() & 0xffff,
                                        CommonMethods.ipIntToString(ipHeader.getDestinationIP()), tcpHeader.getDestinationPort() & 0xffff,
                                        session.RemoteHost);
                            }

                            if (session.IsSelfPort == 1) {
                                destinationIP = session.RemoteIP;
                                if (ProxyConfig.IS_DEBUG) {
                                    System.out.printf("onIPPacketReceived 3 vpn自己 real ip %s, fake id %s\n",
                                            CommonMethods.ipIntToString(session.RemoteRealIP), CommonMethods.ipIntToString(session.RemoteIP));
                                }
                            }

                            ipHeader.setSourceIP(destinationIP);
                            tcpHeader.setSourcePort(session.RemotePort);
                            ipHeader.setDestinationIP(LOCAL_IP);

                            if (ProxyConfig.IS_DEBUG) {
                                System.out.printf("onIPPacketReceived 4 %s:%d=>%s:%d, host: %s, size: %d, tcp size: %d, ip size: %d\n",
                                        CommonMethods.ipIntToString(ipHeader.getSourceIP()), tcpHeader.getSourcePort() & 0xffff,
                                        CommonMethods.ipIntToString(ipHeader.getDestinationIP()), tcpHeader.getDestinationPort() & 0xffff,
                                        session.RemoteHost, size, tcpHeader.getHeaderLength(), ipHeader.getDataLength());
                            }

                            CommonMethods.ComputeTCPChecksum(ipHeader, tcpHeader);
                            m_VPNOutputStream.write(ipHeader.m_Data, ipHeader.m_Offset, size);
                            mTcpTrafficMonitor.updateTrafficReceive(ipHeader, tcpHeader, size);
                        } else {
                            System.out.printf("NoSession: %s %s\n", ipHeader.toString(), tcpHeader.toString());
                        }
                    } else {
                        // 添加端口映射
                        NatSession session = NatSessionManager.getSession(sourcePort);
                        if (session == null || session.RemoteIP != destinationIP || session.RemotePort != destinationPort) {
                            session = NatSessionManager.createSession(sourcePort, destinationIP, destinationPort);
                        }

                        if (ProxyConfig.IS_DEBUG) {
                            System.out.printf("onIPPacketReceived 1 portKey: %d, %s:%d=>%s:%d, packetSent: %d, remote: %s\n",
                                    sourcePort & 0xffff,
                                    CommonMethods.ipIntToString(ipHeader.getSourceIP()), tcpHeader.getSourcePort() & 0xffff,
                                    CommonMethods.ipIntToString(ipHeader.getDestinationIP()),  tcpHeader.getDestinationPort() & 0xffff,
                                    session.PacketSent, session.RemoteHost != null ? session.RemoteHost : "null");
                        }

                        session.LastNanoTime = System.nanoTime();
                        session.PacketSent++;//注意顺序

                        switch (session.IsSelfPort) {
                            case 0:
                                //vpn自己不走代理,把fake IP改为real IP
                                if (ProxyConfig.isFakeIP(destinationIP) && isSelfTcpPort(sourcePort)) {
                                    session.RemoteRealIP = m_DnsProxy.translateToRealIp(destinationIP);
                                    if (ProxyConfig.IS_DEBUG) {
                                        System.out.printf("onIPPacketReceived 1 vpn自己 real ip %s, fake id %s\n",
                                                CommonMethods.ipIntToString(session.RemoteRealIP), CommonMethods.ipIntToString(session.RemoteIP));
                                    }
                                    session.IsSelfPort = 1;
                                } else {
                                    session.IsSelfPort = -1;
                                }
                            case 1:
                                destinationIP = session.RemoteRealIP;
                                break;
                        }

                        int tcpDataSize = ipHeader.getDataLength() - tcpHeader.getHeaderLength();
                        if (session.PacketSent == 2 && tcpDataSize == 0) {
                            return;//丢弃tcp握手的第二个ACK报文。因为客户端发数据的时候也会带上ACK，这样可以在服务器Accept之前分析出HOST信息。
                        }

                        //分析数据，找到host
                        if (session.BytesSent == 0 && tcpDataSize > 10) {
                            int dataOffset = tcpHeader.m_Offset + tcpHeader.getHeaderLength();
                            String host = HttpHostHeaderParser.parseHost(tcpHeader.m_Data, dataOffset, tcpDataSize);
                            if (host != null) {
                                session.RemoteHost = host;
                            } else {
                                System.out.printf("No host name found: %s\n", session.RemoteHost);
                            }
                        }

                        // 转发给本地TCP服务器
                        ipHeader.setSourceIP(destinationIP);
                        ipHeader.setDestinationIP(LOCAL_IP);
                        tcpHeader.setDestinationPort(m_TcpProxyServer.Port);

                        if (ProxyConfig.IS_DEBUG) {
                            System.out.printf("onIPPacketReceived 2 portKey: %d, %s:%d=>%s:%d, packetSent: %d, host: %s, size: %d, tcp size: %d, ip size: %d\n",
                                    sourcePort & 0xffff,
                                    CommonMethods.ipIntToString(ipHeader.getSourceIP()), tcpHeader.getSourcePort() & 0xffff,
                                    CommonMethods.ipIntToString(ipHeader.getDestinationIP()), tcpHeader.getDestinationPort() & 0xffff,
                                    session.PacketSent, session.RemoteHost, size, tcpHeader.getHeaderLength(),
                                    ipHeader.getDataLength());
                        }

                        CommonMethods.ComputeTCPChecksum(ipHeader, tcpHeader);
                        m_VPNOutputStream.write(ipHeader.m_Data, ipHeader.m_Offset, size);
                        session.BytesSent += tcpDataSize;//注意顺序
                        mTcpTrafficMonitor.updateTrafficSend(ipHeader, tcpHeader, size);
                    }
                }
                break;
            case IPHeader.UDP:
                UDPHeader udpHeader = m_UDPHeader;
                udpHeader.m_Offset = ipHeader.getHeaderLength();
                if (sourceIp == LOCAL_IP) {
                    if (udpHeader.getDestinationPort() == 53) {
                        m_DNSBuffer.clear();
                        m_DNSBuffer.limit(ipHeader.getDataLength() - 8);
                        DnsPacket dnsPacket = DnsPacket.FromBytes(m_DNSBuffer);
                        if (dnsPacket != null && dnsPacket.Header.QuestionCount > 0) {
                            m_DnsProxy.onDnsRequestReceived(ipHeader, udpHeader, dnsPacket);
                            String question = dnsPacket.Questions[0].Domain;
                            if (ProxyConfig.Instance.needProxy(question, m_DnsProxy.getIPFromCache(question))) {
                                Firebase.getInstance(this).logEvent("DNS", "走代理", dnsPacket.Questions[0].Domain);
                            } else {
                                Firebase.getInstance(this).logEvent("DNS", "不走代理", dnsPacket.Questions[0].Domain);
                            }
                        }
                    } else {
                        mUdpProxy.onUdpRequestReceived(ipHeader, udpHeader);
                    }
                }
                break;
        }
    }

    private void waitUntilPreapred() {
        while (prepare(this) != null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private ParcelFileDescriptor establishVPN() throws Exception {
        Builder builder = new Builder();
        builder.setMtu(ProxyConfig.Instance.getMTU());
        if (ProxyConfig.IS_DEBUG)
            System.out.printf("setMtu: %d\n", ProxyConfig.Instance.getMTU());

        IPAddress ipAddress = ProxyConfig.Instance.getDefaultLocalIP();
        LOCAL_IP = CommonMethods.ipStringToInt(ipAddress.Address);
        builder.addAddress(ipAddress.Address, ipAddress.PrefixLength);
        if (ProxyConfig.IS_DEBUG)
            System.out.printf("addAddress: %s/%d\n", ipAddress.Address, ipAddress.PrefixLength);

        for (IPAddress dns : ProxyConfig.Instance.getDnsList()) {
            builder.addDnsServer(dns.Address);
            if (ProxyConfig.IS_DEBUG)
                System.out.printf("addDnsServer: %s\n", dns.Address);
        }

        if (ProxyConfig.Instance.getRouteList().size() > 0) {
            for (IPAddress routeAddress : ProxyConfig.Instance.getRouteList()) {
                builder.addRoute(routeAddress.Address, routeAddress.PrefixLength);
                if (ProxyConfig.IS_DEBUG)
                    System.out.printf("addRoute: %s/%d\n", routeAddress.Address, routeAddress.PrefixLength);
            }
            builder.addRoute(CommonMethods.ipIntToString(ProxyConfig.FAKE_NETWORK_IP), 16);

            if (ProxyConfig.IS_DEBUG)
                System.out.printf("addRoute for FAKE_NETWORK: %s/%d\n", CommonMethods.ipIntToString(ProxyConfig.FAKE_NETWORK_IP), 16);
        } else {
            builder.addRoute("0.0.0.0", 0);
            if (ProxyConfig.IS_DEBUG)
                System.out.printf("addDefaultRoute: 0.0.0.0/0\n");
        }


        Class<?> SystemProperties = Class.forName("android.os.SystemProperties");
        Method method = SystemProperties.getMethod("get", new Class[]{String.class});
        ArrayList<String> servers = new ArrayList<String>();
        for (String name : new String[]{"net.dns1", "net.dns2", "net.dns3", "net.dns4",}) {
            String value = (String) method.invoke(null, name);
            if (value != null && !"".equals(value) && !servers.contains(value)) {
                servers.add(value);
                if (value.replaceAll("\\d", "").length() == 3){//防止IPv6地址导致问题
                    builder.addRoute(value, 32);
                } else {
                    builder.addRoute(value, 128);
                }
                if (ProxyConfig.IS_DEBUG)
                    System.out.printf("%s=%s\n", name, value);
            }
        }
//        if (AppProxyManager.isLollipopOrAbove) {
//            builder.addDisallowedApplication(getPackageName());
//        }
//        if (AppProxyManager.isLollipopOrAbove){
//            if (AppProxyManager.Instance.proxyAppInfo.size() == 0){
//                writeLog("Proxy All Apps");
//            }
//            for (AppInfo app : AppProxyManager.Instance.proxyAppInfo){
////                builder.addAllowedApplication("com.vm.shadowsocks");//需要把自己加入代理，不然会无法进行网络连接
//                try{
//                    builder.addAllowedApplication(app.getPkgName());
//                    writeLog("Proxy App: " + app.getAppLabel());
//                } catch (Exception e){
//                    e.printStackTrace();
//                    writeLog("Proxy App Fail: " + app.getAppLabel());
//                }
//            }
//        } else {
//            writeLog("No Pre-App proxy, due to low Android version.");
//        }

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        builder.setConfigureIntent(pendingIntent);

        builder.setSession(ProxyConfig.Instance.getSessionName());
        ParcelFileDescriptor pfdDescriptor = builder.establish();
        onStatusChanged(ProxyConfig.Instance.getSessionName(), true);
        return pfdDescriptor;
    }

    public void disconnectVPN() {
        try {
            if (m_VPNInterface != null) {
                m_VPNInterface.close();
                m_VPNInterface = null;
            }
            onStatusChanged(ProxyConfig.Instance.getSessionName(), false);
        } catch (Exception e) {
            // ignore
        }
        this.m_VPNOutputStream = null;
    }

    private synchronized void dispose() {
        // 断开VPN
        disconnectVPN();

        // 停止TcpServer
        if (m_TcpProxyServer != null) {
            m_TcpProxyServer.stop();
            m_TcpProxyServer = null;
            writeLog("LocalTcpServer stopped.");
        }

        // 停止DNS解析器
        if (m_DnsProxy != null) {
            m_DnsProxy.stop();
            m_DnsProxy = null;
            writeLog("LocalDnsProxy stopped.");
        }

        // 停止Udp转发
        if (mUdpProxy != null) {
            mUdpProxy.stop();
            mUdpProxy = null;
            writeLog("LocalDnsProxy stopped.");
        }

        //停止定时服务和通知
        stopSchedule();
        stopNotification();

        stopSelf();
        IsRunning = false;
//        System.exit(0);
    }

    @Override
    public void onDestroy() {
        System.out.printf("VPNService(%s) destoried.\n", ID);
        if (m_VPNThread != null) {
            m_VPNThread.interrupt();
        }
        stopSchedule();
        VpnNotification.showVpnStoppedNotificationGlobe(this, true);
        System.out.println("VPNService " + IsRunning);
//        startService(new Intent(this, InterruptVpnIntentService.class));
        super.onDestroy();
    }

    public Thread getVpnThread() {
        return m_VPNThread;
    }

    public TcpProxyServer getTcpProxyServer() {
        return m_TcpProxyServer;
    }

    public DnsProxy getDnsProxy() {
        return  m_DnsProxy;
    }

    public TcpTrafficMonitor getTcpTrafficMonitor() {
        return mTcpTrafficMonitor;
    }

    public void updateTraffic() {
        m_Handler.post(new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<onStatusChangedListener, Object> entry : m_OnStatusChangedListeners.entrySet()) {
                    entry.getKey().onTrafficUpdated(mTcpTrafficMonitor);
                }
            }
        });
    }

    public UdpProxy getUdpProxy () {
        return mUdpProxy;
    }

    private boolean isSelfTcpPort(short portNumber) {
        int pid = android.os.Process.myPid();
        String tcpFilename = String.format(Locale.ENGLISH, "/proc/%d/net/tcp", pid);
        File tcpFile = new File(tcpFilename);
        Scanner scanner = null;
        String nextLine = null;
        boolean hasPort = false;
        String[] parts = null;
        String source = null;
        String uid = null;
        String port = String.format(Locale.ENGLISH, "%04x", portNumber & 0xffff);
        int puid = android.os.Process.myUid();
        String myUid = String.valueOf(puid);
        try {
            scanner = new Scanner(tcpFile);
            while (scanner.hasNextLine()) {
                nextLine = scanner.nextLine().toLowerCase().trim();
                parts = nextLine.split("\\s+");
                if (parts.length > 8) {
                    source = parts[1];
                    uid = parts[7];
                    if (source.endsWith(port) && myUid.equals(uid)) {
                        hasPort = true;
                        break;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            ShadowsocksApplication.handleException(e);
        }
        return hasPort;
    }

}
