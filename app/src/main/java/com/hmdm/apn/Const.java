package com.hmdm.apn;

public class Const {
    public static final String LOG_TAG = "com.hmdm.Apn";

    public static final String PREF_NAME = "com.hmdm.apn.SETTINGS";

    public static final String PREF_APN_DESC = "apn_desc";
    public static final String PREF_APN_NAME = "apn_name";
    public static final String PREF_MCC = "apn_mcc";
    public static final String PREF_MNC = "apn_mnc";
    public static final String PREF_PROXY_ADDR = "apn_proxy_addr";
    public static final String PREF_PROXY_PORT = "apn_proxy_port";
    public static final String PREF_USER = "apn_user";
    public static final String PREF_PASSWORD = "apn_password";

    public static final String PREF_APN_CLEAR = "apn_clear";

    public static final String NOTIFICATION_CONFIG_UPDATED = "com.hmdm.push.configUpdated";

    public static final int HMDM_RECONNECT_DELAY_FIRST = 5000;
    public static final int HMDM_RECONNECT_DELAY_NEXT = 60000;
}
