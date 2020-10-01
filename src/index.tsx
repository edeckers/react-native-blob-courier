import { NativeModules } from 'react-native';

type BlobDownloaderType = {
  multiply(a: number, b: number): Promise<number>;
};

const { BlobDownloader } = NativeModules;

export default BlobDownloader as BlobDownloaderType;
