//  Copyright (c) Ely Deckers.
// 
//  This source code is licensed under the MPL-2.0 license found in the
//  LICENSE file in the root directory of this source tree.
import Foundation

open class DownloaderParameterFactory: NSObject {
  static func filterHeaders(unfilteredHeaders: NSDictionary) -> NSDictionary {
    Dictionary(uniqueKeysWithValues: unfilteredHeaders
      .map { key, value in (key as? String, value as? String) }
      .filter({ $0.1 != nil }))
      .mapValues { $0! } as NSDictionary
  }

  // swiftlint:disable function_body_length
  static func validateParameters(input: NSDictionary) -> Result<DownloadParameters, BlobCourierError> {
    try? Errors.assertRequiredParameter(
      input: input, type: "String", parameterName: Constants.parameterFilename)
    try? Errors.assertRequiredParameter(
      input: input, type: "String", parameterName: Constants.parameterTaskId)
    try? Errors.assertRequiredParameter(
      input: input, type: "String", parameterName: Constants.parameterUrl)

    let iosSettings =
      (input[Constants.parameterIOSSettings] as? NSDictionary) ??
        NSDictionary()

    let target =
      (iosSettings[Constants.parameterTarget] as? String) ??
        Constants.defaultTarget

    if !isValidTargetValue(target) {
      let invalidTargetError =
        Errors.createInvalidValue(parameterName: Constants.parameterTarget, value: target)

      return .failure(invalidTargetError)
    }

    let progressIntervalMilliseconds =
      (input[Constants.parameterProgressInterval] as? Int) ??
        Constants.defaultProgressIntervalMilliseconds

    let headers =
      filterHeaders(unfilteredHeaders:
        (input[Constants.parameterHeaders] as? NSDictionary) ??
          NSDictionary())

    guard let targetUrl =
      try? FileManager.default.url(
        for: target == Constants.targetData ? .documentDirectory : .cachesDirectory,
        in: .userDomainMask,
        appropriateFor: nil,
        create: false) else {
          return .failure(BlobCourierError(code: "FIX_THIS_CODE", message: "FIX THIS MESSAGE", error: nil))
        }

    guard let filename = input[Constants.parameterFilename] as? String,
          let taskId = input[Constants.parameterTaskId] as? String,
          let url = input[Constants.parameterUrl] as? String else {
            return .failure(BlobCourierError(
              code: Errors.errorMissingRequiredParameter,
              message: "One of the required parameters is missing", // FIXME ED Use generic error handler
              error: nil))
          }

    let destinationFileUrl = targetUrl.appendingPathComponent(filename)

    guard let fileUrl = URL(string: url) else { return .failure(BlobCourierError(
      code: Errors.errorMissingRequiredParameter,
      message: "One of the required parameters is missing", // FIXME ED Use generic error handler
      error: nil)) }

    return .success(DownloadParameters(
      filename: filename,
      headers: headers,
      progressIntervalMilliseconds: progressIntervalMilliseconds,
      targetDirectory: targetUrl,
      taskId: taskId,
      url: fileUrl))
  }
  // swiftlint:enable function_body_length

  static func fromInput(input: NSDictionary) -> Result<DownloadParameters, BlobCourierError> {
    do {
      return validateParameters(input: input)
    } catch {
      return .failure(Errors.createUnexpectedError(error: error))
    }
  }

  static func isValidTargetValue(_ value: String) -> Bool {
    return Constants.targetValues.contains(value)
  }
}
