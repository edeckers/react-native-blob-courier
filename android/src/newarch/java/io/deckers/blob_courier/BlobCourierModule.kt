/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap

import io.deckers.blob_courier.common.LIBRARY_NAME

class BlobCourierModule(private val reactContext: ReactApplicationContext) :
  NativeBlobCourierSpec(reactContext) {

  private val blobCourier = BlobCourier(reactContext)

  companion object {
    const val NAME = LIBRARY_NAME
  }

  override fun getName(): String = LIBRARY_NAME

  override fun cancelRequest(input: ReadableMap, promise: Promise) {
    blobCourier.cancelRequest(input, promise)
  }

  override fun fetchBlob(input: ReadableMap, promise: Promise) {
    blobCourier.fetchBlob(input, promise)
  }

  override fun uploadBlob(input: ReadableMap, promise: Promise) {
    blobCourier.uploadBlob(input, promise)
  }
}
