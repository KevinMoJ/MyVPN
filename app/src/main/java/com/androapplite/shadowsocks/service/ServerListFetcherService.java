package com.androapplite.shadowsocks.service;


import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by jim on 16/11/7.
 */

public class ServerListFetcherService extends IntentService {

    public ServerListFetcherService(){
        super("ServletListFetcher");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sp = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
        sp.edit().remove(SharedPreferenceKey.SERVER_LIST).apply();
        OkHttpClient client = new OkHttpClient();
        String url = "http://192.168.31.29/vpn/server_list.json";
        Request request = new Request.Builder()
                .url(url)
                .build();


        try {
            Response response = client.newCall(request).execute();
            String jsonString = response.body().string();
//            SharedPreferences sp = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(this);
            sp.edit().putString(SharedPreferenceKey.SERVER_LIST, jsonString).apply();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void fetchServerListAsync(Context context){
        Intent intent = new Intent(context, ServerListFetcherService.class);
        context.startService(intent);
    }


}
