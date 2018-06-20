package com.androapplite.shadowsocks.connect;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.model.PriorityConfig;
import com.androapplite.shadowsocks.model.ServerConfig;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.androapplite.shadowsocks.service.VpnManageService;
import com.androapplite.shadowsocks.utils.InternetUtil;
import com.androapplite.shadowsocks.utils.RealTimeLogger;
import com.androapplite.shadowsocks.utils.ShadowSocksProxyRunnable;
import com.androapplite.shadowsocks.utils.WarnDialogUtil;
import com.androapplite.vpn3.R;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.vm.shadowsocks.core.LocalVpnService;
import com.vm.shadowsocks.core.VpnNotification;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Kevin.Mo on 2018/6/4.
 */

public class ConnectVpnHelper {
    private static final String TAG = "测试";

    private static ConnectVpnHelper instance;
    public static final String URL_BING = "https://www.bing.com";
    public static final String URL_GOOGLE = "http://www.gstatic.com/generate_204";
    public static final String URL_YOUTUBE = "https://m.youtube.com";
    public static final String URL_FB = "https://m.facebook.com";
    public static final String URL_FB_IP = "https://157.240.22.35";

    private static final ArrayList<String> URLS = new ArrayList<>();
    private TestConnectCallable youtubeCallable;
    private TestConnectCallable facebookCallable;
    private TestConnectCallable bingCallable;
    private ShadowSocksProxyRunnable mSocksProxyRunnable;

    static {
        URLS.add(URL_FB);
        URLS.add(URL_YOUTUBE);
        URLS.add(URL_BING);
    }

    private Context mContext;
    private SharedPreferences mSharedPreference;
    private List<ServerConfig> errorsList;
    private List<ServerConfig> resultList;
    private ServerConfig currentConfig;
    private Timer mTimer;
    private OkHttpClient client;
    private Request request;
    private Firebase mFirebase;
    private List<Timer> mTimerList;
    private ArrayList<TestConnectCallable> tasks;

    private boolean mIsFindLocalServer; //找到与服务器匹配的国家
    private boolean mIsPriorityConnect; //找到优先选择的国家
    private boolean mIsTimerCheck; //是每隔5s的检查连接


    private ConnectVpnHelper() {
    }

    public static ConnectVpnHelper getInstance(Context context) {
        if (instance == null) {
            synchronized (ConnectVpnHelper.class) {
                if (instance == null) {
                    instance = new ConnectVpnHelper();
                    instance.mContext = context.getApplicationContext();
                    instance.initData();
                }
            }
        }
        return instance;
    }

    private void initData() {
        mSharedPreference = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(mContext);
        errorsList = new ArrayList<>();
        resultList = new ArrayList<>();
        mTimerList = new ArrayList<>();
        mFirebase = Firebase.getInstance(mContext);
        mSocksProxyRunnable = new ShadowSocksProxyRunnable();
    }

