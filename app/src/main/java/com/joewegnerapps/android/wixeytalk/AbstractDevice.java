package com.joewegnerapps.android.wixeytalk;

import java.util.Locale;

public abstract class AbstractDevice {
    private static final String TAG = AbstractDevice.class.getSimpleName();
    boolean mDeviceOn = false;
    boolean mConnected = false;
    static boolean mPoint1 = true;

    enum DeviceVersion {AngleGaugeSimulation, WixeyAngleGauge_Ver1, WixeyAngleGauge_Ver2}
    DeviceVersion mDeviceVersion = DeviceVersion.AngleGaugeSimulation;
    public void setDeviceVersion(DeviceVersion deviceVersion) {
        mDeviceVersion = deviceVersion;
    }
    public DeviceVersion getDeviceVersion() {
        return mDeviceVersion;
    }

    enum DeviceType { AngleGauge, SimAngleGauge }
    DeviceType mDeviceType;

    public abstract String powerButton();
    public boolean simButton() {return false;}
    public abstract String zeroButton();
    public DisplayInfo parseData(String data) {return parseData(data.getBytes());}
    public DisplayInfo parseData(byte[] data) {return new DisplayInfo("");}
    public DisplayInfo parseData(float data) {return new DisplayInfo("");}
    public void disconnect() {mConnected = false;};
    public String getName() {return "Unknown device";}
    public DeviceType getDeviceType() {return  mDeviceType;}
    public void setDeviceOn(boolean on) {mDeviceOn = on;}
    static public void setAccuracy(boolean point1) {mPoint1 = point1;}
    public void orOrientationChange() {}

    static String formatAngle(float angle) {
        String text;
        if (!mPoint1) {
            int iangle = Math.round(angle);
            text = String.format(Locale.getDefault(), "%d", iangle);
        }
        else {
            text = String.format(Locale.getDefault(), "%.1f", angle);
        }
        return text;
    }

    //////////////////////////////
    //
    // Helper code to convert hold info helpful in presenting data to UI
    //
    /////////////////////////////

    static class DisplayInfo {
        String mDisplay;
        String mSpeak;
        boolean mDeviceOn;
        boolean mDataIsAngle;

        DisplayInfo(String text) {
            setData(text, text, true, true);
        }
        DisplayInfo(String display, String speak) {
            setData(display, speak, true, true);
        }
        DisplayInfo(String text, boolean deviceOn) {
            setData(text, text, deviceOn, true);
        }
        DisplayInfo(String display, String speak, boolean deviceOn) {
            setData(display, speak, deviceOn, true);
        }
        DisplayInfo(boolean isAngle, String text) {
            setData(text, text, true, isAngle);
        }
        DisplayInfo(boolean isAngle, String display, String speak) {
            setData(display, speak, true, isAngle);
        }
        DisplayInfo(boolean isAngle, String text, boolean deviceOn) {
            setData(text, text, deviceOn, isAngle);
        }
        DisplayInfo(boolean isAngle, String display, String speak, boolean deviceOn) {
            setData(display, speak, deviceOn, isAngle);
        }


        void setData (String display, String speak, boolean deviceOn, boolean isAngle) {
            mDisplay = display;
            mSpeak = speak;
            mDeviceOn = deviceOn;
            mDataIsAngle = isAngle;
        }
    }
}
