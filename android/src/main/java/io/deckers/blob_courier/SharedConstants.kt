/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier

const val ERROR_INVALID_VALUE = "ERROR_INVALID_VALUE"
const val ERROR_UNEXPECTED_EXCEPTION = "ERROR_UNEXPECTED_EXCEPTION"
const val ERROR_UNEXPECTED_ERROR = "ERROR_UNEXPECTED_ERROR"
const val ERROR_UNEXPECTED_EMPTY_VALUE = "ERROR_UNEXPECTED_EMPTY_VALUE"

const val DEFAULT_FETCH_METHOD = "GET"
const val DEFAULT_UPLOAD_METHOD = "POST"

const val DEFAULT_MIME_TYPE = "text/plain"

const val LIBRARY_NAME = "BlobCourier"

const val DOWNLOAD_TYPE_MANAGED = "Managed"
const val DOWNLOAD_TYPE_UNMANAGED = "Unmanaged"

const val MANAGED_DOWNLOAD_SUCCESS = "SUCCESS"
const val MANAGED_DOWNLOAD_FAILURE = "FAILURE"

const val DEVICE_EVENT_PROGRESS = "BlobCourierProgress"

const val DEFAULT_PROGRESS_TIMEOUT_MILLISECONDS = 500
