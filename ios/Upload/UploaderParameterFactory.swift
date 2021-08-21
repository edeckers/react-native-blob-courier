//  Copyright (c) Ely Deckers.
// 
//  This source code is licensed under the MPL-2.0 license found in the
//  LICENSE file in the root directory of this source tree.
import Foundation

open class UploaderParameterFactory: NSObject {
  static func filterHeaders(unfilteredHeaders: NSDictionary) -> NSDictionary {
    Dictionary(uniqueKeysWithValues: unfilteredHeaders
      .map { key, value in (key as? String, value as? String) }
      .filter({ $0.1 != nil }))
      .mapValues { $0! } as NSDictionary
  }

  static func validateParameters(input: NSDictionary) -> Result<UploadParameters, BlobCourierError> {
    guard let taskId = input[Constants.parameterTaskId] as? String else {
      return .failure(Errors.createMissingParameter(parameterName: Constants.parameterTaskId, type: "String"))
    }

    guard let url = input[Constants.parameterUrl] as? String else {
      return .failure(Errors.createMissingParameter(parameterName: Constants.parameterUrl, type: "String"))
    }

    let method = input[Constants.parameterMethod] as? String ?? Constants.defaultUploadMethod

    let parts = input[Constants.parameterParts] as? NSArray ?? NSArray()

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

    return .success(UploadParameters(
      headers: headers,
      method: method,
      parts: parts,
      progressIntervalMilliseconds: progressIntervalMilliseconds,
      returnResponse: returnResponse,
      taskId: taskId,
      url: fileUrl))
  }

  static func fromInput(input: NSDictionary) -> Result<UploadParameters, BlobCourierError> {
    do {
      return validateParameters(input: input)
    } catch {
      return .failure(Errors.createUnexpectedError(error: error))
    }
  }
}
