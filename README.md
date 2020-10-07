# react-native-blob-courier

[![License: MPL 2.0](https://img.shields.io/badge/License-MPL%202.0-brightgreen.svg)](https://opensource.org/licenses/MPL-2.0)

Use this library to efficiently download and upload blobs in React Native. The library was inspired by [rn-fetch-blob](https://github.com/joltup/rn-fetch-blob), and aims to focus strictly on blob transfers.

## Installation

Install using yarn

```sh
yarn add react-native-blob-courier
```

Or install using npm

```
npm install react-native-blob-courier
```

## Usage

```tsx
import BlobCourier from "react-native-blob-courier";

// ...

const request: Readonly<BlobRequest> = {
  filename: 'my_downloaded_file.pdf',
  method: 'GET'
  target: CommonPath.Document,
  url: 'https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf'
};

try {
  const result = await BlobCourier.fetchBlob(request);

  // ...
} catch (e) {
  // ...
}
```

## Examples

You can find an example of how to use the library in the [example](example) directory.

## Android

**Permissions**

**Android 5.0 and below (API level < 23)**

Add the following line to `AndroidManifest.xml`.

```diff
<manifest xmlns:android="http://schemas.android.com/apk/res/android" (...)>

+   <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
+   <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
+   <uses-permission android:name="android.permission.DOWNLOAD_WITHOUT_NOTIFICATION" />
    (...)
    <application (...)>
      <activity (...)>
        <intent-filter>
          (...)
+          <action android:name="android.intent.action.DOWNLOAD_COMPLETE"/>
        </intent-filter>
    (...)
```

**Android 6.0+ (API level 23+)**
Grant permissions using the [PermissionAndroid API](https://facebook.github.io/react-native/docs/permissionsandroid.html), like so:
```tsx
const function App = () => {

  // ...

  React.useEffect(() => {
    const requestPermissionAsync = async () => {
      try {
        await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE
        );

        // ...
      } catch (err) {
        console.errr(err);
      }

      // ...
    };

    requestPermissionAsync();
  }, []);

  // ...
```

## iOS

Add to `Info.plist` of your app:
```xml
<key>NSAllowsArbitraryLoads</key>
<true/>
<key>LSSupportsOpeningDocumentsInPlace</key>
<true/>
<key>UIFileSharingEnabled</key>
<true/>
```

## Using the integrated download manager for Android

This library allows you to use the integrated download manager on Android, this option is not available for iOS.

To enable the download manager, simply hand an `AndroidBlobRequest` to `fetchBlob` with the `useDownloadManager`-field set to `true`.

```tsx
import BlobCourier from "react-native-blob-courier";

// ...

const request: Readonly<AndroidBlobRequest> = {
  // ...
  useDownloadManager: true
};

try {
  const result = await BlobCourier.fetchBlob(request);

  // ...
} catch (e) {
  // ...
}
```

## Shared directories
As this library is focussed on transferring files, it only supports storage to the app's data directory. To move files from the data directory to the Downloads, or Documents directory, use another library like [@react-native-community/cameraroll](https://github.com/react-native-community/react-native-cameraroll)


## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the  repository and the development workflow.

## License

MPL-2.0
