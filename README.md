# react-native-blob-downloader

Download blobs

## Installation

```sh
npm install react-native-blob-downloader
```

## Usage

```js
import BlobDownloader from "react-native-blob-downloader";

// ...

const result = await BlobDownloader.fetch_blob("https://url.to/binary.bin");
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

MIT
