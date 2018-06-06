package com.androapplite.shadowsocks.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.model.ServerConfig;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.androapplite.shadowsocks.service.VpnManageService;
import com.androapplite.vpn3.R;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.vm.shadowsocks.core.LocalVpnService;
import com.vm.shadowsocks.core.VpnNotification;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by Kiven.Mo on 2018/6/4.
 */

public class ConnectVpnHelper {
    private static ConnectVpnHelper Instance;
    private Context mContext;
    private SharedPreferences mSharedPreference;
    private boolean mIsFindLocalServer; //找到与服务器匹配的国家
    private boolean mIsPriorityConnect; //找到优先选择的国家


    private ConnectVpnHelper() {
    }

    public static ConnectVpnHelper getInstance(Context context) {
        if (Instance == null) {
            Instance = new ConnectVpnHelper();
            Instance.mContext = context;
        }

        if (Instance.mSharedPreference == null)
            Instance.initData();

        return Instance;
    }

    private void initData() {
        mSharedPreference = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(mContext);
    }

    public void switchProxyService() {
        if (LocalVpnService.IsRunning) {
            ServerConfig serverConfig = ServerConfig.loadFromSharedPreference(mSharedPreference);
            Firebase.getInstance(mContext).logEvent("切换代理", "开始监测");
            if (serverConfig != null) {
                try {
                    VpnManageService.stopVpnForAutoSwitchProxy();
                    VpnNotification.gSupressNotification = true;
                    ServerConfig testConfig = testServerIpAndPort(serverConfig);
                    if (testConfig == null) {
                        Log.d("FindProxyService", "old proxy " + serverConfig.server + " 联不通");
                        serverConfig = findVPNServer();
                        if (serverConfig != null) {
                            Log.d("FindProxyService", "new proxy " + serverConfig.server);
                            LocalVpnService.ProxyUrl = serverConfig.toProxyUrl();
                            LocalVpnService.IsRunning = true;
                            serverConfig.saveInSharedPreference(mSharedPreference);
                            mSharedPreference.edit().putBoolean(SharedPreferenceKey.IS_SWITCH_PROXY, true).apply();
                        } else {
                            Log.d("FindProxyService", "没有可用的proxy");
                            VpnManageService.stopVpnForSwitchProxyFailed();
                            Firebase.getInstance(mContext).logEvent("切换代理", "所有代理连不通");
                        }
                    } else {
                        LocalVpnService.IsRunning = true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void reconnectVpn() {
        if (LocalVpnService.IsRunning) {
            VpnManageService.stopVpnForAutoSwitchProxy();
            VpnNotification.gSupressNotification = true;
        }
        ServerConfig serverConfig = findVPNServer();
        if (serverConfig != null) {
            LocalVpnService.ProxyUrl = serverConfig.toProxyUrl();
            LocalVpnService.IsRunning = true;
            mContext.startService(new Intent(mContext, LocalVpnService.class));
            serverConfig.saveInSharedPreference(mSharedPreference);
        } else {
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(Action.ACTION_NO_AVAILABLE_VPN));
        }
    }

    public ServerConfig findVPNServer() {
        ServerConfig serverConfig = null;
        ArrayList<ServerConfig> serverConfigs = loadServerList();
        String localNation = "";
        if (serverConfigs != null && !serverConfigs.isEmpty()) {
            final String defaultNation = mContext.getString(R.string.vpn_nation_opt);
            String nation = mSharedPreference.getString(SharedPreferenceKey.VPN_NATION, defaultNation);
            //处理换语言的情况
            if (!nation.equals(defaultNation)) {
                TypedArray array = mContext.getResources().obtainTypedArray(R.array.vpn_nations);
                int i = 0;
                for (; i < array.length(); i++) {
                    String n = array.getString(i);
                    if (nation.equals(n)) {
                        break;
                    }
                }
                if (i >= array.length()) {
                    nation = defaultNation;
                    mSharedPreference.edit()
                            .putString(SharedPreferenceKey.VPN_NATION, nation)
                            .putString(SharedPreferenceKey.VPN_FLAG, mContext.getResources().getResourceEntryName(R.drawable.ic_flag_global))
                            .apply();
                }
            }
            //处理本地和服务器列表切换的问题
            String defaultName = mContext.getString(R.string.vpn_name_opt);
            String name = mSharedPreference.getString(SharedPreferenceKey.CONNECTING_VPN_NAME, defaultName);
            if (!name.equals(defaultName)) {
                String serverlist = mSharedPreference.getString(SharedPreferenceKey.FETCH_SERVER_LIST, null);
                if (serverlist != null && !serverlist.contains(name)) {
                    nation = defaultNation;
                    mSharedPreference.edit()
                            .putString(SharedPreferenceKey.VPN_NATION, nation)
                            .putString(SharedPreferenceKey.VPN_FLAG, mContext.getResources().getResourceEntryName(R.drawable.ic_flag_global))
                            .apply();
                }
            }

            final boolean isGlobalOption = nation.equals(defaultNation);
            ArrayList<MyCallable> tasks = new ArrayList<>();
            if (isGlobalOption) { //没有选择国家
                String countryCode = mSharedPreference.getString(SharedPreferenceKey.COUNTRY_CODE, "unkown");
                TypedArray nationCode = mContext.getResources().obtainTypedArray(R.array.vpn_nations_code);
                TypedArray nations = mContext.getResources().obtainTypedArray(R.array.vpn_nations);

                //测试用
//                countryCode = "ZA";
                for (int k = 0; k < nationCode.length(); k++) { //通过国家code找到当地有服务器
                    String code = nationCode.getString(k);
                    if (countryCode.equals("FR")) //单独处理法国，因为现在暂时没有法国的服务器，法国默认连接美国
                        break;
                    if (countryCode.toUpperCase().equals(code)) {
                        localNation = nations.getString(k);
                        mIsFindLocalServer = true;
                        Firebase.getInstance(mContext).logEvent("找到本地服务器", countryCode, localNation);
                        break;
                    } else {
                        mIsFindLocalServer = false;
                    }
                }

                if (!mIsFindLocalServer)
                    Firebase.getInstance(mContext).logEvent("没有找到本地服务器", countryCode);

                //根据国家代码 有限选择当前国家的服务器
                if (!TextUtils.isEmpty(localNation)) {
                    for (ServerConfig config : serverConfigs) {
                        if (localNation.equals(config.nation)) {
                            tasks.add(new MyCallable(this, config));
                        }
                    }
                } else {
                    serverConfigs.remove(0); // 如果没有找到当前国家有服务器的话，先根据国家来优先链接服务器，
                    nation = getPriorityNation(countryCode);
                    for (ServerConfig config : serverConfigs) {
                        if (mIsPriorityConnect) {
                            if (nation.equals(config.nation))
                                tasks.add(new MyCallable(this, config));
                        } else {
                            tasks.add(new MyCallable(this, config));
                        }
                    }
                }
            } else {
                for (ServerConfig config : serverConfigs) {
                    if (nation.equals(config.nation)) {
                        tasks.add(new MyCallable(this, config));
                    }
                }
            }

            ExecutorService executorService = Executors.newCachedThreadPool();
            ExecutorCompletionService<ServerConfig> ecs = new ExecutorCompletionService<>(executorService);
            for (MyCallable callable : tasks) {
                ecs.submit(callable);
            }

            for (int i = 0; i < tasks.size(); i++) {
                try {
                    Future<ServerConfig> future = ecs.take();
                    ServerConfig sc = future.get(10, TimeUnit.SECONDS);
                    if (sc != null) {
                        serverConfig = sc;
                        break;
                    }
                } catch (Exception e) {
                }
            }
            executorService.shutdown();
            if (serverConfig == null && (mIsFindLocalServer || mIsPriorityConnect)) {
                serverConfig = findOtherConfig(serverConfigs);
            }
        }
        return serverConfig;
    }

    private String getPriorityNation(String nationCode) {
        String priorityNation = "";
        TypedArray nation_code = mContext.getResources().obtainTypedArray(R.array.nation_code);
        TypedArray nation = mContext.getResources().obtainTypedArray(R.array.nation);

        for (int i = 0; i < nation_code.length(); i++) {
            String code = nation_code.getString(i);
            if (nationCode.equals(code)) {
                priorityNation = nation.getString(i);
                mIsPriorityConnect = true;
                break;
            } else {
                mIsPriorityConnect = false;
            }
        }

        Firebase.getInstance(mContext).logEvent("默认优先链接服务器", nationCode, priorityNation);
        return priorityNation;
    }

    //当前国家有VPN服务器，但是都链接失败了，就从头到尾再从新链接一下其他国家服务器（排除链接当前国家）
    private ServerConfig findOtherConfig(List<ServerConfig> serverConfigs) {
        ServerConfig serverConfig = null;
        ArrayList<MyCallable> tasks = new ArrayList<>();

        serverConfigs.remove(0);
        for (ServerConfig config : serverConfigs) {
            tasks.add(new MyCallable(this, config));
        }

        ExecutorService executorService = Executors.newCachedThreadPool();
        ExecutorCompletionService<ServerConfig> ecs = new ExecutorCompletionService<>(executorService);
        for (MyCallable callable : tasks) {
            ecs.submit(callable);
        }

        for (int i = 0; i < tasks.size(); i++) {
            try {
                Future<ServerConfig> future = ecs.take();
                ServerConfig sc = future.get(10, TimeUnit.SECONDS);
                if (sc != null) {
                    serverConfig = sc;
                    break;
                }
            } catch (Exception e) {
            }
        }

        executorService.shutdown();

        if (serverConfig != null)
            Firebase.getInstance(mContext).logEvent("优先连接服务器失败连接连接其他服务器", serverConfig.nation, serverConfig.server);
        return serverConfig;
    }

    private static class MyCallable implements Callable<ServerConfig> {
        private WeakReference<ConnectVpnHelper> mReference;
        private ServerConfig mConfig;

        MyCallable(ConnectVpnHelper helper, ServerConfig config) {
            mReference = new WeakReference<>(helper);
            mConfig = config;
        }

        @Override
        public ServerConfig call() throws Exception {
            ConnectVpnHelper helper = mReference.get();

            if (helper != null)
                return helper.testServerIpAndPort(mConfig);
            else
                throw new Exception("activity is null");
        }
    }

    // && isPortOpen(config.server, config.port, 5000)
    public ServerConfig testServerIpAndPort(ServerConfig config) throws Exception {
        int remote_pingLoad = (int) FirebaseRemoteConfig.getInstance().getLong("ping_load");
        int pingLoad = ping(config.server);
        boolean connect = ping(config.server) <= remote_pingLoad;
        if (connect) {
            return config;
        } else {
            RealTimeLogger.getInstance(mContext).logEventAsync("ping", "vpn_ip", config.server, "vpn_load", String.valueOf(pingLoad)
                    , "vpn_country", config.nation, "vpn_city", config.name, "net_type", InternetUtil.getNetworkState(mContext),
                    "time", WarnDialogUtil.getDateTime());
        }
        return null;
    }

    private ArrayList<ServerConfig> loadServerList() {
        ArrayList<ServerConfig> result = null;
        String serverlist = mSharedPreference.getString(SharedPreferenceKey.FETCH_SERVER_LIST, null);
        ArrayList<ServerConfig> serverList = null;
        if (serverlist != null) {
            serverList = ServerConfig.createServerList(mContext, serverlist); // 返回null 内部catch
        }
        if (serverList != null && serverList.size() > 1) {
            result = serverList;
        }
        return result;
    }

    private int ping(String ipAddress) {
        boolean status = false;
        int load = 0;
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        String stringLoad = null;
        try {
            connection = (HttpURLConnection) new URL(String.format("http://%s:8080/vpn_server_guard/load", ipAddress)).openConnection();
            connection.setConnectTimeout(1000 * 5);
            connection.setReadTimeout(1000 * 5);
//            status = connection.getResponseCode() == HttpURLConnection.HTTP_OK;
            inputStream = connection.getInputStream();
            int total = 0;
            byte[] bs = new byte[100];
            while ((total = inputStream.read(bs)) != -1) {
                stringLoad = new String(bs, 0, total);
            }

        } catch (Exception e) {
            ShadowsocksApplication.handleException(e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            load = Integer.parseInt(stringLoad);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        Firebase.getInstance(mContext).logEvent("ping", ipAddress, stringLoad);
        return load;
    }

    private boolean isPortOpen(final String ip, final int port, final int timeout) {
        Socket socket = null;
        OutputStreamWriter osw;
        boolean result = false;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), timeout);
            osw = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
            osw.write(ip, 0, ip.length());
            osw.flush();
            result = true;
        } catch (Exception ex) {
            ShadowsocksApplication.handleException(ex);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                ShadowsocksApplication.handleException(e);
            }
        }
        Log.d("MyCaller", ip + ":" + port + " " + result);
        if (!result) {
            Firebase.getInstance(mContext).logEvent("port", ip + ":" + String.valueOf(port), String.valueOf(result));
        }
        return result;
    }

}