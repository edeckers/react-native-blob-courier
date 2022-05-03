// import { NativeModules } from 'react-native';
import type {
  BlobFetchInput,
  BlobFetchResponse,
  BlobMultipartArrayUploadRequest,
  BlobRequestSettings,
  BlobRequestTask,
  BlobUploadResponse,
} from './ExposedTypes';
import NativeBlobCourier from './NativeBlobCourier';

// eslint-disable-next-line @typescript-eslint/no-unused-vars
// const { BlobCourier } = NativeModules;

export type BlobCancelNativeInput = BlobRequestTask;

export type BlobFetchNativeInput = BlobFetchInput & BlobRequestTask;

export type BlobUploadMultipartNativeInput = BlobMultipartArrayUploadRequest &
  BlobRequestSettings &
  BlobRequestTask;

export type BlobCourierType = {
  cancelRequest(input: BlobCancelNativeInput): Promise<{}>;
  fetchBlob(input: BlobFetchNativeInput): Promise<BlobFetchResponse>;
  uploadBlob(
    input: BlobUploadMultipartNativeInput
  ): Promise<BlobUploadResponse>;
};

export const createModule = () => {
  return NativeBlobCourier as any as BlobCourierType;
  // return BlobCourier as BlobCourierType;
};
