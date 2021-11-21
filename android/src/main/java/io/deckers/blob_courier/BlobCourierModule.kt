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
import com.facebook.react.modules.network.OkHttpClientProvider
import io.deckers.blob_courier.cancel.CancellationParameterFactory
import io.deckers.blob_courier.cancel.RequestCanceller
import io.deckers.blob_courier.common.DEFAULT_PROGRESS_TIMEOUT_MILLISECONDS
import io.deckers.blob_courier.common.ERROR_UNEXPECTED_ERROR
import io.deckers.blob_courier.common.ERROR_UNEXPECTED_EXCEPTION
import io.deckers.blob_courier.common.ERROR_UNKNOWN_HOST
import io.deckers.blob_courier.common.Failure
import io.deckers.blob_courier.common.LIBRARY_NAME
import io.deckers.blob_courier.common.Logger
import io.deckers.blob_courier.common.Success
import io.deckers.blob_courier.common.`do`
import io.deckers.blob_courier.common.fold
import io.deckers.blob_courier.fetch.BlobDownloader
import io.deckers.blob_courier.fetch.DownloaderParameterFactory
import io.deckers.blob_courier.react.CongestionAvoidingProgressNotifierFactory
import io.deckers.blob_courier.react.processUnexpectedError
import io.deckers.blob_courier.react.processUnexpectedException
import io.deckers.blob_courier.react.toReactMap
import io.deckers.blob_courier.upload.BlobUploader
import io.deckers.blob_courier.upload.UploaderParameterFactory
import java.net.UnknownHostException
import kotlin.concurrent.thread

private fun createHttpClient() = OkHttpClientProvider.getOkHttpClient()
private fun createProgressFactory(
  reactContext: ReactApplicationContext,
  progressTimeoutMilliseconds: Int = DEFAULT_PROGRESS_TIMEOUT_MILLISECONDS
) =
  CongestionAvoidingProgressNotifierFactory(
    reactContext,
    progressTimeoutMilliseconds
  )

private val TAG = BlobCourierModule::class.java.name
private val logger = Logger(TAG)
private fun le(m: String, e: Throwable? = null) = logger.e(m, e)
private fun li(m: String) = logger.i(m)
private fun lv(m: String, e: Throwable? = null) = logger.v(m, e)

@ReactModule(name = LIBRARY_NAME)
class BlobCourierModule(private val reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  override fun getName(): String = LIBRARY_NAME

  @ReactMethod
  fun cancelRequest(input: ReadableMap, promise: Promise) {
    li("Calling cancelRequest")

    thread {
      try {
        val errorOrCancelResult =
          CancellationParameterFactory()
            .fromInput(input)
            .fold(::Failure, ::Success)
            .map { RequestCanceller(reactContext).cancel(it.taskId) }

        errorOrCancelResult
          .fmap { Success(emptyMap<String, Any>().toReactMap()) }
          .`do`(
            { e ->
              lv("Something went wrong during cancellation (code=${e.code},message=${e.message})")
              promise.reject(e.code, e.message)
            },
            promise::resolve
          )
      } catch (e: Exception) {
        le("Unexpected exception", e)
        promise.reject(ERROR_UNEXPECTED_EXCEPTION, processUnexpectedException(e).message)
      } catch (e: Error) {
        le("Unexpected error", e)
        promise.reject(ERROR_UNEXPECTED_ERROR, processUnexpectedError(e).message)
      }
    }

    li("Called cancelRequest")
  }

  @ReactMethod
  fun fetchBlob(input: ReadableMap, promise: Promise) {
    li("Calling fetchBlob")
    thread {
      try {
        val errorOrDownloadResult =
          DownloaderParameterFactory()
            .fromInput(input)
            .fold(::Failure, ::Success)
            .fmap { pxs ->
              BlobDownloader(
                reactContext,
                createHttpClient(),
                createProgressFactory(
                  reactContext,
                  pxs.progressInterval
                ),
              ).download(pxs)
            }

        errorOrDownloadResult
          .fmap { Success(it.toReactMap()) }
          .`do`(
            { f ->
              lv("Something went wrong during fetch (code=${f.code},message=${f.message})")
              promise.reject(f.code, f.message)
            },
            promise::resolve
          )
      } catch (e: UnknownHostException) {
        lv("Unknown host", e)
        promise.reject(ERROR_UNKNOWN_HOST, e)
      } catch (e: Exception) {
        le("Unexpected exception", e)
        promise.reject(ERROR_UNEXPECTED_EXCEPTION, processUnexpectedException(e).message)
      } catch (e: Error) {
        le("Unexpected error", e)
        promise.reject(ERROR_UNEXPECTED_ERROR, processUnexpectedError(e).message)
      }
    }
    li("Called fetchBlob")
  }

  @ReactMethod
  fun uploadBlob(input: ReadableMap, promise: Promise) {
    li("Calling uploadBlob")
    thread {
      try {
        UploaderParameterFactory()
          .fromInput(input)
          .fold(::Failure, ::Success)
          .fmap { pxs ->
            BlobUploader(
              reactContext,
              createHttpClient(),
              createProgressFactory(
                reactContext,
                pxs.progressInterval
              )
            ).upload(pxs)
          }
          .map { it.toReactMap() }
          .`do`(
            { f ->
              lv("Something went wrong during upload (code=${f.code},message=${f.message})")
              promise.reject(f.code, f.message)
            },
            promise::resolve
          )
      } catch (e: UnknownHostException) {
        lv("Unknown host", e)
        promise.reject(ERROR_UNKNOWN_HOST, e)
      } catch (e: Exception) {
        le("Unexpected exception", e)
        promise.reject(ERROR_UNEXPECTED_EXCEPTION, processUnexpectedException(e).message)
      } catch (e: Error) {
        le("Unexpected error", e)
        promise.reject(ERROR_UNEXPECTED_ERROR, processUnexpectedError(e).message)
      }
    }
    li("Called uploadBlob")
  }
}
