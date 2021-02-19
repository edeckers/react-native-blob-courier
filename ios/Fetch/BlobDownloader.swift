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
    let sessionConfig = URLSessionConfiguration.default

    let group = DispatchGroup()
    let groupId = UUID().uuidString

    var result: Result<NSDictionary, BlobCourierError> = .success([:])

    print("Entering group (id=\(groupId))")
    group.enter()

    var cancelObserver: NSObjectProtocol?

    DispatchQueue.global(qos: .background).async {
      let successfulResult = { (theResult: NSDictionary) -> Void in
        result = .success(theResult)

        print("Leaving group (id=\(groupId),status=resolve)")
        group.leave()
      }

      let failedResult = { (error: BlobCourierError) -> Void in
        result = .failure(error)

        print("Leaving group (id=\(groupId),status=reject)")
        group.leave()
      }

      let destinationFileUrl = parameters.targetDirectory.appendingPathComponent(parameters.filename)

      let downloaderDelegate =
        DownloaderDelegate(
          taskId: parameters.taskId,
          destinationFileUrl: destinationFileUrl,
          progressIntervalMilliseconds: parameters.progressIntervalMilliseconds,
          resolve: successfulResult,
          reject: failedResult)

      cancelObserver = startFetchBlob(
        sessionConfig: sessionConfig,
        delegate: downloaderDelegate,
        fileURL: parameters.url,
        headers: parameters.headers,
        taskId: parameters.taskId)
    }

    print("Waiting for group (id=\(groupId))")
    group.wait()
    print("Left group (id=\(groupId))")

    NotificationCenter.default.removeObserver(cancelObserver)

    return result
  }

  private static func startFetchBlob(
    sessionConfig: URLSessionConfiguration,
    delegate: DownloaderDelegate,
    fileURL: URL,
    headers: NSDictionary,
    taskId: String) -> NSObjectProtocol? {
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

    let task = session.downloadTask(with: request)

    task.resume()

    return CancelController.registerCancelObserver(session: session, taskId: taskId)
  }
}