    public void switchProxyService() {
        if (LocalVpnService.IsRunning) {
            mSharedPreference.edit().putBoolean(SharedPreferenceKey.IS_AUTO_SWITCH_PROXY, true).apply();
            ServerConfig serverConfig = findOtherVPNServerWithOutFailServer();
            mFirebase.logEvent("切换代理", "开始监测");
            try {
                if (serverConfig != null) {
                    Log.i(TAG + "开始切换找到的服务器", String.format("%s--->%s--->%s", serverConfig.server, serverConfig.port, serverConfig.nation));
                    Log.i(TAG + "当前失败的服务器", String.format("%s--->%s--->%s", currentConfig.server, currentConfig.port, currentConfig.nation));
                    mFirebase.logEvent("自动切换当前失败的服务器", String.format("%s|%s|%s", currentConfig.server, currentConfig.port, currentConfig.nation));
                    mFirebase.logEvent("自动切换链接的服务器", String.format("%s|%s|%s", serverConfig.server, serverConfig.port, serverConfig.nation));
                    RealTimeLogger.getInstance(mContext).logEventAsync("auto_switch", "fail_server", String.format("%s|%s|%s", currentConfig.server, currentConfig.port, currentConfig.nation)
                            , "switch_server", String.format("%s|%s|%s", serverConfig.server, serverConfig.port, serverConfig.nation));
                    if (LocalVpnService.IsRunning) {
                        VpnManageService.stopVpnForAutoSwitchProxy();
                        VpnNotification.gSupressNotification = true;
                        LocalVpnService.ProxyUrl = serverConfig.toProxyUrl();
                        monitorVpnState();
                        serverConfig.saveInSharedPreference(mSharedPreference);
                        Log.i(TAG, "switchProxyService:   自动切换");
                        currentConfig = serverConfig;
                        startConnectAfterFirstTest();
                    }
                } else {
                    Log.d("FindProxyService", "没有可用的proxy");
                    VpnManageService.stopVpnForSwitchProxyFailed();
                    mFirebase.logEvent("切换代理", "所有代理连不通");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void monitorVpnState() {
        for (int i = 0; i < (10 * 1000) / 10; i++) {
            if (LocalVpnService.IsStopped) {
                LocalVpnService.IsRunning = true;
                break;
            } else {
                SystemClock.sleep(10);
            }
        }
    }

    private ServerConfig getTestServer() {
        ServerConfig[] configs = {
                new ServerConfig("Tokyo", "Japan", "198.13.45.183", 10, 40050, "ic_flag_jp"),
        };
        return configs[0];//(int) (Math.random() + 0.5)
    }

    public void reconnectVpn() {
        if (LocalVpnService.IsRunning) {
            VpnManageService.stopVpnForAutoSwitchProxy();
            VpnNotification.gSupressNotification = true;
            release();
        }
        ServerConfig serverConfig = findVPNServer();
        if (serverConfig != null) {
            LocalVpnService.ProxyUrl = serverConfig.toProxyUrl();
            LocalVpnService.IsRunning = true;
            mContext.startService(new Intent(mContext, LocalVpnService.class));
            currentConfig = serverConfig;
            serverConfig.saveInSharedPreference(mSharedPreference);
        } else {
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(Action.ACTION_NO_AVAILABLE_VPN));
        }
    }

    public ServerConfig findVPNServer() {
        ServerConfig serverConfig = null;
        synchronized (ConnectVpnHelper.class) {
            resultList.clear();
        }
        ArrayList<ServerConfig> serverConfigs = loadServerList();
        mSocksProxyRunnable.start();
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
                        mFirebase.logEvent("找到本地服务器", countryCode, localNation);
                        break;
                    } else {
                        mIsFindLocalServer = false;
                    }
                }

                if (!mIsFindLocalServer)
                    mFirebase.logEvent("没有找到本地服务器", countryCode);

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
            mSocksProxyRunnable.stop();
            if (serverConfig == null && (mIsFindLocalServer || mIsPriorityConnect)) {
                serverConfig = findOtherConfig(serverConfigs);
            }
        }
//        serverConfig = new ServerConfig("Tokyo", "Japan", "149.28.139.6", 2, mContext.getResources().getResourceEntryName(R.drawable.ic_flag_ca));
        currentConfig = serverConfig;
        return serverConfig;
    }

