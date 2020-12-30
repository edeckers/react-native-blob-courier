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

fun <TFIn, TGIn, TOut> compose(
  f: (i: TFIn) -> TOut,
  g: (i: TGIn) -> TFIn
): (t: TGIn) -> TOut = { input -> f(g(input)) }
