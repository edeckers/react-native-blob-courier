/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
export const BLOB_FETCH_FALLBACK_PARAMETERS = {
  androidDownloadManager: {},
  headers: {},
  method: 'GET',
  useAndroidDownloadManager: false,
};

export const BLOB_UPLOAD_FALLBACK_PARAMETERS = {
  headers: {},
  method: 'POST',
  returnResponse: false,
};

export const BLOB_COURIER_PROGRESS_EVENT_NAME = 'BlobCourierProgress';
