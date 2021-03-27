//  Copyright (c) Ely Deckers.
// 
//  This source code is licensed under the MPL-2.0 license found in the
//  LICENSE file in the root directory of this source tree.
import Foundation

open class SenderParameterFactory: NSObject {
  static func filterHeaders(unfilteredHeaders: NSDictionary) -> NSDictionary {
    Dictionary(uniqueKeysWithValues: unfilteredHeaders
      .map { key, value in (key as? String, value as? String) }
      .filter({ $0.1 != nil }))
      .mapValues { $0! } as NSDictionary
  }

  static func validateParameters(input: NSDictionary) -> Result<SendParameters, BlobCourierError> {
    guard let taskId = input[Constants.parameterTaskId] as? String else {
      return .failure(Errors.createMissingParameter(parameterName: Constants.parameterTaskId, type: "String"))
    }

    guard let url = input[Constants.parameterUrl] as? String else {
      return .failure(Errors.createMissingParameter(parameterName: Constants.parameterUrl, type: "String"))
    }

    guard let absoluteFilePath = input[Constants.parameterAbsoluteFilePath] as? String else {
      return .failure(Errors.createMissingParameter(parameterName: Constants.parameterAbsoluteFilePath, type: "String"))
    }

    guard let method = input[Constants.parameterMethod] as? String else {
      return .failure(Errors.createMissingParameter(parameterName: Constants.parameterMethod, type: "String"))
    }

    let progressIntervalMilliseconds =
      (input[Constants.parameterProgressInterval] as? Int) ??
        Constants.defaultProgressIntervalMilliseconds

    let returnResponse = (input[Constants.parameterReturnResponse] as? Bool) ?? false

    let headers =
      filterHeaders(unfilteredHeaders:
        (input[Constants.parameterHeaders] as? NSDictionary) ??
          NSDictionary())

    guard let fileUrl = URL(string: url) else {
      return .failure(Errors.createInvalidValue(parameterName: Constants.parameterUrl, receivedValue: url))
    }

    return .success(SendParameters(
      absoluteFilePath: absoluteFilePath,
      headers: headers,
      method: method,
      progressIntervalMilliseconds: progressIntervalMilliseconds,
      returnResponse: returnResponse,
      taskId: taskId,
      url: fileUrl))
  }

  static func fromInput(input: NSDictionary) -> Result<SendParameters, BlobCourierError> {
    do {
      return validateParameters(input: input)
    } catch {
      return .failure(Errors.createUnexpectedError(error: error))
    }
  }
}
