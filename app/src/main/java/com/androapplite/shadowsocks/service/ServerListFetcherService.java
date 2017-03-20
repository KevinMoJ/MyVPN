package com.androapplite.shadowsocks.service;


import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.vpn3.BuildConfig;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.http.RealResponseBody;
import okio.GzipSource;
import okio.Okio;

/**
 * Created by jim on 16/11/7.
 */

public class ServerListFetcherService extends IntentService {
    private boolean hasStart;

    public ServerListFetcherService(){
        super("ServletListFetcher");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(intent != null && !hasStart){
            hasStart = true;
            if(!fetchServerListByIp()) {
                fetchServerListByDomain();
            }
            hasStart = false;
        }

    }

    private boolean fetchServerListByIp() {
        SharedPreferences.Editor editor = DefaultSharedPrefeencesUtil.getDefaultSharedPreferencesEditor(this);
        editor.remove(SharedPreferenceKey.SERVER_LIST).commit();
        Cache cache = new Cache(getCacheDir(), 1024 * 1024);
        final OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .cache(cache)
                .addInterceptor(new UnzippingInterceptor());

        if(BuildConfig.DEBUG){
            builder.addInterceptor(new LoggingInterceptor());
        }
        OkHttpClient client = builder.build();

        String url = "http://23.20.85.166:8080/VPNServerList/fsl";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept-Encoding", "gzip")
                .build();


        long t1 = System.currentTimeMillis();
        Firebase firebase = Firebase.getInstance(this);
        try {
            Response response = client.newCall(request).execute();
            long t2 = System.currentTimeMillis();
            firebase.logEvent("访问服务器列表", "成功", t2-t1);
            String jsonString = response.body().string();
            if(jsonString != null && !jsonString.isEmpty()){
                editor.putString(SharedPreferenceKey.SERVER_LIST, jsonString).commit();
            }
            broadcastServerListFetchFinish();
            Log.d("ServerListFetcher", "ip");
            return true;
        } catch (IOException e) {
            long t2 = System.currentTimeMillis();
            firebase.logEvent("访问服务器列表", "失败", t2-t1);
            ShadowsocksApplication.handleException(e);
            broadcastServerListFetchError(e, true);
            return false;
        }
    }

    private void fetchServerListByDomain() {
        SharedPreferences.Editor editor = DefaultSharedPrefeencesUtil.getDefaultSharedPreferencesEditor(this);
        editor.remove(SharedPreferenceKey.SERVER_LIST).commit();
        Cache cache = new Cache(getCacheDir(), 1024 * 1024);
        final OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .cache(cache)
                .addInterceptor(new UnzippingInterceptor());

        if(BuildConfig.DEBUG){
            builder.addInterceptor(new LoggingInterceptor());
        }
        OkHttpClient client = builder.build();

        String url = "http://s3c.vpnnest.com:8080/VPNServerList/fsl";
//            String url = "http://192.168.31.29:8080/VPNServerList/fsl";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept-Encoding", "gzip")
                .build();


        long t1 = System.currentTimeMillis();
        Firebase firebase = Firebase.getInstance(this);
        try {
            Response response = client.newCall(request).execute();
            long t2 = System.currentTimeMillis();
            firebase.logEvent("访问服务器列表", "成功", t2-t1);
            String jsonString = response.body().string();
            if(jsonString != null && !jsonString.isEmpty()){
                editor.putString(SharedPreferenceKey.SERVER_LIST, jsonString).commit();
            }
            broadcastServerListFetchFinish();
            Log.d("ServerListFetcher", "domain");
        } catch (IOException e) {
            long t2 = System.currentTimeMillis();
            firebase.logEvent("访问服务器列表", "失败", t2-t1);
            ShadowsocksApplication.handleException(e);
            broadcastServerListFetchError(e, false);
        }
    }

    public static void fetchServerListAsync(Context context){
        Intent intent = new Intent(context, ServerListFetcherService.class);
        context.startService(intent);
    }

    public void broadcastServerListFetchError(Exception e, boolean isIpUrl){
        final Intent intent = new Intent(Action.SERVER_LIST_FETCH_FINISH);
        String errMsg = e.getMessage();
        if(errMsg != null) {
            intent.putExtra("ErrMsg", e.getMessage());
        }else{
            intent.putExtra("ErrMsg", e.toString());
        }
        intent.putExtra("IsIpUrl", isIpUrl);
        Firebase firebase = Firebase.getInstance(this);
        if(errMsg != null){
            if(isIpUrl) {
                firebase.logEvent("取服务器列表失败", "IP", errMsg);
            }else{
                firebase.logEvent("取服务器列表失败", "Domain", errMsg);
            }

        }else {
            if(isIpUrl) {
                firebase.logEvent("取服务器列表失败", "IP", e.toString());
            }else {
                firebase.logEvent("取服务器列表失败", "Domain", e.toString());
            }
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

    }

    private void broadcastServerListFetchFinish(){
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Action.SERVER_LIST_FETCH_FINISH));
    }

    static class LoggingInterceptor implements Interceptor {
        @Override public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();

            long t1 = System.nanoTime();
            Log.d("OkHttp", String.format("Sending request %s on %s%n%s",
                    request.url(), chain.connection(), request.headers()));

            Response response = chain.proceed(request);

            long t2 = System.nanoTime();
            Log.d("OkHttp", String.format("Received response for %s in %.1fms%n%s",
                    response.request().url(), (t2 - t1) / 1e6d, response.headers()));

            return response;
        }
    }

    static class UnzippingInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Response response = chain.proceed(chain.request());
            return unzip(response);
        }

        private Response unzip(final Response response) throws IOException {

            if (response.body() == null || !"gzip".equals(response.header("Content-Encoding"))) {
                return response;
            }

            GzipSource responseBody = new GzipSource(response.body().source());
            Headers strippedHeaders = response.headers().newBuilder()
                    .removeAll("Content-Encoding")
                    .removeAll("Content-Length")
                    .build();
            return response.newBuilder()
                    .headers(strippedHeaders)
                    .body(new RealResponseBody(strippedHeaders, Okio.buffer(responseBody)))
                    .build();
        }
    }

}
