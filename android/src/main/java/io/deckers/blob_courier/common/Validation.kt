/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.common

fun <T> isNotNullOrEmpty(name: String): (value: T?) -> ValidationResult<T> =
  { value: T? ->
    maybe(value)
      .fold(
        { left(ValidationError.IsNull(name)) },
        { v -> if (v == "") left(ValidationError.IsEmpty(name)) else right(v) }
      )
  }

fun <T> isNotNull(name: String): (value: T?) -> ValidationResult<T> =
  { value: T? ->
    maybe(value)
      .map(::ValidationSuccess)
      .ifNone(ValidationFailure(ValidationError.IsNull(name)))
  }

fun <A> validate(input: A?, test: (v: A?) -> ValidationResult<A>) = test(input)

fun <TValue, TContextLast, TContextRest, TInputValue> testDrop(
  test: ((v: TValue?) -> ValidationResult<TValue>),
  mapInput: (v: TInputValue?) -> TValue?
): (Pair<TInputValue, Writer<TContextLast, TContextRest>>)
-> ValidationResult<Pair<TContextLast, Writer<TContextLast, TContextRest>>> =
  { context: Pair<TInputValue, Writer<TContextLast, TContextRest>> ->
    context.second drop validate(mapInput(context.first), test)
  }

fun <TValue, TContextLast, TContextRest> testDrop(
  test: ((v: TValue?) -> ValidationResult<TValue>)
): (Pair<TValue, Writer<TContextLast, TContextRest>>) ->
ValidationResult<Pair<TContextLast, Writer<TContextLast, TContextRest>>> =
  { context: Pair<TValue, Writer<TContextLast, TContextRest>> ->
    testDrop<TValue, TContextLast, TContextRest, TValue>(test, { context.first })(context)
  }

fun <TValue, TContextLast, TContextRest, TInputValue> testTake(
  test: ((v: TValue?) -> ValidationResult<TValue>),
  mapInput: (v: TInputValue?) -> TValue?
): (Pair<TInputValue, Writer<TContextLast, TContextRest>>)
-> ValidationResult<Pair<TValue, Writer<TValue, Writer<TContextLast, TContextRest>>>> =
  { context: Pair<TInputValue, Writer<TContextLast, TContextRest>> ->
    context.second take validate(mapInput(context.first), test)
  }

fun <TValue, TContextLast, TContextRest> testTake(
  test: ((v: TValue?) -> ValidationResult<TValue>)
): (Pair<TValue, Writer<TContextLast, TContextRest>>) ->
ValidationResult<Pair<TValue, Writer<TValue, Writer<TContextLast, TContextRest>>>> =
  { context: Pair<TValue, Writer<TContextLast, TContextRest>> ->
    testTake<TValue, TContextLast, TContextRest, TValue>(test, { context.first })(context)
  }