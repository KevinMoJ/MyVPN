package com.androapplite.shadowsocks.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.androapplite.shadowsocks.R;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by jim on 16/11/7.
 */

public class ServerConfig implements Parcelable{
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

    public static String shuffleRemoteConfig(){
        FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
        String jsonArrayString = remoteConfig.getString("server_list");
        return shuffleStaticServerListJson(jsonArrayString);
    }

    public static String shuffleStaticServerListJson(String jsonArrayString){
        String shuffleJsonString = null;
        Log.d("server_list", jsonArrayString);
        try {
            JSONObject jsonObject = new JSONObject(jsonArrayString);
            JSONArray cityArray = jsonObject.optJSONArray("city");
            JSONArray ipArray = jsonObject.optJSONArray("ip");
            JSONArray signalArray = jsonObject.optJSONArray("signal");
            JSONArray portArray = jsonObject.optJSONArray("port");
            if(cityArray != null && ipArray != null && signalArray != null){
                Random random = new Random();
                for(int i = 0; i<cityArray.length() * 3 / 4; i++){
                    int pos = random.nextInt(cityArray.length());
                    //新旧位置不相等才交换
                    if(i != pos) {
                        swipePosition(cityArray, i, pos);
                        swipePosition(ipArray, i, pos);
                        swipePosition(signalArray, i, pos);
                        if (portArray != null) {
                            swipePosition(portArray, i, pos);
                        }
                    }
                }
                shuffleJsonString = jsonObject.toString();
                Log.d("server_list shuffle", shuffleJsonString);
            }

        }catch (Exception e){
            ShadowsocksApplication.handleException(e);
        }
        return shuffleJsonString;
    }



    private static void swipePosition(JSONArray array, int pos1, int pos2){
        try {
            Object obj = array.get(pos1);
            array.put(pos1, array.get(pos2));
            array.put(pos2, obj);
        }catch (Exception e){
            ShadowsocksApplication.handleException(e);
        }
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
                .apply();
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

    public static boolean checkServerConfigJsonString(String jsonString){
        try{
            JSONObject jsonObject = new JSONObject(jsonString);
            return jsonObject.has("city") && jsonObject.has("ip") && jsonObject.has("signal") && jsonObject.has("port");
        }catch (Exception e){
            return false;
        }
    }


    public void writeToParcel(Parcel out, int flags) {
        out.writeString(name);
        out.writeString(server);
        out.writeString(flag);
        out.writeString(nation);
        out.writeInt(signal);
        out.writeInt(port);
    }

    public static final Parcelable.Creator<ServerConfig> CREATOR = new Parcelable.Creator<ServerConfig>() {
        public ServerConfig createFromParcel(Parcel in)
        {
            return new ServerConfig(in);
        }
        public ServerConfig[] newArray(int size)
        {
            return new ServerConfig[size];
        }
    };

    private ServerConfig(Parcel in) {
        name = in.readString();
        server = in.readString();
        flag = in.readString();
        nation = in.readString();
        signal = in.readInt();
        port = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toProxyUrl() {
        try {
            String password = URLEncoder.encode("vpnnest!@#123d", "UTF-8");
            String url = String.format("ss://%s:%s@%s:%d", "aes-256-cfb", password, server, port);
            return url;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
