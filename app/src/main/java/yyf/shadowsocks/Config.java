package yyf.shadowsocks;

import android.os.Parcel;
import android.os.Parcelable;

import java.net.UnknownHostException;

public class Config implements Parcelable {
    public boolean isGlobalProxy = true;
    public boolean isGFWList = true;
    public boolean isBypassApps = false;
    public boolean isTrafficStat = false;
    public boolean isUdpDns = false;

    public String profileName = "US Server";
    private String proxy = "hub.vpnnest.com";
    private String sitekey = "vpnnest!@#123d";

    private int remotePort = 40010;
    /*临时测试server
    public String proxy = "52.10.0.180";
    public String sitekey = "abc!@#123d";
    public int remotePort = 28388;
     */

    public int localPort = 1080;
    public String proxiedAppString = "";
    public String encMethod = "aes-256-cfb";
    //public String route = "all";
    public static final Creator<Config> CREATOR = new Creator<Config>() {
        public Config createFromParcel(Parcel in) {
            return new Config(in);
        }

        public Config[] newArray(int size) {
            return new Config[size];
        }
    };

    public Config() {

    }

    public Config(boolean isGlobalProxy, boolean isGFWList, boolean isBypassApps,
                  boolean isTrafficStat, boolean isUdpDns, String profileName, String proxy, String sitekey,
                  String encMethod, String proxiedAppString, String route, int remotePort, int localPort) {
        this.isGlobalProxy = isGlobalProxy;
        this.isGFWList = isGFWList;
        this.isBypassApps = isBypassApps;
        this.isTrafficStat = isTrafficStat;
        this.isUdpDns = isUdpDns;
        this.profileName = profileName;
        this.proxy = proxy;
        this.sitekey = sitekey;
        this.encMethod = encMethod;
        this.proxiedAppString = proxiedAppString;
        //this.route = route;
        this.remotePort = remotePort;
        this.localPort = localPort;
    }

    private Config(Parcel in) {
        readFromParcel(in);
    }

    public void readFromParcel(Parcel in) {
        isGlobalProxy = in.readInt() == 1;
        isGFWList = in.readInt() == 1;
        isBypassApps = in.readInt() == 1;
        isTrafficStat = in.readInt() == 1;
        isUdpDns = in.readInt() == 1;
        profileName = in.readString();
        proxy = in.readString();
        sitekey = in.readString();
        encMethod = in.readString();
        proxiedAppString = in.readString();
        //route = in.readString();
        remotePort = in.readInt();
        localPort = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(isGlobalProxy ? 1 : 0);
        out.writeInt(isGFWList ? 1 : 0);
        out.writeInt(isBypassApps ? 1 : 0);
        out.writeInt(isTrafficStat ? 1 : 0);
        out.writeInt(isUdpDns ? 1 : 0);
        out.writeString(profileName);
        out.writeString(proxy);
        out.writeString(sitekey);
        out.writeString(encMethod);
        out.writeString(proxiedAppString);
        //out.writeString(route);
        out.writeInt(remotePort);
        out.writeInt(localPort);
    }

    public void setProxy(String proxy){
        this.proxy = proxy;
    }

    public String getProxy(){
        return proxy;
    }

    public String getSitekey(){
        return sitekey;
    }

    public int getRemotePort(){
        return remotePort;
    }


}
