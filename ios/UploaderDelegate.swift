/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
import Foundation

@objc(UploaderDelegate)
open class UploaderDelegate: NSObject, URLSessionDataDelegate, URLSessionTaskDelegate {
  private static let UPLOAD_TYPE_UNMANAGED  = "Unmanaged"

  private let resolve: RCTPromiseResolveBlock
  private let reject: RCTPromiseRejectBlock
  private let taskId: String

  private let eventEmitter : BlobCourierEventEmitter? = BlobCourierEventEmitter.shared

  private var receivedData: Data = Data()

  init(taskId: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    self.resolve = resolve
    self.reject = reject
    self.taskId = taskId
  }

  public func urlSessionDidFinishEvents(forBackgroundURLSession session: URLSession) {
  }

  public func urlSession(_ session: URLSession, dataTask: URLSessionDataTask, didReceive data: Data) {
      self.receivedData.append(data)
  }

  public func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
    self.processCompletedUpload(data: self.receivedData, response: task.response, error: error)
  }

  public func urlSession(_ session: URLSession, task: URLSessionTask, didSendBodyData bytesSent: Int64, totalBytesSent: Int64, totalBytesExpectedToSend: Int64)
  {
      self.eventEmitter?.sendEvent(withName: BlobCourierEventEmitter.EVENT_PROGRESS, body: ["taskId": self.taskId, "total": totalBytesExpectedToSend, "written": totalBytesSent])
  }

  func processCompletedUpload(data: Data, response: URLResponse?, error: Error?) {
     if error != nil {
        print(
          "Error took place while uploading a file. Error description: \(error?.localizedDescription ?? "")"
        )
        BlobCourierErrors.processUnexpectedException(reject: reject, e: error as NSError?)
        return
      }

      if let statusCode = (response as? HTTPURLResponse)?.statusCode {
        let rawResponse = String(data: data, encoding: String.Encoding.utf8)

        let result : NSDictionary = [
          "type": UploaderDelegate.UPLOAD_TYPE_UNMANAGED,
          "data": [
            "code": statusCode,
            "data": rawResponse,
            "headers": []
          ]
        ]

        resolve(result)
        return
      }

      let noStatusCodeError = NSError(domain: BlobCourier.LIBRARY_DOMAIN, code: -1, userInfo: [NSLocalizedDescriptionKey: "Received no status code"])

      BlobCourierErrors.processUnexpectedException(reject: reject, e: noStatusCodeError)
  }
}
