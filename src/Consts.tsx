/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
import type { TargetType } from './ExposedTypes';

export const ANDROID_DOWNLOAD_MANAGER_FALLBACK_PARAMETERS = {
  description: undefined,
  enableNotifications: true,
  title: undefined,
};

export const DEFAULT_FETCH_TARGET: TargetType = 'cache';
export const DEFAULT_PROGRESS_UPDATE_INTERVAL_MILLISECONDS = 200;
export const DEFAULT_FILE_MULTIPART_FIELD_NAME = 'file';

export const BLOB_FETCH_FALLBACK_PARAMETERS = {
  android: {
    downloadManager: ANDROID_DOWNLOAD_MANAGER_FALLBACK_PARAMETERS,
    target: DEFAULT_FETCH_TARGET,
    useDownloadManager: false,
  },
  headers: {},
  ios: {
    target: DEFAULT_FETCH_TARGET,
  },
  method: 'GET',
  progressIntervalMilliseconds: DEFAULT_PROGRESS_UPDATE_INTERVAL_MILLISECONDS,
};

export const BLOB_UPLOAD_FALLBACK_PARAMETERS = {
  headers: {},
  method: 'POST',
  returnResponse: false,
};

export const BLOB_MULTIPART_UPLOAD_FALLBACK_PARAMETERS = {
  ...BLOB_UPLOAD_FALLBACK_PARAMETERS,
  parts: {},
};

export const BLOB_COURIER_PROGRESS_EVENT_NAME = 'BlobCourierProgress';
