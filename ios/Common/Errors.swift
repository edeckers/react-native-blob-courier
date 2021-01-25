//  Copyright (c) Ely Deckers.
// 
//  This source code is licensed under the MPL-2.0 license found in the
//  LICENSE file in the root directory of this source tree.
import Foundation

enum DeprecatedBlobCourierError: Error {
  case withMessage(code: String, message: String)
}

struct BlobCourierError: Error {
  let code: String
  let message: String
  let error: Error?

  init(code: String, message: String, error: Error?) {
    self.code = code
    self.message = message
    self.error = error
  }
}

open class Errors: NSObject {
  static let errorMissingRequiredParameter = "ERROR_MISSING_REQUIRED_PARAMETER"
  static let errorUnexpectedException = "ERROR_UNEXPECTED_EXCEPTION"
  static let errorUnexpectedValue = "ERROR_UNEXPECTED_VALUE"
  static let errorInvalidValue = "ERROR_INVALID_VALUE"

  static func createUnexpectedError(error: Error) -> BlobCourierError {
    return BlobCourierError(
      code: Errors.errorUnexpectedException,
      message: "An unexpected exception occurred: \(error.localizedDescription)",
      error: error)
  }

  static func createInvalidValue(parameterName: String, value: String) -> BlobCourierError {
    return BlobCourierError(
      code: Errors.errorInvalidValue,
      message: "Parameter `\(parameterName)` cannot be `\(value)`",
      error: nil)
  }
}
