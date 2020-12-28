/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.common

// Credit: https://medium.com/@Robert_Chrzanow/kotlins-missing-type-either-51602db80fda
// Robert Chrzanowski - Aug 26, 2017·3 min read
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
    is Either.Left -> left(this.v)
    is Either.Right -> right(this.v)
  }

fun <TLeft, TRight> right(r: TRight): Either<TLeft, TRight> = Either.Right(r)
fun <TLeft, TRight> left(l: TLeft): Either<TLeft, TRight> = Either.Left(l)


