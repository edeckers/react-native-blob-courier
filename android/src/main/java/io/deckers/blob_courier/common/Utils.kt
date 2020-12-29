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

fun <H, TT> cons(head: H, tail: TT) = Pair(head, tail)
fun <H, TT> read(v: VResult<H>, acc: TT) =
  v.map { cons(it, acc) }

fun <H, P> ValidateParameter(
  value: H?,
  validate: (value: H?) -> VResult<H>,
  prev: P
) =
  read(validate(value), prev)

@Suppress("SameParameterValue")
fun <H> ValidateParameter(
  value: H?,
  validate: (value: H?) -> VResult<H>
): VResult<Pair<H, Unit>> =
  ValidateParameter(value, validate, Unit)

fun <T> isNotNullOrEmpty(name: String): (value: T?) -> VResult<T> =
  { value: T? ->
    maybe(value).fold(
      { left(ValidationError.IsNull(name)) },
      { v -> if (v == "") left(ValidationError.IsEmpty(name)) else right(v) }
    )
  }

fun <T> isNotNull(name: String): (value: T?) -> VResult<T> =
  { value: T? -> maybe(value).fold({ left(ValidationError.IsNull(name)) }, { v -> right(v) }) }
