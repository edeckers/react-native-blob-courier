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
  static let errorUnexpectedException = "ERROR_UNEXPECTED_EXCEPTION"
  static let errorCanceledException = "ERROR_CANCELED_EXCEPTION"

  static let errorEmptyValue = "ERROR_EMPTY_VALUE"
  static let errorInvalidValue = "ERROR_INVALID_VALUE"
  static let errorMissingRequiredParameter = "ERROR_MISSING_REQUIRED_PARAMETER"
  static let errorParameterErrorIsNull = "ERROR_PARAMETER_IS_NULL"

  static func createUnexpectedError(error: Error) -> BlobCourierError {
    return BlobCourierError(
      code: Errors.errorUnexpectedException,
      message: "An unexpected exception occurred: \(error.localizedDescription)",
      error: error)
  }

  static func createKeyDoesNotExist(parameterName: String, type: String) -> BlobCourierError {
    return BlobCourierError(
      code: Errors.errorMissingRequiredParameter,
      message: "Unexpected empty value for `\(parameterName)`",
      error: nil)
  }

  static func createMissingParameter(parameterName: String, type: String) -> BlobCourierError {
    return BlobCourierError(
      code: Errors.errorMissingRequiredParameter,
      message: "`\(parameterName)` is a required parameter of type `\(type)`",
      error: nil)
  }

  static func createInvalidValue(parameterName: String, receivedValue: String) -> BlobCourierError {
    return BlobCourierError(
      code: Errors.errorInvalidValue,
      message: "`\(parameterName)` has incorrect value `\(receivedValue)`",
      error: nil)
  }
}
