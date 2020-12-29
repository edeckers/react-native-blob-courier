/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.common

import android.app.DownloadManager
import android.content.Context
import okhttp3.Headers

fun createDownloadManager(context: Context) =
  context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

fun mapHeadersToMap(headers: Headers): Map<String, String> =
  headers
    .toMultimap()
    .map { entry -> Pair(entry.key, entry.value.joinToString()) }
    .toMap()

fun filterHeaders(unfilteredHeaders: Map<String, Any>): Map<String, String> =
  unfilteredHeaders
    .mapValues { (_, v) -> v as? String }
    .filter { true }
    .mapNotNull { (k, v) -> v?.let { k to it } }
    .toMap()

fun <T> isNotNullOrEmpty(name: String): (value: T?) -> VResult<T> =
  { value: T? ->
    maybe(value).fold(
      { left(ValidationError.IsNull(name)) },
      { v -> if (v == "") left(ValidationError.IsEmpty(name)) else right(v) }
    )
  }

fun <T> isNotNull(name: String): (value: T?) -> VResult<T> =
  { value: T? -> maybe(value).fold({ left(ValidationError.IsNull(name)) }, ::right) }

fun <A> test(input: A?, validate: (v: A?) -> VResult<A>) = validate(input)
