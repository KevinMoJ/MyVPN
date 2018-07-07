package com.androapplite.shadowsocks.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;

import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.activity.VIPActivity;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.androapplite.vpn3.R;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

/**
 * Created by jim on 16/11/7.
 */

public class ServerConfig implements Parcelable {
    private static final int DEFAULT_PORT = 40050;

    public String name; //city
    public String server; // ip
    public String flag; // icon
    public String nation; // country
    public int signal; // load
    public int port;  // port
    private int load;

    public static final int[] SINAL_IMAGES = {
            R.drawable.server_signal_full,
            R.drawable.server_signal_2,
            R.drawable.server_signal_3,
            R.drawable.server_signal_4
    };

    private static ServerConfig addGlobalConfig(Resources resources) {
        String city = resources.getString(R.string.vpn_name_opt);
        String ip = resources.getString(R.string.vpn_server_opt);
        String icon = resources.getResourceEntryName(VIPActivity.isVIPUser(ShadowsocksApplication.getGlobalContext()) ? R.drawable.icon_vip_server : R.drawable.ic_flag_global);
        String country = resources.getString(R.string.vpn_nation_opt);
        return new ServerConfig(city, country, ip, SINAL_IMAGES.length - 1, icon);
    }

    public ServerConfig(String city, String country, String ip, int load, String icon) {
        this(city, country, ip, load, DEFAULT_PORT, icon);
    }

    public ServerConfig(String city, String country, String ip, int load, int port, String icon) {
        this.name = city;
        this.nation = country;
        this.server = ip;
        this.signal = getLoadLevel(load);
        this.port = port;
        this.flag = icon;
        this.load = load;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof ServerConfig)) {
            return false;
        } else {
            ServerConfig other = (ServerConfig) o;
            return name.equals(other.name) && server.equals(other.server)
                    && flag.equals(other.flag) && nation.equals(other.nation);
        }

    }

    // [{ "ct":"Miami","ip":"192.168.1.1","ld":80,"pt":[40050]},{"city":"Miami","ip":"192.168.1.1","ld":40, "pt":[40050, 40051]}]
    public static ArrayList<ServerConfig> createServerList(Context context, String jsonArrayString) {
        ArrayList<ServerConfig> arrayList = new ArrayList<>();
        ListCompare listCompare = new ListCompare();

        try {
            JSONArray jsonArray = new JSONArray(jsonArrayString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject object = (JSONObject) jsonArray.get(i);
                String city = object.getString("ct");
                String ip = object.getString("ip");
                int ld = object.getInt("ld");
                JSONArray ptJsonArray = object.getJSONArray("pt");

                if (city != null && ip != null && ptJsonArray.length() != 0) {
                    Resources resources = context.getResources();
                    TypedArray names = resources.obtainTypedArray(R.array.vpn_names);
                    TypedArray icons = resources.obtainTypedArray(R.array.vpn_icons);
                    TypedArray nations = resources.obtainTypedArray(R.array.vpn_nations);

                    ArrayList<String> nameList = new ArrayList<>();
                    for (int k = 0; k < names.length(); k++) {
                        String name = names.getString(k);
                        nameList.add(name);
                    }

                    int index = nameList.indexOf(city);
                    if (index > -1) {
                        String country = nations.getString(index);
                        String icon = resources.getResourceEntryName(icons.getResourceId(index, VIPActivity.isVIPUser(ShadowsocksApplication.getGlobalContext()) ? R.drawable.icon_vip_server : R.drawable.ic_flag_global));
                        for (int j = 0; j < ptJsonArray.length(); j++) {
                            ServerConfig ServerConfig = new ServerConfig(city, country, ip, ld, Integer.parseInt(ptJsonArray.getString(j)), icon);
                            arrayList.add(ServerConfig);
                        }
                    }
                }
            }
            boolean isFetchToServer = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(context).getBoolean(SharedPreferenceKey.IS_FETCH_SERVER_LIST_AT_SERVER, false);
            if (isFetchToServer)
                Collections.sort(arrayList, listCompare);
            else
                Collections.shuffle(arrayList);
            arrayList.add(0, addGlobalConfig(context.getResources()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return arrayList;
    }

    public static String shuffleRemoteConfig() {
        return FirebaseRemoteConfig.getInstance().getString("fetch_server_list");
    }

    public int getSignalResId() {
        return SINAL_IMAGES[signal];
    }

    private int getLoadLevel(int load) {
        if (load >= 0 && load < 25)
            return 3;
        else if (load >= 25 && load < 50)
            return 2;
        else if (load >= 50 && load < 75)
            return 1;
        else
            return 0;
    }

    private static class ListCompare implements Comparator<ServerConfig> {

        @Override
        public int compare(ServerConfig ar1, ServerConfig ar2) {
            int a = ar1.getLoad() - ar2.getLoad();
            if (a > 0)
                return 1;
            else if (a < 0)
                return -1;
            else
                return 0;
        }
    }

    public void saveInSharedPreference(SharedPreferences sharedPreferences) {
        sharedPreferences.edit()
                .putString(SharedPreferenceKey.CONNECTING_VPN_NAME, name)
                .putString(SharedPreferenceKey.CONNECTING_VPN_SERVER, server)
                .putString(SharedPreferenceKey.CONNECTING_VPN_FLAG, flag)
                .putString(SharedPreferenceKey.CONNECTING_VPN_NATION, name)
                .putInt(SharedPreferenceKey.CONNECTING_VPN_SIGNAL, signal)
                .putInt(SharedPreferenceKey.CONNECTING_VPN_PORT, port)
                .apply();
    }

    public static ServerConfig loadFromSharedPreference(SharedPreferences sharedPreferences) {
        String city = sharedPreferences.getString(SharedPreferenceKey.CONNECTING_VPN_NAME, null);
        String ip = sharedPreferences.getString(SharedPreferenceKey.CONNECTING_VPN_SERVER, null);
        String icon = sharedPreferences.getString(SharedPreferenceKey.CONNECTING_VPN_FLAG, null);
        String country = sharedPreferences.getString(SharedPreferenceKey.CONNECTING_VPN_NATION, null);
        int load = sharedPreferences.getInt(SharedPreferenceKey.CONNECTING_VPN_SIGNAL, 0);
        int port = sharedPreferences.getInt(SharedPreferenceKey.CONNECTING_VPN_PORT, DEFAULT_PORT);
        if (city != null && ip != null && icon != null && country != null) {
            return new ServerConfig(city, country, ip, load, port, icon);
        } else {
            return null;
        }
    }

    public static boolean checkServerConfigJsonString(String jsonString) {
        try {
//            JSONObject jsonObject = new JSONObject(jsonString);
//            return jsonObject.has("ct") && jsonObject.has("ip") && jsonObject.has("ld") && jsonObject.has("pt");
            new JSONArray(jsonString);
            return true;
        } catch (Exception e) {
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
        public ServerConfig createFromParcel(Parcel in) {
            return new ServerConfig(in);
        }

        public ServerConfig[] newArray(int size) {
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
            String url = String.format(Locale.ENGLISH, "ss://%s:%s@%s:%d", "aes-256-cfb", password, server, port);
            return url;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getLoad() {
        return load;
    }
}