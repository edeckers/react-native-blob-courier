/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.common

// Credit: https://medium.com/@Robert_Chrzanow/kotlins-missing-type-either-51602db80fda
// Robert Chrzanowski - Aug 26, 2017·3 min read
typealias Result<V> = Either<BlobCourierError, V>
typealias ValidationResult<V> = Either<ValidationError, V>

fun <V> Failure(e: BlobCourierError): Result<V> = Either.Left(e)
fun <V> Success(v: V): Result<V> = Either.Right(v)

fun <V> ValidationFailure(e: ValidationError): ValidationResult<V> = Either.Left(e)
fun <V> ValidationSuccess(v: V): ValidationResult<V> = Either.Right(v)
