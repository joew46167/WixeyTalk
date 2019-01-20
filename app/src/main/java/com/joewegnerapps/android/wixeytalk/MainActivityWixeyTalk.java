package com.joewegnerapps.android.wixeytalk;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivityWixeyTalk extends Activity implements DeviceTalkService.MessageHandler {

    private final String TAG = this.getClass().getSimpleName();
    Activity mSelf = this;
    Button mSimButton;
    boolean mButtonsSet = false;
    boolean mTbReady = false;

//    DeviceTalkService.ServiceBinder mState.mDeviceTalkService = null;
//    boolean mState.mIsBound = false;

    TextView mAngleTB = null;

    final static int ON_BG = 0xFF79FA08;
    final static int OFF_BG = 0xFF798169;

    class State {
        boolean mIsBound;
        DeviceTalkService.ServiceBinder mDeviceTalkService;
        boolean mTalkState;
        boolean mRepeatState;
        boolean mAccuracyState;
        String mAngle;
        boolean mSimEnabled;
        boolean mSimActive;
        int mBackgroundColor;
        TextToSpeech mTTSActive;
        int mOrientation;

        public State (){
            mIsBound = false;
            mDeviceTalkService=null;
            mTalkState = true;
            mRepeatState = false;
            mAccuracyState = true;
            mAngle = "";
            mSimActive = false;
            mSimEnabled = true;
            mTTSActive = null;
            mBackgroundColor = OFF_BG;
            mOrientation = 0;
        }
    }
    
    State mState = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Constants.setApp(this.getApplication());
        Log.v("CREATE", "creating");
        
        mState = (State) getLastNonConfigurationInstance();

        if (mState == null) {  // new, so need to init everything
            mState = new State();

            // Check if BLE is supported on the device.
            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                Toast.makeText(this,
                        getString(R.string.txtNoBluetooth),
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        else {
            Log.v(TAG, "Retained state");
        }

        if (!mState.mIsBound) {
            Intent gattServiceIntent = new Intent(this, DeviceTalkService.class);
            mState.mIsBound = bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        }

        setupView();
    }

    private void setupView(){
        setContentView(R.layout.activity_main_wixey_talk);

        mAngleTB = (TextView) findViewById(R.id.txtAngle);
        Log.v("JAWTMP", "mAngleTB set");
        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/DSEG7Classic-Regular.ttf");
        mAngleTB.setTypeface(typeface);
        mAngleTB.setBackgroundColor(mState.mBackgroundColor);
        mAngleTB.setText("");//mState.mAngle);
        mAngleTB.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        mTbReady = true;

        TextView purchaseTV = (TextView) findViewById(R.id.txtPurchase);
        purchaseTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.txtUrl)));
                startActivity(intent);
            }
        });

        if (mState.mDeviceTalkService != null) {
            setupButtons(this);
        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        mState.mSimEnabled = mSimButton.isEnabled();
        ((ConstraintLayout)findViewById(R.id.wixeyView)).removeAllViews();
        super.onConfigurationChanged(newConfig);
        setupView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mState.mDeviceTalkService.unregister(this);
        unbindService(mServiceConnection);
        mState.mIsBound = false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("bound", mState.mIsBound);
        outState.putBinder("service", mState.mDeviceTalkService);
        outState.putBoolean("talk", mTalk.isChecked());
        outState.putBoolean("repeat", mRepeat.isChecked());
        outState.putString("angle", mAngleTB.getText().toString());
    }


    @Override
    public Object onRetainNonConfigurationInstance() {
        mState.mAccuracyState = mAccuracy.isChecked();
        mState.mRepeatState = mRepeat.isChecked();
        mState.mTalkState = mTalk.isChecked();
        mState.mAngle = mAngleTB.getText().toString();
        mState.mSimEnabled = mSimButton.isEnabled();
        mState.mSimActive = mSimButton.getText().equals(getString(R.string.btnStopSimulation));
        return mState;
    }

    private final int NEW_DATA = 1;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case Constants.MSG_TYPE_GATT_CONNECTED:
                    Log.v(TAG, "Device connected");
                    break;
                case Constants.MSG_TYPE_GATT_DISCONNECTED:
                    Log.v(TAG, "Device disconnected");
                    setSimButton(true,true);
                    break;
                case Constants.MSG_TYPE_GATT_SERVICES_DISCOVERED:
                    setSimButton(false, true);
                    break;
                case Constants.MSG_TYPE_DATA_AVAILABLE:
                case Constants.MSG_TYPE_SIM_DATA_AVAILABLE:
                    AbstractDevice.DisplayInfo data = (AbstractDevice.DisplayInfo) msg.obj;
                    updateDisplay(data.mDisplay, data.mSpeak, data.mDeviceOn);
