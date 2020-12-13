// Copyright (c) Ely Deckers.
//
// This source code is licensed under the MPL-2.0 license found in the
// LICENSE file in the root directory of this source tree.
import Foundation

@objc(BlobCourierErrors)
open class BlobCourierErrors: NSObject {
  static let errorMissingRequiredParameter = "ERROR_MISSING_REQUIRED_PARAMETER"
  static let errorUnexpectedException = "ERROR_UNEXPECTED_EXCEPTION"
  static let errorUnexpectedValue = "ERROR_UNEXPECTED_VALUE"

  enum BlobCourierError: Error {
    case requiredParameter(parameter: String)
    case withMessage(code: String, message: String)
  }

  static func processUnexpectedException(
    reject: @escaping RCTPromiseRejectBlock, error: Error
  ) {
    reject(
      BlobCourierErrors.errorUnexpectedException,
      "An unexpected exception occurred: \(error.localizedDescription)", error)
  }

  static func processUnexpectedEmptyValue(
    reject: @escaping RCTPromiseRejectBlock, parameterName: String
  ) {
    reject(BlobCourierErrors.errorUnexpectedValue, "Parameter `\(parameterName)` cannot be empty.", nil)
  }
}
