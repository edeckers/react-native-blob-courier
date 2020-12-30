/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.common

data class TakeData<TContext, TResult>(val context: TContext, val result: ValidationResult<TResult>)

class Writer<A, B>(o: A, acc: B) {
  val v = Pair(o, acc)

  operator fun component1() = v.first
  operator fun component2() = v.second

  infix fun <TResult> drop(v: ValidationResult<TResult>) = v.map { Pair(this.component1(), this) }
  infix fun <TContext, TResult> take(contextAndResult: TakeData<TContext, TResult>) =
    contextAndResult.result.map { Pair(contextAndResult.context, Writer(it, this)) }
}

fun <A> write(v: ValidationResult<A>) = v.map { Pair(it, Writer(it, Unit)) }
