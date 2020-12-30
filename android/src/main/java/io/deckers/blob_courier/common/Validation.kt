/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.common

fun <TIn> validationContext(input: TIn?, test: (v: TIn?) -> ValidationResult<TIn>) =
  test(input).pipe(::write)

fun <TIn, TOut> validate(input: TIn?, test: (v: TIn?) -> ValidationResult<TOut>) = test(input)

fun <TIn, TOut, TContextLast, TContextRest, TInputValue> testDiscard(
  test: ((v: TIn?) -> ValidationResult<TOut>),
  mapInput: (v: TInputValue?) -> TIn?
): (Pair<TInputValue, Writer<TContextLast, TContextRest>>) ->
ValidationResult<Pair<TInputValue, Writer<TContextLast, TContextRest>>> =
  { (subject, writer) ->
    writer discard ValidatorContext(subject, validate(mapInput(subject), test))
  }

fun <TIn, TOut, TContextLast, TContextRest> testDiscard(
  test: ((v: TIn?) -> ValidationResult<TOut>)
): (Pair<TIn, Writer<TContextLast, TContextRest>>) ->
ValidationResult<Pair<TIn, Writer<TContextLast, TContextRest>>> =
  { (subject, writer) ->
    testDiscard<
      TIn,
      TOut,
      TContextLast,
      TContextRest,
      TIn>(test, { subject })(Pair(subject, writer))
  }

fun <TValue, TOut, TContextLast, TContextRest, TInputValue> testKeep(
  test: ((v: TValue?) -> ValidationResult<TOut>),
  mapInput: (v: TInputValue?) -> TValue?
): (Pair<TInputValue, Writer<TContextLast, TContextRest>>)
-> ValidationResult<Pair<TInputValue, Writer<TOut, Writer<TContextLast, TContextRest>>>> =
  { (subject, writer) ->
    writer keep ValidatorContext(subject, validate(mapInput(subject), test))
  }

fun <TValue, TOut, TContextLast, TContextRest> testKeep(
  test: ((v: TValue?) -> ValidationResult<TOut>)
): (Pair<TValue, Writer<TContextLast, TContextRest>>) ->
ValidationResult<Pair<TValue, Writer<TOut, Writer<TContextLast, TContextRest>>>> =
  { (subject, writer) ->
    testKeep<
      TValue,
      TOut,
      TContextLast,
      TContextRest,
      TValue>(test, { subject })(Pair(subject, writer))
  }
