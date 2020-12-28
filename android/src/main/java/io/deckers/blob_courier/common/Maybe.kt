/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.common

sealed class Maybe<T> {
  class Nothing<TNothing> : Maybe<TNothing>() {
    override fun <B> fmap(m: (value: TNothing) -> Maybe<B>): Maybe<B> = nothing()
  }

  class Just<TValue>(val v: TValue) : Maybe<TValue>() {
    override fun <B> fmap(m: (value: TValue) -> Maybe<B>): Maybe<B> = m(v)
  }

  abstract fun <B> fmap(m: (value: T) -> Maybe<B>): Maybe<B>
}

fun <T, R> Maybe<T>.fold(ifNothing: () -> R, ifJust: (v: T) -> R) =
  if (this is Maybe.Just) ifJust(v) else ifNothing()

fun <T> Maybe<T>.toEither() =
  if (this is Maybe.Just) Either.Right<Nothing, T>(v) else Either.Left(Unit)

fun <T> just(v: T) = Maybe.Just(v)
fun <T> maybe(v: T?) = v?.let(::just) ?: nothing<T>()
fun <T> nothing() = Maybe.Nothing<T>()
