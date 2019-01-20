package com.joewegnerapps.android.wixeytalk;


import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import static android.icu.lang.UProperty.MATH;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class DeviceTalkService extends Service {
    private final static String TAG = DeviceTalkService.class.getSimpleName();
    private static boolean mBound = false;
    public static boolean isBound() {return mBound;}
    private boolean mDeviceConnected = false;
    private static AbstractDevice mConnectedDevice = null;
    private static final float ANGLE_CONVERT = 360.0f/(2.0f * (float)Math.PI);

    //////////////////////////////
    //
    // Code to share the results to others
    //
    //////////////////////////////
    public interface MessageHandler {
        void sendMessage(int msgType);
        void sendMessage(int msgType, AbstractDevice.DisplayInfo data);
    }

    private ArrayList<MessageHandler> mMsgHandlers = new ArrayList<>();
    private void registerListener(MessageHandler listener) {mMsgHandlers.add(listener);}
    private void unregisterListener(MessageHandler listener) {mMsgHandlers.remove(listener);}

    private void broadcastUpdate(final int msg) {
        synchronized (this) {
            for (MessageHandler handler : mMsgHandlers) {
                handler.sendMessage(msg);
            }
        }
    }

    private void broadcastUpdate(final int msg, final AbstractDevice.DisplayInfo data) {
        synchronized (this) {
            for (MessageHandler handler : mMsgHandlers) {
                handler.sendMessage(msg, data);
            }
        }
    }

    public void setWriteCharacteristic(BluetoothGattCharacteristic characteristic) {
        mBluetoothWriteChar = characteristic;
    }

    //////////////////////////////
    //
    // Code to bind the service
    //
    //////////////////////////////
    @Override
    public IBinder onBind(Intent intent) {
        mBound = true;
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        disconnect();
        close();

        return super.onUnbind(intent);
    }

//    /**
//     * Connects to the GATT server hosted on the Bluetooth LE device.
//     *
//     * @param address The device address of the destination device.
//     *
//     * @return Return true if the connection is initiated successfully. The connection result
//     *         is reported asynchronously through the
//     *         {@code BluetoothGattCallback#onConnectionStateChange(
//     *         android.bluetooth.BluetoothGatt, int, int)}
//     *         callback.
//     */
//    public boolean connect(final String address) {
//        if (mBluetoothAdapter == null || address == null) {
//            Log.v(TAG, "BluetoothAdapter not initialized or unspecified address.");
//            return false;
//        }
//
//        // Previously connected device.  Try to reconnect.
//        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
//                && mBluetoothGatt != null) {
//            Log.v(TAG, "Trying to use an existing mBluetoothGatt for connection.");
//            if (mBluetoothGatt.connect()) {
//                mConnectionState = STATE_CONNECTING;
//                return true;
//            } else {
//                return false;
//            }
//        }
//
//        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
//        if (device == null) {
//            Log.v(TAG, "Device not found.  Unable to connect.");
//            return false;
//        }
//        // We want to directly connect to the device, so we are setting the autoConnect
//        // parameter to false.
//        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
//        Log.v(TAG, "Trying to create a new connection.");
//        mBluetoothDeviceAddress = address;
//        mConnectionState = STATE_CONNECTING;
//        return true;
//    }

//    private static final int STATE_DISCONNECTED = 0;
//    private static final int STATE_CONNECTING = 1;
//    private static final int STATE_CONNECTED = 2;
//    private static int mConnectionState = STATE_DISCONNECTED;
//    enum GattConnection {STATE_DISCONNECTED, STATE_CONNECTING, STATE_CONNECTED};
//    private static GattConnection mConnectionState = GattConnection.STATE_DISCONNECTED;
//
//    /**
//     * Disconnects an existing connection or cancel a pending connection. The disconnection result
//     * is reported asynchronously through the
//     * {@code BluetoothGattCallback#onConnectionStateChange(
//     * android.bluetooth.BluetoothGatt, int, int)}
//     * callback.
//     */
//    public void disconnect() {
//        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
//            Log.v(TAG, "BluetoothAdapter not initialized");
//            return;
//        }
//
//        if ((mConnectionState == GattConnection.STATE_CONNECTING) || (mConnectionState == GattConnection.STATE_CONNECTED)) {
//            mBluetoothGatt.disconnect();
//        }
//    }
//
//    /**
//     * After using a given BLE device, the app must call this method to ensure resources are
//     * released properly.
//     */
//    public void close() {
//        if (mBluetoothGatt == null) {
//            return;
//        }
//        mBluetoothGatt.close();
//        mBluetoothGatt = null;
//    }
//
//    /**
//     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
//     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(
//     * android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
//     * callback.
//     *
//     * @param characteristic The characteristic to read from.
//     */
//    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
//        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
//            Log.v(TAG, "BluetoothAdapter not initialized");
//            return;
//        }
//        mBluetoothGatt.readCharacteristic(characteristic);
//    }
//
//    /**
//     * Enables or disables notification on a give characteristic.
//     *
//     * @param characteristic Characteristic to act on.
//     * @param enabled If true, enable notification.  False otherwise.
//     */
//    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
//                                              boolean enabled) {
//        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
//            Log.v(TAG, "BluetoothAdapter not initialized");
//            return;
//        }
//        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
//    }
//
//    /**
//     * Retrieves a list of supported GATT services on the connected device. This should be
//     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
//     *
//     * @return A {@code List} of supported services.
//     */
//    public List<BluetoothGattService> getSupportedGattServices() {
//        if (mBluetoothGatt == null) return null;
//
//        return mBluetoothGatt.getServices();
//    }
//
//    public void writeToDevice(String value) {
//        if ((mBluetoothWriteChar != null) && (mBluetoothGatt != null)) {
//            mBluetoothWriteChar.setValue(value);
//            mBluetoothGatt.writeCharacteristic(mBluetoothWriteChar);
//        }
//    }

    //////////////////////////////
    //
    // Sensor for simulation
    //
    /////////////////////////////
    private long mLastSimTime = 0L;
    private boolean mSimEnabled;

    public void enableSimulation(boolean enable) {
        if ((mConnectedDevice != null) && (mConnectedDevice.getDeviceType() != AbstractDevice.DeviceType.SimAngleGauge)) {
            mConnectedDevice.disconnect();
            mConnectedDevice = null;
        }

        if (enable) {
            if (mConnectedDevice == null) {
                mConnectedDevice = new SimDevice();
            }
            configureSimulation();
        }
        else {
            releaseSimulation();
            mConnectedDevice = null;
        }
        mSimEnabled = enable;
    }

    public void configureSimulation() {
        SensorManager sensorManager;
        Sensor sensor;
        Context ctx = Constants.getApplicationContext();

        sensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION) != null) {
            // success! we have an gyroscope
            sensor = sensorManager.getDefaultSensor( Sensor.TYPE_ORIENTATION );

            sensorManager.registerListener(mSensorListener, sensor, Constants.SENSOR_REPORT_FREQ_US);
            Log.v(TAG, "simulation set up");
        }
    }

    public void releaseSimulation() {
        SensorManager sensorManager;
        Sensor sensor;
        Context ctx = Constants.getApplicationContext();

        sensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION) != null) {
            sensorManager.unregisterListener(mSensorListener);
        }
    }

    private final SensorEventListener mSensorListener = new SensorEventListener () {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() != Sensor.TYPE_ORIENTATION) return;
            if (mConnectedDevice == null) return;
            if (mConnectedDevice.getDeviceType() != AbstractDevice.DeviceType.SimAngleGauge) return;

            long now = System.currentTimeMillis();
            if ((now-mLastSimTime) < 1000) return;
            mLastSimTime = now;

            float orientation[] = new float[3];
            System.arraycopy(event.values, 0, orientation, 0, 3);

            float angle = -orientation[SensorManager.DATA_Z];

            broadcastUpdate(Constants.MSG_TYPE_DATA_AVAILABLE, mConnectedDevice.parseData(angle));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };


    //////////////////////////////
    //
    // Code to give client access
    //
    /////////////////////////////

    private final IBinder mBinder = new ServiceBinder();

    public class ServiceBinder extends Binder {
        DeviceTalkService getService() {
            return DeviceTalkService.this;
        }

        public void powerButton() {
            DeviceTalkService.this.powerButton();
        }

        public void zeroButton() {
            DeviceTalkService.this.zeroButton();
        }

        public void CommConnection(Activity act) {
            DeviceTalkService.this.CommConnection(act);
        }

        public boolean bluetoothAdapterAvailable() {
            return DeviceTalkService.this.bluetoothAdapterAvailable();
        }

        public void talkSwitch(boolean talk) {
            DeviceTalkService.this.talkSwitch(talk);
        }

        public void accuracySwitch(boolean point1) {
            DeviceTalkService.this.accuracySwitch(point1);
        }

        public void repeatSwitch(boolean repeat) {
            DeviceTalkService.this.repeatSwitch(repeat);
        }

        public boolean startSimButton() {
            return DeviceTalkService.this.startSimButton();
        }

        public void speakData(String text, boolean repeatInfo) {
            DeviceTalkService.this.speakData(text, repeatInfo);
        }

        public void register(MessageHandler listener) {
            DeviceTalkService.this.registerListener(listener);
        }

        public void unregister(MessageHandler listener) {
            DeviceTalkService.this.registerListener(listener);
        }

        public void initBluetooth(Context ctx) {
            DeviceTalkService.this.initBluetooth(ctx);
        }

        public void disconnect() {
            DeviceTalkService.this.disconnect();
        }

        public boolean isBound() {
            return DeviceTalkService.isBound();
        }

        public boolean isGattConnected() {
            return DeviceTalkService.this.isGattConnected();
        }

        public TextToSpeech createTTS() {
            return DeviceTalkService.this.createTTS();
        }

        public void setTTS(TextToSpeech tts) {
            DeviceTalkService.this.setTTS(tts);
        }

        public boolean isSimulationAvailable() {
            return DeviceTalkService.this.isSimulationAvailable();
        }

        public IBinder getBinder() {
            return mBinder;
        }
    }

    //////////////////////////////
    //
    // Code to implement client needs
    //
    /////////////////////////////
    private boolean isGattConnected() {
        return  (mBluetoothGatt != null);
    }

    private void powerButton() {
        if (mConnectedDevice != null) {
            String cmd = mConnectedDevice.powerButton();
            if (cmd.length() > 0) {
                writeToDevice(cmd);
            }
        }
    }

    private void zeroButton() {
        if (mConnectedDevice != null) {
            String cmd = mConnectedDevice.zeroButton();
            if (cmd.length() > 0) {
                writeToDevice(cmd);
            }
        }
    }

    private void CommConnection(Activity act) {
        if (!checkLocationPermission()) {
            Toast.makeText(act, getString(R.string.txtTurnOnBT), Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(act,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    Constants.LOC_ENABLE_REQUEST);
        }
        else {
            checkBT(act, true);
        }
    }

    private TextToSpeech createTTS() {
        mTTS = new TextToSpeech();
        return mTTS;
    }

    private void setTTS(TextToSpeech tts) {
        mTTS = tts;
    }

        private void startScan() {
        startScanner();
    }

    private void setDevice(AbstractDevice device) {
        mConnectedDevice = device;
    }

    private AbstractDevice getDevice() {
        return mConnectedDevice;
    }

    private boolean isSimulationAvailable() {
        Context ctx = Constants.getApplicationContext();

        SensorManager sensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        return (sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION) != null);
    }

    private TextToSpeech mTTS;

    private void accuracySwitch(boolean point1) { AbstractDevice.setAccuracy(point1);}
    private void talkSwitch(boolean talk) { mTTS.talkSwitch(talk);}
    private void repeatSwitch(boolean repeat) { mTTS.repeatSwitch(repeat);}
    private void speakData(String text, boolean repeatInfo) {
        mTTS.speakData(text, repeatInfo);}

    private boolean startSimButton() {
        enableSimulation(!mSimEnabled);
        return mSimEnabled;
    }

    //////////////////////////////
    //
    // Code for Bluetooth
    //
    /////////////////////////////

    //////////////////////////////
    //
    // Code for Bluetooth adapter
    //
    /////////////////////////////

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter = null;
    private boolean bluetoothAdapterAvailable() {return mBluetoothAdapter == null;}
    private void initBluetooth(Context ctx) {mBluetoothAdapter = initialize(ctx);}
    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public BluetoothAdapter initialize(Context ctx) {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        BluetoothAdapter blueToothAdapter=null;
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) ctx.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return null;
            }
        }

        blueToothAdapter = mBluetoothManager.getAdapter();
        if (blueToothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
        }

        return blueToothAdapter;
    }

    //////////////////////////////
    //
    // Scanner callback
    //
    /////////////////////////////
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            String name = device.getName();
            Log.v(TAG, "Found a device name " + name);
            if (name != null) {
                String nameCheck = name.toUpperCase();
                boolean found = false;

                if (nameCheck.equals("WIXEY")) {
                    found = true;
                    Log.v(TAG, "Found a WIXEY device ");  // device with QC 1812 sticker
                }
                else if (nameCheck.equals("GR_BLE")) {
                    found = true;
                    Log.v(TAG, "Found a GR_BLR device ");  // device with QC 1708 sticker
                }

                if (found) {
                    // disable simulation
                    enableSimulation(false);
                    // set up device
                    if (mConnectedDevice != null) {
                        Log.v(TAG, "Disconnecting device: " + mConnectedDevice.getName());
                        mConnectedDevice.disconnect();
                    }
                    mConnectedDevice = new WixeyAngleGuage(nameCheck);

                    // stop scanning
                    mHandler.removeCallbacks(mStartRun);
                    mHandler.removeCallbacks(mRestartRun);
                    mHandler.removeCallbacks(mStopRun);
                    scanLeDevice(false);
                    mDeviceAddress = device.getAddress();
                    Log.v(TAG, "Connecting " + device.getName() + " " + mDeviceAddress);
                    final boolean connected = connect(mDeviceAddress);
                }
            }
        }
    };

    //////////////////////////////
    //
    // GATT server
    //
    /////////////////////////////

    private String mBluetoothDeviceAddress;
    private static BluetoothGatt mBluetoothGatt = null;

