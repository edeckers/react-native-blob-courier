// Copyright (c) Ely Deckers.
//
// This source code is licensed under the MPL-2.0 license found in the
// LICENSE file in the root directory of this source tree.
import Foundation

open class UploaderDelegate: NSObject, URLSessionDataDelegate, URLSessionTaskDelegate {
  typealias SuccessHandler = (NSDictionary) -> Void
  typealias FailureHandler = (BlobCourierError) -> Void

  private static let uploadTypeUnmanaged  = "Unmanaged"

  private let resolve: SuccessHandler
  private let reject: FailureHandler

  private let taskId: String
  private let returnResponse: Bool

  private let eventEmitter: BlobCourierDelayedEventEmitter

  private var receivedData: Data = Data()

  init(
    taskId: String,
    returnResponse: Bool,
    progressIntervalMilliseconds: Int,
    resolve: @escaping SuccessHandler,
    reject: @escaping FailureHandler) {
    self.taskId = taskId
    self.returnResponse = returnResponse

    self.resolve = resolve
    self.reject = reject

    self.eventEmitter =
      BlobCourierDelayedEventEmitter(
        taskId: taskId,
        progressIntervalMilliseconds: progressIntervalMilliseconds)
  }

  public func urlSessionDidFinishEvents(forBackgroundURLSession session: URLSession) {
  }

  public func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
    guard let theError = error else {
      processCompletedUpload(data: self.receivedData, response: task.response, error: error)
      return
    }

    processFailedUpload(error: theError)
  }

  public func urlSession(_ session: URLSession, dataTask: URLSessionDataTask, didReceive data: Data) {
      self.receivedData.append(data)
  }

  public func urlSession(
    _ session: URLSession,
    task: URLSessionTask,
    didSendBodyData bytesSent: Int64,
    totalBytesSent: Int64,
    totalBytesExpectedToSend: Int64) {
    self.eventEmitter.notifyBridgeOfProgress(
      totalBytesWritten: totalBytesSent,
      totalBytesExpectedToWrite: totalBytesExpectedToSend)
  }

  func processFailedUpload(error: Error) {
    if (error as NSError).code == NSURLErrorCancelled {
      self.reject(BlobCourierError(code: Errors.errorCanceledException, message: "Request was cancelled", error: error))

      return
    }

    self.reject(Errors.createUnexpectedError(error: error))
  }

  func processCompletedUpload(data: Data, response: URLResponse?, error: Error?) {
     if let error = error {
        print(
          "Error while uploading a file. Error description: \(error.localizedDescription)"
        )
        reject(Errors.createUnexpectedError(error: error))
        return
     }

      if let statusCode = (response as? HTTPURLResponse)?.statusCode {
        let maybeRawResponse = returnResponse ? String(data: data, encoding: String.Encoding.utf8) : nil
        let rawResponse = maybeRawResponse ?? ""

        let result: NSDictionary = [
          "response": [
            "code": statusCode,
            "data": rawResponse,
            "headers": []
          ]
        ]

        resolve(result)
        return
      }

      let noStatusCodeError =
        NSError(
          domain: Constants.libraryDomain,
          code: -1,
          userInfo: [NSLocalizedDescriptionKey: "Received no status code"])

      reject(Errors.createUnexpectedError(error: noStatusCodeError))
  }
}
