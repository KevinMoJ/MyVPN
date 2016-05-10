package yyf.shadowsocks.service;


import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.*;
import android.util.Log;

import com.androapplite.shadowsocks.BuildConfig;
import com.androapplite.shadowsocks.R;
import com.androapplite.shadowsocks.ShadowsocksApplication;

import org.apache.http.conn.util.InetAddressUtils;
import org.xbill.DNS.AAAARecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import yyf.shadowsocks.Config;
import yyf.shadowsocks.jni.*;
import yyf.shadowsocks.jni.System;
import yyf.shadowsocks.utils.ConfigUtils;
import yyf.shadowsocks.utils.Console;
import yyf.shadowsocks.utils.Constants;
import yyf.shadowsocks.utils.TrafficMonitor;
import yyf.shadowsocks.utils.TrafficMonitorThread;

/**
 * Created by yyf on 2015/6/18.
 */
public class ShadowsocksVpnService extends BaseService {

    private static final String TAG = "ShadowsocksVpnService";
    private static final int VPN_MTU = 1500;
    private static final String PRIVATE_VLAN = "25.25.25.%s";

    private ParcelFileDescriptor conn = null;
    private NotificationManager notificationManager = null;
    private BroadcastReceiver receiver = null;
    private ShadowsocksVpnThread mShadowsocksVpnThread;


    //Array<ProxiedApp> apps = null; 功能去掉...


    //private ShadowsocksApplication application = This.getApplication();//<ShadowsocksApplication>;
    boolean isByass() {
        //       val info = net.getInfo;
        //     info.isInRange(config.proxy);
        //TODO :完成此函数  参数 SubnetUtils net
        return false;
    }

    boolean isPrivateA(int a) {
        if (a == 10 || a == 192 || a == 172) {
            return true;
        } else {
            return false;
        }
    }

    boolean isPrivateB(int a, int b) {
        if (a == 10 || (a == 192 && b == 168) || (a == 172 && b >= 16 && b < 32)) {
            return true;
        } else {
            return false;
        }
    }

    public void startShadowsocksDaemon() {
        //ACL 写入文件
        //String[] acl =  getResources().getStringArray(R.array.private_route);
        String[] acl =  getResources().getStringArray(R.array.chn_route_full);
        PrintWriter printWriter = ConfigUtils.printToFile(new File(Constants.Path.BASE + "acl.list"));
        for (int i = 0; i < acl.length; i++)
            printWriter.println(acl[i]);
        printWriter.close();

        //读取配置并写入文件
        String conf = String.format(Locale.ENGLISH,ConfigUtils.SHADOWSOCKS,config.proxy, config.remotePort, config.localPort,
                config.sitekey, config.encMethod, 10);
        printWriter =ConfigUtils.printToFile(new File(Constants.Path.BASE + "ss-local-vpn.conf"));
        printWriter.println(conf);
        printWriter.close();

        //执行命令build
        String[] cmd = {
                Constants.Path.BASE + "ss-local", "-u",
                "-v",
                "-V",
//                "-A",
                "-P", Constants.Path.BASE,
                "-b", "127.0.0.1",
                "-t", "600", "-c",
                Constants.Path.BASE + "ss-local-vpn.conf", "-f ",
                Constants.Path.BASE + "ss-local-vpn.pid"
        };
        //加入 acl
        List<String> list = new ArrayList<>(Arrays.asList(cmd));
        list.add("--acl");
        list.add(Constants.Path.BASE + "acl.list");
        cmd = list.toArray(new String[0]);


        if (BuildConfig.DEBUG)
            Log.d(TAG, cmd.toString());
        //Log.d(TAG, cmd.mkString(" "));
        Console.runCommand(Console.mkCMD(cmd));
        ShadowsocksApplication.debug("ss-vpn", Console.mkCMD(cmd));
    }

    public void startDnsTunnel() {
        //读取配置 并写入文件
        String conf = String.format(Locale.ENGLISH,ConfigUtils
                .SHADOWSOCKS,config.proxy, config.remotePort, 8163,
                        config.sitekey, config.encMethod, 10);

        PrintWriter printWriter = ConfigUtils.printToFile(new File(Constants.Path.BASE + "ss-tunnel-vpn.conf"));
        printWriter.println(conf);
        printWriter.close();
        ShadowsocksApplication.debug("ss-vpn", "DnsTunnel:write to file");
        String[] cmd = {
                Constants.Path.BASE + "ss-tunnel"
                , "-v"
                , "-V"
                , "-u"
//                , "-A"
                , "-t", "10"
                , "-b", "127.0.0.1"
                , "-l", "8163"
                , "-L", "8.8.8.8:53"
                , "-P", Constants.Path.BASE
                , "-c", Constants.Path.BASE + "ss-tunnel-vpn.conf"
                , "-f", Constants.Path.BASE + "ss-tunnel-vpn.pid"};
        //执行
        if (BuildConfig.DEBUG)
            Log.d(TAG, Console.mkCMD(cmd));
        //Log.d(TAG, cmd.mkString(" "))
        Console.runCommand(Console.mkCMD(cmd));
        ShadowsocksApplication.debug("ss-vpn", "start DnsTun");
        ShadowsocksApplication.debug("ss-vpn", Console.mkCMD(cmd));
    }

