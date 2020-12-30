/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.common

sealed class Maybe<TValue> {
  class Nothing<TNothing> : Maybe<TNothing>() {
    override fun <B> fmap(m: (value: TNothing) -> Maybe<B>): Maybe<B> = nothing()
    override fun <B> map(m: (value: TNothing) -> B): Maybe<B> = nothing()
  }

  class Just<TJust>(val v: TJust) : Maybe<TJust>() {
    override fun <B> fmap(m: (value: TJust) -> Maybe<B>): Maybe<B> = m(v)
    override fun <B> map(m: (value: TJust) -> B): Maybe<B> = Just(m(v))
  }

  abstract fun <B> fmap(m: (value: TValue) -> Maybe<B>): Maybe<B>
  abstract fun <B> map(m: (value: TValue) -> B): Maybe<B>
}

fun <TValue> Maybe<TValue>.ifNone(fallback: TValue): TValue = when (this) {
  is Maybe.Nothing -> fallback
  is Maybe.Just -> v
}

fun <T, R> Maybe<T>.fold(ifNothing: () -> R, ifJust: (v: T) -> R) =
  if (this is Maybe.Just) ifJust(v) else ifNothing()

fun <T> Maybe<T>.toEither() =
  when (this) {
    is Maybe.Just -> Either.Right<Nothing, T>(v)
    is Maybe.Nothing -> Either.Left(Unit)
  }

fun <T> just(v: T) = Maybe.Just(v)
fun <T> maybe(v: T?): Maybe<T> = v?.let(::just) ?: nothing<T>()
fun <T> nothing() = Maybe.Nothing<T>()
