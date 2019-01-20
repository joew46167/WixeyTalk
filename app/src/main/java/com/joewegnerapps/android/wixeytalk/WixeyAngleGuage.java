package com.joewegnerapps.android.wixeytalk;

import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.Locale;
import java.util.UUID;

public class WixeyAngleGuage extends AbstractDevice {
    private static final String TAG = WixeyAngleGuage.class.getSimpleName();
    public static String String_Wixey_UUID =
            "02010604-FF47-5221-1609-47525F424C45";
    public final static ParcelUuid ParcelUuid_Wixey =
            ParcelUuid.fromString(String_Wixey_UUID);
    public final static UUID UUID_Wixey =
            UUID.fromString(String_Wixey_UUID);

    public WixeyAngleGuage() {
        setWixeyAngleGauge(DeviceVersion.WixeyAngleGauge_Ver2);
    }

    public WixeyAngleGuage(String name) {
        if ((name == null) || (name.length() == 0) || name.toUpperCase().equals("GR_BLE")){
            // assume if its an error it's an old one
            setWixeyAngleGauge(DeviceVersion.WixeyAngleGauge_Ver1);
        }
        else if (name.toUpperCase().equals("WIXEY")){
            setWixeyAngleGauge(DeviceVersion.WixeyAngleGauge_Ver2);
        }
        else {
            // some other error, still assume old
            setWixeyAngleGauge(DeviceVersion.WixeyAngleGauge_Ver1);
        }
    }

    private void setWixeyAngleGauge(DeviceVersion version) {
        mDeviceVersion = version;
        mDeviceType = DeviceType.AngleGauge;
        mConnected=true;
    }

    @Override
    public String getName() {
        String name;
        switch (mDeviceVersion) {
            case WixeyAngleGauge_Ver1:
                name = "GR_BLE";
                break;
            case WixeyAngleGauge_Ver2:
                name = "WIXEY";
                break;
            default:
                name = "GR_BLE";
                break;
        }

        return name;
    }

    //    //////////////////////////////
//    //
//    // Code to convert data to a string
//    //
//    /////////////////////////////
//
//    static class DisplayInfo {
//        String mDisplay;
//        String mSpeak;
//        boolean mDeviceOn;
//        boolean mDataIsAngle;
//
//        DisplayInfo(String text) {
//            setData(text, text, true, true);
//        }
//        DisplayInfo(String display, String speak) {
//            setData(display, speak, true, true);
//        }
//        DisplayInfo(String text, boolean deviceOn) {
//            setData(text, text, deviceOn, true);
//        }
//        DisplayInfo(String display, String speak, boolean deviceOn) {
//            setData(display, speak, deviceOn, true);
//        }
//        DisplayInfo(boolean isAngle, String text) {
//            setData(text, text, true, isAngle);
//        }
//        DisplayInfo(boolean isAngle, String display, String speak) {
//            setData(display, speak, true, isAngle);
//        }
//        DisplayInfo(boolean isAngle, String text, boolean deviceOn) {
//            setData(text, text, deviceOn, isAngle);
//        }
//        DisplayInfo(boolean isAngle, String display, String speak, boolean deviceOn) {
//            setData(display, speak, deviceOn, isAngle);
//        }
//
//        void setData (String display, String speak, boolean deviceOn, boolean isAngle) {
//            mDisplay = display;
//            mSpeak = speak;
//            mDeviceOn = deviceOn;
//            mDataIsAngle = isAngle;
//        }
//    }

    static public DisplayInfo parseData(DeviceVersion version, byte[] data) {
        Log.v(TAG, "version " + version + " data " + new String(data));
        DisplayInfo angle;
        switch (version) {
            case WixeyAngleGauge_Ver1:
                angle = parseV1(data);
                break;
            case WixeyAngleGauge_Ver2:
                angle = parseV2(data);
                break;
            case AngleGaugeSimulation:
            default:
                angle = parseV2(data);
                break;
        }

        return angle;
    }

    public DisplayInfo parseData(byte[] data) {
        Log.v(TAG, "version " + mDeviceVersion + " data " + new String(data));
        DisplayInfo angle;
        switch (mDeviceVersion) {
            case WixeyAngleGauge_Ver1:
                angle = parseV1(data);
                break;
            case WixeyAngleGauge_Ver2:
                angle = parseV2(data);
                break;
            case AngleGaugeSimulation:
            default:
                angle = parseV2(data);
                break;
        }

        return angle;
    }