    public void startDnsDaemon() {
        String reject = getResources().getString(R.string.reject);
        String blackList = getResources().getString(R.string.black_list);

        String conf = String.format(Locale.ENGLISH, ConfigUtils.PDNSD_DIRECT, "0.0.0.0", 8153,
                Constants.Path.BASE + "pdnsd-vpn.pid", reject, blackList, 8163);
        ShadowsocksApplication.debug("ss-vpn", "DnsDaemon:config write to file");
        PrintWriter printWriter = ConfigUtils.printToFile(new File(Constants.Path.BASE + "pdnsd-vpn.conf"));
        printWriter.println(conf);
        printWriter.close();

        String cmd = Constants.Path.BASE + "pdnsd -c " + Constants.Path.BASE + "pdnsd-vpn.conf";

        if (BuildConfig.DEBUG)
            Log.d(TAG, cmd);
        Console.runCommand(cmd);
        ShadowsocksApplication.debug("ss-vpn", "start DnsDaemon");
        ShadowsocksApplication.debug("ss-vpn", cmd);
    }


    String getVersionName(){
        String version = null;
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pi.versionName;

        } catch (PackageManager.NameNotFoundException e){
            version = "Package name not found";

        }
        return version;
    }

    void startVpn() throws PackageManager.NameNotFoundException {

        Builder builder = new Builder();
        String str = String.format(Locale.ENGLISH, PRIVATE_VLAN, "1");
        builder
                .setSession(config.profileName)
                .setMtu(VPN_MTU)
                .addAddress(str, 24)
                .addDnsServer("8.8.8.8");
        ShadowsocksApplication.debug("ss-vpn", "startRealVpn!!!!");
        if (ConfigUtils.isLollipopOrAbove()) {
            builder.allowFamily(android.system.OsConstants.AF_INET6);
            builder.addDisallowedApplication(this.getPackageName());
            //TODO 利用 builder.addDisallowedApplication 实现app不走vpn
        }

        /*if (InetAddressUtils.isIPv6Address(config.proxy)) {
            builder.addRoute("0.0.0.0", 0);
        }  TODO 添加 ipv6 支持 */





        String  list[];
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            list = getResources().getStringArray(R.array.simple_route);
        } else {
            list = getResources().getStringArray(R.array.gfw_route);
        }

        builder.addRoute("8.8.0.0", 16);
        builder.addRoute("0.0.0.0", 0);

//        for(int i = 0;i < list.length;i++) {
//            String [] addr = list[i].split("/");
//            builder.addRoute(addr[0],Integer.valueOf(addr[1]));
//        }

        //TODO 5.0以上

        try {
            conn = builder.establish();
        } catch (IllegalStateException e){
            ShadowsocksApplication.handleException(e);
            changeState(Constants.State.STOPPED, e.getMessage());
            conn = null;
        }

        if (conn == null) {
            stopRunner();
            return;
        }

        int fd = conn.getFd();

        String cmd = String.format(Locale.ENGLISH,
                Constants.Path.BASE +
                        "tun2socks --netif-ipaddr %s "
                        + "--netif-netmask 255.255.255.0 "
                        + "--socks-server-addr 127.0.0.1:%d "
                        + "--tunfd %d "
                        + "--tunmtu %d "
                        + "--loglevel 3 "
                        + "--pid %stun2socks-vpn.pid "
                        + "--sock-path %ssock_path "
                        + "--logger stdout",
                String.format(Locale.ENGLISH,PRIVATE_VLAN, "2"), config.localPort, fd, VPN_MTU, Constants.Path.BASE, Constants.Path.BASE);

        //if (config.isUdpDns)
//            cmd += " --enable-udprelay";
        //else
            cmd += String.format(Locale.ENGLISH," --dnsgw %s:8153", String.format(Locale.ENGLISH,PRIVATE_VLAN,"1"));

       // if (ConfigUtils.isLollipopOrAbove()) {
            //cmd += " --fake-proc";
      //  }

        if (BuildConfig.DEBUG)
            Log.d(TAG, cmd);

        Console.runCommand(cmd);
        sendFd(fd);
