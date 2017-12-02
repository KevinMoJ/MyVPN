package com.androapplite.shadowsocks.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.util.Log;

import com.androapplite.vpn3.R;
import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.model.ServerConfig;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.vm.shadowsocks.core.LocalVpnService;
import com.vm.shadowsocks.core.VpnNotification;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class FindProxyService extends IntentService {
    private SharedPreferences mSharedPreference;
    public FindProxyService() {
        super("FindProxyService");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null && LocalVpnService.IsRunning) {
            mSharedPreference = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
            ServerConfig serverConfig = ServerConfig.loadFromSharedPreference(mSharedPreference);
            Firebase.getInstance(this).logEvent("切换代理", "开始监测");
            if (serverConfig != null) {
                try {
                    LocalVpnService.IsRunning = false;
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
                        } else {
                            Log.d("FindProxyService", "没有可用的proxy");
                            LocalVpnService.IsRunning = false;
                            Firebase.getInstance(this).logEvent("切换代理", "所有代理连不通");
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

    private ServerConfig findVPNServer(){
        ServerConfig serverConfig = null;
        ArrayList<ServerConfig> serverConfigs = loadServerList();
        if(serverConfigs != null && !serverConfigs.isEmpty()) {
            final String defaultNation = getString(R.string.vpn_nation_opt);
            String nation = mSharedPreference.getString(SharedPreferenceKey.VPN_NATION, defaultNation);
            //处理换语言的情况
            if(!nation.equals(defaultNation)){
                TypedArray array = getResources().obtainTypedArray(R.array.vpn_nations);
                int i = 0;
                for(;i<array.length();i++){
                    String n = array.getString(i);
                    if(nation.equals(n)){
                        break;
                    }
                }
                if(i >= array.length()){
                    nation = defaultNation;
                    mSharedPreference.edit()
                            .putString(SharedPreferenceKey.VPN_NATION, nation)
                            .putString(SharedPreferenceKey.VPN_FLAG, getResources().getResourceEntryName(R.drawable.ic_flag_global))
                            .apply();
                }
            }
            //处理本地和服务器列表切换的问题
            String defaultName = getString(R.string.vpn_name_opt);
            String name = mSharedPreference.getString(SharedPreferenceKey.CONNECTING_VPN_NAME, defaultName);
            if(!name.equals(defaultName)){
                String serverlist = mSharedPreference.getString(SharedPreferenceKey.SERVER_LIST, null);
                if(serverlist != null && !serverlist.contains(name)){
                    nation = defaultNation;
                    mSharedPreference.edit()
                            .putString(SharedPreferenceKey.VPN_NATION, nation)
                            .putString(SharedPreferenceKey.VPN_FLAG, getResources().getResourceEntryName(R.drawable.ic_flag_global))
                            .apply();
                }
            }

            final boolean isGlobalOption = nation.equals(defaultNation);
            ArrayList<MyCallable> tasks = new ArrayList<>();
            if (isGlobalOption) {
                serverConfigs.remove(0);
                for(ServerConfig config: serverConfigs) {
                    tasks.add(new MyCallable(this, config));
                }
            } else {
                for (ServerConfig config : serverConfigs) {
                    if (nation.equals(config.nation)) {
                        tasks.add(new MyCallable(this, config));
                    }
                }
            }

            ExecutorService executorService = Executors.newFixedThreadPool(tasks.size());
            ExecutorCompletionService<ServerConfig> ecs = new ExecutorCompletionService<>(executorService);
            for (MyCallable callable: tasks) {
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
        }
        return serverConfig;
    }

    private ArrayList<ServerConfig> loadServerList() {
        ArrayList<ServerConfig> result = null;
        String serverlist = mSharedPreference.getString(SharedPreferenceKey.SERVER_LIST, null);
        ArrayList<ServerConfig> serverList = null;
        if(serverlist != null){
            serverList = ServerConfig.createServerList(this, serverlist);
        }
        if(serverList != null && serverList.size() > 1) {
            result = serverList;
        }
        return result;
    }

    private static class MyCallable implements Callable<ServerConfig> {
        private WeakReference<FindProxyService> mReference;
        private ServerConfig mConfig;
        MyCallable(FindProxyService mainActivity, ServerConfig config) {
            mReference = new WeakReference<FindProxyService>(mainActivity);
            mConfig = config;
        }
        @Override
        public ServerConfig call() throws Exception {
            Log.d("MyCallable", String.format("test server %s:%d", mConfig.server, mConfig.port));
            FindProxyService service = mReference.get();
            if (service != null) {
                return service.testServerIpAndPort(mConfig);
            } else {
                throw new Exception("service is null");
            }
        }
    }

    private ServerConfig testServerIpAndPort(ServerConfig config) throws Exception{
        if (ping(config.server) && isPortOpen(config.server, config.port, 5000)) {
            return config;
        }
        return null;
    }

    private boolean ping(String ipAddress){
        int  timeOut =  5000 ;  //超时应该在3钞以上
        boolean status = false;
        try {
            status = InetAddress.getByName(ipAddress).isReachable(timeOut);     // 当返回值是true时，说明host是可用的，false则不可。
        }catch (Exception e){
            ShadowsocksApplication.handleException(e);
        }
        Log.d("MyCaller", "ping: " + ipAddress + " " + status);
        if (!status) {
            Firebase.getInstance(this).logEvent("ping", ipAddress, String.valueOf(status));
        }
        return status;
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
        }finally {
            try {
                socket.close();
            } catch (IOException e) {
                ShadowsocksApplication.handleException(e);
            }
        }
        Log.d("MyCaller", ip + ":" + port + " " + result);
        if (!result) {
            Firebase.getInstance(this).logEvent("port", ip + ":" + port, String.valueOf(result));
        }
        return result;
    }

    public static void switchProxy(Context context) {
        context.startService(new Intent(context, FindProxyService.class));
    }

}
