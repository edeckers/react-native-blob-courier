import { NativeModules } from 'react-native';

const buildPathEnum = (items: string[], initial = {}) => {
  console.debug(
    `Build PathEnum type from ${items}, merged with ${JSON.stringify(initial)}`
  );

  const paths = items.reduce(
    (p, c) => ({
      ...p,
      [c]: `enum://${c}`,
    }),
    initial
  );

  console.debug('Built PathEnum type:', JSON.stringify(paths));

  return paths;
};

const AndroidPathType = {
  DCIM: '',
  Download: '',
  Movie: '',
  Music: '',
  Picture: '',
  Ringtone: '',
  SDCard: '',
};

const CommonPathType = {
  Cache: '',
  Documents: '',
};

const IOSPathType = {
  MainBundle: '',
};

export type AndroidPathType = typeof AndroidPathType;
export type CommonPathType = typeof CommonPathType;
export type IOSPathType = typeof IOSPathType;

export const CommonPath = buildPathEnum(
  Object.keys(CommonPathType)
) as CommonPathType;

export const IOSPath = buildPathEnum(
  Object.keys(IOSPathType),
  CommonPath
) as IOSPathType;

export const AndroidPath = buildPathEnum(
  Object.keys(AndroidPathType),
  CommonPath
) as AndroidPathType;

export declare interface BlobRequest {
  readonly filename: string;
  readonly headers?: Headers;
  readonly method?: string;
  readonly url: string;
  readonly target: string;
}

export declare interface AndroidBlobRequest extends BlobRequest {
  readonly useDownloadManager: boolean;
}

type BlobDownloaderType = {
  fetch_blob(input: AndroidBlobRequest | BlobRequest): Promise<Response>;
};

const { BlobDownloader } = NativeModules;

export default BlobDownloader as BlobDownloaderType;
