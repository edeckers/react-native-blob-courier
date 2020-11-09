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
  AndroidDownloadManagerToggle,
  AndroidDownloadManager,
  BlobProgressEvent,
} from './ExposedTypes';
import { createTaskId, prefixDict } from './Utils';

type BlobFetchInput = BlobFetchRequest &
  BlobRequestTask &
  BlobRequestSettings &
  AndroidDownloadManagerToggle &
  AndroidDownloadManager;

type BlobUploadInput = BlobUploadRequest &
  BlobRequestTask &
  BlobRequestSettings;

type BlobCourierType = {
  fetchBlob(input: BlobFetchInput): Promise<BlobFetchResponse>;
  uploadBlob(input: BlobUploadInput): Promise<BlobUploadResponse>;
};

const { BlobCourier, BlobCourierEventEmitter } = NativeModules;

const EventEmitter = new NativeEventEmitter(BlobCourierEventEmitter);

const BLOB_COURIER_PROGRESS = 'BlobCourierProgress';
const SETTINGS_PREFIX = 'settings';

const addProgressListener = (
  taskId: string,
  fn: (e: BlobProgressEvent) => void
) =>
  EventEmitter.addListener(BLOB_COURIER_PROGRESS, (e: any) => {
    const parsedEvent: BlobProgressEvent = {
      written: parseInt(e.written, 10),
      total: parseInt(e.total, 10),
    };

    if (e.taskId === taskId) {
      fn(parsedEvent);
    }
  });

const prefixSettings = (settings: BlobRequestSettings) =>
  prefixDict(settings, SETTINGS_PREFIX);

const sanitizeSettingsData = <T extends BlobFetchInput | BlobUploadInput>(
  input: Readonly<T>
) => {
  const { progressIntervalMilliseconds } = input;

  return prefixSettings({
    progressIntervalMilliseconds,
  });
};

const sanitizeFetchData = <T extends BlobFetchInput>(
  input: Readonly<T>
): BlobFetchInput => {
  const { filename, headers, method, mimeType, url } = input;

  const { taskId } = input;

  const { useAndroidDownloadManager, androidDownloadManager } = input;

  const settings = sanitizeSettingsData(input);

  const request = {
    filename,
    headers,
    method,
    mimeType,
    url,
  };

  const androidDownloadManagerSettings = {
    useAndroidDownloadManager,
    androidDownloadManager,
  };

  return {
    ...settings,
    ...androidDownloadManagerSettings,
    ...request,
    taskId,
  };
};

const sanitizeUploadData = <T extends BlobUploadInput>(
  input: Readonly<T>
): BlobUploadInput => {
  const { filePath, headers, method, mimeType, returnResponse, url } = input;

  const { taskId } = input;

  const settings = sanitizeSettingsData(input);

  const request = {
    filePath,
    headers,
    method,
    mimeType,
    returnResponse,
    url,
  };

  return {
    ...settings,
    ...request,
    taskId,
  };
};

const wrapEmitter = async <T,>(
  taskId: string,
  wrappedFn: () => Promise<T>,
  fnOnProgress?: (e: BlobProgressEvent) => void
) => {
  const emitterSubscription = fnOnProgress
    ? addProgressListener(taskId, fnOnProgress)
    : undefined;

  const result = await wrappedFn();

  emitterSubscription?.remove();

  return result;
};

const fetchBlob = <T extends BlobFetchInput>(input: Readonly<T>) =>
  wrapEmitter(
    input.taskId,
    () => (BlobCourier as BlobCourierType).fetchBlob(sanitizeFetchData(input)),
    input.onProgress
  );

const uploadBlob = <T extends BlobUploadInput>(input: Readonly<T>) =>
  wrapEmitter(
    input.taskId,
    () =>
      (BlobCourier as BlobCourierType).uploadBlob(sanitizeUploadData(input)),
    input.onProgress
  );

const onProgress = (
  taskId: string,
  fn: (e: BlobProgressEvent) => void,
  requestSettings?: BlobRequestSettings
) => ({
  fetchBlob: (input: BlobFetchRequest) =>
    fetchBlob({
      ...input,
      ...requestSettings,
      onProgress: fn,
      taskId,
    }),
  uploadBlob: (input: BlobUploadRequest) =>
    uploadBlob({
      ...input,
      ...requestSettings,
      onProgress: fn,
      taskId,
    }),
  useDownloadManagerOnAndroid: (
    downloadManagerSettings?: AndroidDownloadManagerSettings
  ) =>
    useDownloadManagerOnAndroid(
      createTaskId(),
      downloadManagerSettings,
      requestSettings
    ),
});

const useDownloadManagerOnAndroid = (
  taskId: string,
  downloadManagerSettings?: AndroidDownloadManagerSettings,
  requestSettings?: BlobRequestSettings
) => ({
  fetchBlob: (input: BlobFetchRequest) =>
    fetchBlob({
      ...input,
      ...requestSettings,
      useAndroidDownloadManager: true,
      androidDownloadManager: downloadManagerSettings,
      taskId,
    }),
});

const settings = (requestSettings: BlobRequestSettings) => {
  const taskId = createTaskId();

  return {
    fetchBlob: (input: BlobFetchRequest) =>
      fetchBlob({
        ...input,
        ...requestSettings,
        taskId,
      }),
    onProgress: (fn: (e: BlobProgressEvent) => void) =>
      onProgress(taskId, fn, requestSettings),
    uploadBlob: (input: BlobUploadRequest) =>
      uploadBlob({
        ...input,
        ...requestSettings,
        taskId,
      }),
    useDownloadManagerOnAndroid: (
      downloadManagerSettings: AndroidDownloadManagerSettings
    ) =>
      useDownloadManagerOnAndroid(
        taskId,
        downloadManagerSettings,
        requestSettings
      ),
  };
};

export default {
  fetchBlob,
  onProgress: (fn: (e: BlobProgressEvent) => void) =>
    onProgress(createTaskId(), fn),
  settings,
  uploadBlob,
  useDownloadManagerOnAndroid: (
    downloadManagerSettings: AndroidDownloadManagerSettings
  ) => useDownloadManagerOnAndroid(createTaskId(), downloadManagerSettings),
};

export * from './ExposedTypes';
