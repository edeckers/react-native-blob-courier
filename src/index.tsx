/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
import { NativeEventEmitter, NativeModules } from 'react-native';
import {
  BLOB_FETCH_FALLBACK_PARAMETERS,
  BLOB_UPLOAD_FALLBACK_PARAMETERS,
} from './Consts';
import './Extensions';
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
import { uuid } from './Utils';
import { dict } from './Extensions';

type BlobFetchInput = BlobFetchRequest &
  BlobRequestSettings &
  AndroidDownloadManagerToggle &
  AndroidDownloadManager;

type BlobFetchNativeInput = BlobFetchInput & BlobRequestTask;

type BlobUploadInput = BlobUploadRequest & BlobRequestSettings;

type BlobUploadNativeInput = BlobUploadInput & BlobRequestTask;

type BlobCourierType = {
  fetchBlob(input: BlobFetchNativeInput): Promise<BlobFetchResponse>;
  uploadBlob(input: BlobUploadInput): Promise<BlobUploadResponse>;
};

const { BlobCourier, BlobCourierEventEmitter } = NativeModules;

const EventEmitter = new NativeEventEmitter(BlobCourierEventEmitter);

const BLOB_COURIER_PROGRESS_EVENT_NAME = 'BlobCourierProgress';

export const createTaskId = () => `rnbc-req-${uuid()}`;

const addProgressListener = (
  taskId: string,
  fn: (e: BlobProgressEvent) => void
) =>
  EventEmitter.addListener(BLOB_COURIER_PROGRESS_EVENT_NAME, (e: any) => {
    const parsedEvent: BlobProgressEvent = {
      written: parseInt(e.written, 10),
      total: parseInt(e.total, 10),
    };

    if (e.taskId === taskId) {
      fn(parsedEvent);
    }
  });

const sanitizeSettingsData = <T extends BlobFetchNativeInput | BlobUploadInput>(
  input: Readonly<T>
) => {
  const { progressIntervalMilliseconds } = input;

  return {
    progressIntervalMilliseconds,
  };
};

const sanitizeFetchData = <T extends BlobFetchNativeInput>(
  input: Readonly<T>
): BlobFetchNativeInput => {
  const { filename, headers, method, mimeType, url } = input;

  const { taskId } = input;

  const { useAndroidDownloadManager, androidDownloadManager } = input;

  const settings = sanitizeSettingsData(input);

  const request = {
    filename,
    mimeType,
    url,
  };

  const optionalRequestParameters = dict({
    headers,
    method,
  }).intersect(BLOB_FETCH_FALLBACK_PARAMETERS);

  const androidDownloadManagerSettings = dict({
    useAndroidDownloadManager,
    androidDownloadManager,
  }).intersect(BLOB_FETCH_FALLBACK_PARAMETERS);

  return {
    ...settings,
    ...androidDownloadManagerSettings,
    ...optionalRequestParameters,
    ...request,
    taskId,
  };
};

const sanitizeUploadData = <T extends BlobUploadNativeInput>(
  input: Readonly<T>
): BlobUploadNativeInput => {
  const { filePath, headers, method, mimeType, returnResponse, url } = input;

  const { taskId } = input;

  const settings = sanitizeSettingsData(input);

  const request = {
    filePath,
    mimeType,
    url,
  };

  const optionalRequestParameters = dict({
    headers,
    method,
    returnResponse,
  }).intersect(BLOB_UPLOAD_FALLBACK_PARAMETERS);

  return {
    ...settings,
    ...optionalRequestParameters,
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

const fetchBlob = <T extends BlobFetchNativeInput>(input: Readonly<T>) =>
  wrapEmitter(
    input.taskId,
    () => (BlobCourier as BlobCourierType).fetchBlob(sanitizeFetchData(input)),
    input.onProgress
  );

const uploadBlob = <T extends BlobUploadNativeInput>(input: Readonly<T>) =>
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
  fetchBlob: (input: BlobFetchInput) =>
    fetchBlob({ ...input, taskId: createTaskId() }),
  onProgress: (fn: (e: BlobProgressEvent) => void) =>
    onProgress(createTaskId(), fn),
  settings,
  uploadBlob: (input: BlobUploadInput) =>
    uploadBlob({ ...input, taskId: createTaskId() }),
  useDownloadManagerOnAndroid: (
    downloadManagerSettings: AndroidDownloadManagerSettings
  ) => useDownloadManagerOnAndroid(createTaskId(), downloadManagerSettings),
};

export * from './ExposedTypes';
