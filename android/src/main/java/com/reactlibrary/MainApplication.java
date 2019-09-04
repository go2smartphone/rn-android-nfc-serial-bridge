package com.reactlibrary;

import android.app.Application;

import com.facebook.react.ReactPackage;
import com.facebook.react.shell.MainReactPackage;

import java.util.Arrays;
import java.util.List;

public class MainApplication extends Application {

    protected List<ReactPackage> getPackages() {
        return Arrays.<ReactPackage>asList(
                new MainReactPackage(),
                new RNAndroidNfcCommBridgePackage()); // rename this to match your "package" java file.
    }


}
