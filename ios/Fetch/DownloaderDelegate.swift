// Copyright (c) Ely Deckers.
//
// This source code is licensed under the MPL-2.0 license found in the
// LICENSE file in the root directory of this source tree.
import Foundation

open class DownloaderDelegate: NSObject, URLSessionDownloadDelegate, URLSessionDelegate {
  typealias SuccessHandler = (NSDictionary) -> Void
  typealias FailureHandler = (BlobCourierError) -> Void

  private static let downloadTypeUnmanaged  = "Unmanaged"

  let destinationFileUrl: URL
  let resolve: SuccessHandler
  let reject: FailureHandler
  let taskId: String

  let eventEmitter: BlobCourierDelayedEventEmitter

  init(
    taskId: String,
    destinationFileUrl: URL,
    progressIntervalMilliseconds: Int,
    resolve: @escaping SuccessHandler,
    reject: @escaping FailureHandler) {
    self.destinationFileUrl = destinationFileUrl
    self.resolve = resolve
    self.reject = reject

    self.taskId = taskId

    self.eventEmitter =
      BlobCourierDelayedEventEmitter(
        taskId: taskId,
        progressIntervalMilliseconds: progressIntervalMilliseconds)
  }

  public func urlSessionDidFinishEvents(forBackgroundURLSession session: URLSession) {
  }

  public func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
      guard let error = error else { return }

      if (error as NSError).code == NSURLErrorCancelled {
        self.reject(BlobCourierError(
          code: Errors.errorCanceledException,
          message: "Request was cancelled",
          error: error))

	return
      }

      self.reject(Errors.createUnexpectedError(error: error))
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
    self.eventEmitter.notifyBridgeOfProgress(
      totalBytesWritten: totalBytesWritten,
      totalBytesExpectedToWrite: totalBytesExpectedToWrite)
  }

  func processCompletedDownload(location: URL, response: URLResponse?, error: Error?) {
    if let error = error {
      print(
        "Error took place while downloading a file from \(response?.url). " +
	"Error description: \(error.localizedDescription)"
      )

      self.reject(Errors.createUnexpectedError(error: error))
      return
    }

    if let statusCode = (response as? HTTPURLResponse)?.statusCode {
      let result: NSDictionary = [
        "type": DownloaderDelegate.downloadTypeUnmanaged,
        "data": [
          "absoluteFilePath": "\(self.destinationFileUrl)",
          "response": [
            "code": statusCode
          ]
        ]
      ]

      print("Successfully downloaded \(self.destinationFileUrl). Status code: \(statusCode)")
      do {
        try? FileManager.default.removeItem(at: self.destinationFileUrl)
        try FileManager.default.copyItem(at: location, to: self.destinationFileUrl)
        print("Successfully moved file to \(self.destinationFileUrl)")
        self.resolve(result)
      } catch let writeError {
        self.reject(Errors.createUnexpectedError(error: writeError))
      }
    }
  }
}
