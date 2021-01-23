//  Copyright (c) Ely Deckers.
// 
//  This source code is licensed under the MPL-2.0 license found in the
//  LICENSE file in the root directory of this source tree.
import Foundation

open class BlobDownloader: NSObject {
  func filterHeaders(unfilteredHeaders: NSDictionary) -> NSDictionary {
    Dictionary(uniqueKeysWithValues: unfilteredHeaders
      .map { key, value in (key as? String, value as? String) }
      .filter({ $0.1 != nil }))
      .mapValues { $0! } as NSDictionary
  }

  // swiftlint:disable function_body_length
  func fetchBlobFromValidatedParameters(
    input: NSDictionary,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) throws {
    let taskId = (input[Errors.parameterTaskId] as? String) ?? ""

    let iosSettings =
      (input[Errors.parameterIOSSettings] as? NSDictionary) ??
      NSDictionary()

    let target =
      (iosSettings[Errors.parameterTarget] as? String) ??
      BlobDownloader.defaultTarget

    if !isValidTargetValue(target) {
      BlobDownloaderErrors.processInvalidValue(
        reject: reject,
        parameterName: Errors.parameterTarget,
        value: target)

      return
    }

    let progressIntervalMilliseconds =
      (input[Errors.parameterProgressInterval] as? Int) ??
        BlobDownloader.defaultProgressIntervalMilliseconds

    let url = (input[Errors.parameterUrl] as? String) ?? ""

    let filename = (input[Errors.parameterFilename] as? String) ?? ""

    let headers =
      filterHeaders(unfilteredHeaders:
        (input[Errors.parameterHeaders] as? NSDictionary) ??
        NSDictionary())

    let targetUrl: URL =
      try FileManager.default.url(
        for: target == BlobDownloader.targetData ? .documentDirectory : .cachesDirectory,
        in: .userDomainMask,
        appropriateFor: nil,
        create: false)
    let destinationFileUrl = targetUrl.appendingPathComponent(filename)

    let fileURL = URL(string: url)
    let sessionConfig = URLSessionConfiguration.default
    let downloaderDelegate =
      DownloaderDelegate(
        taskId: taskId,
        destinationFileUrl: destinationFileUrl,
        progressIntervalMilliseconds: progressIntervalMilliseconds,
        resolve: resolve,
        reject: reject)

    startFetchBlob(
      sessionConfig: sessionConfig,
      delegate: downloaderDelegate,
      reject: reject,
      fileURL: fileURL!,
      headers: headers)
  }
  // swiftlint:enable function_body_length

  func startFetchBlob(
    sessionConfig: URLSessionConfiguration,
    delegate: DownloaderDelegate,
    reject: @escaping RCTPromiseRejectBlock,
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

  func isValidTargetValue(_ value: String) -> Bool {
    return BlobDownloader.targetValues.contains(value)
  }
}
