/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
import { NativeEventEmitter, NativeModules } from 'react-native';
import {
  BLOB_COURIER_PROGRESS_EVENT_NAME,
  BLOB_FETCH_FALLBACK_PARAMETERS,
  BLOB_MULTIPART_UPLOAD_FALLBACK_PARAMETERS,
  DEFAULT_FILE_MULTIPART_FIELD_NAME,
} from './Consts';
import './Extensions';
import {
  BlobFetchRequest,
  BlobRequestSettings,
  BlobRequestTask,
  BlobFetchResponse,
  BlobUploadRequest,
  BlobUploadResponse,
  AndroidDownloadManagerSettings,
  BlobProgressEvent,
  BlobRequestOnProgress,
  BlobMultipartUploadRequest as BlobUploadMultipartRequest,
  AndroidFetchSettings,
  AndroidRootDirectory,
  AndroidFetchTarget,
} from './ExposedTypes';
import { uuid } from './Utils';
import { dict } from './Extensions';

type BlobFetchInput = BlobFetchRequest &
  BlobRequestSettings &
  AndroidFetchSettings;

type BlobFetchNativeInput = BlobFetchInput & BlobRequestTask;

type BlobUploadInput = BlobUploadRequest & BlobRequestSettings;

type BlobUploadNativeInput = BlobUploadInput & BlobRequestTask;

type BlobUploadMultipartInput = BlobUploadMultipartRequest &
  BlobRequestSettings;

type BlobUploadMultipartNativeInput = BlobUploadMultipartInput &
  BlobRequestTask;

type BlobCourierType = {
  fetchBlob(input: BlobFetchNativeInput): Promise<BlobFetchResponse>;
  uploadBlob(
    input: BlobUploadMultipartNativeInput
  ): Promise<BlobUploadResponse>;
};

const { BlobCourier, BlobCourierEventEmitter } = NativeModules;

const EventEmitter = new NativeEventEmitter(BlobCourierEventEmitter);

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

const sanitizeSettingsData = <T extends BlobRequestSettings>(
  input: Readonly<T>
) => {
  const { progressIntervalMilliseconds } = input;

  return {
    progressIntervalMilliseconds,
  };
};

const sanitizeAndroidFetchSettings = (data: any) => ({
  ...data,
  target: {
    ...data?.target,
    rootDirectory: AndroidRootDirectory[data?.target?.rootDirectory || 0],
  },
});

const sanitizeFetchData = <T extends BlobFetchNativeInput>(
  input: Readonly<T>
): BlobFetchNativeInput => {
  const { android, filename, headers, method, mimeType, taskId, url } = input;

  const settings = sanitizeSettingsData(input);

  const request = {
    filename,
    mimeType,
    url,
  };

  const optionalRequestParameters = dict({
    ...settings,
    android: sanitizeAndroidFetchSettings(android),
    headers,
    method,
  }).fallback(BLOB_FETCH_FALLBACK_PARAMETERS);

  return {
    ...optionalRequestParameters,
    ...request,
    taskId,
  };
};

const stringifyPartsValues = (parts: { [key: string]: any }) => {
  const stringify = (part: { [key: string]: any }) =>
    part.type === 'string' && typeof part.payload === 'object'
      ? { ...part, payload: JSON.stringify(part.payload) }
      : part;

  return Object.keys(parts).reduce(
    (p, c) => ({
      ...p,
      [c]: stringify(parts[c]),
    }),
    {}
  );
};

const sanitizeMultipartUploadData = <T extends BlobUploadMultipartNativeInput>(
  input: Readonly<T>
): BlobUploadMultipartNativeInput => {
  const { parts, headers, method, returnResponse, url } = input;

  const { taskId } = input;

  const settings = sanitizeSettingsData(input);

  const request = {
    mimeType: 'multipart/form-data',
    parts: stringifyPartsValues(parts),
    url,
  };

  const optionalRequestParameters = dict({
    headers,
    method,
    returnResponse,
  }).fallback(BLOB_MULTIPART_UPLOAD_FALLBACK_PARAMETERS);

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

const uploadParts = <T extends BlobUploadMultipartNativeInput>(
  input: Readonly<T>
) =>
  wrapEmitter(
    input.taskId,
    () =>
      (BlobCourier as BlobCourierType).uploadBlob(
        sanitizeMultipartUploadData(input)
      ),
    input.onProgress
  );

const uploadBlob = <T extends BlobUploadNativeInput>(input: Readonly<T>) => {
  const { absoluteFilePath, filename, mimeType, multipartName } = input;

  return uploadParts({
    ...input,
    parts: {
      [multipartName ?? DEFAULT_FILE_MULTIPART_FIELD_NAME]: {
        payload: {
          absoluteFilePath,
          filename,
          mimeType,
        },
        type: 'file',
      },
    },
  });
};

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
  uploadParts: (input: BlobUploadMultipartRequest) =>
    uploadParts({
      ...input,
      ...requestSettings,
      onProgress: fn,
      taskId,
    }),
  useDownloadManagerOnAndroid: (
    downloadManagerSettings?: AndroidDownloadManagerSettings
  ) =>
    useDownloadManagerOnAndroid(taskId, downloadManagerSettings, {
      ...requestSettings,
      onProgress: fn,
    }),
});

const useFetchTarget = (
  taskId: string,
  target: {
    android?: AndroidFetchTarget;
  },
  input0: any
) => ({
  useDownloadManagerOnAndroid: (
    downloadManagerSettings?: AndroidDownloadManagerSettings
  ) => useDownloadManagerOnAndroid(taskId, downloadManagerSettings, input0),
  fetchBlob: (input: BlobFetchRequest) =>
    fetchBlob({
      ...input,
      ...input0,
      target,
      taskId,
    }),
});

const useDownloadManagerOnAndroid = (
  taskId: string,
  downloadManagerSettings?: AndroidDownloadManagerSettings,
  requestSettings?: BlobRequestSettings & BlobRequestOnProgress
) => ({
  fetchBlob: (input: BlobFetchRequest) =>
    fetchBlob({
      ...input,
      ...requestSettings,
      android: {
        downloadManager: downloadManagerSettings,
        useDownloadManager: true,
      },
      taskId,
    }),
});

const settings = (taskId: string, requestSettings: BlobRequestSettings) => ({
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
  uploadParts: (input: BlobUploadMultipartInput) =>
    uploadParts({
      ...input,
      ...requestSettings,
      taskId,
    }),
  useDownloadManagerOnAndroid: (
    downloadManagerSettings?: AndroidDownloadManagerSettings
  ) =>
    useDownloadManagerOnAndroid(
      taskId,
      downloadManagerSettings,
      requestSettings
    ),
  useFetchTarget: (x: { android?: AndroidFetchTarget }) =>
    useFetchTarget(taskId, x, requestSettings),
});

export default {
  fetchBlob: (input: BlobFetchInput) =>
    fetchBlob({ ...input, taskId: createTaskId() }),
  onProgress: (fn: (e: BlobProgressEvent) => void) =>
    onProgress(createTaskId(), fn),
  settings: (input: BlobRequestSettings) => settings(createTaskId(), input),
  uploadBlob: (input: BlobUploadInput) =>
    uploadBlob({ ...input, taskId: createTaskId() }),
  uploadParts: (input: BlobUploadMultipartInput) =>
    uploadParts({ ...input, taskId: createTaskId() }),
  useDownloadManagerOnAndroid: (
    downloadManagerSettings: AndroidDownloadManagerSettings
  ) => useDownloadManagerOnAndroid(createTaskId(), downloadManagerSettings),
};

export * from './ExposedTypes';
