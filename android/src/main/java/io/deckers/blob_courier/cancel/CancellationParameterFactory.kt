/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.cancel

import com.facebook.react.bridge.ReadableMap
import io.deckers.blob_courier.common.PARAMETER_TASK_ID
import io.deckers.blob_courier.common.PROVIDED_PARAMETERS
import io.deckers.blob_courier.common.ValidationResult
import io.deckers.blob_courier.common.ValidationSuccess
import io.deckers.blob_courier.common.hasRequiredStringField
import io.deckers.blob_courier.common.isNotNull
import io.deckers.blob_courier.common.right
import io.deckers.blob_courier.common.testKeep
import io.deckers.blob_courier.common.validationContext

data class RequiredParameters(
  val taskId: String,
)

data class CancellationParameters(
  val taskId: String,
)

private fun verifyRequiredParametersProvided(input: ReadableMap):
  ValidationResult<RequiredParameters> =
  validationContext(input, isNotNull(PROVIDED_PARAMETERS))
    .fmap(testKeep(hasRequiredStringField(PARAMETER_TASK_ID)))
    .fmap { (_, validatedParameters) ->
      val (taskId, _) = validatedParameters

      ValidationSuccess(RequiredParameters(taskId))
    }

class CancellationParameterFactory {
  fun fromInput(input: ReadableMap): ValidationResult<CancellationParameters> =
    verifyRequiredParametersProvided(input)
      .fmap {
        val (taskId) = it

        right(CancellationParameters(
          taskId
        ))
      }
}
