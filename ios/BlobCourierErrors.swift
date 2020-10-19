/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
import Foundation

@objc(BlobCourierErrors)
open class BlobCourierErrors: NSObject {
  static let ERROR_MISSING_REQUIRED_PARAMETER = "ERROR_MISSING_REQUIRED_PARAMETER"
  static let ERROR_UNEXPECTED_EXCEPTION = "ERROR_UNEXPECTED_EXCEPTION"
  static let ERROR_UNEXPECTED_EMPTY_VALUE = "ERROR_UNEXPECTED_EMPTY_VALUE"

  enum BlobCourierError: Error {
    case withMessage(code: String, message: String)
  }

  static func processUnexpectedException(
    reject: @escaping RCTPromiseRejectBlock, e: NSError?
  ) {
    reject(BlobCourierErrors.ERROR_UNEXPECTED_EXCEPTION, "An unexpected exception occurred: \(e?.localizedDescription ?? "")", e)
  }

  static func processUnexpectedEmptyValue(
    reject: @escaping RCTPromiseRejectBlock, parameterName: String
  ) {
    reject(BlobCourierErrors.ERROR_UNEXPECTED_EMPTY_VALUE, "Parameter `\(parameterName)` cannot be empty.", nil)
  }
}
