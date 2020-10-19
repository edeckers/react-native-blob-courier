# react-native-blob-courier

[![License: MPL 2.0](https://img.shields.io/badge/License-MPL%202.0-brightgreen.svg)](https://opensource.org/licenses/MPL-2.0)

Use this library to efficiently download and upload blobs in React Native. The library was inspired by [rn-fetch-blob](https://github.com/joltup/rn-fetch-blob), and aims to focus strictly on blob transfers.

## Installation

Install using _yarn_

```sh
yarn add react-native-blob-courier
```

Or install using _npm_

```sh
npm install react-native-blob-courier
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
  url: 'http://ipv4.download.thinkbroadband.com/5MB.zip',
} as BlobRequest

const fetchedResult = await BlobCourier.fetchBlob(request0);
console.log(fetchedResult)
// {
//   "data": {
//     "response": {
//       "headers": {
//         "some_header": "some_value",
//         ...
//       },
//       "code":200
//     },
//     "fullFilePath": "/path/to/app/cache/5MB.zip"
//   },
//   "type":"Unmanaged"
//   }

// ...

// Upload a file
const filePath =
  fetchedResult.type === BlobResponseType.Managed
    ? (fetchedResult.data as BlobManagedData).fullFilePath
    : (fetchedResult.data as BlobUnmanagedData).fullFilePath;

const request1 = {
  filePath,
  method: 'POST',
  mimeType: 'text/plain',
  url: 'https://file.io',
} as BlobUploadRequest)

const uploadResult = await BlobCourier.uploadBlob(request1)

console.log(uploadResult)
// {
//   "response": {
//     "headers": {
//       "some_header": "some_value",
//        ...
//      },
//     "data": "<some response>"
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
```

### Managed download on Android (not available on iOS)

```tsx
import BlobCourier from "react-native-blob-courier";

// ...

const request = {
  filename: '5MB.zip',
  method: 'GET',
  url: 'http://ipv4.download.thinkbroadband.com/5MB.zip',
  useDownloadManager: true // <--- set useDownloadManager to "true"
} as AndroidBlobRequest; // <--- use AndroidBlobRequest instead of BlobRequest

const fetchResult = await BlobCourier.fetchBlob(request);

console.log(fetchedResult)
// {
//   "data": {
//     "result": "SUCCESS",
//     "fullFilePath": "/path/to/app/cache/5MB.zip"
//   },
//   "type":"Managed"
//   }

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

To enable the download manager, simply hand an `AndroidBlobRequest` to `fetchBlob` with the `useDownloadManager`-field set to `true`.

## Shared directories

As this library is focussed on transferring files, it only supports storage to the app's data directory. To move files from the data directory to the Downloads, or Documents directory, use another library like [@react-native-community/cameraroll](https://github.com/react-native-community/react-native-cameraroll)

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the  repository and the development workflow.

## License

MPL-2.0
