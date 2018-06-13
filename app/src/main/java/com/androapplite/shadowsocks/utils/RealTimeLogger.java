package com.androapplite.shadowsocks.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.connect.ConnectVpnHelper;
import com.androapplite.shadowsocks.model.ServerConfig;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.vm.shadowsocks.core.NatSessionManager;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by huangjian on 28/03/2018.
 */

public class RealTimeLogger implements Callback {
    private static RealTimeLogger sInstance;
    private Answers mAnswers;
    private ExecutorService mService;
    private OkHttpClient mHttpClient;
    private Context mContext;
    private static final String TAG = "RealTimeLogger";

    private static final int TIMEOUT_MILLI = 10000;
    private static String sApacheUrl;
    private Request mConnectTestRequest;

    private RealTimeLogger(Context context) {
        mAnswers = Answers.getInstance();
        mService = Executors.newSingleThreadExecutor();

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_MILLI, TimeUnit.MILLISECONDS)
                .readTimeout(TIMEOUT_MILLI, TimeUnit.MILLISECONDS)
                .writeTimeout(TIMEOUT_MILLI, TimeUnit.MILLISECONDS);
        mHttpClient = builder.build();
        FirebaseRemoteConfig config = FirebaseRemoteConfig.getInstance();
        sApacheUrl = config.getString("apache_log_url");
        mContext = context.getApplicationContext();

        mConnectTestRequest = new Request.Builder().url("https://www.bing.com/").build();

    }

    public static RealTimeLogger getInstance(Context context) {
        if (sInstance == null) {
            synchronized (RealTimeLogger.class) {
                if (sInstance == null) {
                    sInstance = new RealTimeLogger(context);
                }
            }
        }
        return sInstance;
    }

    /*
    name:事件名称
    args:事件参数
    例如：连不上服务器，需要上报vpn服务器地址，和手机网络，
    logEventAsync("连不上服务器", "vpn", "63.14.234.23", "network_type", "wifi");
     */
    public void logEventAsync(String name, String... args) {
        mService.submit(new EventRunnable(this, name, args));
    }

