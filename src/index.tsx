import { NativeModules } from 'react-native';
import type { AndroidBlobRequest, BlobRequest } from './Requests';

type BlobDownloaderType = {
  fetch_blob(input: AndroidBlobRequest | BlobRequest): Promise<Response>;
};

const { BlobDownloader } = NativeModules;

export default BlobDownloader as BlobDownloaderType;
export * from './Paths';
export * from './Requests';