//    private static final int STATE_DISCONNECTED = 0;
//    private static final int STATE_CONNECTING = 1;
//    private static final int STATE_CONNECTED = 2;
//    private static int mConnectionState = STATE_DISCONNECTED;

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(
     *         android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.v(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            if (mBluetoothGatt.connect()) {
                mConnectionState = GattConnection.STATE_CONNECTING;
                return true;
            }
            else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.v(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        mBluetoothDeviceAddress = address;
        mConnectionState = GattConnection.STATE_CONNECTING;
        return true;
    }

    enum GattConnection {STATE_DISCONNECTED, STATE_CONNECTING, STATE_CONNECTED};
    private static GattConnection mConnectionState = GattConnection.STATE_DISCONNECTED;

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(
     * android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.v(TAG, "BluetoothAdapter not initialized");
            return;
        }

        if ((mConnectionState == GattConnection.STATE_CONNECTING) || (mConnectionState == GattConnection.STATE_CONNECTED)) {
            mBluetoothGatt.disconnect();
        }
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(
     * android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.v(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.v(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    public void writeToDevice(String value) {
        if ((mBluetoothWriteChar != null) && (mBluetoothGatt != null)) {
            mBluetoothWriteChar.setValue(value);
            mBluetoothGatt.writeCharacteristic(mBluetoothWriteChar);
        }
    }


    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
//                intentAction = Constants.ACTION_GATT_CONNECTED;
//                broadcastUpdate(intentAction);
//                enableSimulation(false);

                mConnectionState = GattConnection.STATE_CONNECTED;
                broadcastUpdate(Constants.MSG_TYPE_GATT_CONNECTED);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                intentAction = Constants.ACTION_GATT_DISCONNECTED;
                mConnectionState = GattConnection.STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
//                broadcastUpdate(intentAction);
                broadcastUpdate(Constants.MSG_TYPE_GATT_DISCONNECTED);
                startScanner();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
//                broadcastUpdate(Constants.ACTION_GATT_SERVICES_DISCOVERED);
                broadcastUpdate(Constants.MSG_TYPE_GATT_SERVICES_DISCOVERED);
                checkGattServices(getSupportedGattServices());
            } else {
                Log.v(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                AbstractDevice.DisplayInfo dispInfo = mConnectedDevice.parseData(characteristic.getValue());
                mConnectedDevice.setDeviceOn(dispInfo.mDeviceOn);
//                broadcastUpdate(Constants.ACTION_DATA_AVAILABLE, dispInfo);
                broadcastUpdate(Constants.MSG_TYPE_DATA_AVAILABLE, dispInfo);
                Log.v(TAG, "onCharacteristicRead: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            AbstractDevice.DisplayInfo dispInfo = mConnectedDevice.parseData(characteristic.getValue());
//            broadcastUpdate(Constants.ACTION_DATA_AVAILABLE, dispInfo);
            mConnectedDevice.setDeviceOn(dispInfo.mDeviceOn);
            broadcastUpdate(Constants.MSG_TYPE_DATA_AVAILABLE, dispInfo);
            Log.v(TAG, "onCharacteristicChanged");
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            Log.v(TAG, "onDescriptorRead " + descriptor.toString() + " " + status );
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.v(TAG, "onDescriptorWrite " + descriptor.toString() + " " + status );
        }
    };

    private void checkGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            Log.v(TAG, "checking service " + gattService.getUuid().toString());

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                int prop = gattCharacteristic.getProperties();
                Log.v(TAG, "   checking chars " + gattCharacteristic.getUuid().toString() + " properties are " + prop);
                if (prop == BluetoothGattCharacteristic.PROPERTY_NOTIFY) {
                    setCharacteristicNotification(gattCharacteristic, true);
                }
                if (prop == BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) {
                    setWriteCharacteristic(gattCharacteristic);
                }

            }
        }

    }


    private static BluetoothGattCharacteristic mBluetoothWriteChar = null;


    ////////////////////////
    //
    //  Bluetooth
    //
    ////////////////////////

    BluetoothAdapter mBTA = null;
    BluetoothLeScanner mBluetoothLeScanner = null;
    String mDeviceAddress;

//    static Constants.WixeyVersion mDeviceVersion = Constants.WixeyVersion.AngleGaugeSimulation;

//    public void CommConnection(Activity act) {
//        if (!checkLocationPermission()) {
//            Toast.makeText(act, getString(R.string.txtTurnOnBT), Toast.LENGTH_LONG).show();
//            ActivityCompat.requestPermissions(act,
//                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
//                    Constants.LOC_ENABLE_REQUEST);
//        }
//        else {
//            checkBT(act, true);
//        }
//    }

    private void checkBT(Activity act, boolean request) {
        // get the adapter
        if (mBTA == null) {
            mBTA = BluetoothAdapter.getDefaultAdapter();
        }

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBTA == null || !mBTA.isEnabled()) {
            if (request && (act != null)) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                act.startActivityForResult(enableBtIntent, Constants.BT_ENABLE_REQUEST);
            }
        }
        else {
            getBluetoothAdapterAndLeScanner();
            startScanner();
        }
    }

    public void onRequestPermissionsResult (int requestCode,
                                            String[] permissions,
                                            int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (requestCode == Constants.LOC_ENABLE_REQUEST) { // should never get here if null, but checking just in case
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkBT(null, false);  // double check permission and start the scanner
                Log.v( TAG, "Granted permission to  use bluetooth location");
            }
            else {
                Toast.makeText(this, getString(R.string.txtTurnOnBTLoc), Toast.LENGTH_LONG).show();
                Log.e( TAG, "No permission to use bluetooth location");
            }
        }
    }


    private void getBluetoothAdapterAndLeScanner() {
        // Get BluetoothAdapter and BluetoothLeScanner.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBTA = bluetoothManager.getAdapter();

        mScanning = false;
    }


    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private static final long WAIT_PERIOD = 10000;
    private boolean mScanning = false;

    Handler mHandler = new Handler();

    Runnable mStartRun = new Runnable() {
        @Override
        public void run() {
            scanLeDevice(true);
            mHandler.postDelayed(mStopRun, SCAN_PERIOD);
        }
    };

    Runnable mRestartRun = new Runnable() {
        @Override
        public void run() {
            stopScanner();
        }
    };

    Runnable mStopRun = new Runnable() {
        @Override
        public void run() {
            reStartScanner();
        }
    };

    private void startScanner() {
        Log.v(TAG, "Start scanning");
//            scanLeDevice(true);
        mHandler.postDelayed(mStartRun, 500);
    }

    private void reStartScanner() {
        Log.v(TAG, "Restart scanning");
        scanLeDevice(true);
        mHandler.postDelayed(mRestartRun, WAIT_PERIOD);
    }

    private void stopScanner() {
        Log.v(TAG, "Stop scanning");
        scanLeDevice(false);
        mHandler.postDelayed(mStopRun, SCAN_PERIOD);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mScanning = true;
            mBTA.startLeScan(mLeScanCallback);
        } else if (mScanning) {
            mScanning = false;
            mBTA.stopLeScan(mLeScanCallback);
        }
    }


    private boolean checkPermission(final String permission) {
        int result = getApplicationContext().checkPermission(permission, android.os.Process.myPid(), android.os.Process.myUid());
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkLocationPermission() {
        return checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION) || checkPermission(Manifest.permission.ACCESS_FINE_LOCATION);
    }


}