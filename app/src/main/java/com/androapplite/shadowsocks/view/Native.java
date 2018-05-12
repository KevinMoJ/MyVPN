package com.androapplite.shadowsocks.view;

/**
 * Created by jikai on 8/1/17.
 */

public class Native {
    static {
        System.loadLibrary("des");
    }

    public static native String decode(String text);
    public static native String encode(String text);
    public static native String decode_base64(String text);
    public static native String encode_base64(String text);
}
