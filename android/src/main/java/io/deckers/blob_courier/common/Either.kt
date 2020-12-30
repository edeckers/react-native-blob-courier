/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.common

sealed class Either<TLeft, TRight> {
  class Left<TLeft, TRight>(val v: TLeft) : Either<TLeft, TRight>() {
    override fun <B> fmap(m: (right: TRight) -> Either<TLeft, B>): Either<TLeft, B> = Left(v)
    override fun <B> map(m: (right: TRight) -> B): Either<TLeft, B> = Left(v)
  }

  class Right<TLeft, TRight>(val v: TRight) : Either<TLeft, TRight>() {
    override fun <B> fmap(m: (right: TRight) -> Either<TLeft, B>): Either<TLeft, B> = m(v)
    override fun <B> map(m: (right: TRight) -> B): Either<TLeft, B> = fmap { v -> Right(m(v)) }
  }

  abstract fun <B> fmap(m: (right: TRight) -> Either<TLeft, B>): Either<TLeft, B>
  abstract fun <B> map(m: (right: TRight) -> B): Either<TLeft, B>

  fun <B> pipe(m: (e: Either<TLeft, TRight>) -> Either<TLeft, B>): Either<TLeft, B> = m(this)
}

fun <TLeft, TRight> Either<TLeft, TRight>.ifLeft(fallback: TRight) = when (this) {
  is Either.Left -> fallback
  is Either.Right -> this.v
}

fun <TLeft, TRight> Either<TLeft, TRight>.component1() =
  when (this) {
    is Either.Right -> null
    is Either.Left -> v
  }

fun <TLeft, TRight> Either<TLeft, TRight>.component2() =
  when (this) {
    is Either.Right -> v
    is Either.Left -> null
  }

fun <TLeft, TRight, R> Either<TLeft, TRight>.fold(
  ifLeft: (v: TLeft) -> R,
  ifRight: (v: TRight) -> R
) =
  when (this) {
    is Either.Right -> ifRight(v)
    is Either.Left -> ifLeft(v)
  }

fun <TLeft, TRight, TOut> Either<TLeft, TRight>.`do`(
  left: (v: TLeft) -> TOut,
  right: (v: TRight) -> TOut
) =
  when (this) {
    is Either.Left -> left(v)
    is Either.Right -> right(v)
  }

fun <TLeft, TRight> right(r: TRight): Either<TLeft, TRight> = Either.Right(r)
fun <TLeft, TRight> left(l: TLeft): Either<TLeft, TRight> = Either.Left(l)

fun <TLeft, TRight> List<Either<TLeft, TRight>>.toEither(): Either<TLeft, List<TRight>> =
  this.fold(
    right(emptyList()),
    { acc, eitherItem ->
      eitherItem.fold(
        { acc },
        { m -> acc.map { p0 -> p0.plus(m) } }
      )
    }
  )
