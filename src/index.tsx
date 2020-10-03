import { NativeModules } from 'react-native';

type BlobDownloaderType = {
  fetch_blob(url: string): Promise<boolean>;
};

const { BlobDownloader } = NativeModules;

export default BlobDownloader as BlobDownloaderType;
