import { NativeModules } from 'react-native';

type BlobDownloaderType = {
  fetch_blob(a: number, b: number): Promise<number>;
};

const { BlobDownloader } = NativeModules;

export default BlobDownloader as BlobDownloaderType;
