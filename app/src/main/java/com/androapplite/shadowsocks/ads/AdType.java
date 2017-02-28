package com.androapplite.shadowsocks.ads;

public class AdType {
    public static final int ADMOB_BANNER = 1;
    public static final int ADMOB_NATIVE = 2;
    public static final int ADMOB_FULL = 3;
    public static final int FACEBOOK_BANNER = 4;
    public static final int FACEBOOK_NATIVE = 5;
    public static final int FACEBOOK_FULL = 6;

    private int type;
    public AdType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
