
# rn-android-nfc-comm-bridge

## Getting started

`$ npm install react-native-android-nfc-comm-bridge --save`

### Mostly automatic installation

`$ react-native link react-native-android-nfc-comm-bridge`

### Manual installation


#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.reactlibrary.RNAndroidNfcCommBridgePackage;` to the imports at the top of the file
  - Add `new RNAndroidNfcCommBridgePackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-android-nfc-comm-bridge'
  	project(':react-native-android-nfc-comm-bridge').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-android-nfc-comm-bridge/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-android-nfc-comm-bridge')
  	```


## Usage
```javascript
import RNAndroidNfcCommBridge from 'react-native-android-nfc-comm-bridge';

// TODO: What to do with the module?
RNAndroidNfcCommBridge;
```
  # rn-android-nfc-serial-bridge
