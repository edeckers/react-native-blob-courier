import { NativeModules } from 'react-native';
import type {
  BlobFetchInput,
  BlobFetchResponse,
  BlobMultipartArrayUploadRequest,
  BlobRequestSettings,
  BlobRequestTask,
  BlobUploadResponse,
} from './ExposedTypes';

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
  const isTurboModuleEnabled = (global as any).__turboModuleProxy != null;

  if (isTurboModuleEnabled) {
    const NativeBlobCourier = require('./NativeBlobCourier').default;

    return NativeBlobCourier as any as BlobCourierType;
  }

  return NativeModules.BlobCourier;
};
