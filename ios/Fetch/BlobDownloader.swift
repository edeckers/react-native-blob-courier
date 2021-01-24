//  Copyright (c) Ely Deckers.
// 
//  This source code is licensed under the MPL-2.0 license found in the
//  LICENSE file in the root directory of this source tree.
import Foundation

open class BlobDownloader: NSObject {
  static func filterHeaders(unfilteredHeaders: NSDictionary) -> NSDictionary {
    Dictionary(uniqueKeysWithValues: unfilteredHeaders
      .map { key, value in (key as? String, value as? String) }
      .filter({ $0.1 != nil }))
      .mapValues { $0! } as NSDictionary
  }

  // swiftlint:disable function_body_length
  static func fetchBlobFromValidatedParameters(input: NSDictionary) throws ->
    Result<NSDictionary, BlobCourierError> {
    let taskId = (input[Constants.parameterTaskId] as? String) ?? ""

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

    let url = (input[Constants.parameterUrl] as? String) ?? ""

    let filename = (input[Constants.parameterFilename] as? String) ?? ""

    let headers =
      filterHeaders(unfilteredHeaders:
        (input[Constants.parameterHeaders] as? NSDictionary) ??
        NSDictionary())

    let targetUrl: URL =
      try FileManager.default.url(
        for: target == Constants.targetData ? .documentDirectory : .cachesDirectory,
        in: .userDomainMask,
        appropriateFor: nil,
        create: false)
    let destinationFileUrl = targetUrl.appendingPathComponent(filename)

    let fileURL = URL(string: url)
    let sessionConfig = URLSessionConfiguration.default

    let group = DispatchGroup()
    let queue = DispatchQueue.global()

    var result: Result<NSDictionary, BlobCourierError> = .success([:])

    group.enter()

    queue.async(group: group) {
      let succesfulResult = { (theResult: NSDictionary) -> Void in
        result = .success(theResult)

        group.leave()
      }

      let failedResult = { (error: BlobCourierError) -> Void in
        result = .failure(error)

        group.leave()
      }

      let downloaderDelegate =
        DownloaderDelegate(
          taskId: taskId,
          destinationFileUrl: destinationFileUrl,
          progressIntervalMilliseconds: progressIntervalMilliseconds,
          resolve: succesfulResult,
          reject: failedResult)

      startFetchBlob(
        sessionConfig: sessionConfig,
        delegate: downloaderDelegate,
        fileURL: fileURL!,
        headers: headers)
    }

    group.wait(timeout: .now() + DispatchTimeInterval.seconds(Constants.defaultRequestTimeoutSeconds))

    return result
  }
  // swiftlint:enable function_body_length

  private static func startFetchBlob(
    sessionConfig: URLSessionConfiguration,
    delegate: DownloaderDelegate,
    fileURL: URL,
    headers: NSDictionary) {
    let session =
     URLSession(
       configuration: sessionConfig,
       delegate: delegate,
       delegateQueue: nil)

    var request = URLRequest(url: fileURL)
    for (key, value) in headers {
      if let headerKey = key as? String, let headerValue = value as? String {
        request.setValue(
          headerValue,
          forHTTPHeaderField: headerKey)
      }
    }

    session.downloadTask(with: request).resume()
  }

  static func isValidTargetValue(_ value: String) -> Bool {
    return Constants.targetValues.contains(value)
  }
}
