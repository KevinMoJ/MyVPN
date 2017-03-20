package com.androapplite.shadowsocks.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.GAHelper;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class ConnectionTestService extends IntentService {
    private static final String SERVER_NAME = "SERVER_NAME";

    public ConnectionTestService() {
        super("ConnectionTestService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void testConnection(Context context, String serverName) {
        Intent intent = new Intent(context, ConnectionTestService.class);
        intent.putExtra(SERVER_NAME, serverName);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String serverName = intent.getStringExtra(SERVER_NAME);
            OkHttpClient client = new OkHttpClient();
            String url = "http://www.bing.com/";
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            long t1 = System.currentTimeMillis();
            Response response = null;
            Firebase firebase = Firebase.getInstance(this);
            try {
                response = client.newCall(request).execute();
                long t2 = System.currentTimeMillis();
                firebase.logEvent("连接后测试成功", serverName, t2-t1);
            } catch (IOException e) {
                long t2 = System.currentTimeMillis();
                firebase.logEvent("连接后测试失败", serverName, t2-t1);
                ShadowsocksApplication.handleException(e);
            }finally {
                if(response != null) {
                    try {
                        response.body().close();
                    }catch (Exception e){
                        ShadowsocksApplication.handleException(e);
                    }
                }
            }
        }
    }

}
