package com.androapplite.shadowsocks.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.model.ServerConfig;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.androapplite.vpn3.R;
import com.vm.shadowsocks.core.LocalVpnService;
import com.vm.shadowsocks.core.VpnNotification;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
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
public class ReconnectIntentService extends IntentService {
    private SharedPreferences mSharedPreference;

    public ReconnectIntentService() {
        super("ReconnectIntentService");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            mSharedPreference = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
            if (LocalVpnService.IsRunning) {
                VpnManageService.stopVpnForAutoSwitchProxy();
                VpnNotification.gSupressNotification = true;
            }
            ServerConfig serverConfig = findVPNServer();
            if (serverConfig != null) {
                LocalVpnService.ProxyUrl = serverConfig.toProxyUrl();
                LocalVpnService.IsRunning = true;
                startService(new Intent(this, LocalVpnService.class));
                serverConfig.saveInSharedPreference(mSharedPreference);
            } else {
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Action.ACTION_NO_AVAILABLE_VPN));
            }

        }
    }


    private ServerConfig findVPNServer(){
        ServerConfig serverConfig = null;
//        ArrayList<ServerConfig> serverConfigs = ServerListHelper.loadServerList(this, mSharedPreference);
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
//                String serverlist = ServerListHelper.getDecryptServerList(mSharedPreference);
                String serverlist =  mSharedPreference.getString(SharedPreferenceKey.SERVER_LIST, null);
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

            ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
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
            executorService.shutdown();
        }
        return serverConfig;
    }

    private static class MyCallable implements Callable<ServerConfig> {
        private WeakReference<ReconnectIntentService> mReference;
        private ServerConfig mConfig;
        MyCallable(ReconnectIntentService mainActivity, ServerConfig config) {
            mReference = new WeakReference<ReconnectIntentService>(mainActivity);
            mConfig = config;
        }
        @Override
        public ServerConfig call() throws Exception {
            Log.d("MyCallable", String.format("test server %s:%d", mConfig.server, mConfig.port));
            ReconnectIntentService service = mReference.get();
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

    private ArrayList<ServerConfig> loadServerList() {
        ArrayList<ServerConfig> result = null;
        String serverlist = mSharedPreference.getString(SharedPreferenceKey.SERVER_LIST, null);
        ArrayList<ServerConfig> serverList = null;
        if(serverlist != null){
            serverList = ServerConfig.createServerList(this, serverlist); // 返回null 内部catch
        }
        if(serverList != null && serverList.size() > 1) {
            result = serverList;
        }
        return result;
    }

    private boolean ping(String ipAddress){
        boolean status = false;
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(String.format("http://%s/ping.html", ipAddress)).openConnection();
            connection.setConnectTimeout(1000 * 5);
            connection.setReadTimeout(1000 * 5);
            status = connection.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            ShadowsocksApplication.handleException(e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
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
            Firebase.getInstance(this).logEvent("port", ip + ":" + String.valueOf(port), String.valueOf(result));
        }
        return result;
    }

    public static void start(Context context) {
        context.startService(new Intent(context, ReconnectIntentService.class));
    }

}
