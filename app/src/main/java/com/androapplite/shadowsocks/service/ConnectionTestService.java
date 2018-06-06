package com.androapplite.shadowsocks.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.os.SystemClock;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.utils.ConnectVpnHelper;

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
    private static final String URL_BING = "https://www.bing.com/";
    private static final String URL_GOOGLE = "http://www.gstatic.com/generate_204";
    private static final String URL = "URL";

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
    public static void testConnectionWithVPN(Context context, String serverName) {
        Intent intent = new Intent(context, ConnectionTestService.class);
        intent.putExtra(SERVER_NAME, serverName).putExtra(URL, URL_GOOGLE);
        context.startService(intent);
    }

    public static void testConnectionWithoutVPN(Context context) {
        Intent intent = new Intent(context, ConnectionTestService.class);
        intent.putExtra(URL, URL_BING);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String serverName = intent.getStringExtra(SERVER_NAME);
            String url = intent.getStringExtra(URL);
            OkHttpClient client = new OkHttpClient();
            try {
                Request request = new Request.Builder()
                        .url(url)
                        .build();
                Firebase firebase = Firebase.getInstance(this);
                long t1 = System.currentTimeMillis();
                boolean result = false;
                for (int i = 0; i < 3; i++) {
                    if (testConnection(client, request)) {
                        result = true;
                        break;
                    }
                    SystemClock.sleep(50);
                }
                long timeConsume = System.currentTimeMillis() - t1;
                if (url.equals(URL_GOOGLE)) {
                    if (result) {
                        firebase.logEvent("连接后测试成功", serverName, timeConsume);
                    } else {
                        firebase.logEvent("连接后测试失败", serverName, timeConsume);
                        ConnectVpnHelper.getInstance(this).switchProxyService();
                    }
                } else if (url.equals(URL_BING)) {
                    firebase.logEvent("连接失败后测试", String.valueOf(result), timeConsume);
                }
            } catch (Exception e) {
                ShadowsocksApplication.handleException(e);
            }
        }
    }

    private boolean testConnection(OkHttpClient client, Request request) {
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

}
