package com.androapplite.shadowsocks.model;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.net.Uri;
import android.support.annotation.IdRes;

import com.androapplite.shadowsocks.R;
import com.androapplite.shadowsocks.ShadowsocksApplication;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by jim on 16/11/7.
 */

public class ServerConfig {
    public String name;
    public String server;
    public String flag;
    public int connection;
    public Uri iconUri;

    public ServerConfig(JSONObject json){
        try{
            name = json.optString("name", null);
            server = json.optString("server", null);
            flag = json.optString("flag", null);
            connection = json.optInt("connection", -1);
        }catch (Exception e){
            ShadowsocksApplication.handleException(e);
        }
    }

    public ServerConfig(String name, String server, Uri uri){
        this.name = name;
        this.server = server;
        iconUri = uri;
    }

    public ArrayList<ServerConfig> createServerList(String jsonArrayString){
        ArrayList<ServerConfig> arrayList = null;
        try{
            JSONArray jsonArray = new JSONArray(jsonArrayString);
            arrayList = new ArrayList<>(jsonArray.length());
            for(int i=0; i< jsonArray.length(); i++){
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                ServerConfig serverConfig = new ServerConfig(jsonObject);
                arrayList.add(serverConfig);
            }
        }catch (Exception e){
            ShadowsocksApplication.handleException(e);
        }
        return arrayList;
    }

    public ArrayList<ServerConfig> createDefaultServerList(Context context){
        Resources resources = context.getResources();
        TypedArray names = resources.obtainTypedArray(R.array.vpn_names);
        TypedArray icons = resources.obtainTypedArray(R.array.vpn_icons);
        TypedArray servers = resources.obtainTypedArray(R.array.vpn_servers);
        ArrayList<ServerConfig> arrayList = new ArrayList<>(names.length());
        for(int i=0; i<names.length(); i++){
            String name = names.getString(i);
            String server = servers.getString(i);
            Uri uri = createUriFromResourceId(resources, icons.getResourceId(i, R.drawable.ic_close_24dp));
            ServerConfig serverConfig = new ServerConfig(name, server, uri);
            arrayList.add(serverConfig);
        }
        return arrayList;
    }

    public Uri createUriFromResourceId(Resources resources, @IdRes int resId){
        final Uri uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(resources.getResourcePackageName(resId))
                .appendPath(resources.getResourceTypeName(resId))
                .appendPath(resources.getResourceEntryName(resId))
                .build();
        return uri;
    }
}
