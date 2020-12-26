package io.deckers.blob_courier.react

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import io.deckers.blob_courier.common.DEVICE_EVENT_PROGRESS

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

fun Array<*>.toReactArray(): WritableArray {
  val thisMap = this

  return Arguments.createArray().apply {
    thisMap.forEach { v ->
      when {
        (v is Array<*>) -> {
          pushArray(v.toReactArray())
        }
        (v is String) ->
          pushString(v)
        (v is Map<*, *>) -> {
          pushMap(v.toReactMap())
        }
        else ->
          pushString(v.toString())
      }
    }
  }
}

fun Map<*, *>.toReactMap(): WritableMap {
  val thisMap = this

  return Arguments.createMap().apply {
    thisMap.forEach { (k, v) ->
      when {
        (v is Array<*>) -> {
          putArray(k.toString(), v.toReactArray())
        }
        (v is String) ->
          putString(k.toString(), v)
        (v is Map<*, *>) -> {
          putMap(k.toString(), v.toReactMap())
        }
        else ->
          putString(k.toString(), v.toString())
      }
    }
  }
}
