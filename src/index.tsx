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
  BlobRequestTask,
  BlobResponse,
  BlobUploadRequest,
} from './Requests';
import { uuid } from './Utils';

type BlobCourierType = {
  fetchBlob(
    input: (AndroidBlobRequest | BlobRequest) & BlobRequestTask
  ): Promise<BlobResponse>;
  uploadBlob(input: BlobUploadRequest & BlobRequestTask): Promise<BlobResponse>;
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

const createTaskId = () => `task-${uuid()}`;

const extendInputWithTaskId = (
  input: AndroidBlobRequest | BlobRequest | BlobUploadRequest
) =>
  ({
    ...input,
    taskId: createTaskId(),
  } as (AndroidBlobRequest | BlobRequest | BlobUploadRequest) &
    BlobRequestTask);

class BlobCourierWrapper {
  public static fetchBlob = (
    input: AndroidBlobRequest | BlobRequest
  ): BlobCourierProgress<BlobResponse> =>
    extendWithProgress(
      (BlobCourier as BlobCourierType).fetchBlob(
        extendInputWithTaskId(input) as (AndroidBlobRequest | BlobRequest) &
          BlobRequestTask
      )
    );

  public static uploadBlob = (
    input: BlobUploadRequest
  ): BlobCourierProgress<BlobResponse> =>
    extendWithProgress(
      (BlobCourier as BlobCourierType).uploadBlob(
        extendInputWithTaskId(input) as BlobUploadRequest & BlobRequestTask
      )
    );
}

export default BlobCourierWrapper;

export * from './Requests';
