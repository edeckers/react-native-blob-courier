//  Copyright (c) Ely Deckers.
// 
//  This source code is licensed under the MPL-2.0 license found in the
//  LICENSE file in the root directory of this source tree.
import Foundation

open class DownloaderParameterFactory: NSObject {
  static func validateParameters(input: NSDictionary) throws {
    try Errors.assertRequiredParameter(
      input: input, type: "String", parameterName: Constants.parameterFilename)
    try Errors.assertRequiredParameter(
      input: input, type: "String", parameterName: Constants.parameterTaskId)
    try Errors.assertRequiredParameter(
      input: input, type: "String", parameterName: Constants.parameterUrl)
  }

  static func fromInput(input: NSDictionary) -> Result<DownloadParameters, BlobCourierError> {
    do {
      try validateParameters(input: input)

      guard let filename = input[Constants.parameterFilename] as? String,
            let taskId = input[Constants.parameterTaskId] as? String,
            let url = input[Constants.parameterUrl] as? String else {
              return .failure(BlobCourierError(code: "FIX_THIS_CODE", message: "FIX_THIS_MESSAGE", error: nil))
            }

      let xxx = DownloadParameters(
        filename: filename,
        taskId: taskId,
        url: url)
      // let xxx = DownloadParameters(
      //   filename: filename,
      //   taskId: taskId,
      //   url: url)

      return .success(xxx)
    } catch {
      return .failure(Errors.createUnexpectedError(error: error))
    }
  }
}