//                    mAngleTB.setText(data.mDisplay);
                    break;

                case Constants.MSG_TYPE_SERVICE_READY:
                    setupButtons(mSelf);
                    break;

                default:
                    break;
            }
        }
    };

    public void sendMessage(int msgType, AbstractDevice.DisplayInfo data) {
        Message msg = mHandler.obtainMessage(msgType,data);
        mHandler.sendMessage(msg);
    }

    public void sendMessage(int msgType) {
        Message msg = mHandler.obtainMessage(msgType);
        mHandler.sendMessage(msg);
    }

    @Override
    protected void onResume() {
        super.onResume();
//        mAngleTB = (TextView) findViewById(R.id.txtAngle);
//        Log.v("JAWTMP", "mAngleTB set");
//        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/DSEG7Classic-Regular.ttf");
//        mAngleTB.setTypeface(typeface);
//        mAngleTB.setBackgroundColor(mState.mBackgroundColor);
//        mAngleTB.setText(mState.mAngle);
//        mAngleTB.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
//
//        TextView purchaseTV = (TextView) findViewById(R.id.txtPurchase);
//        purchaseTV.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.txtUrl)));
//                startActivity(intent);
//            }
//        });
//
//
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_GATT_CONNECTED);
        intentFilter.addAction(Constants.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(Constants.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(Constants.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(Constants.ACTION_SIM_DATA_AVAILABLE);
        return intentFilter;
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.v(TAG, "Broadcast on receive " + action);
            if (Constants.ACTION_GATT_CONNECTED.equals(action)) {
            } else if (Constants.ACTION_GATT_DISCONNECTED.equals(action)) {
                setSimButton(true,true);
            } else if (Constants.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                setSimButton(false, true);
            } else if (Constants.ACTION_DATA_AVAILABLE.equals(action)) {
                AbstractDevice.DisplayInfo data = (AbstractDevice.DisplayInfo) intent.getExtras().get(Constants.EXTRA_DATA);
            } else if (Constants.ACTION_SIM_DATA_AVAILABLE.equals(action)) {
                AbstractDevice.DisplayInfo data = (AbstractDevice.DisplayInfo) intent.getExtras().get(Constants.EXTRA_DATA);
            }
        }
    };

    private Switch mRepeat;
    private Switch mTalk;
    private Switch mAccuracy;

    private void setupButtons(final Context ctx) {
        mButtonsSet = true;
        Button button = (Button) findViewById(R.id.btnOnOff);
        button.setEnabled(true);
        button.setVisibility(View.VISIBLE);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSimButton.isEnabled()) {
                    boolean start = mState.mDeviceTalkService.startSimButton();
                    setSimButton(true, !start);
                }
                else {
                    mState.mDeviceTalkService.powerButton();
                }
            }
        });

        button = (Button) findViewById(R.id.btnZero);
        button.setEnabled(true);
        button.setVisibility(View.VISIBLE);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mState.mDeviceTalkService.zeroButton();
            }
        });

        mTalk = (Switch) findViewById(R.id.btnTalk);
        mTalk.setEnabled(true);
        mTalk.setChecked(mState.mTalkState);
        mTalk.setVisibility(View.VISIBLE);
        mTalk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean checked = ((Switch) view).isChecked();
                mState.mDeviceTalkService.talkSwitch(checked);
                mRepeat.setEnabled(checked);
                if (!checked) {
                    mRepeat.setChecked(false);
                    mState.mRepeatState = false;
                    mState.mDeviceTalkService.repeatSwitch(false);
                }
                mState.mTalkState = checked;
            }
        });

        mRepeat = (Switch) findViewById(R.id.btnRepeat);
        mRepeat.setEnabled(mState.mTalkState);
        mRepeat.setChecked(mState.mRepeatState);
        mRepeat.setVisibility(View.VISIBLE);
        mRepeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean checked = ((Switch) view).isChecked();
                mState.mDeviceTalkService.repeatSwitch(checked);
                mState.mRepeatState = checked;
            }
        });

        button = (Button) findViewById(R.id.btnBuffer);
        button.setEnabled(false);
        button.setVisibility(View.INVISIBLE);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        mAccuracy = (Switch) findViewById(R.id.btnAccuracy);
        mAccuracy.setEnabled(true);
        mAccuracy.setVisibility(View.VISIBLE);
        mAccuracy.setChecked(mState.mAccuracyState);
        mPoint1 = mState.mAccuracyState;
        mState.mDeviceTalkService.accuracySwitch(mState.mAccuracyState);
        mAccuracy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPoint1 = ((Switch) view).isChecked();
                mState.mAccuracyState = mPoint1;
                mState.mDeviceTalkService.accuracySwitch(mState.mAccuracyState);
            }
        });

        mSimButton = (Button) findViewById(R.id.btnSimulation);
        setSimButton(mState.mSimEnabled, !mState.mSimActive);
        mSimButton.setVisibility(View.VISIBLE);
        mSimButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean start = mState.mDeviceTalkService.startSimButton();
                setSimButton(true, !start);
            }
        });
    }

    private void setSimButton(boolean enabled, boolean start){
        if (mState.mDeviceTalkService.isSimulationAvailable()) {
            mSimButton.setEnabled(enabled);
            mState.mSimActive = !start;
            mSimButton.setTag(start);
            if (start) {
                if (enabled) {
                    mSimButton.setText(getString(R.string.btnStartSimulation));
                    mSimButton.setContentDescription(getString(R.string.btnStartSimulation));
                    updateDisplay("", "", false);
                }
                else {
                    mSimButton.setText(getString(R.string.btnNoSimulationConnected));
                    mSimButton.setContentDescription(getString(R.string.btnNoSimulationConnected));
                }
            } else {
                mSimButton.setText(getString(R.string.btnStopSimulation));
                mSimButton.setContentDescription(getString(R.string.btnStopSimulation));
            }
        }
        else {
            mSimButton.setText(getString(R.string.btnNoSimulation));
            mSimButton.setContentDescription(getString(R.string.btnNoSimulation));
            mSimButton.setEnabled(false);
        }
    }

    boolean mPoint1 = true;

    private void showOnOff(boolean on) {
        if (on) {
            mAngleTB.setBackgroundColor(ON_BG);
            mState.mBackgroundColor = ON_BG;
        }
        else {
            mAngleTB.setBackgroundColor(OFF_BG);
            mState.mBackgroundColor = OFF_BG;
            if (mTbReady) mAngleTB.setText("");
        }
    }

    private void updateDisplay(String display, String speech, boolean onOff) {
        if (mTbReady) {
            mAngleTB.setText(display);
            mState.mDeviceTalkService.speakData(speech, onOff);
            showOnOff(onOff);
        }
    }

    public void onRequestPermissionsResult (int requestCode,
                                                 String[] permissions,
                                                 int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (requestCode == Constants.LOC_ENABLE_REQUEST) { // should never get here if null, but checking just in case

            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                checkBT();
            }
            else {
                Toast.makeText(this, getString(R.string.txtTurnOnBTLoc), Toast.LENGTH_LONG).show();
                Log.e( TAG, "No permission to use bluetooth location");
            }

        }
    }

    //////////////////////////////
    //
    // Code to manage Service lifecycle.
    //
    /////////////////////////////
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.v(TAG, "Service Connected");

            if (!mButtonsSet) {
                Message msg = mHandler.obtainMessage(Constants.MSG_TYPE_SERVICE_READY);
                mHandler.sendMessage(msg);
            }

            mState.mDeviceTalkService = (DeviceTalkService.ServiceBinder) service;
            if (!mState.mDeviceTalkService.bluetoothAdapterAvailable()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            // mState.mDeviceTalkService.connect(mDeviceAddress);

            if (mState.mTTSActive == null) {
                mState.mTTSActive = mState.mDeviceTalkService.createTTS();
            }
            else {
                mState.mDeviceTalkService.setTTS(mState.mTTSActive);
            }

            // now that the service is set up, start up to use that service
            mState.mDeviceTalkService.CommConnection(mSelf);
            mState.mDeviceTalkService.register((DeviceTalkService.MessageHandler) mSelf);
            if (!mState.mDeviceTalkService.isGattConnected()) {
                mState.mDeviceTalkService.initBluetooth(mSelf.getApplicationContext());
            }
            mState.mDeviceTalkService.talkSwitch(mState.mTalkState);
            Log.v(TAG, "Set talk state = " + mState.mTalkState);
            //
            //            mState.mDeviceTalkService.setupTts();
            //            mState.mDeviceTalkService.speakTimer();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mState.mDeviceTalkService.unregister((DeviceTalkService.MessageHandler) mSelf);
            mState.mDeviceTalkService.disconnect();
            mState.mDeviceTalkService = null;
        }
    };
}
