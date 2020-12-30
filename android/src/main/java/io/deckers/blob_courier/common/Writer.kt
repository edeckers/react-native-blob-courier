/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.common

data class ValidatorContext<TContext, TResult>(
  val context: TContext,
  val result: ValidationResult<TResult>
)

class Writer<A, B>(o: A, acc: B) {
  val v = Pair(o, acc)

  operator fun component1() = v.first
  operator fun component2() = v.second

  infix fun <TContext, TResult> discard(contextAndResult: ValidatorContext<TContext, TResult>) =
    contextAndResult.result.map { Pair(contextAndResult.context, this) }

  infix fun <TContext, TResult> keep(contextAndResult: ValidatorContext<TContext, TResult>) =
    contextAndResult.result.map { result -> Pair(contextAndResult.context, Writer(result, this)) }
}

fun <TContext, A, B> popToContext(c: Pair<TContext, Writer<A, B>>): Pair<A, B> {
  val (_, w) = c
  val (newContext, rest) = w

  return Pair(newContext, rest)
}

fun <TContext, A, B> readContext(c: Pair<TContext, Writer<A, B>>): TContext {
  val (v, _) = c

  return v
}

fun <A> write(v: ValidationResult<A>) = v.map { Pair(it, Writer(it, Unit)) }
