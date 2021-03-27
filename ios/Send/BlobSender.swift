//  Copyright (c) Ely Deckers.
// 
//  This source code is licensed under the MPL-2.0 license found in the
//  LICENSE file in the root directory of this source tree.
import Foundation

open class BlobSender: NSObject {
  static func filterHeaders(unfilteredHeaders: NSDictionary) -> NSDictionary {
    Dictionary(uniqueKeysWithValues: unfilteredHeaders
      .map { key, value in (key as? String, value as? String) }
      .filter({ $0.1 != nil }))
      .mapValues { $0! } as NSDictionary
  }

  static func isValidTargetValue(_ value: String) -> Bool {
    return Constants.targetValues.contains(value)
  }

  static func buildRequestDataForFileSend(
    method: String,
    url: URL,
    absoluteFilePath: String,
    headers: NSDictionary) throws -> (URLRequest, Data) {
    var request = URLRequest(url: url)
    request.httpMethod = method

    for (key, value) in headers {
      if let headerKey = key as? String, let headerValue = value as? String {
        request.setValue(
          headerValue,
          forHTTPHeaderField: headerKey)
      }
    }

    let fileUrl = URL(string: absoluteFilePath)!

    let fileData = try Data(contentsOf: fileUrl)

    return (request, fileData)
  }

  // swiftlint:disable function_body_length
  static func sendBlobFromValidatedParameters(parameters: SendParameters) ->
    Result<NSDictionary, BlobCourierError> {
    let sessionConfig = URLSessionConfiguration.default

    let group = DispatchGroup()
    let groupId = UUID().uuidString

    let queue = DispatchQueue.global()

    var result: Result<NSDictionary, BlobCourierError> = .success([:])

    print("Entering group (id=\(groupId))")
    group.enter()

    var cancelObserver: NSObjectProtocol?

    queue.async(group: group) {
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

      let senderDelegate =
        SenderDelegate(
          taskId: parameters.taskId,
          returnResponse: parameters.returnResponse,
          progressIntervalMilliseconds: parameters.progressIntervalMilliseconds,
          resolve: successfulResult,
          reject: failedResult)

      let session = URLSession(configuration: sessionConfig, delegate: senderDelegate, delegateQueue: nil)

      let headers = parameters.headers

      do {
        let (request, fileData) =
          try buildRequestDataForFileSend(
            method: parameters.method,
            url: parameters.url,
            absoluteFilePath: parameters.absoluteFilePath,
            headers: headers)

        session.uploadTask(with: request, from: fileData).resume()

        cancelObserver = CancelController.registerCancelObserver(
            session: session, taskId: parameters.taskId)
      } catch {
        failedResult(Errors.createUnexpectedError(error: error))
      }
    }

    print("Waiting for group (id=\(groupId))")
    group.wait()
    print("Left group (id=\(groupId))")

    NotificationCenter.default.removeObserver(cancelObserver)

    return result
  }
  // swiftlint:enable function_body_length
}
