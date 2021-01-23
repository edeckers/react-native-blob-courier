//  Copyright (c) Ely Deckers.
// 
//  This source code is licensed under the MPL-2.0 license found in the
//  LICENSE file in the root directory of this source tree.
import Foundation

open class Errors: NSObject {
  static let errorMissingRequiredParameter = "ERROR_MISSING_REQUIRED_PARAMETER"
  static let errorUnexpectedException = "ERROR_UNEXPECTED_EXCEPTION"
  static let errorUnexpectedValue = "ERROR_UNEXPECTED_VALUE"
  static let errorInvalidValue = "ERROR_INVALID_VALUE"

  enum BlobCourierError: Error {
    case requiredParameter(parameter: String)
    case withMessage(code: String, message: String)
  }

  static func processUnexpectedException(
    reject: @escaping RCTPromiseRejectBlock, error: Error
  ) {
    reject(
      Errors.errorUnexpectedException,
      "An unexpected exception occurred: \(error.localizedDescription)",
      error)
  }

  static func processUnexpectedEmptyValue(
    reject: @escaping RCTPromiseRejectBlock, parameterName: String
  ) {
    reject(Errors.errorUnexpectedValue, "Parameter `\(parameterName)` cannot be empty.", nil)
  }

  static func processInvalidValue(
    reject: @escaping RCTPromiseRejectBlock, parameterName: String, value: String
  ) {
    reject(Errors.errorInvalidValue, "Parameter `\(parameterName)` cannot be `\(value)`.", nil)
  }

  static func assertRequiredParameter(input: NSDictionary, type: String, parameterName: String) throws {
    let maybeValue = input[parameterName]

    if maybeValue == nil {
      throw BlobCourierError.requiredParameter(parameter: parameterName)
    }
  }
}
