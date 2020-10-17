package io.deckers.blob_courier

import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableNativeMap
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
