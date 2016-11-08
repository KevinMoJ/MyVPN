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
import java.util.HashMap;
import java.util.List;

/**
 * Created by jim on 16/11/7.
 */

public class ServerConfig {
    public String name;
    public String server;
    public String flag;
//    public int connection;
    public Uri iconUri;
    private HashMap<String, Integer> flagResMap;

//    private HashMap<String, Integer> flagResMap = new HashMap<>();
//    static{
//        flagResMap.put("ic_flag_global_anim", R.drawable.ic_flag_global_anim);
//        flagResMap.put("ic_flag_us", R.drawable.ic_flag_us);
//        flagResMap.put("ic_flag_uk", R.drawable.ic_flag_uk);
//        flagResMap.put("ic_flag_fr", R.drawable.ic_flag_fr);
//        flagResMap.put("ic_flag_jp", R.drawable.ic_flag_jp);
//        flagResMap.put("ic_flag_au", R.drawable.ic_flag_au);
//        flagResMap.put("ic_flag_de", R.drawable.ic_flag_de);
//        flagResMap.put("ic_flag_sg", R.drawable.ic_flag_sg);
//        flagResMap.put("ic_flag_nl", R.drawable.ic_flag_nl);
//    }

    public ServerConfig(Resources resources, JSONObject json){
        flagResMap = new HashMap<>();
        TypedArray icons = resources.obtainTypedArray(R.array.vpn_icons);
        for(int i=0; i<icons.length(); i++){
            int resId = icons.getResourceId(i, R.drawable.ic_flag_global_anim);
            flagResMap.put(resources.getResourceEntryName(resId), resId);
        }

        try{
            name = json.optString("name", null);
            server = json.optString("server", null);
            flag = json.optString("flag", null);
            if(!flag.startsWith("http://")){
                int flagRes = flagResMap.get(flag);
                iconUri = createUriFromResourceId(resources, flagRes);
            }
        }catch (Exception e){
            ShadowsocksApplication.handleException(e);
        }
    }

    private static ServerConfig addGlobalConfig(Resources resources){
        return new ServerConfig("Global", "opt.vpnnest.com",
                createUriFromResourceId(resources, R.drawable.ic_flag_global_anim));
    }

    public ServerConfig(String name, String server, Uri uri){
        this.name = name;
        this.server = server;
        iconUri = uri;
    }

    public static ArrayList<ServerConfig> createServerList(Resources resources, String jsonArrayString){
        ArrayList<ServerConfig> arrayList = null;
        try{
            JSONArray jsonArray = new JSONArray(jsonArrayString);
            if(jsonArray.length() > 0){
                arrayList = new ArrayList<>(jsonArray.length());
                arrayList.add(addGlobalConfig(resources));
                for(int i=0; i< jsonArray.length(); i++){
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    ServerConfig serverConfig = new ServerConfig(resources, jsonObject);
                    arrayList.add(serverConfig);
                }
            }


        }catch (Exception e){
            ShadowsocksApplication.handleException(e);
        }
        return arrayList;
    }

    public static ArrayList<ServerConfig> createDefaultServerList(Context context){
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

    public static Uri createUriFromResourceId(Resources resources, int resId){
        final Uri uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(resources.getResourcePackageName(resId))
                .appendPath(resources.getResourceTypeName(resId))
                .appendPath(resources.getResourceEntryName(resId))
                .build();
        return uri;
    }

    public static int getResourceId(Context context, Uri uri){
        List<String> pars = uri.getPathSegments();
        return context.getResources().getIdentifier(pars.get(1),pars.get(0), context.getPackageName());
    }

    public int getResourceId(Context context){
        return getResourceId(context, iconUri);
    }
}
