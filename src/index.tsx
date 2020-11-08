/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
import { NativeEventEmitter, NativeModules } from 'react-native';
import type {
  BlobFetchRequest,
  BlobRequestSettings,
  BlobRequestTask,
  BlobFetchResponse,
  BlobUploadRequest,
  BlobUploadResponse,
  AndroidDownloadManagerSettings,
} from './Requests';
import { uuid } from './Utils';

type BlobCourierType = {
  fetchBlob<T extends BlobFetchRequest & BlobRequestTask>(
    input: T
  ): Promise<BlobFetchResponse>;
  uploadBlob<T extends BlobUploadRequest & BlobRequestTask>(
    input: T
  ): Promise<BlobUploadResponse>;
};

const { BlobCourier, BlobCourierEventEmitter } = NativeModules;

const EventEmitter = new NativeEventEmitter(BlobCourierEventEmitter);

const BLOB_COURIER_PROGRESS = 'BlobCourierProgress';
const SETTINGS_PREFIX = 'settings';

const createTaskId = () => `rnbc-req-${uuid()}`;

const prefixDict = <T extends { [key: string]: any }>(
  dict: T,
  prefix: string
) =>
  Object.keys(dict).reduce(
    (p, k) => ({
      ...p,
      [`${prefix}.${k}`]: (dict as any)[k],
    }),
    {}
  ) as T;

const prefixSettings = (settings: BlobRequestSettings) =>
  prefixDict(settings, SETTINGS_PREFIX);

const fetchBlob = (BlobCourier as BlobCourierType).fetchBlob;

const uploadBlob = (BlobCourier as BlobCourierType).uploadBlob;

const onProgress = (taskId: string, fn: (e: any) => void) => {
  EventEmitter.addListener(BLOB_COURIER_PROGRESS, (e: any) => {
    if (e.taskId === taskId) {
      fn(e);
    }
  });

  return {
    fetchBlob: (input: BlobFetchRequest) =>
      fetchBlob({
        ...input,
        taskId,
      }),
    uploadBlob: (input: BlobUploadRequest) =>
      uploadBlob({
        ...input,
        taskId,
      }),
  };
};

const onProgressFluent = (taskId: string, fn: (e: any) => void) => {
  const result = onProgress(taskId, fn);

  return {
    ...result,
    useDownloadManagerOnAndroid: (
      downloadManagerSettings?: AndroidDownloadManagerSettings
    ) => useDownloadManagerOnAndroid(createTaskId(), downloadManagerSettings),
  };
};

const useDownloadManagerOnAndroid = (
  taskId: string,
  downloadManagerSettings?: AndroidDownloadManagerSettings
) => ({
  fetchBlob: (input: BlobFetchRequest) =>
    fetchBlob({
      ...input,
      ...downloadManagerSettings,
      useDownloadManager: true,
      taskId,
    }),
});

const settings = (requestSettings: BlobRequestSettings) => {
  const applySettings = <T,>(input: T) =>
    ({
      ...input,
      ...prefixSettings(requestSettings),
    } as T & BlobRequestSettings);

  const taskId = createTaskId();

  return {
    fetchBlob: (input: BlobFetchRequest) =>
      fetchBlob({
        ...applySettings(input),
        taskId,
      }),
    onProgress: (fn: (e: any) => void) => onProgressFluent(taskId, fn),
    uploadBlob: (input: BlobUploadRequest) =>
      uploadBlob({
        ...applySettings(input),
        taskId,
      }),
    useDownloadManagerOnAndroid: (
      downloadManagerSettings: AndroidDownloadManagerSettings
    ) => useDownloadManagerOnAndroid(taskId, downloadManagerSettings),
  };
};

export default {
  fetchBlob,
  fluent: () => ({
    settings,
    fetchBlob,
    uploadBlob,
    onProgress: (fn: (e: any) => void) => onProgressFluent(createTaskId(), fn),
    useDownloadManagerOnAndroid: (
      downloadManagerSettings: AndroidDownloadManagerSettings
    ) => useDownloadManagerOnAndroid(createTaskId(), downloadManagerSettings),
  }),
  onProgress: (fn: (e: any) => void) => onProgress(createTaskId(), fn),
  uploadBlob,
};

export * from './Requests';
