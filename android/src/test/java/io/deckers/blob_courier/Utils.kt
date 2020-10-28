/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap

// TODO Make this a little prettier
fun <T> create_sublists_from_list(theList: List<T>, length: Int): List<List<T>> {
  val restCount = length - 1
  val subLists = mutableListOf<List<T>>()

  for (i in theList.indices) {
    val head = theList[i]
    val next = i + 1
    val tailList = theList.takeLast(theList.size - next)
    for (j in 0..tailList.size - restCount) {
      subLists.add(listOf(head) + tailList.subList(j, j + restCount))
    }
  }

  return subLists.toList()
}

fun Map<String, String>.toReactMap(): WritableMap {
  val thisMap = this

  return Arguments.createMap().apply {
    thisMap.forEach { (k, v) -> putString(k, v) }
  }
}
