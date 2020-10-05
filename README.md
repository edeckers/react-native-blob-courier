# react-native-blob-courier

[![License: MPL 2.0](https://img.shields.io/badge/License-MPL%202.0-brightgreen.svg)](https://opensource.org/licenses/MPL-2.0)

Use this library to efficiently download and upload blobs in React Native. The library was inspired by [rn-fetch-blob](joltup/rn-fetch-blob), and aims to focus strictly on blob transfers.

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

```js
import BlobCourier from "react-native-blob-courier";

// ...

const result = await BlobCourier.fetchBlob("https://url.to/binary.bin");
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

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MPL-2.0