    static float mLastAngle = 0.0f;
    static float mZeroAngle = 0.0f;
    static private DisplayInfo parseV1(byte[] data) {
        String text = new String(data);
        DisplayInfo ret;
        try {
            float angle = Float.parseFloat(text) / 100.0f;
            mLastAngle = angle;
            angle = angle - mZeroAngle;
            text = formatAngle(angle);
        }
        catch(Exception e) {
            text = "";
        }
        finally {
            ret = new DisplayInfo(text);
        }
        return ret;
    }

    static private DisplayInfo parseV2(byte[] data) {
        Context context = Constants.getApplicationContext();
        if (context == null) {
            return new DisplayInfo("");
        }

        DisplayInfo text = new DisplayInfo("");
        String dataStr = "";

        dataStr = new String(data);

        Log.v(TAG, "Text: " + dataStr);

        if (dataStr.startsWith("----")) {
            if (context != null) {
                text = new DisplayInfo(false, "----", context.getString(R.string.spkWaiting));
            }
            else {
                text = new DisplayInfo(false, "----", "waiting");
            }
        }
        else if (dataStr.startsWith("*;0---;----;")) {
            // waiting, maybe after a zero?
//            return;
            if (context != null) {
                text = new DisplayInfo(false, "----", context.getString(R.string.spkWaiting));
            }
            else {
                text = new DisplayInfo(false, "----", "waiting");
            }
        }
        else if (dataStr.startsWith("*;1")) {
            //Error
//            updateDisplay(context.getString(R.string.txtError), context.getString(R.string.spkError), true);
//            return;
            if (context != null) {
                text = new DisplayInfo(false, context.getString(R.string.txtError), context.getString(R.string.spkError));
            }
            else {
                text = new DisplayInfo(false, "Erro", "Error");
            }
        }
        else if (dataStr.toLowerCase().startsWith("*;key_zero;") || dataStr.toLowerCase().startsWith("*;cmd_zero;")) {
            // Zero button pushed
//            updateDisplay("----", context.getString(R.string.spkZeroing), true);
//            return;
            if (context != null) {
                text = new DisplayInfo(false, "----", context.getString(R.string.spkZeroing), false);
            }
            else {
                text = new DisplayInfo(false, "----", "Zeroing", false);
            }
        }
        else if (dataStr.toLowerCase().startsWith("*;key_off;") || dataStr.toLowerCase().startsWith("*;cmd_off;")) {
            // turning off (display only, device stays on)
//            updateDisplay("----", context.getString(R.string.spkTurningOff), false);
//            return;
            if (context != null) {
                text = new DisplayInfo(false, "----", context.getString(R.string.spkTurningOff), false);
            }
            else {
                text = new DisplayInfo(false, "----", "Turning off");
            }
        }
        else if (dataStr.toLowerCase().startsWith("*;key_on;") || dataStr.toLowerCase().startsWith("*;cmd_on;")) {
            // device turning on
//            byte[] angleData = new byte[data.length - 11];
//            System.arraycopy(data, 11, angleData, 0, angleData.length);
//            updateDisplay("", context.getString(R.string.spkTurningOn), false);
            //return;
            if (context != null) {
                text = new DisplayInfo(false, "----", context.getString(R.string.spkTurningOn), false);
            }
            else {
                text = new DisplayInfo(false, "----", "Turning on");
            }
        }
        else if (dataStr.contains("*;01--;0") || dataStr.contains("*;00--;0")) {
            // +- 0, so show it correctly
            String angleStr = formatAngle(0);
            text = new DisplayInfo(angleStr);
        }
        else if (dataStr.length() < 9) {
            text = new DisplayInfo("");
//            return;
        }
        else {
            float angle = convertAngle(data);
            String angleStr = formatAngle(angle);
            text = new DisplayInfo(angleStr);
        }

        return text;
    }

    static private float convertAngle(byte[] data) {
        boolean negative = (data[3] == '1');
        
        float angle = 0.0f;
        for (int i = 7; i < data.length - 2; i++) { // skip CRLF
            angle = (angle * 10.0f) + (float) (data[i] - '0');
        }
        if (negative && (angle != 0)) angle = -angle;
        return angle/100.0f;
    }
    
    public String powerButton() {
        String cmd = "";
        if (mConnected) {
            if (mDeviceOn) {
                cmd = "*set;off;\n";
            }
            else {
                cmd = "*set;on;\n";
            }
        }
        return cmd;
    }

    public String zeroButton() {
        mZeroAngle = mLastAngle;
        String cmd="*set;z;\n";

        return cmd;
    }

    @Override
    public void disconnect() {
        super.disconnect();
        // add disconnect code
    }
}