//        yyf.shadowsocks.jni.System.exec(cmd);
    }

    private boolean sendFd(int fd) {
        if (fd != -1) {
            int tries = 1;
            while (tries < 5) {
                try {
                    Thread.sleep(1000 * tries);
                } catch (InterruptedException e) {
                    ShadowsocksApplication.handleException(e);
                }
                if (System.sendfd(fd, Constants.Path.BASE + "sock_path") != -1) {
                    return true;
                }
                tries += 1;
            }
        }
        return false;
    }

    /**
     * Called when the activity is first created.
     */

    @Override
    public IBinder onBind(Intent intent) {
        ShadowsocksApplication.debug("ss-vpn", "onBind");
        String action = intent.getAction();
        if (VpnService.SERVICE_INTERFACE == action) {
            return super.onBind(intent);
        } else if (Constants.Action.SERVICE == action) {
            ShadowsocksApplication.debug("ss-vpn", "getBinder");
            return binder;
        }
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ShadowsocksApplication.debug("ss-vpn", "onCreate");

    }


    @Override
    public void onRevoke() {
        stopRunner();
        ShadowsocksApplication.debug("ss-vpn", "onRevoke");
    }

    public void killProcesses() {
        String[] tasks = {"ss-local", "ss-tunnel", "pdnsd", "tun2socks"};
        for (String task : tasks) {
            File f = new File(Constants.Path.BASE + task + "-vpn.pid");
            if(f.exists()){
                FileReader fr = null;
                BufferedReader br = null;
                try {
                    fr = new FileReader(f);
                    br = new BufferedReader(fr);
                    String line = br.readLine();
                    if(line != null && !line.isEmpty()) {
                        Integer pid = Integer.valueOf(line);
                        if(pid != null)
                            android.os.Process.killProcess(pid);
                    }
                    br.close();
                    br = null;
                    fr.close();
                    fr = null;

                } catch (Exception e) {
                    ShadowsocksApplication.handleException(e);
                    if(br != null){
                        try {
                            br.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                    if(fr != null){
                        try {
                            fr.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            }
        }
    }


    @Override
    public void stopBackgroundService() {
        stopSelf();
    }

    @Override
    public void startRunner(Config c) {
        ShadowsocksApplication.debug("ss-vpn", "startRunner");
        super.startRunner(c);
        mShadowsocksVpnThread = new ShadowsocksVpnThread(this);
        mShadowsocksVpnThread.start();

        changeState(Constants.State.CONNECTING);
        if (config != null) {
            // reset the context
            killProcesses();
            // Resolve the server address TODO:增加IPV6的支持
            boolean resolved = false;
            if (!InetAddressUtils.isIPv4Address(config.proxy)) {
                if(resolve(config.proxy, Type.A).isEmpty()){
                    if(!resolve(config.proxy).isEmpty()) {
                        resolve(config.proxy);
                        resolved = true;
                    }
                }else{
                    config.proxy = resolve(config.proxy, Type.A);
                    resolved = true;
                }
            } else {
                resolved = true;
            }

            ShadowsocksApplication.debug("ss-vpn", "resolved:" + resolved);
            if (resolved && handleConnection()) {
                changeState(Constants.State.CONNECTED);
            } else {
                changeState(Constants.State.STOPPED);
                stopRunner();
            }
        }
    }

    public boolean handleConnection() {
        ShadowsocksApplication.debug("ss-vpn", "handleConnection");
        startShadowsocksDaemon();
        startDnsDaemon();
        startDnsTunnel();
        try {
            startVpn();
        }catch(PackageManager.NameNotFoundException e){
            ShadowsocksApplication.handleException(e);
        }
        return true;
    }

    @Override
    public void stopRunner() {
        // channge the state
        changeState(Constants.State.STOPPING);
        // send event
//        application.tracker.send(new EventBuilder()
//                .setCategory(TAG)
//                .setAction("stop")
//                .setLabel(getVersionName())
//                .build());
        // reset VPN
        killProcesses();
        if(mShadowsocksVpnThread != null){
            mShadowsocksVpnThread.stopThread();
            mShadowsocksVpnThread = null;
        }
        // close connections
        if (conn != null) {
            try {
                conn.close();
                conn = null;
            }catch(IOException e){
                ShadowsocksApplication.handleException(e);
            }
        }
        // stop the service if no callback registered
        if (getCallbacksCount() == 0) {
            stopSelf();
        }
        // clean up the context
        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
        // channge the state
        changeState(Constants.State.STOPPED);
        super.stopRunner();
    }


    @Override
    public Constants.Mode getServiceMode() {
        return Constants.Mode.VPN;
    }

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public Context getContext() {
        return getBaseContext();
    }
    String resolve(String host,int addrType){
        try {
            Lookup lookup = new Lookup(host, addrType);
            SimpleResolver resolver = new SimpleResolver("114.114.114.114");
            resolver.setTimeout(5);
            lookup.setResolver(resolver);
            Record[] result = lookup.run();
            if (result == null)
                return null;
            for (Record r : result) {
                switch(addrType) {
                    case Type.A :
                        return ((ARecord)r).getAddress().getHostAddress();
                    case Type.AAAA :
                        return ((AAAARecord)r).getAddress().getHostAddress();
                }
            }
        } catch(java.net.UnknownHostException e){
            ShadowsocksApplication.handleException(e);
        } catch (org.xbill.DNS.TextParseException e){
            ShadowsocksApplication.handleException(e);
        }
        return null;
    }

    String resolve(String host){
        try {
            InetAddress addr = InetAddress.getByName(host);
            return addr.getHostAddress();
        } catch (java.net.UnknownHostException e){
            ShadowsocksApplication.handleException(e);
        }
        return null;
    }
}
