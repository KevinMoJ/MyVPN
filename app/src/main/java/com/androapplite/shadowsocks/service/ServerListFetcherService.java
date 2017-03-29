package com.androapplite.shadowsocks.service;


import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Pair;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.model.ServerConfig;
import com.androapplite.vpn3.BuildConfig;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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

public class ServerListFetcherService extends IntentService implements Handler.Callback{
    private boolean hasStart;
    private static final String SGP_URL = "http://s3-ap-southeast-1.amazonaws.com/vpn-sl-sgp/3.json";
    private static final String BOM_URL = "http://s3.ap-south-1.amazonaws.com/vpn-sl-bom/3.json";
    private static final String IP_URL = "http://23.20.85.166:8080/VPNServerList/fsl";
    private static final String DOMAIN_URL = "http://s3c.vpnnest.com:8080/VPNServerList/fsl";
    private static final ArrayList<String> FAST_URLS = new ArrayList<>();
    static {
        FAST_URLS.add(SGP_URL);
        FAST_URLS.add(BOM_URL);
        FAST_URLS.add(IP_URL);
    }
    private static final HashMap<String, String> URL_KEY_MAP = new HashMap<>();
    static {
        URL_KEY_MAP.put(SGP_URL, "sgp");
        URL_KEY_MAP.put(BOM_URL, "bom");
        URL_KEY_MAP.put(IP_URL, "ip");
        URL_KEY_MAP.put(DOMAIN_URL, "domain");
    }
    private ScheduledExecutorService mGetFirstServerListService;
    private String mServerListJsonString;
    private Handler mServerListFastFetchHandler;
    private OkHttpClient mHttpClient;
    private String mUrl;

    public ServerListFetcherService(){
        super("ServletListFetcher");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(intent != null && !hasStart){
            hasStart = true;
            SharedPreferences.Editor editor = DefaultSharedPrefeencesUtil.getDefaultSharedPreferencesEditor(this);
            editor.remove(SharedPreferenceKey.SERVER_LIST).commit();
            Cache cache = new Cache(getCacheDir(), 1024 * 1024);
            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .writeTimeout(5, TimeUnit.SECONDS)
                    .cache(cache)
                    .addInterceptor(new UnzippingInterceptor());
            if(BuildConfig.DEBUG){
                builder.addInterceptor(new LoggingInterceptor());
            }
            mHttpClient = builder.build();
            Collections.shuffle(FAST_URLS);
            mGetFirstServerListService = Executors.newScheduledThreadPool(FAST_URLS.size() + 1);
            HandlerThread serverListFastFetchHandlerThread = new HandlerThread("serverListFastFetchHandlerThread");
            serverListFastFetchHandlerThread.start();
            mServerListFastFetchHandler = new Handler(serverListFastFetchHandlerThread.getLooper(), this);
            long t1 = System.currentTimeMillis();
            for(int i = 0; i < FAST_URLS.size(); i++){
                String url = FAST_URLS.get(i);
                mGetFirstServerListService.schedule(new FastFetchServerListRunnable(this, url), i, TimeUnit.SECONDS);
            }
            mGetFirstServerListService.schedule(new FastFetchServerListRunnable(this, DOMAIN_URL), FAST_URLS.size(), TimeUnit.SECONDS);
            try {
                mGetFirstServerListService.awaitTermination(15, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                ShadowsocksApplication.handleException(e);
            }
            serverListFastFetchHandlerThread.quit();
            long t2 = System.currentTimeMillis();
            String urlKey = URL_KEY_MAP.get(mUrl);
            if(urlKey == null){
                urlKey = "没有匹配的url";
            }
            //取结果
            if(mServerListJsonString != null){
                editor.putString(SharedPreferenceKey.SERVER_LIST, mServerListJsonString).commit();
                Firebase.getInstance(this).logEvent("取服务器列表成功总时间", urlKey, t2-t1);
            }else{
                Firebase.getInstance(this).logEvent("取服务器列表失败总时间", t2-t1);
            }
            String localCountry = getResources().getConfiguration().locale.getDisplayCountry();
            TelephonyManager manager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
            String simOperator = manager.getSimOperator();
            String iosCountry = manager.getSimCountryIso();
            Firebase.getInstance(this).logEvent("服务器url", urlKey, String.format("%s|%s|%s", iosCountry, simOperator, localCountry));
            broadcastServerListFetchFinish();
            hasStart = false;
        }

    }

    private static class FastFetchServerListRunnable implements Runnable{
        private WeakReference<ServerListFetcherService> mServiceReference;
        private String mUrl;
        FastFetchServerListRunnable(ServerListFetcherService service, String url){
            mServiceReference = new WeakReference<ServerListFetcherService>(service);
            mUrl = url;
        }

        @Override
        public void run() {
            ServerListFetcherService service = mServiceReference.get();
            if(service != null){
                Request request = new Request.Builder()
                        .url(mUrl)
                        .addHeader("Accept-Encoding", "gzip")
                        .build();
                long t1 = System.currentTimeMillis();
                Firebase firebase = Firebase.getInstance(service);
                String urlKey = URL_KEY_MAP.get(mUrl);
                if(urlKey == null){
                    urlKey = "没有匹配的url";
                }
                try {
                    Response response = service.mHttpClient.newCall(request).execute();
                    long t2 = System.currentTimeMillis();
                    firebase.logEvent("访问服务器列表成功", urlKey, t2-t1);
                    String jsonString = response.body().string();
                    if(jsonString != null && !jsonString.isEmpty() && ServerConfig.checkServerConfigJsonString(jsonString)) {
                        Message message = service.mServerListFastFetchHandler.obtainMessage();
                        message.obj = new Pair<String, String>(mUrl, jsonString);
                        service.mServerListFastFetchHandler.sendMessage(message);
                    }
                } catch (IOException e) {
                    long t2 = System.currentTimeMillis();

                    firebase.logEvent("访问服务器列表失败", urlKey, t2 - t1);

                    String errMsg = e.getMessage();
                    if(errMsg != null) {
                        firebase.logEvent("访问服务器列表失败", urlKey, errMsg);
                    }else{
                        firebase.logEvent("访问服务器列表失败", urlKey, e.toString());
                    }
                }
            }
        }
    }

    public static void fetchServerListAsync(Context context){
        Intent intent = new Intent(context, ServerListFetcherService.class);
        context.startService(intent);
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

    @Override
    public boolean handleMessage(Message msg) {
        if(mServerListJsonString == null){
            Pair<String, String> pair = (Pair<String, String>)msg.obj;
            mUrl = pair.first;
            mServerListJsonString = pair.second;
        }
        mGetFirstServerListService.shutdown();
        mGetFirstServerListService.shutdownNow();
        return true;
    }
}
