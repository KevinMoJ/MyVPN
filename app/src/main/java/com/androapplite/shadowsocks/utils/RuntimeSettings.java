package com.androapplite.shadowsocks.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.model.VpnState;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;

/**
 * Created by KevinMo.J on 2018/7/26.
 */

public class RuntimeSettings {

    private RuntimeSettings(Context context) {
    }

    private static SharedPreferences getSharedPreferences() {
        return DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(ShadowsocksApplication.getGlobalContext());
    }

    public static void setVPNNation(String nation) {
        getSharedPreferences().edit().putString(SharedPreferenceKey.VPN_NATION, nation).apply();
    }

    public static String getVPNNation(String defaultNation) {
        return getSharedPreferences().getString(SharedPreferenceKey.VPN_NATION, defaultNation);
    }

    public static void setFlag(String nation) {
        getSharedPreferences().edit().putString(SharedPreferenceKey.VPN_FLAG, nation).apply();
    }

    public static String getFlag(String flag) {
        return getSharedPreferences().getString(SharedPreferenceKey.VPN_FLAG, flag);
    }

    public static void setLuckPanFreeDay(long day) {
        getSharedPreferences().edit().putLong(SharedPreferenceKey.LUCK_PAN_GET_FREE_DAY, day).apply();
    }

    public static long getLuckPanFreeDay() {
        return getSharedPreferences().getLong(SharedPreferenceKey.LUCK_PAN_GET_FREE_DAY, 0);
    }

    public static void setClickRotateStartCount(int count) {
        getSharedPreferences().edit().putInt(SharedPreferenceKey.LUCK_PAN_CLICK_START_COUNT, count).apply();
    }

    public static int getClickRotateStartCount() {
        return getSharedPreferences().getInt(SharedPreferenceKey.LUCK_PAN_CLICK_START_COUNT, 0);
    }

    public static void setLuckPanDialogShowCount(int count) {
        getSharedPreferences().edit().putInt(SharedPreferenceKey.LUCK_PAN_DIALOG_SHOW_COUNT, count).apply();
    }

    public static int getLuckPanDialogShowCount() {
        return getSharedPreferences().getInt(SharedPreferenceKey.LUCK_PAN_DIALOG_SHOW_COUNT, 0);
    }

    public static void setLuckPanGetRecord(long day) {
        getSharedPreferences().edit().putLong(SharedPreferenceKey.LUCK_PAN_GET_DAY_TO_RECORD, day).apply();
    }

    public static long getLuckPanGetRecord() {
        return getSharedPreferences().getLong(SharedPreferenceKey.LUCK_PAN_GET_DAY_TO_RECORD, 0);
    }

    public static void setNewUserFreeUseTime(long time) {
        getSharedPreferences().edit().putLong(SharedPreferenceKey.NEW_USER_FREE_USER_TIME, time).apply();
    }

    public static long getNewUserFreeUseTime() {
        return getSharedPreferences().getLong(SharedPreferenceKey.NEW_USER_FREE_USER_TIME, 0);
    }

    public static void setLuckPanOpenStartTime(long time) {
        getSharedPreferences().edit().putLong(SharedPreferenceKey.LUCK_PAN_OPEN_START_TIME, time).apply();
    }

    public static long getLuckPanOpenStartTime() {
        return getSharedPreferences().getLong(SharedPreferenceKey.LUCK_PAN_OPEN_START_TIME, 0);
    }

    public static void setServerList(String serverList) {
        getSharedPreferences().edit().putString(SharedPreferenceKey.FETCH_SERVER_LIST, serverList).apply();
    }

    public static String getServerList() {
        return getSharedPreferences().getString(SharedPreferenceKey.FETCH_SERVER_LIST, null);
    }

    public static void setConnectingVPNName(String name) {
        getSharedPreferences().edit().putString(SharedPreferenceKey.CONNECTING_VPN_NAME, name).apply();
    }

    public static String getConnectingVPNName(String defaultName) {
        return getSharedPreferences().getString(SharedPreferenceKey.CONNECTING_VPN_NAME, defaultName);
    }

    public static void setConnectingVPNServer(String server) {
        getSharedPreferences().edit().putString(SharedPreferenceKey.CONNECTING_VPN_SERVER, server).apply();
    }

    public static String getConnectingVPNServer() {
        return getSharedPreferences().getString(SharedPreferenceKey.CONNECTING_VPN_SERVER, null);
    }

    public static void setConnectingVPNFlag(String flag) {
        getSharedPreferences().edit().putString(SharedPreferenceKey.CONNECTING_VPN_FLAG, flag).apply();
    }

    public static String getConnectingVPNFlag() {
        return getSharedPreferences().getString(SharedPreferenceKey.CONNECTING_VPN_FLAG, null);
    }

