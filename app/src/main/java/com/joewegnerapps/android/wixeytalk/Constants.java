package com.joewegnerapps.android.wixeytalk;

import android.app.Application;
import android.content.Context;

public class Constants {
    private static Application mApp = null;

    public static Application getApp() {
        return mApp;
    }

    public static void setApp(Application app) {
        mApp = app;
    }

    static public Context getApplicationContext() {
        if (mApp == null) return null;
        return mApp.getApplicationContext();
    }

    public final static int BT_ENABLE_REQUEST = 1;
    public final static int LOC_ENABLE_REQUEST = 2;

    public final static String ACTION_GATT_CONNECTED           = "AngleGauge.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED        = "AngleGauge.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "AngleGauge.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE           = "AngleGauge.ACTION_DATA_AVAILABLE";
    public final static String ACTION_SIM_DATA_AVAILABLE       = "AngleGauge.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA                      = "AngleGauge.EXTRA_DATA";

    public final static int MSG_TYPE_GATT_CONNECTED           = 1;
    public final static int MSG_TYPE_GATT_DISCONNECTED        = 2;
    public final static int MSG_TYPE_GATT_SERVICES_DISCOVERED = 3;
    public final static int MSG_TYPE_DATA_AVAILABLE           = 4;
    public final static int MSG_TYPE_SIM_DATA_AVAILABLE       = 5;
    public final static int MSG_TYPE_SERVICE_READY            =6;
    //public final static int EXTRA_DATA                        = "AngleGauge.EXTRA_DATA";

    public final static int SENSOR_REPORT_FREQ_US = 100000;  // 10 per second - app can throttle if needed
    public final static int MIN_TALK_DELAY_MS = 1000;        // Talk no more than once per second per Barry
    public final static int REPEAT_FREQ_MS = 5000;           // Repeat after 5 seconds per Barry
}
