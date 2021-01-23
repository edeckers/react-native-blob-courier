//  Copyright (c) Ely Deckers.
// 
//  This source code is licensed under the MPL-2.0 license found in the
//  LICENSE file in the root directory of this source tree.
import Foundation

open class BlobUploader: NSObject {
  func filterHeaders(unfilteredHeaders: NSDictionary) -> NSDictionary {
    Dictionary(uniqueKeysWithValues: unfilteredHeaders
      .map { key, value in (key as? String, value as? String) }
      .filter({ $0.1 != nil }))
      .mapValues { $0! } as NSDictionary
  }

  func isValidTargetValue(_ value: String) -> Bool {
    return BlobUploader.targetValues.contains(value)
  }

  func buildRequestDataForFileUpload(
    url: URL,
    parts: NSArray,
    headers: NSDictionary) throws -> (URLRequest, Data) {
    let boundary = UUID().uuidString

    var request = URLRequest(url: url)
    request.httpMethod = "POST"

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
          let payload = part[Errors.parameterPartPayload] as? [String: String] ?? [:]
          let absoluteFilePath = payload[Errors.parameterAbsoluteFilePath]!

          let fileUrl = URL(string: absoluteFilePath)!
          let filename = payload[Errors.parameterFilename] ?? fileUrl.lastPathComponent
          let mimeType = payload[Errors.parameterMimeType] ?? BlobUploader.defaultMimeType

          try data.addFilePart(
            part: FilePart(
              boundary: boundary,
              paramName: paramName,
              absoluteFilePath: absoluteFilePath,
              filename: filename,
              mimeType: mimeType))
          continue
        }

        let formDataValue = part[Errors.parameterPartPayload] as? String ?? ""

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

  func uploadBlobFromValidatedParameters(
    input: NSDictionary,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) throws {
    let taskId = (input[Errors.parameterTaskId] as? String) ?? ""

    let progressIntervalMilliseconds =
      (input[Errors.parameterProgressInterval] as? Int) ??
        BlobUploader.defaultProgressIntervalMilliseconds

    let url = (input[Errors.parameterUrl] as? String) ?? ""

    let urlObject = URL(string: url)!

    let parts = (input[Errors.parameterParts] as? NSArray) ?? NSArray()

    let returnResponse = (input[Errors.parameterReturnResponse] as? Bool) ?? false

    let sessionConfig = URLSessionConfiguration.default
    let uploaderDelegate =
      UploaderDelegate(
        taskId: taskId,
        returnResponse: returnResponse,
        progressIntervalMilliseconds: progressIntervalMilliseconds,
        resolve: resolve,
        reject: reject)
    let session = URLSession(configuration: sessionConfig, delegate: uploaderDelegate, delegateQueue: nil)

    let headers =
      filterHeaders(unfilteredHeaders:
        (input[Errors.parameterHeaders] as? NSDictionary) ??
        NSDictionary())

    let (request, fileData) = try buildRequestDataForFileUpload(url: urlObject, parts: parts, headers: headers)
    session.uploadTask(with: request, from: fileData).resume()
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
