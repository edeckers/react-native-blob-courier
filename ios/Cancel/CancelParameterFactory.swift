//  Copyright (c) Ely Deckers.
// 
//  This source code is licensed under the MPL-2.0 license found in the
//  LICENSE file in the root directory of this source tree.
import Foundation

open class CancelParameterFactory: NSObject {
  static func validateParameters(input: NSDictionary) -> Result<CancelParameters, BlobCourierError> {
    guard let taskId = input[Constants.parameterTaskId] as? String else {
      return .failure(Errors.createMissingParameter(parameterName: Constants.parameterTaskId, type: "String"))
    }

    return .success(CancelParameters(taskId: taskId))
  }

  static func fromInput(input: NSDictionary) -> Result<CancelParameters, BlobCourierError> {
    return validateParameters(input: input)
  }
}
