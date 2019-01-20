package com.joewegnerapps.android.wixeytalk;


public class SimDevice extends AbstractDevice {
    float mLastAngle = 0.0f;
    float mZeroAngle = 0.0f;
    float mRotationAngle = 0.0f;
    private static final String TAG = SimDevice.class.getSimpleName();

    public SimDevice() {
        mDeviceVersion = DeviceVersion.AngleGaugeSimulation;
        mDeviceType = DeviceType.SimAngleGauge;
    }

    @Override
    public DisplayInfo parseData(float angle) {
        mLastAngle = angle;
        float dispAngle = angle - mZeroAngle;// + mRotationAngle;
//        if (dispAngle > 180.0f) {
//            float over = dispAngle - 180.0f;
//            dispAngle = -180.0f + over;
//        }
//        else if (dispAngle < -180.0f) {
//            float over = 180.0f - dispAngle;
//            dispAngle = 180.0f - over;
//        }
        String angleStr = formatAngle(dispAngle);
        return new DisplayInfo(angleStr);
    }

    public String powerButton() {
        mDeviceOn = !mDeviceOn;
        return "";
    }

    public boolean simButton() {
        mDeviceOn = !mDeviceOn;
        return mDeviceOn;
    }

    public void orOrientationChange() {
//        if ((mLastAngle > -45.0) && (mLastAngle <= 45.0)) {
//            mRotationAngle = 0.0f;
//        }
//        else if ((mLastAngle > 45.0) && (mLastAngle <= 135.0)) {
//            mRotationAngle = 90.0f;
//        }
//        else if ((mLastAngle > -135.0) && (mLastAngle <= -45.0)) {
//            mRotationAngle = -90.0f;
//        }
//        else {
//            mRotationAngle = 180.0f;
//        }
    }

    public String zeroButton() {
        mZeroAngle = mLastAngle + mRotationAngle;
        return "";
    }

    public String getName() {return "Angle Gauge Simulation";}

    @Override
    public void disconnect() {
        super.disconnect();
        // add disconnect code
    }
}
