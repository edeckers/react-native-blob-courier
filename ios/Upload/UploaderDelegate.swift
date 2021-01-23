// Copyright (c) Ely Deckers.
//
// This source code is licensed under the MPL-2.0 license found in the
// LICENSE file in the root directory of this source tree.
import Foundation
import React

open class UploaderDelegate: NSObject, URLSessionDataDelegate, URLSessionTaskDelegate {
  private static let uploadTypeUnmanaged  = "Unmanaged"

  private let resolve: RCTPromiseResolveBlock
  private let reject: RCTPromiseRejectBlock

  private let taskId: String
  private let returnResponse: Bool

  private let eventEmitter: BlobCourierDelayedEventEmitter

  private var receivedData: Data = Data()

  init(
    taskId: String,
    returnResponse: Bool,
    progressIntervalMilliseconds: Int,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock) {
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
      processCompletedUpload(data: self.receivedData, response: task.response, error: error)
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

  func processCompletedUpload(data: Data, response: URLResponse?, error: Error?) {
     if let theError = error {
        print(
          "Error while uploading a file. Error description: \(theError.localizedDescription)"
        )
        Errors.processUnexpectedException(reject: reject, error: theError)
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

      Errors.processUnexpectedException(reject: reject, error: noStatusCodeError)
  }
}
