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
fun <H, TT> read(v: Either<ValidationError, H>, acc: TT) =
  v.map { cons(it, acc) }

fun <H, P> ValidateParameter(
  name: String,
  value: H?,
  validate: (name: String, value: H?) -> Either<ValidationError, H>,
  prev: P
) =
  read(validate(name, value), prev)

@Suppress("SameParameterValue")
fun <H> ValidateParameter(
  name: String,
  value: H?,
  validate: (name: String, value: H?) -> Either<ValidationError, H>
): Either<ValidationError, Pair<H, Unit>> =
  ValidateParameter(name, value, validate, Unit)

fun <T> isNotNullOrEmpty(name: String, value: T?): Either<ValidationError, T> =
  maybe(value).fold(
    { left(ValidationIsNull(name)) },
    { v -> if (v == "") left(ValidationIsEmpty(name)) else right(v) }
  )

fun <T> isNotNull(name: String, value: T?): Either<ValidationError, T> =
  maybe(value).fold({ left(ValidationIsNull(name)) }, { v -> right(v) })
