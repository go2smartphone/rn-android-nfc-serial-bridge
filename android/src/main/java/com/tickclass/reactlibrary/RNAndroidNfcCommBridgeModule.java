
package com.tickclass.reactlibrary;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidParameterException;

import android_serialport_api.SerialPort;

public class RNAndroidNfcCommBridgeModule extends ReactContextBaseJavaModule implements ActivityEventListener, LifecycleEventListener {
    private SerialPort serialPort;
    private InputStream inputStream;
    private ReadThread readThread;
    private final ReactApplicationContext reactContext;
    private static final String TAG = "NFCSerial";
    private static RNAndroidNfcCommBridgeModule instance = null;
    private String rfidString = "";
    private NfcAdapter mAdapter;


    private class ReadThread extends Thread {
        public ReadThread() {
            super("ReadThread");
        }

        @Override
        public void run() {
            super.run();
            int totalSize = 0;
            StringBuffer cardNumber = new StringBuffer();
            while (!isInterrupted()) {
                int size = 0;
                try {
                    byte[] buffer = new byte[1024];

                    if (inputStream == null)
                        return;
                    size = inputStream.read(buffer);
                    if (size > 0) {
                        if (buffer[0] == 85) {
                            totalSize = 0;
                            cardNumber.delete(0, cardNumber.length());
                        }
                        totalSize = totalSize + size;
                        String partialNumber = byteArrayToHexString(buffer, size);
                        cardNumber.append(partialNumber);
                        if (totalSize > 6) {
                            // String cardNumberFinal= cardNumber.toString().substring(4,12);
                            String temp = cardNumber.toString().substring(4, 12);
                            String cardNumberFinal = temp.substring(6, 8) + temp.substring(4, 6) + temp.substring(2, 4)
                                    + temp.substring(0, 2);

                            Log.d(TAG, "final card number:" + cardNumberFinal);

                            WritableMap infoMap = Arguments.createMap();
                            infoMap.putString("cardNumber", cardNumberFinal);

                            sendEvent("SERIAL_NFC_EVENT", infoMap);
                            totalSize = 0;
                            cardNumber.delete(0, cardNumber.length());

                        }

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    public static RNAndroidNfcCommBridgeModule initModule(ReactApplicationContext reactContext) {
        instance = new RNAndroidNfcCommBridgeModule(reactContext);
        return instance;
    }

    public static RNAndroidNfcCommBridgeModule getInstance() {
        return instance;
    }

    public RNAndroidNfcCommBridgeModule(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addActivityEventListener(this);
        reactContext.addLifecycleEventListener(this);
        this.reactContext = reactContext;

    }

    @Override
    public String getName() {
        return "RNAndroidNfcCommBridge";
    }

    @ReactMethod
    public void showMessage(String message) {
        Toast.makeText(reactContext.getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    @ReactMethod

    public void setSerialPort(String path, int baudrate) {
        this.initBuiltInNFC();
        this.setSerialNFC(path, baudrate);

    }

    public void  setSerialNFC(String path, int baudrate) {
        try {
            if (this.serialPort == null) {

//                SerialPortFinder serialPortFinder = new SerialPortFinder();
//                String[] devices = serialPortFinder.getAllDevicesPath();
//                int i;
                   boolean portFound = true;
//                for (i=0; i<devices.length; i++) {
//                    if (devices[i].equals(path)) {
//                        Log.d(TAG, "Found new device: " + devices[i]);
//                        portFound = true;
//                    }
//                }
                if (portFound) {
                    Log.d(TAG, "enter1 set serial port");
                    /* Open the serial port */
                    serialPort = new SerialPort(new File(path), baudrate, 0);
                    Log.d(TAG, "serial port created");
                    inputStream = serialPort.getInputStream();
                    Log.d(TAG, "input stream created");
                    readThread = new ReadThread();
                    readThread.start();
                }

            }

        } catch (Exception e) {
            Log.d(TAG, "error to create serial port" + e.getMessage());

        }
    }

    @ReactMethod
    public void closeSerialPort() {
        Log.d(TAG, "close set serial port");
        if (this.serialPort != null) {
            this.serialPort.close();
            this.serialPort = null;
        }
    }

    private void sendEvent(String eventName, WritableMap params) {
        this.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
    }

    private String byteArrayToHexString(final byte[] bytes, int size) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            sb.append(String.format("%02x", bytes[i] & 0xff));
        }
        return sb.toString();
    }


    public void onKeyUpEvent(int keyCode, KeyEvent keyEvent) {
        Log.d(TAG, "key " + keyCode);
        char pressedKey = (char) keyEvent.getUnicodeChar();
        rfidString = rfidString + pressedKey;
        Log.d(TAG, "rfid " + rfidString);
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            Log.d(TAG, "send card number event ");
            WritableMap infoMap = Arguments.createMap();
            infoMap.putString("cardNumber", rfidString);
            sendEvent("SERIAL_NFC_EVENT", infoMap);
            rfidString = "";
        }
    }

    ;

    public void initBuiltInNFC() {
        try {
            Log.d(TAG, "init built-in NFC  ");
            //IntentFilter techFilter = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
            // IntentFilter tagFilter = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);

            WritableMap params = Arguments.createMap();
            mAdapter = NfcAdapter.getDefaultAdapter(getReactApplicationContext());
            Bundle readerModeExtras = new Bundle();
            readerModeExtras.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 100);

            mAdapter.enableReaderMode(reactContext.getCurrentActivity(), new NfcAdapter.ReaderCallback() {
                @Override
                public void onTagDiscovered(Tag tag) {

                    WritableMap infoMap = Arguments.createMap();
                    String tagId = byteArrayToHexString(tag.getId(), tag.getId().length);
                    infoMap.putString("cardNumber", tagId);
                    sendEvent("SERIAL_NFC_EVENT", infoMap);
                    Log.d(TAG, "NFC onTagDiscovered" + tagId);
                }
            }, NfcAdapter.FLAG_READER_NFC_A, readerModeExtras);
            Log.d(TAG, "init built-in NFC  ");
            if (mAdapter != null) {
                if (mAdapter.isEnabled()) {
                    Log.d(TAG, "builtin NFC is enabled ");
                    params.putString("nfcAvailable", "yes"); //status is ok
                } else {
                    Log.d(TAG, "builtin NFC is disabled ");
                }
            } else {
                Log.d(TAG, "madapter:" + mAdapter.toString());
            }
        } catch (Exception e) {
            Log.d(TAG, "error to set up builtin nfc port" + e.getMessage());
        }
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        //Toast.makeText(activity.getApplication().getApplicationContext(), "some mesg ", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onNewIntent(Intent intent) {

        Log.d(TAG, "enter onNewIntent:");
        resolveIntent(intent);
    }

    @Override
    public void onHostResume() {

        Log.d(TAG, "onHostResume entered ");
        //PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        //mAdapter.enableForegroundDispatch(reactContext.getCurrentActivity(), pendingIntent, null, null);

        //resolveIntent(reactContext.().getIntent());

    }

    @Override
    public void onHostPause() {
        Log.d(TAG, "onHostPause entered ");
        if (mAdapter != null) {
            mAdapter.disableForegroundDispatch(reactContext.getCurrentActivity());
        }

    }

    @Override
    public void onHostDestroy() {
        Log.d(TAG, "onHostDestroy entered ");
    }

    private void resolveIntent(Intent intent) {
        String action = intent.getAction();
        Log.i("MYTAG", "enter resolveIntent: " + action);
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            WritableMap infoMap = Arguments.createMap();
            String NFCID = this.ByteArrayToHexString(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID));
            infoMap.putString("cardNumber", NFCID);
            sendEvent("SERIAL_NFC_EVENT", infoMap);
            Log.d("TAG", "TAG ID" + NFCID);


        }
    }

    private String ByteArrayToHexString(byte[] inarray) {
        int i, j, in;
        String[] hex = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};
        String out = "";
        for (j = 0; j < inarray.length; ++j) {
            in = (int) inarray[j] & 0xff;
            i = (in >> 4) & 0x0f;
            out += hex[i];
            i = in & 0x0f;
            out += hex[i];
        }
        return out;
    }
}