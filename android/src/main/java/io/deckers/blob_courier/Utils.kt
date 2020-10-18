package io.deckers.blob_courier

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableNativeMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import org.json.JSONException
import org.json.JSONObject

@Throws(JSONException::class)
fun convertJsonToMap(jsonObject: JSONObject): WritableMap {
  val map = WritableNativeMap()

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

// FIXME Code duplication
fun notifyBridge(
  context: ReactApplicationContext,
  taskId: String,
  totalNumberOfBytesRead: Long,
  totalLength: Long
) =
  context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
    .emit(
      DEVICE_EVENT_PROGRESS,
      Arguments.createMap().apply {
        putString("taskId", taskId)
        putString("written", totalNumberOfBytesRead.toString())
        putString("total", totalLength.toString())
      }
    )
