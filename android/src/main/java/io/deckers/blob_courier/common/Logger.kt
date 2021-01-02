package io.deckers.blob_courier.common

import android.util.Log

class Logger(private val tag: String) {
  fun e(message: String, throwable: Throwable? = null) = Log.e(tag, message, throwable)
  fun i(message: String) = Log.i(tag, message)
  fun v(message: String, throwable: Throwable? = null) = Log.v(tag, message, throwable)
  fun w(message: String, throwable: Throwable? = null) = Log.w(tag, message, throwable)
}
