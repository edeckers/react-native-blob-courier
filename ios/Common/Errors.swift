//  Copyright (c) Ely Deckers.
// 
//  This source code is licensed under the MPL-2.0 license found in the
//  LICENSE file in the root directory of this source tree.
import Foundation

open class Errors: NSObject {
  func assertRequiredParameter(input: NSDictionary, type: String, parameterName: String) throws {
    let maybeValue = input[parameterName]

    if maybeValue == nil {
      throw BlobCourierErrors.BlobCourierError.requiredParameter(parameter: parameterName)
    }
  }
}
