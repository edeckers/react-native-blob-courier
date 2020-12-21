package io.deckers.blob_courier

import com.facebook.react.bridge.ReadableMap

data class DownloaderParameters(val a: String)

class DownloaderParameterFactory() {
  fun fromInput(input: ReadableMap): DownloaderParameters {
    return DownloaderParameters(input.getString("a")!!)
  }
}
