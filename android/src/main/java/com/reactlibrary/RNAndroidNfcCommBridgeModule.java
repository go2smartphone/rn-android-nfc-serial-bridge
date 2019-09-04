
package com.reactlibrary;

import android.util.Log;
import android.widget.Toast;

import com.facebook.react.bridge.Arguments;
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

public class RNAndroidNfcCommBridgeModule extends ReactContextBaseJavaModule {
    protected SerialPort serialPort;
    protected InputStream inputStream;
    protected ReadThread readThread;
    private final ReactApplicationContext reactContext;
    private static final String TAG = "NFCSerial";
    private class ReadThread extends Thread {
        public ReadThread() {
            super("ReadThread");
        }
        @Override
        public void run() {
            super.run();
            int totalSize = 0;
            StringBuffer cardNumber = new StringBuffer();
            while(!isInterrupted()) {
                int size = 0;
                try {
                    byte[] buffer = new byte[1024];


                    if (inputStream == null) return;
                    size = inputStream.read(buffer);
                    if (size > 0) {
                        if (buffer[0] == 85)
                        {
                            totalSize = 0;
                            cardNumber.delete(0,cardNumber.length());
                        }
                        totalSize = totalSize + size;
                        String partialNumber = byteArrayToHexString(buffer,size);
                        cardNumber.append(partialNumber);
                        if (buffer[size-1] == 118)
                        {
                            String cardNumberFinal= cardNumber.toString().substring(4,12);
                            Log.d(TAG,"final card number:" + cardNumberFinal);

                            WritableMap infoMap = Arguments.createMap();
                            infoMap.putString("carNumber",cardNumberFinal);

                            sendEvent("SERIAL_NFC_EVENT",infoMap);
                            totalSize = 0;
                            cardNumber.delete(0,cardNumber.length());

                        }


                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    public RNAndroidNfcCommBridgeModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "RNAndroidNfcCommBridge";
    }

    @ReactMethod
    public void showMessage() {
        Toast.makeText(reactContext.getApplicationContext(), "NATIVE CODE IS WORKING", Toast.LENGTH_LONG).show();
    }
    @ReactMethod

     public void setSerialPort(String path, int baudrate) throws SecurityException, IOException, InvalidParameterException {
        if (this.serialPort == null) {
            Log.d(TAG, "enter set serial port");
            /* Open the serial port */
            serialPort = new SerialPort(new File(path), baudrate, 0);
            Log.d(TAG, "serial port created");
            inputStream = serialPort.getInputStream();
            Log.d(TAG, "input stream created");
            readThread = new ReadThread();
            readThread.start();
        }
    }

    @ReactMethod
    public void setPort() {
        Log.d(TAG, "set  port no para");
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
        this.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }
    public  String byteArrayToHexString(final byte[] bytes,int size) {
        StringBuilder sb = new StringBuilder();
        for(int i=0;i<size;i++){
            sb.append(String.format("%02x", bytes[i]&0xff));
        }
        return sb.toString();
    }

}