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
    public String name; //city
    public String server;
    public String flag;
    public String nation;

    public ServerConfig(JSONObject json){
        try{
            name = json.optString("name", null);
            server = json.optString("server", null);
            flag = json.optString("flag", null);
            nation = json.optString("nation", null);
        }catch (Exception e){
            ShadowsocksApplication.handleException(e);
        }
    }

    private static ServerConfig addGlobalConfig(){
        return new ServerConfig("Global", "opt.vpnnest.com", "ic_flag_global", "Select the fastest server");
    }

    private static ServerConfig addGlobalConfig(Resources resources){
        String name = resources.getString(R.string.vpn_name_opt);
        String server = resources.getString(R.string.vpn_server_opt);
        String flag = resources.getResourceEntryName(R.drawable.ic_flag_global);
        String nation = resources.getString(R.string.vpn_nation_opt);
        return new ServerConfig(name, server, flag, nation);
    }

    public ServerConfig(String name, String server, String flag, String nation){
        this.name = name;
        this.server = server;
        this.flag = flag;
        this.nation = nation;
    }

//    public static ArrayList<ServerConfig> createServerList(String jsonArrayString){
//        ArrayList<ServerConfig> arrayList = null;
//        try{
//            JSONArray jsonArray = new JSONArray(jsonArrayString);
//            if(jsonArray.length() > 0){
//                arrayList = new ArrayList<>(jsonArray.length());
//                arrayList.add(addGlobalConfig());
//                for(int i=0; i< jsonArray.length(); i++){
//                    JSONObject jsonObject = jsonArray.getJSONObject(i);
//                    ServerConfig serverConfig = new ServerConfig(jsonObject);
//                    arrayList.add(serverConfig);
//                }
//            }
//        }catch (Exception e){
//            ShadowsocksApplication.handleException(e);
//        }
//        return arrayList;
//    }

    public static ArrayList<ServerConfig> createServerList(Context context, String jsonArrayString){
        ArrayList<ServerConfig> arrayList = null;
        try{
            JSONArray jsonArray = new JSONArray(jsonArrayString);
            if(jsonArray.length() > 0){
                arrayList = new ArrayList<>(jsonArray.length());
                arrayList.add(addGlobalConfig(context.getResources()));
                for(int i=0; i< jsonArray.length(); i++){
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    ServerConfig serverConfig = new ServerConfig(jsonObject);
                    arrayList.add(serverConfig);
                }
            }
        }catch (Exception e){
            ShadowsocksApplication.handleException(e);
        }
        return arrayList;
    }

    public static ArrayList<ServerConfig> createDefaultServerList(Resources resources){
        TypedArray names = resources.obtainTypedArray(R.array.vpn_names);
        TypedArray icons = resources.obtainTypedArray(R.array.vpn_icons);
        TypedArray servers = resources.obtainTypedArray(R.array.vpn_servers);
        TypedArray nations = resources.obtainTypedArray(R.array.vpn_nations);
        ArrayList<ServerConfig> arrayList = new ArrayList<>(names.length());
        for(int i=0; i<names.length(); i++){
            String name = names.getString(i);
            String server = servers.getString(i);
            String flag = resources.getResourceEntryName(icons.getResourceId(i, R.drawable.ic_close_24dp));
            String nation = nations.getString(i);
            ServerConfig serverConfig = new ServerConfig(name, server, flag, nation);
            arrayList.add(serverConfig);
        }
        return arrayList;
    }

    public int getResourceId(Context context){
        return context.getResources().getIdentifier(flag, "drawable", context.getPackageName());
    }

}