    private String getPriorityNation(String nationCode) {
//      http://www.jctrans.com/tool/gjym.htm
        String priorityNation = "";
        String priorityList = FirebaseRemoteConfig.getInstance().getString("priority_connect_nation_list");
        ArrayList<PriorityConfig> configs = new ArrayList<>();
        try {
            JSONArray rootJsonArray = new JSONArray(priorityList);
            for (int i = 0; i < rootJsonArray.length(); i++) {
                JSONObject jsonObject = (JSONObject) rootJsonArray.get(i);
                String code = jsonObject.getString("code");
                String nation = jsonObject.getString("nation");
                PriorityConfig config = new PriorityConfig(code, nation);
                configs.add(config);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!configs.isEmpty()) {
            for (PriorityConfig config : configs) {
                if (config.getCode().equals(nationCode)) {
                    priorityNation = config.getNation();
                    mIsPriorityConnect = true;
                    break;
                } else {
                    mIsPriorityConnect = false;
                }
            }
        }

        mFirebase.logEvent("默认优先链接服务器", nationCode, priorityNation);
        return priorityNation;
    }

    //测试失败的时候去链接其他的服务器，失败的服务器会被存在一个列表里，切换的时候不会切换
    private ServerConfig findOtherVPNServerWithOutFailServer() {
        ServerConfig serverConfig = null;
        ArrayList<ServerConfig> serverConfigs = loadServerList();
        ArrayList<MyCallable> tasks = new ArrayList<>();
        mSocksProxyRunnable.start();
        if (errorsList.size() == serverConfigs.size() - 1) { //当错误列表和服务器列表大小一样的话，表示所有服务器都测试失败，-1为移除服务器列表第一个全局
            mFirebase.logEvent("失败列表和服务器列表大小一样", "所有的服务器都测试过");
            errorsList.clear();
            return null;
        }
        ArrayList<ServerConfig> copy = null;
        synchronized (ConnectVpnHelper.class) {
            copy = new ArrayList<>(resultList.size());
            copy.addAll(resultList);
        }
        if (!copy.isEmpty()) {
            for (ServerConfig config : copy) {
                if (config != null && !errorsList.contains(config)) {
                    try {
                        serverConfig = testServerPing(config);
                    } catch (Exception e) {
                    }
                    if (serverConfig != null)
                        return serverConfig;
                }
            }
        }

        if (serverConfig == null && serverConfigs != null && !serverConfigs.isEmpty()) {
            serverConfigs.remove(0);
            for (ServerConfig config : serverConfigs) {
                if (!errorsList.isEmpty()) {
                    if (!errorsList.get(0).nation.equals(config.nation))
                        tasks.add(new MyCallable(this, config));
                } else {
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
        mSocksProxyRunnable.stop();

        return serverConfig;
    }

    //当前国家有VPN服务器，但是都链接失败了，就从头到尾再从新链接一下其他国家服务器
    private ServerConfig findOtherConfig(List<ServerConfig> serverConfigs) {
        ServerConfig serverConfig = null;
        ArrayList<MyCallable> tasks = new ArrayList<>();
        mSocksProxyRunnable.start();
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
        mSocksProxyRunnable.stop();

        if (serverConfig != null)
            Firebase.getInstance(mContext).logEvent("优先连接服务器失败连接连接其他服务器", serverConfig.nation, serverConfig.server);
        return serverConfig;
    }

    private void startTimerMonitor() {
        if (!mIsTimerCheck) {
            int time = (int) FirebaseRemoteConfig.getInstance().getLong("connect_test_link_time");
            int delayCheck = (int) FirebaseRemoteConfig.getInstance().getLong("test_delay_check_time");
            mTimer = new Timer();
            if (!mTimerList.contains(mTimer))
                mTimerList.add(mTimer);
            mTimer.schedule(new MonitorTask(), delayCheck * 1000, TimeUnit.SECONDS.toMillis(time));
            Log.i(TAG, "testConnection:  新起一个Timer ");
        }
    }

    public void startTestConnectionWithVPN() {
        mIsTimerCheck = false;
        startTimerMonitor();
    }

    public void startConnectAfterFirstTest() {
//        if (LocalVpnService.IsRunning) {
//            long start = System.currentTimeMillis();
////            boolean result = visitNetTest();
//            boolean result = false;
//            int count = mSharedPreference.getInt(SharedPreferenceKey.TEST_CONNECT_FAILED_TIME_COUNT, 0);
//            if (result) {
//                if (errorsList.contains(currentConfig))
//                    errorsList.remove(currentConfig);
//                if (LocalVpnService.IsRunning)
//                    startTestConnectionWithVPN();
//            } else {
//                if (!errorsList.contains(currentConfig))
//                    errorsList.add(currentConfig);
//                mSharedPreference.edit().putInt(SharedPreferenceKey.TEST_CONNECT_FAILED_TIME_COUNT, count + 1).apply();
//                mFirebase.logEvent("链接第一次测试失败", String.format("%s|%s|%s", currentConfig.server, currentConfig.nation, currentConfig.port), (System.currentTimeMillis() - start));
//                if (LocalVpnService.IsRunning)
//                    switchProxyService();
//            }
//            if (count == 3)
//                resetFailCount();
//        }
        startTestConnectionWithVPN();
    }

    public void startTestConnectionWithOutVPN(String url, ServerConfig config) {
        long startTime = 0;
        boolean result = false;
        try {
            if (client == null)
                client = new OkHttpClient();

            if (request == null)
                request = new Request.Builder()
                        .url(url)
                        .build();
            startTime = System.currentTimeMillis();
            for (int i = 0; i < 3; i++) {
                if (testConnectionStatus(client, request)) {
                    result = true;
                    break;
                }
                SystemClock.sleep(50);
            }
        } catch (Exception e) {
            ShadowsocksApplication.handleException(e);
        }
        mFirebase.logEvent("连接失败后测试", String.valueOf(result), System.currentTimeMillis() - startTime);
    }

    private void testConnection(ServerConfig config) { // 测试连接
        if (LocalVpnService.IsRunning) {
            if (currentConfig != null && !currentConfig.server.equals(config.server)) {
                for (Timer timer : mTimerList)
                    timer.cancel();
            }
            currentConfig = config;
            long startTime = System.currentTimeMillis();

            boolean result = visitNetTest();

            long timeConsume = System.currentTimeMillis() - startTime;
            mFirebase.logEvent("访问测试网站用时", String.valueOf(result), timeConsume);
            RealTimeLogger.getInstance(mContext).logEventAsync("request_test_url", "result", String.format("%s|%s", result, timeConsume));
            if (result) {
                if (errorsList.contains(config))
                    errorsList.remove(config);
                if (config != null) {// && !mIsTimerCheck
                    mFirebase.logEvent("连接后测试成功", String.format("%s|%s|%s", config.server, config.nation, config.port), System.currentTimeMillis() - startTime);
                    RealTimeLogger.getInstance(mContext).logEventAsync("test_success", "server",
                            String.format("%s|%s|%s", config.server, config.nation, config.port));
                }
                Log.i(TAG, "testConnection:  链接后测试成功     " + (System.currentTimeMillis() - startTime));
                resetFailCount();
            } else {
                if (config != null) {// && !mIsTimerCheck
                    mFirebase.logEvent("连接后测试失败", String.format("%s|%s|%s", config.server, config.nation, config.name), System.currentTimeMillis() - startTime);
                    RealTimeLogger.getInstance(mContext).logEventAsync("test_fail", "server",
                            String.format("%s|%s|%s", config.server, config.nation, config.port), "time", String.valueOf(System.currentTimeMillis() - startTime));
                }
                Log.i(TAG, "testConnection:   链接后测试失败     " + (System.currentTimeMillis() - startTime));
                int count = mSharedPreference.getInt(SharedPreferenceKey.TEST_CONNECT_FAILED_COUNT, 0);
                int failCount = (int) FirebaseRemoteConfig.getInstance().getLong("connect_test_fail_count");

                if (count < failCount) {
                    mSharedPreference.edit().putInt(SharedPreferenceKey.TEST_CONNECT_FAILED_COUNT, count + 1).apply();
                } else {
                    if (!errorsList.contains(config))
                        errorsList.add(config);
                    release();
                    switchProxyService();
                    if (config != null) {
                        mFirebase.logEvent("达到失败次数重连", String.format("%s|%s", config.server, config.nation), count); //失败的服务器，国家
                        RealTimeLogger.getInstance(mContext).logEventAsync("test_fail_switch", "server", String.format("%s|%s|%s", config.server, config.nation, config.port), "fail_count", String.valueOf(count));
                    }
                    resetFailCount();
                }
            }
        }
    }

    private boolean testConnectionStatus(OkHttpClient client, Request request) { //测试网络状态的测试，当VPN链接失败时候用
        boolean result = false;
        Response response = null;
        try {
            response = client.newCall(request).execute();
            result = true;
        } catch (Exception e) {
            ShadowsocksApplication.handleException(e);
        } finally {
            if (response != null) {
                response.body().close();
            }
        }
        return result;
    }

    private class MonitorTask extends TimerTask {

        @Override
        public void run() {
            if (LocalVpnService.IsRunning) {
                mIsTimerCheck = true;
                Log.i(TAG, "开始监控" + currentConfig.server + "  " + currentConfig.nation);
                testConnection(currentConfig);
            }
        }
    }

    private boolean visitNetTest() { // 测试VPN连上了 访问三个个网址的测试
        boolean result = false;
        int timeOut = (int) FirebaseRemoteConfig.getInstance().getLong("connect_test_time_out");
//        int failCount = mSharedPreference.getInt(SharedPreferenceKey.TEST_CONNECT_FAILED_TIME_COUNT, 0);
        int failCount = mSharedPreference.getInt(SharedPreferenceKey.TEST_CONNECT_FAILED_COUNT, 0);
//        if (failCount >= FirebaseRemoteConfig.getInstance().getLong("connect_test_fail_count"))
//            mSharedPreference.edit().putInt(SharedPreferenceKey.TEST_CONNECT_FAILED_TIME_COUNT, 0).apply();

        timeOut = timeOut * (failCount + 1);
        Log.i(TAG, "visitNetTest: timeOut    " + timeOut);
        int errorCount = 0;
        if (tasks == null)
            tasks = new ArrayList<>();

        if (youtubeCallable == null)
            youtubeCallable = new TestConnectCallable(URL_YOUTUBE, this);
        if (facebookCallable == null)
            facebookCallable = new TestConnectCallable(URL_FB, this);
        if (bingCallable == null)
            bingCallable = new TestConnectCallable(URL_BING, this);

        if (!tasks.contains(youtubeCallable))
            tasks.add(youtubeCallable);
        if (!tasks.contains(facebookCallable))
            tasks.add(facebookCallable);
        if (!tasks.contains(bingCallable))
            tasks.add(bingCallable);

        ExecutorService executorService = Executors.newCachedThreadPool();
        ExecutorCompletionService<Boolean> ecs = new ExecutorCompletionService<>(executorService);
        for (TestConnectCallable callable : tasks) {
            ecs.submit(callable);
        }

        for (int i = 0; i < timeOut * 1000 / 100; i++) {
            try {
                if (errorCount == URLS.size()) {
                    errorCount = 0;
                    break;
                }

                Future<Boolean> future = ecs.poll(100, TimeUnit.MILLISECONDS);
                if (future != null) {
                    result = future.get();
                    Log.i(TAG + "future拿到的结果    ", result + "");
                    if (result)
                        break;
                }
            } catch (Exception e) {
                errorCount++;
                Log.i(TAG + "future抛出异常", "testConnectionError: " + errorCount);
                ShadowsocksApplication.handleException(e);
            }
        }
        executorService.shutdown();
        return result;
    }

    private void resetFailCount() {
        if (mSharedPreference != null)
            mSharedPreference.edit().putInt(SharedPreferenceKey.TEST_CONNECT_FAILED_COUNT, 0).apply();
    }

    public ServerConfig getCurrentConfig() {
        return currentConfig;
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
            ServerConfig serverConfig;
            if (helper != null) {
                serverConfig = helper.testServerPing(mConfig);
                if (serverConfig != null) {
                    synchronized (ConnectVpnHelper.class) {
                        helper.resultList.add(serverConfig);
                    }
                }
                return serverConfig;
            } else {
                throw new Exception("ConnectVpnHelper is null");
            }
        }
    }

    // && isPortOpen(config.server, config.port, 5000)
    private ServerConfig testServerPing(ServerConfig config) throws Exception {
        int remote_pingLoad = (int) FirebaseRemoteConfig.getInstance().getLong("ping_load");
        int pingLoad = ping(config.server);
        boolean connect = pingLoad <= remote_pingLoad;
        if (connect) {
            return beforeConnectTestStatus(config) ? config : null;
        } else {
            RealTimeLogger.getInstance(mContext).logEventAsync("ping", "vpn_ip", config.server, "vpn_load", String.valueOf(pingLoad)
                    , "vpn_country", config.nation, "vpn_city", config.name, "net_type", InternetUtil.getNetworkState(mContext),
                    "time", WarnDialogUtil.getDateTime());
        }
        return null;
    }

    private boolean beforeConnectTestStatus(ServerConfig config) {
        boolean result = false;
        String testURL = FirebaseRemoteConfig.getInstance().getString("before_connect_test_net");
        try {
            Proxy proxy = mSocksProxyRunnable.createShadowsocksProxy(config.server, config.port);
            HttpURLConnection connection = (HttpURLConnection) new URL(testURL).openConnection(proxy);
            connection.setDoInput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            result = connection.getResponseCode() == HttpURLConnection.HTTP_OK;
            Firebase.getInstance(mContext).logEvent("连接前测试访问网站", "结果", String.valueOf(result));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
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
            status = connection.getResponseCode() == HttpURLConnection.HTTP_OK;
            if (!status)
                stringLoad = "120";// 网站失败访问失败
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
            if (stringLoad == null)
                stringLoad = "110";//没读到数据
            load = Integer.parseInt(stringLoad);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        mFirebase.logEvent("ping", String.format("%s|%s", ipAddress, stringLoad), String.valueOf(status));
        return load;
    }

    public void release() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
            Log.i(TAG, "release:   关闭当前的Timer");
        }
        mIsTimerCheck = false;
//        resetFailCount();
        if (mTimerList != null && !mTimerList.isEmpty()) {
            for (Timer timer : mTimerList) {
                timer.cancel();
                Log.i(TAG, "release:   关闭Timer");
            }
            mTimerList.clear();
        }

        if (tasks != null)
            tasks.clear();
    }

    public void clearErrorList() {
        if (errorsList != null)
            errorsList.clear();
        resetFailCount();
    }

    private static class TestConnectCallable implements Callable<Boolean> {
        private String mURL;
        private ConnectVpnHelper mHelper;

        TestConnectCallable(String url, ConnectVpnHelper helper) {
            mURL = url;
            mHelper = helper;
        }

        @Override
        public Boolean call() throws Exception {
            boolean requestResult = getRequestResult();
            Log.i(TAG, "call:  " + requestResult + "    " + mURL);
            return requestResult;
        }

        private boolean getRequestResult() {
            boolean result;
            HttpURLConnection connection = null;
            int timeOut = (int) FirebaseRemoteConfig.getInstance().getLong("connect_test_time_out");
//            timeOut = timeOut * (mHelper.mSharedPreference.getInt(SharedPreferenceKey.TEST_CONNECT_FAILED_TIME_COUNT, 0) + 1);
            timeOut = timeOut * (mHelper.mSharedPreference.getInt(SharedPreferenceKey.TEST_CONNECT_FAILED_COUNT, 0) + 1);
            try {
                connection = (HttpURLConnection) new URL(mURL).openConnection();
                connection.setUseCaches(false);
                connection.setConnectTimeout(1000 * timeOut);
                connection.setReadTimeout(1000 * timeOut);
                connection.connect();
                result = connection.getResponseCode() == HttpURLConnection.HTTP_OK;
            } catch (IOException e) {
                result = false;
                Log.i(TAG, "getRequestResult: 崩溃 " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (connection != null)
                    connection.disconnect();
            }
            return result;
        }
    }
}
