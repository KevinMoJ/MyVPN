package com.androapplite.shadowsocks.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.support.annotation.DrawableRes;

import com.androapplite.vpn3.R;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.activity.ConnectivityActivity;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by jim on 16/11/7.
 */

public class ServerConfig {
    public String name; //city
    public String server;
    public String flag;
    public String nation;
    public int signal;
    public int port;

    public static final int[] SINAL_IMAGES = {
            R.drawable.server_signal_1,
            R.drawable.server_signal_2,
            R.drawable.server_signal_3,
            R.drawable.server_signal_4
    };

    private static final int DEFAULT_PORT = 40010;

    private static ServerConfig addGlobalConfig(Resources resources){
        String name = resources.getString(R.string.vpn_name_opt);
        String server = resources.getString(R.string.vpn_server_opt);
        String flag = resources.getResourceEntryName(R.drawable.ic_flag_global);
        String nation = resources.getString(R.string.vpn_nation_opt);
        return new ServerConfig(name, server, flag, nation, SINAL_IMAGES.length - 1);
    }

    public ServerConfig(String name, String server, String flag, String nation, int signal){
        this(name, server, flag, nation, signal, DEFAULT_PORT);
    }

    public ServerConfig(String name, String server, String flag, String nation, int signal, int port){
        this.name = name;
        this.server = server;
        this.flag = flag;
        this.nation = nation;
        this.signal = signal;
        this.port = port;
    }



    @Override
    public boolean equals(Object o) {
        if(this == o){
            return true;
        }else if(!(o instanceof ServerConfig)){
            return false;
        }else{
            ServerConfig other = (ServerConfig)o;
            return name.equals(other.name) && server.equals(other.server)
                    && flag.equals(other.flag) && nation.equals(other.nation);
        }

    }

    public static ArrayList<ServerConfig> createServerList(Context context, String jsonArrayString){
        ArrayList<ServerConfig> arrayList = null;
        try{
            JSONObject jsonObject = new JSONObject(jsonArrayString);
            JSONArray cityArray = jsonObject.optJSONArray("city");
            JSONArray ipArray = jsonObject.optJSONArray("ip");
            JSONArray signalArray = jsonObject.optJSONArray("signal");
            JSONArray portArray = jsonObject.optJSONArray("port");


            if(cityArray != null && ipArray != null && signalArray != null){
                Resources resources = context.getResources();
                TypedArray names = resources.obtainTypedArray(R.array.vpn_names);
                TypedArray icons = resources.obtainTypedArray(R.array.vpn_icons);
                TypedArray nations = resources.obtainTypedArray(R.array.vpn_nations);

                ArrayList<String> nameList = new ArrayList<>(names.length());
                for(int i=0; i<names.length(); i++){
                    String name = names.getString(i);
                    nameList.add(name);
                }

                arrayList = new ArrayList<>(cityArray.length() + 1);
                arrayList.add(addGlobalConfig(context.getResources()));
                for(int i=0; i<cityArray.length(); i++){
                    String city = cityArray.optString(i);
                    int index = nameList.indexOf(city);
                    if(index > -1) {
                        String nation = nations.getString(index);
                        String icon = resources.getResourceEntryName(icons.getResourceId(index, R.drawable.ic_flag_global));
                        String ip = ipArray.optString(i);
                        int signal = signalArray.optInt(i);
                        ServerConfig serverConfig = null;
                        if(portArray != null){
                            int port = portArray.optInt(i);
                            serverConfig = new ServerConfig(city, ip, icon, nation, signal, port);
                        }else {
                            serverConfig = new ServerConfig(city, ip, icon, nation, signal);
                        }
                        arrayList.add(serverConfig);
                    }
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
        for(int i=0; i<servers.length(); i++){
            String name = names.getString(i);
            String server = servers.getString(i);
            String flag = resources.getResourceEntryName(icons.getResourceId(i, R.drawable.ic_bluetooth_24dp));
            String nation = nations.getString(i);
            ServerConfig serverConfig = new ServerConfig(name, server, flag, nation, 3);
            arrayList.add(serverConfig);
        }
        return arrayList;
    }

    public int getResourceId(Context context){
        return context.getResources().getIdentifier(flag, "drawable", context.getPackageName());
    }

    public int getSignalResId(){
        return SINAL_IMAGES[signal];
    }

    public void saveInSharedPreference(SharedPreferences sharedPreferences) {
        sharedPreferences.edit()
                .putString(SharedPreferenceKey.CONNECTING_VPN_NAME, name)
                .putString(SharedPreferenceKey.CONNECTING_VPN_SERVER, server)
                .putString(SharedPreferenceKey.CONNECTING_VPN_FLAG, flag)
                .putString(SharedPreferenceKey.CONNECTING_VPN_NATION, nation)
                .putInt(SharedPreferenceKey.CONNECTING_VPN_SIGNAL, signal)
                .putInt(SharedPreferenceKey.CONNECTING_VPN_PORT, port)
                .commit();
    }

    public static ServerConfig loadFromSharedPreference(SharedPreferences sharedPreferences){
        String vpnName = sharedPreferences.getString(SharedPreferenceKey.CONNECTING_VPN_NAME, null);
        String server = sharedPreferences.getString(SharedPreferenceKey.CONNECTING_VPN_SERVER, null);
        String flag = sharedPreferences.getString(SharedPreferenceKey.CONNECTING_VPN_FLAG, null);
        String nation = sharedPreferences.getString(SharedPreferenceKey.CONNECTING_VPN_NATION, null);
        int signal = sharedPreferences.getInt(SharedPreferenceKey.CONNECTING_VPN_SIGNAL, 0);
        int port = sharedPreferences.getInt(SharedPreferenceKey.CONNECTING_VPN_PORT, DEFAULT_PORT);
        if(vpnName != null && server != null && flag != null && nation != null){
            return new ServerConfig(vpnName, server, flag, nation, signal, port);
        }else{
            return null;
        }

    }
}
