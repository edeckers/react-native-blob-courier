# react-native-blob-courier

[![License: MPL 2.0](https://img.shields.io/badge/License-MPL%202.0-brightgreen.svg)](https://opensource.org/licenses/MPL-2.0)
![Build](https://github.com/edeckers/react-native-blob-courier/workflows/Build%20Android%20and%20iOS/badge.svg)

Use this library to efficiently _download_ and _upload_ blobs in React Native. The library was inspired by [rn-fetch-blob](https://github.com/joltup/rn-fetch-blob), and aims to focus strictly on blob transfers.

## Installation

Install using _yarn_

```sh
yarn add react-native-blob-courier
```

Or install using _npm_

```sh
npm install react-native-blob-courier
```

Link the library:

NB. Linking can be skipped when the project uses React Native 0.60 or greater, because [autolinking](https://reactnative.dev/blog/2019/07/03/version-60#native-modules-are-now-autolinked) will take care of it

```sh
react-native link react-native-blob-courier
```

If _CocoaPods_ is used in the project, make sure to install the pod:

```sh
cd ios && pod install
```

## Usage

### Straightforward down- and upload

```tsx
import BlobCourier from "react-native-blob-courier";

// ...

// Download a file
const request0 = {
  filename: '5MB.zip',
  method: 'GET',
  mimeType: 'application/zip',
  url: 'http://ipv4.download.thinkbroadband.com/5MB.zip',
} as BlobFetchRequest

const fetchedResult = await BlobCourier.fetchBlob(request0);
console.log(fetchedResult)
// {
//   "data": {
//     "absoluteFilePath": "/path/to/app/cache/5MB.zip",
//     "response": {
//       "code":200,
//       "headers": {
//         "some_header": "some_value",
//         ...
//       }
//     },
//   },
//   "type":"Unmanaged"
// }

// ...

// Upload a file
const filePath = fetchedResult.data.absoluteFilePath

const request1 = {
  filePath,
  method: 'POST',
  mimeType: 'application/zip',
  url: 'https://file.io',
} as BlobUploadRequest)

const uploadResult = await BlobCourier.uploadBlob(request1)

console.log(uploadResult)
// {
//   "response": {
//     "code": {
//     "data": "<some response>",
//     "headers": {
//       "some_header": "some_value",
//        ...
//      }
//   }
// }

```

### Transfer progress reporting

```tsx
import BlobCourier from "react-native-blob-courier";

// ...

// Download a file
const request0 = ...

const fetchedResult =
  await BlobCourier
    .fetchBlob(request0)
    .onProgress((e: any) => {
      console.log(e)
      // {
      //  "written": <some_number_of_bytes_written>,
      //  "total": <some_total_number_of_bytes>
      // }
    });

// ...

// Upload a file
const request1 = ...

const uploadResult =
  await BlobCourier
    .uploadBlob(request1)
    .onProgress((e: any) => {
      // ...
    });

// ...

// Set request settings
const request2 = ...

const someResult =
  await BlobCourier
    .settings({
      progressIntervalMilliseconds: 1000,
    })
    .fetchBlob(request2)
    .onProgress((e:any) => {
      // ...
    });
```

### Managed download on Android (not available on iOS)

```tsx
import BlobCourier from "react-native-blob-courier";

// ...

const request = {
  filename: '5MB.zip',
  method: 'GET',
  mimeType: 'application/zip',
  url: 'http://ipv4.download.thinkbroadband.com/5MB.zip',
  useDownloadManager: true // <--- set useDownloadManager to "true"
} as AndroidBlobFetchRequest; // <--- use AndroidBlobFetchRequest instead of BlobRequest

const fetchResult = await BlobCourier.fetchBlob(request);

console.log(fetchedResult)
// {
//   "data": {
//     "result": "SUCCESS",
//     "absoluteFilePath": "/path/to/app/cache/5MB.zip"
//   },
//   "type":"Managed"
// }

```

## Example app

You can find an example of how to use the library in the [example](example) directory.

## Android

### Permissions

### Android 5.0 and below (API level < 23)

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

### Android 6.0+ (API level 23+)

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
        console.error(err);
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
```

## Using the integrated download manager for Android

This library allows you to use the integrated download manager on Android, this option is not available for iOS.

To enable the download manager, simply hand an `AndroidBlobFetchRequest` to `fetchBlob` with the `useDownloadManager`-field set to `true`.

## Shared directories

As this library is focussed on transferring files, it only supports storage to the app's _cache_ directory. To move files from the _cache_ directory to the _Downloads_, or _Documents_ directory, use another library like [@react-native-community/cameraroll](https://github.com/react-native-community/react-native-cameraroll), e.g.:

```tsx
import BlobCourier from "react-native-blob-courier";
import CameraRoll from '@react-native-community/cameraroll';

// ...

const request = {
  filename: 'teh_cage640x360.png',
  method: 'GET',
  mimeType: 'image/png',
  url: 'https://www.placecage.com/640/360',
} as BlobFetchRequest;

const cageResult = await BlobCourier.fetchBlob(request)

const cageLocalPath = (cageResult.data as BlobFilePathData).absoluteFilePath ?? ''

CameraRoll.save(cageLocalPath);
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the  repository and the development workflow.

## License

MPL-2.0
