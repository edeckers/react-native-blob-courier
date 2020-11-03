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
  BlobFetchResponse,
  BlobUploadRequest,
  BlobUploadResponse,
} from './Requests';
import { uuid } from './Utils';

type BlobCourierType = {
  fetchBlob(
    input: (AndroidBlobFetchRequest | BlobFetchRequest) & BlobRequestTask
  ): Promise<BlobFetchResponse>;
  uploadBlob(
    input: BlobUploadRequest & BlobRequestTask
  ): Promise<BlobUploadResponse>;
};

const { BlobCourier, BlobCourierEventEmitter } = NativeModules;

const EventEmitter = new NativeEventEmitter(BlobCourierEventEmitter);

const BLOB_COURIER_PROGRESS = 'BlobCourierProgress';
const SETTINGS_PREFIX = 'settings';

export interface BlobCourierProgress<T> extends Promise<T> {
  onProgress: (fn: (e: any) => void) => Promise<T>;
}

const extendWithProgress = <T extends unknown>(
  p: Promise<T>,
  taskId: String
): BlobCourierProgress<T> => ({
  [Symbol.toStringTag]: p[Symbol.toStringTag],
  catch: (onRejected) => p.catch(onRejected),
  finally: (onFinally) => p.finally(onFinally),
  onProgress: (fn: (e: any) => void) => {
    EventEmitter.addListener(BLOB_COURIER_PROGRESS, (e: any) => {
      if (e.taskId === taskId) {
        fn(e);
      }
    });

    return p;
  },
  then: (onFulfilled, onRejected) => p.then(onFulfilled, onRejected),
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
  private static prefixSettings = (settings: BlobRequestSettings) =>
    Object.keys(settings).reduce(
      (p, k) => ({
        ...p,
        [`${SETTINGS_PREFIX}.${k}`]: (settings as any)[k],
      }),
      {}
    ) as BlobRequestSettings;

  public static settings = (settings: BlobRequestSettings) => ({
    fetchBlob: (
      input: AndroidBlobFetchRequest | BlobFetchRequest
    ): BlobCourierProgress<BlobFetchResponse> =>
      BlobCourierWrapper.fetchBlob({
        ...BlobCourierWrapper.prefixSettings(settings),
        ...input,
      }),
    uploadBlob: (
      input: BlobUploadRequest
    ): BlobCourierProgress<BlobUploadResponse> =>
      BlobCourierWrapper.uploadBlob({
        ...BlobCourierWrapper.prefixSettings(settings),
        ...input,
      }),
  });

  public static fetchBlob = (
    input: AndroidBlobFetchRequest | BlobFetchRequest
  ): BlobCourierProgress<BlobFetchResponse> => {
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
  ): BlobCourierProgress<BlobUploadResponse> => {
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
