// Copyright (c) Ely Deckers.
//
// This source code is licensed under the MPL-2.0 license found in the
// LICENSE file in the root directory of this source tree.
import Foundation

@objc(DownloaderDelegate)
open class DownloaderDelegate: NSObject, URLSessionDownloadDelegate {

  static let downloadTypeUnmanaged  = "Unmanaged"

  let destinationFileUrl: URL
  let resolve: RCTPromiseResolveBlock
  let reject: RCTPromiseRejectBlock
  let taskId: String

  let eventEmitter: BlobCourierEventEmitter? = BlobCourierEventEmitter.shared

  init(
    taskId: String,
    destinationFileUrl: URL,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock) {
    self.destinationFileUrl = destinationFileUrl
    self.resolve = resolve
    self.reject = reject
    self.taskId = taskId
  }

  public func urlSessionDidFinishEvents(forBackgroundURLSession session: URLSession) {
  }

  public func urlSession(
    _ session: URLSession,
    downloadTask: URLSessionDownloadTask,
    didFinishDownloadingTo location: URL) {
      self.processCompletedDownload(
        location: location,
        response: downloadTask.response,
        error: downloadTask.error)
  }

  public func urlSession(
    _ session: URLSession,
    downloadTask: URLSessionDownloadTask,
    didWriteData bytesWritten: Int64,
    totalBytesWritten: Int64,
    totalBytesExpectedToWrite: Int64) {
      self.eventEmitter?.sendEvent(
        withName: BlobCourierEventEmitter.eventProgress,
        body: ["taskId": self.taskId, "total": totalBytesExpectedToWrite, "written": totalBytesWritten])
  }

  func processCompletedDownload(location: URL, response: URLResponse?, error: Error?) {
    if error != nil {
      print(
        "Error took place while downloading a file. Error description: \(error?.localizedDescription ?? "")"
      )

      BlobCourierErrors.processUnexpectedException(reject: self.reject, error: error as NSError?)
      return
    }

    if let statusCode = (response as? HTTPURLResponse)?.statusCode {
      let result: NSDictionary = [
        "type": DownloaderDelegate.downloadTypeUnmanaged,
        "data": [
          "fullFilePath": "\(self.destinationFileUrl)",
          "response": [
            "code": statusCode
          ]
        ]
      ]

      print("Successfully downloaded. Status code: \(statusCode)")
      do {
        self.eventEmitter?.sendEvent(withName: BlobCourierEventEmitter.eventProgress, body: result)
        try? FileManager.default.removeItem(at: self.destinationFileUrl)
        try FileManager.default.copyItem(at: location, to: self.destinationFileUrl)
        print("Successfully moved file to \(self.destinationFileUrl)")
        self.resolve(result)
      } catch let writeError {
        BlobCourierErrors.processUnexpectedException(reject: reject, error: writeError as NSError?)
      }
    }
  }
}
