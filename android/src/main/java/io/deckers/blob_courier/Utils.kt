/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier

import android.app.DownloadManager
import android.content.Context
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import org.json.JSONException
import org.json.JSONObject

@Throws(JSONException::class)
fun convertJsonToMap(jsonObject: JSONObject): WritableMap {
  val map = Arguments.createMap()

  for (key in jsonObject.keys()) when (val value = jsonObject.get(key)) {
    is JSONObject -> map.putMap(key, convertJsonToMap(value))
    is Boolean -> map.putBoolean(key, value)
    is Int -> map.putInt(key, value)
    is Double -> map.putDouble(key, value)
    is String -> map.putString(key, value)
    else -> map.putString(key, value.toString())
  }

  return map
}

fun notifyBridgeOfProgress(
  context: ReactApplicationContext,
  taskId: String,
  totalNumberOfBytesRead: Long,
  totalLength: Long
) {
  if (!context.hasActiveCatalystInstance()) {
    return
  }

  context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
    .emit(
      DEVICE_EVENT_PROGRESS,
      Arguments.createMap().apply {
        putString("taskId", taskId)
        putString("written", totalNumberOfBytesRead.toString())
        putString("total", totalLength.toString())
      }
    )
}

fun createDownloadManager(context: Context) =
  context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

fun Map<*, *>.toReactMap(): WritableMap {
  val thisMap = this

  return Arguments.createMap().apply {
    thisMap.forEach { (k, v) ->
      when {
        (v is String) -> putString(k.toString(), v)
        (v is Map<*, *>) -> putMap(k.toString(), v.toReactMap())
        else -> putString(k.toString(), v.toString())
      }
    }
  }
}
