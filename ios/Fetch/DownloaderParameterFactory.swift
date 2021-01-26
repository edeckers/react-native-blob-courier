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
    guard let filename = input[Constants.parameterFilename] as? String else {
      return .failure(Errors.createMissingParameter(parameterName: Constants.parameterFilename, type: "String"))
    }

    guard let taskId = input[Constants.parameterTaskId] as? String else {
      return .failure(Errors.createMissingParameter(parameterName: Constants.parameterTaskId, type: "String"))
    }

    guard let url = input[Constants.parameterUrl] as? String else {
      return .failure(Errors.createMissingParameter(parameterName: Constants.parameterUrl, type: "String"))
    }

    let iosSettings =
      (input[Constants.parameterIOSSettings] as? NSDictionary) ??
        NSDictionary()

    let target =
      (iosSettings[Constants.parameterTarget] as? String) ??
        Constants.defaultTarget

    if !isValidTargetValue(target) {
      let invalidTargetError =
        Errors.createInvalidValue(parameterName: Constants.parameterTarget, receivedValue: target)

      return .failure(invalidTargetError)
    }

    let progressIntervalMilliseconds =
      (input[Constants.parameterProgressInterval] as? Int) ??
        Constants.defaultProgressIntervalMilliseconds

    let headers =
      filterHeaders(unfilteredHeaders:
        (input[Constants.parameterHeaders] as? NSDictionary) ??
          NSDictionary())

    do {
      let targetUrl =
        try FileManager.default.url(
          for: target == Constants.targetData ? .documentDirectory : .cachesDirectory,
          in: .userDomainMask,
          appropriateFor: nil,
          create: false)

      let destinationFileUrl = targetUrl.appendingPathComponent(filename)

      guard let fileUrl = URL(string: url) else {
        return .failure(Errors.createInvalidValue(parameterName: Constants.parameterUrl, receivedValue: url))
      }

      return .success(DownloadParameters(
        filename: filename,
        headers: headers,
        progressIntervalMilliseconds: progressIntervalMilliseconds,
        targetDirectory: targetUrl,
        taskId: taskId,
        url: fileUrl))
    } catch let error {
      return .failure(Errors.createUnexpectedError(error: error))
    }
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
