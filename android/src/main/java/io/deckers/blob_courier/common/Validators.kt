/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.common

import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReadableType
import io.deckers.blob_courier.react.toReactArray
import io.deckers.blob_courier.react.toReactMap

fun isNotNullOrEmptyString(
  key: String,
): (map: String?) -> ValidationResult<String> =
  { value: String? ->
    maybe(value)
      .fold(
        { ValidationFailure(ValidationError.IsNull(key)) },
        { v ->
          if (v == "")
            ValidationFailure(ValidationError.IsEmpty(key))
          else ValidationSuccess(v)
        }
      )
  }

fun <T> isNotNullOrEmptyMap(
  key: String,
  retrieve: (m: ReadableMap) -> T
): (map: ReadableMap?) -> ValidationResult<T> =
  { value: ReadableMap? ->
    maybe(value)
      .fold(
        { ValidationFailure(ValidationError.IsNull(key)) },
        { v ->
          if (testEmptyMap(key, v))
            ValidationFailure(ValidationError.KeyDoesNotExist(key))
          else ValidationSuccess(retrieve(v))
        }
      )
  }

fun <T> isNotNull(name: String): (value: T?) -> ValidationResult<T> =
  { value: T? ->
    maybe(value)
      .map(::ValidationSuccess)
      .ifNone(ValidationFailure(ValidationError.IsNull(name)))
  }

fun hasKey(key: String): (map: ReadableMap?) -> ValidationResult<ReadableMap> =
  { map: ReadableMap? ->
    if (map?.hasKey(key) == true) right(map) else left(ValidationError.KeyDoesNotExist(key))
  }

fun testEmptyMap(key: String, input: ReadableMap) =
  !input.hasKey(key) || when (input.getType(key)) {
    ReadableType.Map -> input.getMap(key)?.toHashMap()?.size == 0
    ReadableType.String -> input.getString(key) == ""
    ReadableType.Null -> true
    ReadableType.Array -> input.getArray(key)?.size() == 0
    else -> false
  }

fun <T> hasReqParam(key: String, retrieve: (m: ReadableMap) -> T, type: Class<T>): (
  map: ReadableMap?
) -> ValidationResult<T> =
  { map: ReadableMap? ->
    val testKey = hasKey(key)
    val testNull =
      { vr: ValidationResult<ReadableMap> ->
        vr.fmap {
          isNotNullOrEmptyMap(key, retrieve)(map)
        }
      }

    val exec = compose(testNull, testKey)

    exec(map).fold(
      { left(ValidationError.MissingParameter(key, type.name)) },
      ::ValidationSuccess
    )
  }

fun hasRequiredArrayField(key: String): (map: ReadableMap?) -> ValidationResult<ReadableArray> =
  hasReqParam(
    key,
    {
      it.getArray(key) ?: emptyArray<Any>().toReactArray()
    },
    ReadableArray::class.java
  )

fun hasRequiredMapField(key: String): (map: ReadableMap?) -> ValidationResult<ReadableMap> =
  hasReqParam(
    key,
    {
      it.getMap(key) ?: emptyMap<String, Any>().toReactMap()
    },
    ReadableMap::class.java
  )

fun hasRequiredStringField(key: String): (map: ReadableMap?) -> ValidationResult<String> =
  hasReqParam(key, { it.getString(key) ?: "" }, String::class.java)
