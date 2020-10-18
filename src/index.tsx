/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
import { DeviceEventEmitter, NativeModules } from 'react-native';
import type {
  AndroidBlobRequest,
  BlobRequest,
  BlobResponse,
  BlobUploadRequest,
} from './Requests';

type BlobCourierType = {
  fetchBlob(input: AndroidBlobRequest | BlobRequest): Promise<BlobResponse>;
  uploadBlob(input: BlobUploadRequest): Promise<BlobResponse>;
};

const { BlobCourier } = NativeModules;

const DEVICE_EVENT_PROGRESS = 'BlobCourierProgress';

export interface BlobCourierProgress<T> extends Promise<T> {
  onProgress: (fn: (e: any) => void) => Promise<T>;
}

const extendWithProgress = <T extends unknown>(
  p: Promise<T>
): BlobCourierProgress<T> => ({
  ...p,
  onProgress: (fn: (e: any) => void) => {
    DeviceEventEmitter.addListener(DEVICE_EVENT_PROGRESS, fn);

    return p;
  },
});

class BlobCourierWrapper {
  public static fetchBlob = (
    input: AndroidBlobRequest | BlobRequest
  ): BlobCourierProgress<BlobResponse> =>
    extendWithProgress((BlobCourier as BlobCourierType).fetchBlob(input));

  public static uploadBlob = (
    input: BlobUploadRequest
  ): BlobCourierProgress<BlobResponse> =>
    extendWithProgress((BlobCourier as BlobCourierType).uploadBlob(input));
}

export default BlobCourierWrapper;

export * from './Requests';
