// Copyright (c) Ely Deckers.
//
// This source code is licensed under the MPL-2.0 license found in the
// LICENSE file in the root directory of this source tree.
import Foundation

open class DownloaderDelegate: NSObject, URLSessionDownloadDelegate {
  typealias SuccessHandler = (NSDictionary) -> Void
  typealias FailureHandler = (Error) -> Void

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
      if let theError = error {
        print("session: didCompleteWithError: \(theError.localizedDescription)")
	self.reject(theError)

        session.finishTasksAndInvalidate()
      }
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
    if let theError = error {
      print(
        "Error took place while downloading a file. Error description: \(theError.localizedDescription)"
      )

      Errors.createUnexpectedError(error: theError)
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

      print("Successfully downloaded. Status code: \(statusCode)")
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
