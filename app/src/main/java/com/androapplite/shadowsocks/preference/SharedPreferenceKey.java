package com.androapplite.shadowsocks.preference;

/**
 * Created by jim on 16/4/27.
 */
public final class SharedPreferenceKey {
    public static final String VPN_NATION = "VPN_NATION";
    public static final String VPN_FLAG = "VPN_FLAG";
    public static final String SERVER_LIST = "SERVER_LIST";
    public static final String FETCH_SERVER_LIST = "FETCH_SERVER_LIST";
    public static final String CONNECTING_VPN_NAME = "CONNECTING_VPN_NAME";
    public static final String CONNECTING_VPN_SERVER = "CONNECTING_VPN_SERVER";
    public static final String CONNECTING_VPN_FLAG = "CONNECTING_VPN_FLAG";
    public static final String CONNECTING_VPN_NATION = "CONNECTING_VPN_NATION";
    public static final String CONNECTING_VPN_SIGNAL = "CONNECTING_VPN_SIGNAL";
    public static final String CONNECTING_VPN_PORT = "CONNECTING_VPN_PORT";
    public static final String USE_TIME = "USE_TIME";
    public static final String SUCCESS_CONNECT_COUNT = "SUCCESS_CONNECT_COUNT";
    public static final String FAILED_CONNECT_COUNT = "FAILED_CONNECT_COUNT";
    public static final String NOTIFICATION_DISABLE_CHECK = "NOTIFICATION_DISABLE_CHECK";
    public static final String VPN_STATE = "VPN_STATE";
    public static final String COUNTRY_CODE = "COUNTRY_CODE";
    public static final String IP = "IP";
    public static final String IS_FETCH_SERVER_LIST_AT_SERVER = "IS_FETCH_SERVER_LIST_AT_SERVER";
    public static final String IS_AUTO_SWITCH_PROXY = "IS_AUTO_SWITCH_PROXY"; //防止StatusGuard切换服务器进而一直弹广告
    public static final String TEST_CONNECT_FAILED_COUNT = "TEST_CONNECT_FAILED_COUNT"; //测试链接失败的次数
    public static final String TEST_CONNECT_FAILED_TIME_COUNT = "TEST_CONNECT_FAILED_TIME_COUNT"; //测试链接失败的时间次数
    public static final String CONNECTING_START_TIME = "CONNECTING_START_TIME"; // VPN开始连接的开始时间

    //关于各种弹窗的数据存储key
    public static final String VPN_CONNECT_START_TIME = "VPN_CONNECT_START_TIME";
    public static final String WIFI_WARN_DIALOG_SHOW_COUNT = "WIFI_WARN_DIALOG_SHOW_COUNT";
    public static final String WIFI_WARN_DIALOG_SHOW_TIME = "WIFI_WARN_DIALOG_SHOW_TIME";
    public static final String NET_SPEED_LOW_WARN_DIALOG_SHOW_COUNT = "NET_SPEED_LOW_WARN_DIALOG_SHOW_COUNT";
    public static final String NET_SPEED_LOW_WARN_DIALOG_SHOW_TIME = "NET_SPEED_LOW_WARN_DIALOG_SHOW_TIME";
    public static final String DEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_COUNT = "DEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_COUNT";
    public static final String DEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_TIME = "DEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_TIME";
    public static final String UNDEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_COUNT = "UNDEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_COUNT";
    public static final String UNDEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_TIME = "UNDEVELOPED_COUNTRY_INACTIVE_USER_WARN_DIALOG_SHOW_TIME";
    public static final String OPEN_APP_TIME_TO_DECIDE_INACTIVE_USER = "OPEN_APP_TIME_TO_DECIDE_INACTIVE_USER";



    //广告投放统计
    public static final String OPEN_MAIN_PAGE_TIME = "OPEN_MAIN_PAGE_TIME";
    public static final String OPEN_MAIN_PAGE_COUNT = "OPEN_MAIN_PAGE_COUNT";
    public static final String CONTINOUS_DAY_TIME = "CONTINOUS_DAY_TIME";
    public static final String CONTINOUS_DAY_COUNT = "CONTINOUS_DAY_COUNT";
    public static final String UNINSTALL_DAY_TIME = "UNINSTALL_DAY_TIME";
    public static final String PAYLOAD_TIME = "PAYLOAD_TIME";
    public static final String PAYLOAD_BYTE = "PAYLOAD_BYTE";
    public static final String PAYLOAD_1M = "PAYLOAD_1M";
    public static final String PAYLOAD_10M = "PAYLOAD_10M";
    public static final String PAYLOAD_100M = "PAYLOAD_100M";
    public static final String INSTALL_APP_TIME = "INSTALL_APP_TIME";
    public static final String PHONE_MODEL_OS_TIME = "PHONE_MODEL_OS_TIME";
    public static final String CLICK_CONNECT_BUTTON_TIME = "CLICK_CONNECT_BUTTON_TIME";
    public static final String CLICK_CONNECT_BUTTON_COUNT = "CLICK_CONNECT_BUTTON_COUNT";

    public static final String GRAB_SPEED_TIME = "GRAB_SPEED_TIME";
    public static final String REPORT_TCP_RECORD = "REPORT_TCP_RECORD";

}
