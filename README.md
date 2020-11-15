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

The library provides both a fluent and a more concise interface. In the examples the concise approach is applied; fluent interface is demonstrated later in this document.

### Straightforward down- and upload

```tsx
import BlobCourier from 'react-native-blob-courier';

// ...

// Download a file
const request0 = {
  filename: '5MB.zip',
  method: 'GET',
  mimeType: 'application/zip',
  url: 'http://ipv4.download.thinkbroadband.com/5MB.zip',
};

const fetchedResult = await BlobCourier.fetchBlob(request0);
console.log(fetchedResult);
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
const filePath = fetchedResult.data.absoluteFilePath;

const request1 = {
  filePath,
  method: 'POST',
  mimeType: 'application/zip',
  url: 'https://file.io',
};

const uploadResult = await BlobCourier.uploadBlob(request1);

console.log(uploadResult):
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
import BlobCourier from 'react-native-blob-courier';

// ...

// Download a file
const request0 = {
  // ...
  onProgress: ((e: BlobProgressEvent) => {
    console.log(e)
    // {
    //  "written": <some_number_of_bytes_written>,
    //  "total": <some_total_number_of_bytes>
    // }
  })
};

const fetchedResult = await BlobCourier.fetchBlob(request0);

// ...

// Upload a file
const request1 = {
  // ...
  onProgress: ((e: BlobProgressEvent) => {
    console.log(e)
    // {
    //  "written": <some_number_of_bytes_written>,
    //  "total": <some_total_number_of_bytes>
    // }
  })
};

const uploadResult = await BlobCourier.uploadBlob(request1)

// ...

// Set progress updater interval
const request2 = ...

const someResult =
  await BlobCourier
    .fetchBlob({
      ...request2,
      progressIntervalMilliseconds: 1000,
    });
```

### Managed download on Android (not available on iOS)

```tsx
import BlobCourier from 'react-native-blob-courier';

// ...

const request = {
  filename: '5MB.zip',
  method: 'GET',
  mimeType: 'application/zip',
  url: 'http://ipv4.download.thinkbroadband.com/5MB.zip',
  useAndroidDownloadManager: true // <--- set useAndroidDownloadManager to "true"
};

const fetchResult = await BlobCourier.fetchBlob(request);

console.log(fetchedResult);
// {
//   "data": {
//     "result": "SUCCESS",
//     "absoluteFilePath": "/path/to/app/cache/5MB.zip"
//   },
//   "type":"Managed"
// }
```

## Fluent interface

Blob Courier provides a fluent interface, that both prevents you from impossible setting combinations and arguably improves readability.

```tsx
const req0 = ...

const someResult =
  await BlobCourier
    .settings({
      progressIntervalMilliseconds: 1000,
    })
    .onProgress((e: BlobProgressEvent) => {
      // ...
    })
    .useDownloadManagerOnAndroid({
      description: "Some file description",
      enableNotification: true,
      title: "Some title"
    })
    .fetchBlob(req0)
```

## Available methods

### `fetchBlob(input: BlobFetchRequest)`

Required

| **Field**  | **Type** | **Description**                                                     |
| ---------- | -------- | ------------------------------------------------------------------- |
| `filename` | `string` | The name the file will have on disk after fetch.                    |
| `mimeType` | `string` | What is the mime type of the blob being transferred?                |
| `url`      | `string` | From which url will the blob be fetched?                            |

Optional

| **Field**                   | **Type**                         | **Description**                           | **Default** |
| ----------------------------| -------------------------------- | ----------------------------------------- | ----------- |
| `headers`                   | `{ [key: string]: string }`      | Map of headers to send with the request   | `{}`        |
| `method`                    | `string`                         | Representing the HTTP method              | `GET`       |
| `onProgress`                | `(e: BlobProgressEvent) => void` | Function handling progress updates        | `() => { }` |
| `useAndroidDownloadManager` | `boolean`                        | Enable download manager on Android?       | `false`     |
| `androidDownloadManager`    | `AndroidDownloadManagerSettings` | Settings to be used on download manager   | `{}`        |

