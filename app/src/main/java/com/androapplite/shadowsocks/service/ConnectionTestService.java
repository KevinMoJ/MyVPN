package com.androapplite.shadowsocks.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;

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
    public static void testConnection(Context context) {
        Intent intent = new Intent(context, ConnectionTestService.class);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            OkHttpClient client = new OkHttpClient();
            String url = "http://www.bing.com/";
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            long t1 = System.currentTimeMillis();
            Response response = null;
            try {
                response = client.newCall(request).execute();
                long t2 = System.currentTimeMillis();
                GAHelper.sendTimingEvent(this, "连接测试", "成功", t2-t1);
            } catch (IOException e) {
                long t2 = System.currentTimeMillis();
                GAHelper.sendTimingEvent(this, "连接测试", "失败", t2-t1);
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