    public static void setConnectingVPNNation(String nation) {
        getSharedPreferences().edit().putString(SharedPreferenceKey.CONNECTING_VPN_NATION, nation).apply();
    }

    public static String getConnectingVPNNation() {
        return getSharedPreferences().getString(SharedPreferenceKey.CONNECTING_VPN_NATION, null);
    }

    public static void setConnectingVPNSignal(int signal) {
        getSharedPreferences().edit().putInt(SharedPreferenceKey.CONNECTING_VPN_SIGNAL, signal).apply();
    }

    public static int getConnectingVPNSignal() {
        return getSharedPreferences().getInt(SharedPreferenceKey.CONNECTING_VPN_SIGNAL, 0);
    }

    public static void setConnectingVPNPort(int port) {
        getSharedPreferences().edit().putInt(SharedPreferenceKey.CONNECTING_VPN_PORT, port).apply();
    }

    public static int getConnectingVPNPort(int defaultPort) {
        return getSharedPreferences().getInt(SharedPreferenceKey.CONNECTING_VPN_PORT, defaultPort);
    }

    public static void setUseTime(long time) {
        getSharedPreferences().edit().putLong(SharedPreferenceKey.USE_TIME, time).apply();
    }

    public static long getUseTime() {
        return getSharedPreferences().getLong(SharedPreferenceKey.USE_TIME, 0);
    }

    public static void setVPNState(int vpnState) {
        getSharedPreferences().edit().putInt(SharedPreferenceKey.VPN_STATE, vpnState).apply();
    }

    public static int getVPNState() {
        return getSharedPreferences().getInt(SharedPreferenceKey.VPN_STATE, VpnState.Init.ordinal());
    }

    public static void setFetchServerListAtServer(boolean fetchServerListAtServer) {
        getSharedPreferences().edit().putBoolean(SharedPreferenceKey.IS_FETCH_SERVER_LIST_AT_SERVER, fetchServerListAtServer).apply();
    }

    public static boolean getFetchServerListAtServer() {
        return getSharedPreferences().getBoolean(SharedPreferenceKey.IS_FETCH_SERVER_LIST_AT_SERVER, false);
    }

    public static void setAutoSwitchProxy(boolean autoSwitchProxy) {
        getSharedPreferences().edit().putBoolean(SharedPreferenceKey.IS_AUTO_SWITCH_PROXY, autoSwitchProxy).apply();
    }

    public static boolean isAutoSwitchProxy() {
        return getSharedPreferences().getBoolean(SharedPreferenceKey.IS_AUTO_SWITCH_PROXY, false);
    }

    public static void setTestConnectFailedCount(int connectFailedCount) {
        getSharedPreferences().edit().putInt(SharedPreferenceKey.TEST_CONNECT_FAILED_COUNT, connectFailedCount).apply();
    }

    public static int getTestConnectFailedCount() {
        return getSharedPreferences().getInt(SharedPreferenceKey.TEST_CONNECT_FAILED_COUNT, VpnState.Init.ordinal());
    }

    public static void setVPNStartTime(long time) {
        getSharedPreferences().edit().putLong(SharedPreferenceKey.CONNECTING_START_TIME, time).apply();
    }

    public static long getVPNStartTime() {
        return getSharedPreferences().getLong(SharedPreferenceKey.CONNECTING_START_TIME, 0);
    }

    public static void setRocketSpeedConnect(boolean rocketSpeedConnect) {
        getSharedPreferences().edit().putBoolean(SharedPreferenceKey.IS_ROCKET_SPEED_CONNECT, rocketSpeedConnect).apply();
    }

    public static boolean isRocketSpeedConnect() {
        return getSharedPreferences().getBoolean(SharedPreferenceKey.IS_ROCKET_SPEED_CONNECT, false);
    }

    public static void setVIP(boolean vip) {
        getSharedPreferences().edit().putBoolean(SharedPreferenceKey.VIP, vip).apply();
    }

    public static boolean isVIP() {
        return getSharedPreferences().getBoolean(SharedPreferenceKey.VIP, false);
    }

    public static void setAutoRenewalVIP(boolean autoRenewalVIP) {
        getSharedPreferences().edit().putBoolean(SharedPreferenceKey.IS_AUTOMATIC_RENEWAL_VIP, autoRenewalVIP).apply();
    }

    public static boolean isAutoRenewalVIP() {
        return getSharedPreferences().getBoolean(SharedPreferenceKey.IS_AUTOMATIC_RENEWAL_VIP, true);
    }

    public static void setVIPPayTime(long payTime) {
        getSharedPreferences().edit().putLong(SharedPreferenceKey.VIP_PAY_TIME, payTime).apply();
    }

    public static long getVIPPayTime() {
        return getSharedPreferences().getLong(SharedPreferenceKey.VIP_PAY_TIME, 0);
    }