Response

| **Field** | **Type**                               | **Description**                                                       |
| --------- | -------------------------------------- | --------------------------------------------------------------------- |
| `type`    | `"Managed" \| "Unmanaged"`             | Was the blob downloaded through Android Download Manager, or without? |
| `data`    | `BlobManagedData \| BlobUnmanagedData` | Either managed or HTTP response data                                  |

### `uploadBlob(input: BlobUploadRequest)`

Required

| **Field**  | **Type** | **Description**                                          |
| ---------- | ---------| -------------------------------------------------------- |
| `filePath` | `string` | Path to the file to be uploaded                          |
| `mimeType` | `string` | Mime type of the blob being transferred                  |
| `url`      | `string` | Url to upload the blob to                                |

Optional

| **Field**        | **Type**                         | **Description**                           | **Default** |
| ---------------- | -------------------------------- | ----------------------------------------- | ----------- |
| `headers`        | `{ [key: string]: string }`      | Map of headers to send with the request   | `{}`        |
| `method`         | `string`                         | The HTTP method to be used in the request | `POST`      |
| `onProgress`     | `(e: BlobProgressEvent) => void` | Function handling progress updates        | `() => { }` |
| `returnResponse` | `boolean`                        | Return the HTTP response body?            | `false`     |

Response

| **Field** | **Type**                     | **Description**   |
| --------- | ---------------------------- | ----------------- |
| `response` | `BlobUnmanagedHttpResponse` | The HTTP response |

#### `AndroidDownloadManagerSettings`

| **Field**             | **Type**  | **Description**                              |
| --------------------- | ----------| -------------------------------------------- |
| `description?`        | `string`  | Description of the downloaded file           |
| `enableNotification?` | `boolean` | Display notification when download completes |
| `title?`              | `string`  | Title to be displayed with the download      |

#### `BlobProgressEvent`

| **Field** | **Type** | **Description**                       |
| --------- | ---------| ------------------------------------- |
| `written` | `number` | Number of bytes processed             |
| `total`   | `number` | Total number of bytes to be processed |

#### `BlobUnmanagedHttpResponse`

| **Field** | **Type**                    | **Description**       |
| --------- | --------------------------- | --------------------- |
| `code`    | `number`                    | HTTP status code      |
| `headers` | `{ [key: string]: string }` | HTTP response headers |

#### `BlobManagedData`

| **Field**          | **Type**                 | **Description**                                     |
| ------------------ | ------------------------ | --------------------------------------------------- |
| `absoluteFilePath` | `string`                 | The absolute file path to where the file was stored |
| `result`           | `"SUCCESS" \| "FAILURE"` | Was the request successful or did it fail?          |

#### `BlobUnmanagedData`

| **Field**          | **Type**                    | **Description**                                     |
| ------------------ | --------------------------- | --------------------------------------------------- |
| `absoluteFilePath` | `string`                    | The absolute file path to where the file was stored |
| `response`         | `BlobUnmanagedHttpResponse` | HTTP response, including headers and status code    |

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

To enable the download manager, simply set the request's `useAndroidDownloadManager` to `true` when passing it to `fetchBlob`, or call the `useDownloadManagerOnAndroid` method when using the fluent interface.

## Shared directories

As this library is focussed on transferring files, it only supports storage to the app's _cache_ directory. To move files from the _cache_ directory to the _Downloads_, or _Documents_ directory, use another library like [@react-native-community/cameraroll](https://github.com/react-native-community/react-native-cameraroll), e.g.:

```tsx
import BlobCourier from 'react-native-blob-courier';
import CameraRoll from '@react-native-community/cameraroll';

// ...

const request = {
  filename: 'teh_cage640x360.png',
  method: 'GET',
  mimeType: 'image/png',
  url: 'https://www.placecage.com/640/360',
};

const cageResult = await BlobCourier.fetchBlob(request)

const cageLocalPath = cageResult.data.absoluteFilePath

CameraRoll.save(cageLocalPath);
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the  repository and the development workflow.

## License

MPL-2.0
