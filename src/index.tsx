/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
import { NativeEventEmitter, NativeModules } from 'react-native';
import type {
  AndroidBlobFetchRequest,
  BlobFetchRequest,
  BlobRequestSettings,
  BlobRequestTask,
  BlobResponse,
  BlobUploadRequest,
} from './Requests';
import { uuid } from './Utils';

type BlobCourierType = {
  fetchBlob(
    input: (AndroidBlobFetchRequest | BlobFetchRequest) & BlobRequestTask
  ): Promise<BlobResponse>;
  uploadBlob(input: BlobUploadRequest & BlobRequestTask): Promise<BlobResponse>;
};

const { BlobCourier, BlobCourierEventEmitter } = NativeModules;

const EventEmitter = new NativeEventEmitter(BlobCourierEventEmitter);

const BLOB_COURIER_PROGRESS = 'BlobCourierProgress';

export interface BlobCourierProgress<T> extends Promise<T> {
  onProgress: (fn: (e: any) => void) => Promise<T>;
}

const extendWithProgress = <T extends unknown>(
  p: Promise<T>,
  taskId: String
): BlobCourierProgress<T> => ({
  ...p,
  onProgress: (fn: (e: any) => void) => {
    EventEmitter.addListener(BLOB_COURIER_PROGRESS, (e: any) => {
      if (e.taskId === taskId) {
        fn(e);
      }
    });

    return p;
  },
});

const createTaskId = () => `task-${uuid()}`;

const extendInputWithTaskId = (
  input: AndroidBlobFetchRequest | BlobFetchRequest | BlobUploadRequest
) =>
  ({
    ...input,
    taskId: createTaskId(),
  } as (AndroidBlobFetchRequest | BlobFetchRequest | BlobUploadRequest) &
    BlobRequestTask);

class BlobCourierWrapper {
  public static settings = (settings: BlobRequestSettings) => ({
    fetchBlob: (
      input: AndroidBlobFetchRequest | BlobFetchRequest
    ): BlobCourierProgress<BlobResponse> =>
      BlobCourierWrapper.fetchBlob({ ...settings, ...input }),
    uploadBlob: (input: BlobUploadRequest): BlobCourierProgress<BlobResponse> =>
      BlobCourierWrapper.uploadBlob({ ...settings, ...input }),
  });

  public static fetchBlob = (
    input: AndroidBlobFetchRequest | BlobFetchRequest
  ): BlobCourierProgress<BlobResponse> => {
    const inputWithTaskId = extendInputWithTaskId(input) as (
      | AndroidBlobFetchRequest
      | BlobFetchRequest
    ) &
      BlobRequestTask;

    return extendWithProgress(
      (BlobCourier as BlobCourierType).fetchBlob(inputWithTaskId),
      inputWithTaskId.taskId
    );
  };

  public static uploadBlob = (
    input: BlobUploadRequest
  ): BlobCourierProgress<BlobResponse> => {
    const inputWithTaskId = extendInputWithTaskId(input) as BlobUploadRequest &
      BlobRequestTask;

    return extendWithProgress(
      (BlobCourier as BlobCourierType).uploadBlob(inputWithTaskId),
      inputWithTaskId.taskId
    );
  };
}

export default BlobCourierWrapper;

export * from './Requests';