//    private void answerLogEvent(String name, String... args) {
//        CustomEvent event = new CustomEvent(name);
//        for(int i=0; i < (args.length / 2) * 2 ; i+=2) {
//            event.putCustomAttribute(args[i], args[i+1]);
//        }
//        mAnswers.logCustom(event);
//    }
//
//    private void apacheLogEvent(String name, String... args) {
//        HttpUrl.Builder urlBuilder = HttpUrl.parse(sApacheUrl).newBuilder();
//        urlBuilder.addQueryParameter("name", name);
//        for(int i=0; i < (args.length / 2) * 2 ; i+=2) {
//            urlBuilder.addQueryParameter(args[i], args[i+1]);
//        }
//        Request request = new Request.Builder()
//                .url(urlBuilder.build())
//                .build();
//        Call call = mHttpClient.newCall(request);
//        call.enqueue(this);
//    }

    private void logEvent(String name, String... args) {
        SharedPreferences sp = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(mContext);
        int ip = sp.getInt(SharedPreferenceKey.IP, 0);
        String country = sp.getString(SharedPreferenceKey.COUNTRY_CODE, "");
        long time = System.currentTimeMillis();
        String serverIp = ServerConfig.loadFromSharedPreference(sp).server;
        if (serverIp == null)
            serverIp = ConnectVpnHelper.getInstance(mContext).getCurrentConfig().server;

        answerLogEvent(name, ip, serverIp, country, time, args);
        apacheLogEvent(name, ip, serverIp, country, time, args);

    }

    private void answerLogEvent(String name, int ip, String serverIp, String country, long time, String... args) {
        CustomEvent event = new CustomEvent(name);
        event.putCustomAttribute("ip", ip)
                .putCustomAttribute("serverIp", serverIp)
                .putCustomAttribute("country", country)
                .putCustomAttribute("time", time);
        for (int i = 0; i < (args.length / 2) * 2; i += 2) {
            event.putCustomAttribute(args[i], args[i + 1]);
        }
        mAnswers.logCustom(event);
    }

    private void apacheLogEvent(String name, int ip, String serverIp, String country, long time, String... args) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(sApacheUrl).newBuilder();
        urlBuilder.addQueryParameter("name", name)
                .addQueryParameter("ip", String.valueOf(ip))
                .addQueryParameter("serverIp", serverIp)
                .addQueryParameter("country", country)
                .addQueryParameter("time", String.valueOf(time));
        for (int i = 0; i < (args.length / 2) * 2; i += 2) {
            urlBuilder.addQueryParameter(args[i], args[i + 1]);
        }
        HttpUrl httpUrl = urlBuilder.build();
        Log.d(TAG, "apacheLogEvent: " + httpUrl.toString());
        Request request = new Request.Builder()
                .url(httpUrl)
                .build();
        Call call = mHttpClient.newCall(request);
        call.enqueue(this);
    }


    private class EventRunnable implements Runnable {
        String mName;
        String[] mArgs;
        RealTimeLogger mLogger;

        EventRunnable(RealTimeLogger logger, String name, String... args) {
            mLogger = logger;
            mName = name;
            mArgs = args;
        }

        @Override
        public void run() {
            mLogger.logEvent(mName, mArgs);
        }
    }

    @Override
    public void onFailure(Call call, IOException e) {

    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {

    }


    public void logNoConnectEventAsync(List<Exception> errors) {
        ArrayList<String> errMsgs = new ArrayList<>(errors.size() * 2 + 2);

        boolean isConnectSuccess = isConnectSuccess();
        errMsgs.add("connectTest");
        errMsgs.add(String.valueOf(isConnectSuccess));

        for (int i = 0; i < errors.size(); i++) {
            errMsgs.add("err_" + i);
            errMsgs.add(errors.get(i).getMessage());
        }

        String[] args = new String[errMsgs.size()];
        errMsgs.toArray(args);
        logEventAsync("连不上", args);
    }

    private boolean isConnectSuccess() {
        boolean isConnectSuccess = false;
        try {
            Response response = mHttpClient.newCall(mConnectTestRequest).execute();
            isConnectSuccess = response.isSuccessful();
        } catch (Exception e) {
            ShadowsocksApplication.handleException(e);
        }
        return isConnectSuccess;
    }

    public void logNoReceiveByteEvenrtAsync() {
        int count = NatSessionManager.getSessionCount();

        SharedPreferences sp = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(mContext);
        ServerConfig serverConfig = ServerConfig.loadFromSharedPreference(sp);

        Exception exception = null;
        int pingResult = 0;
        try {
            pingResult = ping(serverConfig.server);
        } catch (Exception e) {
            exception = e;
            ShadowsocksApplication.handleException(e);
        }

        boolean portResult = false;
        if (pingResult == HttpURLConnection.HTTP_OK) {
            try {
                portResult = isPortOpen(serverConfig.server, serverConfig.port, 5000);
            } catch (Exception e) {
                exception = e;
                ShadowsocksApplication.handleException(e);
            }
        }

        boolean isConnectSuccess = isConnectSuccess();

        ArrayList<String> argList = new ArrayList<>(14);
        argList.add("session_count");
        argList.add(String.valueOf(count));
        argList.add("vpn_server");
        argList.add(serverConfig.server);
        argList.add("ping");
        argList.add(String.valueOf(pingResult));
        if (pingResult == HttpURLConnection.HTTP_OK) {
            argList.add("port");
            argList.add(String.valueOf(portResult));
            if (!portResult && exception != null) {
                argList.add("port_error");
                String errMsg = exception.getMessage();
                if (errMsg == null || errMsg.isEmpty()) {
                    errMsg = exception.toString();
                }
                argList.add(errMsg);
            }
        } else if (exception != null) {
            argList.add("ping_error");
            String errMsg = exception.getMessage();
            if (errMsg == null || errMsg.isEmpty()) {
                errMsg = exception.toString();
            }
            argList.add(errMsg);
        }
        argList.add("connectTest");
        argList.add(String.valueOf(isConnectSuccess));

        String[] args = new String[argList.size()];
        argList.toArray(args);
        logEventAsync("没速度", args);
    }

    private int ping(String ipAddress) throws Exception {
        int status = 0;
        HttpURLConnection connection = null;
        connection = (HttpURLConnection) new URL(String.format("http://%s/ping.html", ipAddress)).openConnection();
        connection.setConnectTimeout(1000 * 5);
        connection.setReadTimeout(1000 * 5);
        status = connection.getResponseCode();
        connection.disconnect();
        return status;
    }

    private boolean isPortOpen(final String ip, final int port, final int timeout) throws Exception {
        return true;
    }

    public void logSocketErrorEvenrtAsync(Exception ex, String host) {
        int count = NatSessionManager.getSessionCount();

        SharedPreferences sp = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(mContext);
        ServerConfig serverConfig = ServerConfig.loadFromSharedPreference(sp);

        Exception exception = null;
        int pingResult = 0;
        try {
            pingResult = ping(serverConfig.server);
        } catch (Exception e) {
            exception = e;
            ShadowsocksApplication.handleException(e);
        }

        boolean portResult = false;
        if (pingResult == HttpURLConnection.HTTP_OK) {
            try {
                portResult = isPortOpen(serverConfig.server, serverConfig.port, 5000);
            } catch (Exception e) {
                exception = e;
                ShadowsocksApplication.handleException(e);
            }
        }

        boolean isConnectSuccess = isConnectSuccess();

        ArrayList<String> argList = new ArrayList<>(20);
        argList.add("session_count");
        argList.add(String.valueOf(count));
        argList.add("vpn_server");
        argList.add(serverConfig.server);
        argList.add("ping");
        argList.add(String.valueOf(pingResult));
        if (pingResult == HttpURLConnection.HTTP_OK) {
            argList.add("port");
            argList.add(String.valueOf(portResult));
            if (!portResult && exception != null) {
                argList.add("port_error");
                String errMsg = exception.getMessage();
                if (errMsg == null || errMsg.isEmpty()) {
                    errMsg = exception.toString();
                }
                argList.add(errMsg);
            }
        } else if (exception != null) {
            argList.add("ping_error");
            String errMsg = exception.getMessage();
            if (errMsg == null || errMsg.isEmpty()) {
                errMsg = exception.toString();
            }
            argList.add(errMsg);
        }
        argList.add("connectTest");
        argList.add(String.valueOf(isConnectSuccess));
        argList.add("error");
        argList.add(ex.getMessage());
        if (host != null) {
            argList.add("host");
            argList.add(host);
        }

        String[] args = new String[argList.size()];
        argList.toArray(args);
        logEventAsync("套接字", args);
    }
}
