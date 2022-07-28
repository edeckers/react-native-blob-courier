# react-native-blob-courier

[![License: MPL 2.0](https://img.shields.io/badge/License-MPL%202.0-brightgreen.svg)](https://opensource.org/licenses/MPL-2.0)
[![Build](https://github.com/edeckers/react-native-blob-courier/workflows/Build%20Android%20and%20iOS/badge.svg)](https://github.com/edeckers/react-native-blob-courier/actions)

Use this library to efficiently download and upload data in React Native. The library was inspired by [rn-fetch-blob](https://github.com/joltup/rn-fetch-blob), and is focused strictly on http file transfers.

## Installation

Install using _yarn_

```sh
yarn add react-native-blob-courier
```

Or install using _npm_

```sh
npm install react-native-blob-courier
```

## Requirements

- Android >= 24
- Android Gradle Plugin >= 7
- iOS >= 10
- JDK >= 11
- React Native >= 0.66.x

_Note: you may have success with earlier versions of React Native but these are neither tested nor supported._

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
const absoluteFilePath = fetchedResult.data.absoluteFilePath;

const request1 = {
  absoluteFilePath,
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

// Multipart file upload
const absoluteFilePath = fetchedResult.data.absoluteFilePath;

const request2 = {
  method: 'POST',
  parts: {
    body: {
      payload: 'some_value',
      type: 'string',
    },
    file: {
      payload: {
        absoluteFilePath,
        mimeType: 'application/zip',
      },
      type: 'file',
    },
  },
  url: 'https://file.io',
};

const multipartUploadResult = await BlobCourier.uploadBlob(request1);

console.log(multipartUploadResult):
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
  android: {
    useDownloadManager: true // <--- set useDownloadManager to "true"
  },
  filename: '5MB.zip',
  method: 'GET',
  mimeType: 'application/zip',
  url: 'http://ipv4.download.thinkbroadband.com/5MB.zip',
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

## Multipart upload

Sometimes order of multipart fields matters, and Blob Courier respects the order in which parts are provided. There is a catch though: due to how JavaScript works, when object keys are regular strings they are kept in the order they were added _unless_ the keys are strings containing numbers, e.g.:

```tsx
Object.keys({
  "b": "some_value1",
  "c": "some_value2",
  "a": "some_value3",
})

// ['b', 'c', 'a']

Object.keys({
  "b": "some_value1",
  "c": "some_value2",
  "a": "some_value3",
  "3": "some_value4",
  "2": "some_value5",
  "1": "some_value6",
});

// ['1', '2', '3', 'b', 'c', 'a']
```

The way to work around this, is to wrap _all_ keys in a `Symbol`, by using `Symbol.for`. Do not use `Symbol(<value>)`, this will not work, e.g.:

```tsx
Object.getOwnPropertySymbols({
  [Symbol.for("b")]: "some_value1",
  [Symbol.for("c")]: "some_value2",
  [Symbol.for("a")]: "some_value3",
  [Symbol.for("3")]: "some_value4",
  [Symbol.for("2")]: "some_value5",
  [Symbol.for("1")]: "some_value6",
});

// [Symbol('b'), Symbol('c'), Symbol('a'), Symbol('3'), Symbol('2'), Symbol('1')]

```

### Cancel request

```tsx
import BlobCourier from 'react-native-blob-courier';

// ...

const abortController = new AbortController();

const { signal } = abortController;

const request0 = {
  // ...
  signal,
};

try {
  BlobCourier.fetchBlob(request0);

  abortController.abort();
} catch (e) {
  if (e.code === ERROR_CANCELED_EXCEPTION) {
    // ...
  }
}

// ...
```

## Fluent interface

Blob Courier provides a fluent interface, that both protects you from using impossible setting combinations and arguably improves readability.

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

| **Field**    | **Type**                         | **Description**                           | **Default** |
| ------------ | -------------------------------- | ----------------------------------------- | ---------------------------------------------------- |
| `android`    | `AndroidSettings`                | Settings to be used on Android            | `{ downloadManager: {}, target: 'cache', useDownloadManager: false }` |
| `ios`        | `IOSSettings`                    | Settings to be used on iOS                | `{ target: 'cache' }`                                |
| `headers`    | `{ [key: string]: string }`      | Map of headers to send with the request   | `{}`                                                 |
| `method`     | `string`                         | Representing the HTTP method              | `GET`                                                |
| `onProgress` | `(e: BlobProgressEvent) => void` | Function handling progress updates        | `() => { }`                                          |
| `signal`     | `AbortSignal`                    | Request cancellation manager              | `null`                                               |

Response

| **Field** | **Type**                               | **Description**                                                       |
| --------- | -------------------------------------- | --------------------------------------------------------------------- |
| `type`    | `"Managed" \| "Unmanaged"`             | Was the blob downloaded through Android Download Manager, or without? |
| `data`    | `BlobManagedData \| BlobUnmanagedData` | Either managed or HTTP response data                                  |

### `uploadBlob(input: BlobUploadRequest)`

Alias for:

```tsx
const someResult =
  await BlobCourier
   // ...
   .uploadParts({
     headers,
     method,
     parts: {
       file:
         payload: {
           absoluteFilePath,
           filename,
           mimeType,
         },
         type: 'file',
       },
     },
     returnResponse,
     url,
   })
```

Required

| **Field**          | **Type** | **Description**                                          |
| ------------------ | -------- | -------------------------------------------------------- |
| `absoluteFilePath` | `string` | Path to the file to be uploaded                          |
| `mimeType`         | `string` | Mime type of the blob being transferred                  |
| `url`              | `string` | Url to upload the blob to                                |

Optional

| **Field**        | **Type**                         | **Description**                           | **Default**                         |
| ---------------- | -------------------------------- | ----------------------------------------- | ----------------------------------- |
| `filename`       | `string`                         | Name of the file on disk                  | `<name part of 'absoluteFilePath'>` |
| `headers`        | `{ [key: string]: string }`      | Map of headers to send with the request   | `{}`                                |
| `method`         | `string`                         | The HTTP method to be used in the request | `"POST"`                            |
| `multipartName`  | `string`                         | Name for the file multipart               | `"file"`                            |
| `onProgress`     | `(e: BlobProgressEvent) => void` | Function handling progress updates        | `() => { }`                         |
| `returnResponse` | `boolean`                        | Return the HTTP response body?            | `false`                             |
| `signal`         | `AbortSignal`                    | Request cancellation manager              | `null`                              | 

### `uploadParts(input: BlobMultipartUploadRequest)`

Required

| **Field**          | **Type**                           | **Description**           |
| ------------------ | ---------------------------------- | --------------------------|
| `parts`            | `{ [key: string]: BlobMultipart }` | The parts to be sent      |
| `url`              | `string`                           | Url to upload the blob to |

Optional

| **Field**        | **Type**                         | **Description**                           | **Default** |
| ---------------- | -------------------------------- | ----------------------------------------- | ----------- |
| `headers`        | `{ [key: string]: string }`      | Map of headers to send with the request   | `{}`        |
| `method`         | `string`                         | The HTTP method to be used in the request | `"POST"`    |
| `onProgress`     | `(e: BlobProgressEvent) => void` | Function handling progress updates        | `() => { }` |
| `returnResponse` | `boolean`                        | Return the HTTP response body?            | `false`     |
| `signal`         | `AbortSignal`                    | Request cancellation manager              | `null`      |

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

#### `AndroidSettings`

| **Field**            | **Type**                         | **Description**                         |
| -------------------- | -------------------------------- | --------------------------------------- |
| `downloadManager`    | `AndroidDownloadManagerSettings` | Settings to be used on download manager |
| `target`             | `"cache" \| "data"`              | Where will the file be stored?          |
| `useDownloadManager` | `boolean`                        | Enable download manager on Android?     |

#### `BlobManagedData`

| **Field**          | **Type**                 | **Description**                                     |
| ------------------ | ------------------------ | --------------------------------------------------- |
| `absoluteFilePath` | `string`                 | The absolute file path to where the file was stored |
| `result`           | `"SUCCESS" \| "FAILURE"` | Was the request successful or did it fail?          |

#### `BlobMultipart`

Required

| **Field**  | **Type**                                             | **Description**                  |
| ---------- | ---------------------------------------------------- | -------------------------------- |
| `payload`  | `BlobMultipartFormData \| BlobMultipartFormDataFile` | Contains the payload of the part |
| `type`     | `"string" \| "file"`                                 | What is the type of the payload? |

#### `BlobMultipartFormData`

Type of `string | { [key:string] : any }`

#### `BlobMultipartFormDataFile`

Required

| **Field**          | **Type** | **Description**                                          |
| ------------------ | -------- | -------------------------------------------------------- |
| `absoluteFilePath` | `string` | Path to the file to be uploaded                          |
| `mimeType`         | `string` | Mime type of the blob being transferred                  |

Optional

| **Field**        | **Type** | **Description**                           | **Default**                         |
| ---------------- | ---------| ----------------------------------------- | ----------------------------------- |
| `filename`       | `string` | Name of the file on disk                  | `<name part of 'absoluteFilePath'>` |

#### `BlobProgressEvent`

| **Field** | **Type** | **Description**                       |
| --------- | ---------| ------------------------------------- |
| `written` | `number` | Number of bytes processed             |
| `total`   | `number` | Total number of bytes to be processed |

#### `BlobUnmanagedData`

| **Field**          | **Type**                    | **Description**                                     |
| ------------------ | --------------------------- | --------------------------------------------------- |
| `absoluteFilePath` | `string`                    | The absolute file path to where the file was stored |
| `response`         | `BlobUnmanagedHttpResponse` | HTTP response, including headers and status code    |

#### `BlobUnmanagedHttpResponse`

| **Field** | **Type**                    | **Description**       |
| --------- | --------------------------- | --------------------- |
| `code`    | `number`                    | HTTP status code      |
| `headers` | `{ [key: string]: string }` | HTTP response headers |

#### `IOSSettings`

| **Field**            | **Type**                         | **Description**                         |
| -------------------- | -------------------------------- | --------------------------------------- |
| `target`             | `"cache" \| "data"`              | Where will the file be stored?          |

## Example app

You can find an example of how to use the library in the [example](example) directory.

## Android

### Permissions

### Android 5.1 and below (API level < 23)

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

To enable the download manager, simply set the request's `useDownloadManager` property of field `android` to `true` when passing it to `fetchBlob`, or call the `useDownloadManagerOnAndroid` method when using the fluent interface.

## Shared directories

As this library is focused on transferring files, it only supports storage to the app's _cache_ and _data_ directories. To move files from these app specific directories to other locations on the filesystem, use another library like [@react-native-community/cameraroll](https://github.com/react-native-community/react-native-cameraroll), e.g.:

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

## Code of Conduct

[Contributor Code of Conduct](CODE_OF_CONDUCT.md). By participating in this project you agree to abide by its terms.

## License

MPL-2.0
