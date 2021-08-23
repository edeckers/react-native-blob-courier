//  Copyright (c) Ely Deckers.
// 
//  This source code is licensed under the MPL-2.0 license found in the
//  LICENSE file in the root directory of this source tree.
import Foundation

open class BlobUploader: NSObject {
  static func filterHeaders(unfilteredHeaders: NSDictionary) -> NSDictionary {
    Dictionary(uniqueKeysWithValues: unfilteredHeaders
      .map { key, value in (key as? String, value as? String) }
      .filter({ $0.1 != nil }))
      .mapValues { $0! } as NSDictionary
  }

  static func isValidTargetValue(_ value: String) -> Bool {
    return Constants.targetValues.contains(value)
  }

  static func buildRequestDataForFileUpload(
    url: URL,
    method: String,
    parts: NSArray,
    headers: NSDictionary) throws -> (URLRequest, Data) {
    let boundary = UUID().uuidString

    var request = URLRequest(url: url)
    request.httpMethod = method

    request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
    for (key, value) in headers {
      if let headerKey = key as? String, let headerValue = value as? String {
        request.setValue(
          headerValue,
          forHTTPHeaderField: headerKey)
      }
    }

    var data = Data()

    for value in parts {
      if let part = value as? [String: Any], let paramName = part["name"] as? String {

        if part["type"] as? String == "file" {
          let payload = part[Constants.parameterPartPayload] as? [String: String] ?? [:]
          let absoluteFilePath = payload[Constants.parameterAbsoluteFilePath]!

          let fileUrl = URL(string: absoluteFilePath)!
          let filename = payload[Constants.parameterFilename] ?? fileUrl.lastPathComponent
          let mimeType = payload[Constants.parameterMimeType] ?? Constants.defaultMimeType

          try data.addFilePart(
            part: FilePart(
              boundary: boundary,
              paramName: paramName,
              absoluteFilePath: absoluteFilePath,
              filename: filename,
              mimeType: mimeType))
          continue
        }

        let formDataValue = part[Constants.parameterPartPayload] as? String ?? ""

        data.addFormDataPart(
          part: StringPart(
              boundary: boundary,
              paramName: paramName,
              value: formDataValue))
      }
    }

    if parts.count > 0 {
      data.append(string: "--\(boundary)--\r\n")
    }

    return (request, data)
  }

  static func uploadBlobFromValidatedParameters(parameters: UploadParameters) ->
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

      let uploaderDelegate =
        UploaderDelegate(
          taskId: parameters.taskId,
          returnResponse: parameters.returnResponse,
          progressIntervalMilliseconds: parameters.progressIntervalMilliseconds,
          resolve: successfulResult,
          reject: failedResult)

      let session = URLSession(configuration: sessionConfig, delegate: uploaderDelegate, delegateQueue: nil)

      let headers = parameters.headers

      do {
        let (request, fileData) =
          try buildRequestDataForFileUpload(
            url: parameters.url, method: parameters.method, parts: parameters.parts, headers: headers)

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
}

struct FilePart {
  let boundary: String
  let paramName: String
  let absoluteFilePath: String
  let filename: String
  let mimeType: String
}

struct StringPart {
  let boundary: String
  let paramName: String
  let value: String
}

extension Data {
  mutating func append(string: String) {
    if let data = string.data(using: .utf8) {
      append(data)
    }
  }

  mutating func addFormDataPart(part: StringPart) {
    append(string: "--\(part.boundary)\r\n")
    append(
      string: "Content-Disposition: form-data; name=\"\(part.paramName)\"\r\n")
    append(string: "Content-Length: \(part.value.count)\r\n")
    append(string: "\r\n")
    append(string: part.value)
    append(string: "\r\n")
  }

  mutating func addFilePart(part: FilePart) throws {
    let fileUrl = URL(string: part.absoluteFilePath)!
    let fileData = try Data(contentsOf: fileUrl)

    let maybeFileAttributes = try? FileManager.default.attributesOfItem(atPath: part.absoluteFilePath)

    append(string: "--\(part.boundary)\r\n")
    append(
      "Content-Disposition: form-data; name=\"\(part.paramName)\"; filename=\"\(part.filename)\"\r\n"
        .data(using: .utf8)!)
    append(string: "Content-Type: \(part.mimeType)\r\n")

    if let fileSize = maybeFileAttributes?[.size] {
      append(string: "Content-Length: \(fileSize)\r\n")
    }

    append(string: "\r\n")
    append(fileData)
    append(string: "\r\n")
  }
}
