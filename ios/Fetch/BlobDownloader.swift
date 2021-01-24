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

  static func fetchBlobFromValidatedParameters(parameters: DownloadParameters) ->
    Result<NSDictionary, BlobCourierError> {
    let taskId = parameters.taskId

    let url = parameters.url

    let filename = parameters.filename

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

      let destinationFileUrl = parameters.targetDirectory.appendingPathComponent(parameters.filename)

      let downloaderDelegate =
        DownloaderDelegate(
          taskId: taskId,
          destinationFileUrl: destinationFileUrl,
          progressIntervalMilliseconds: parameters.progressIntervalMilliseconds,
          resolve: succesfulResult,
          reject: failedResult)

      startFetchBlob(
        sessionConfig: sessionConfig,
        delegate: downloaderDelegate,
        fileURL: parameters.url,
        headers: parameters.headers)
    }

    group.wait()

    return result
  }

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
}