    public static void setVIPPayOneMonth(boolean payOneMonth) {
        getSharedPreferences().edit().putBoolean(SharedPreferenceKey.IS_VIP_PAY_ONE_MONTH, payOneMonth).apply();
    }

    public static boolean isVIPPayOneMonth() {
        return getSharedPreferences().getBoolean(SharedPreferenceKey.IS_VIP_PAY_ONE_MONTH, true);
    }

    public static void setVIPPayHalfYear(boolean payHalfYear) {
        getSharedPreferences().edit().putBoolean(SharedPreferenceKey.IS_VIP_PAY_HALF_YEAR, payHalfYear).apply();
    }

    public static boolean isVIPPayHalfYear() {
        return getSharedPreferences().getBoolean(SharedPreferenceKey.IS_VIP_PAY_HALF_YEAR, true);
    }

    public static void setVPNStartConnectTime(long time) {
        getSharedPreferences().edit().putLong(SharedPreferenceKey.VPN_CONNECT_START_TIME, time).apply();
    }

    public static long getVPNStartConnectTime() {
        return getSharedPreferences().getLong(SharedPreferenceKey.VPN_CONNECT_START_TIME, 0);
    }

    public static void setWifiDialogShowCount(int count) {
        getSharedPreferences().edit().putInt(SharedPreferenceKey.WIFI_WARN_DIALOG_SHOW_COUNT, count).apply();
    }

    public static int getWifiDialogShowCount() {
        return getSharedPreferences().getInt(SharedPreferenceKey.WIFI_WARN_DIALOG_SHOW_COUNT, 0);
    }

    public static void setWifiDialogShowTime(long time) {
        getSharedPreferences().edit().putLong(SharedPreferenceKey.WIFI_WARN_DIALOG_SHOW_TIME, time).apply();
    }

    public static long getWifiDialogShowTime() {
        return getSharedPreferences().getLong(SharedPreferenceKey.WIFI_WARN_DIALOG_SHOW_TIME, 0);
    }

    public static void setNetSpeedLowDialogShowCount(int count) {
        getSharedPreferences().edit().putInt(SharedPreferenceKey.NET_SPEED_LOW_WARN_DIALOG_SHOW_COUNT, count).apply();
    }

    public static int getNetSpeedLowDialogShowCount() {
        return getSharedPreferences().getInt(SharedPreferenceKey.NET_SPEED_LOW_WARN_DIALOG_SHOW_COUNT, 0);
    }

    public static void setNetSpeedLowDialogShowTime(long time) {
        getSharedPreferences().edit().putLong(SharedPreferenceKey.NET_SPEED_LOW_WARN_DIALOG_SHOW_TIME, time).apply();
    }

    public static long getNetSpeedLowDialogShowTime() {
        return getSharedPreferences().getLong(SharedPreferenceKey.NET_SPEED_LOW_WARN_DIALOG_SHOW_TIME, 0);
    }

    public static void setDevelopedCountryInactiveUserDialogShowCount(int count) {
        getSharedPreferences().edit().putInt(SharedPreferenceKey.DEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_COUNT, count).apply();
    }

    public static int getDevelopedCountryInactiveUserDialogShowCount() {
        return getSharedPreferences().getInt(SharedPreferenceKey.DEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_COUNT, 0);
    }

    public static void setDevelopedCountryInactiveUserDialogShowTime(long time) {
        getSharedPreferences().edit().putLong(SharedPreferenceKey.DEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_TIME, time).apply();
    }

    public static long getDevelopedCountryInactiveUserDialogShowTime() {
        return getSharedPreferences().getLong(SharedPreferenceKey.DEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_TIME, 0);
    }

    public static void setUndevelopedCountryInactiveUserDialogShowCount(int count) {
        getSharedPreferences().edit().putInt(SharedPreferenceKey.UNDEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_COUNT, count).apply();
    }

    public static int getUndevelopedCountryInactiveUserDialogShowCount() {
        return getSharedPreferences().getInt(SharedPreferenceKey.UNDEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_COUNT, 0);
    }

    public static void setUndevelopedCountryInactiveUserDialogShowTime(long time) {
        getSharedPreferences().edit().putLong(SharedPreferenceKey.UNDEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_TIME, time).apply();
    }

    public static long getUndevelopedCountryInactiveUserDialogShowTime() {
        return getSharedPreferences().getLong(SharedPreferenceKey.UNDEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_TIME, 0);
    }

    public static void setOpenAppToDecideInactiveTime(long time) {
        getSharedPreferences().edit().putLong(SharedPreferenceKey.OPEN_APP_TIME_TO_DECIDE_INACTIVE_USER, time).apply();
    }

    public static long getOpenAppToDecideInactiveTime() {
        return getSharedPreferences().getLong(SharedPreferenceKey.OPEN_APP_TIME_TO_DECIDE_INACTIVE_USER, 0);
    }

}
