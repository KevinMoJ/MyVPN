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

import com.androapplite.powervpn.R;
import com.androapplite.shadowsocks.activity.MainActivity;
import com.vm.shadowsocks.core.ProxyConfig.IPAddress;
import com.vm.shadowsocks.dns.DnsPacket;
import com.vm.shadowsocks.tcpip.CommonMethods;
import com.vm.shadowsocks.tcpip.IPHeader;
import com.vm.shadowsocks.tcpip.TCPHeader;
import com.vm.shadowsocks.tcpip.UDPHeader;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class LocalVpnService extends VpnService implements Runnable {

    public static LocalVpnService Instance;
    public static String ProxyUrl;
    public static boolean IsRunning = false;

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
    private ScheduledExecutorService mScheduleExecutorService;
    private VpnNotification mNotification;

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

            m_TcpProxyServer = new TcpProxyServer(0);
            m_TcpProxyServer.start();
            writeLog("LocalTcpServer started.");

            m_DnsProxy = new DnsProxy();
            m_DnsProxy.start();
            writeLog("LocalDnsProxy started.");
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
                } else {
                    Thread.sleep(100);
                    //停止定时服务和通知
                    stopSchedule();
                }
            }
        } catch (InterruptedException e) {
            writeLog("Error: Interrupt error: %s", e);
        } catch (Exception e) {
            e.printStackTrace();
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
                if (m_DnsProxy.Stopped || m_TcpProxyServer.Stopped) {
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
        switch (ipHeader.getProtocol()) {
            case IPHeader.TCP:
                TCPHeader tcpHeader = m_TCPHeader;
                tcpHeader.m_Offset = ipHeader.getHeaderLength();
                if (ipHeader.getSourceIP() == LOCAL_IP) {
                    if (tcpHeader.getSourcePort() == m_TcpProxyServer.Port) {// 收到本地TCP服务器数据
                        NatSession session = NatSessionManager.getSession(tcpHeader.getDestinationPort());
                        if (session != null) {
                            if (ProxyConfig.IS_DEBUG) {
                                System.out.printf("onIPPacketReceived 3 %s:%d=>%s:%d, host: %s\n",
                                        CommonMethods.ipIntToString(ipHeader.getSourceIP()), tcpHeader.getSourcePort() & 0xffff,
                                        CommonMethods.ipIntToString(ipHeader.getDestinationIP()), tcpHeader.getDestinationPort() & 0xffff,
                                        session.RemoteHost);
                            }
                            ipHeader.setSourceIP(ipHeader.getDestinationIP());
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
                        int portKey = tcpHeader.getSourcePort();
                        NatSession session = NatSessionManager.getSession(portKey);
                        if (session == null || session.RemoteIP != ipHeader.getDestinationIP() || session.RemotePort != tcpHeader.getDestinationPort()) {
                            session = NatSessionManager.createSession(portKey, ipHeader.getDestinationIP(), tcpHeader.getDestinationPort());
                        }

                        session.LastNanoTime = System.nanoTime();
                        session.PacketSent++;//注意顺序

                        if (ProxyConfig.IS_DEBUG) {
                            System.out.printf("onIPPacketReceived 1 portKey: %d, %s:%d=>%s:%d, packetSent: %d, remote: %s\n",
                                    portKey & 0xffff,
                                    CommonMethods.ipIntToString(ipHeader.getSourceIP()), tcpHeader.getSourcePort() & 0xffff,
                                    CommonMethods.ipIntToString(ipHeader.getDestinationIP()),  session.RemotePort & 0xffff,
                                    session.PacketSent, session.RemoteHost != null ? session.RemoteHost : "null");
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
                        ipHeader.setSourceIP(ipHeader.getDestinationIP());
                        ipHeader.setDestinationIP(LOCAL_IP);
                        tcpHeader.setDestinationPort(m_TcpProxyServer.Port);

                        if (ProxyConfig.IS_DEBUG) {
                            System.out.printf("onIPPacketReceived 2 portKey: %d, %s:%d=>%s:%d, packetSent: %d, host: %s, size: %d, tcp size: %d, ip size: %d\n",
                                    portKey & 0xffff,
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
                // 转发DNS数据包：
                UDPHeader udpHeader = m_UDPHeader;
                udpHeader.m_Offset = ipHeader.getHeaderLength();
                if (ipHeader.getSourceIP() == LOCAL_IP && udpHeader.getDestinationPort() == 53) {
                    m_DNSBuffer.clear();
                    m_DNSBuffer.limit(ipHeader.getDataLength() - 8);
                    DnsPacket dnsPacket = DnsPacket.FromBytes(m_DNSBuffer);
                    if (dnsPacket != null && dnsPacket.Header.QuestionCount > 0) {
                        m_DnsProxy.onDnsRequestReceived(ipHeader, udpHeader, dnsPacket);
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

        for (ProxyConfig.IPAddress dns : ProxyConfig.Instance.getDnsList()) {
            builder.addDnsServer(dns.Address);
            if (ProxyConfig.IS_DEBUG)
                System.out.printf("addDnsServer: %s\n", dns.Address);
        }

        if (ProxyConfig.Instance.getRouteList().size() > 0) {
            for (ProxyConfig.IPAddress routeAddress : ProxyConfig.Instance.getRouteList()) {
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
                onStatusChanged(ProxyConfig.Instance.getSessionName(), false);
            }
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

}
