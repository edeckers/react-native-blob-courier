/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.module.annotations.ReactModule
import io.deckers.blob_courier.common.LIBRARY_NAME

@ReactModule(name = LIBRARY_NAME)
class BlobCourierModule(private val reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  private val blobCourier = BlobCourier(reactContext)

  override fun getName(): String = LIBRARY_NAME

  @ReactMethod
  fun cancelRequest(input: ReadableMap, promise: Promise) {
    blobCourier.cancelRequest(input, promise)
  }

  @ReactMethod
  fun fetchBlob(input: ReadableMap, promise: Promise) {
    blobCourier.fetchBlob(input, promise)
  }

  @ReactMethod
  fun uploadBlob(input: ReadableMap, promise: Promise) {
    blobCourier.uploadBlob(input, promise)
  }
}
