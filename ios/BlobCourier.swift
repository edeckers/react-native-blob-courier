//  Copyright (c) Ely Deckers.
// 
//  This source code is licensed under the MPL-2.0 license found in the
//  LICENSE file in the root directory of this source tree.
import Foundation

// swiftlint:disable type_body_length
@objc(BlobCourier)
open class BlobCourier: NSObject {
  static let downloadTypeUnmanaged  = "Unmanaged"

  static let libraryDomain  = "io.deckers.blob_courier"

  static let parameterFilename = "filename"
  static let parameterAbsoluteFilePath = "absoluteFilePath"
  static let parameterHeaders = "headers"
  static let parameterIOSSettings = "ios"
  static let parameterMethod = "method"
  static let parameterMimeType = "mimeType"
  static let parameterPartPayload = "payload"
  static let parameterParts = "parts"
  static let parameterProgressInterval = "progressIntervalMilliseconds"
  static let parameterReturnResponse = "returnResponse"
  static let parameterTarget = "target"
  static let parameterTaskId = "taskId"
  static let parameterUrl = "url"

  static let targetCache = "cache"
  static let targetData = "data"
  static let targetValues = [targetCache, targetData]

  static let defaultMethod = "GET"
  static let defaultMimeType = "application/octet-stream"
  static let defaultProgressIntervalMilliseconds = 500
  static let defaultTarget = targetCache

  func assertRequiredParameter(input: NSDictionary, type: String, parameterName: String) throws {
    let maybeValue = input[parameterName]

    if maybeValue == nil {
      throw BlobCourierErrors.BlobCourierError.requiredParameter(parameter: parameterName)
    }
  }

  func filterHeaders(unfilteredHeaders: NSDictionary) -> NSDictionary {
    Dictionary(uniqueKeysWithValues: unfilteredHeaders
      .map { key, value in (key as? String, value as? String) }
      .filter({ $0.1 != nil }))
      .mapValues { $0! } as NSDictionary
  }

  // swiftlint:disable function_body_length
  func fetchBlobFromValidatedParameters(
    input: NSDictionary,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) throws {
    let taskId = (input[BlobCourier.parameterTaskId] as? String) ?? ""

    let iosSettings =
      (input[BlobCourier.parameterIOSSettings] as? NSDictionary) ??
      NSDictionary()

    let target =
      (iosSettings[BlobCourier.parameterTarget] as? String) ??
      BlobCourier.defaultTarget

    if !isValidTargetValue(target) {
      BlobCourierErrors.processInvalidValue(
        reject: reject,
        parameterName: BlobCourier.parameterTarget,
        value: target)

      return
    }

    let progressIntervalMilliseconds =
      (input[BlobCourier.parameterProgressInterval] as? Int) ??
        BlobCourier.defaultProgressIntervalMilliseconds

    let url = (input[BlobCourier.parameterUrl] as? String) ?? ""

    let filename = (input[BlobCourier.parameterFilename] as? String) ?? ""

    let headers =
      filterHeaders(unfilteredHeaders:
        (input[BlobCourier.parameterHeaders] as? NSDictionary) ??
        NSDictionary())

    let targetUrl: URL =
      try FileManager.default.url(
        for: target == BlobCourier.targetData ? .documentDirectory : .cachesDirectory,
        in: .userDomainMask,
        appropriateFor: nil,
        create: false)
    let destinationFileUrl = targetUrl.appendingPathComponent(filename)

    let fileURL = URL(string: url)
    let sessionConfig = URLSessionConfiguration.default
    let downloaderDelegate =
      DownloaderDelegate(
        taskId: taskId,
        destinationFileUrl: destinationFileUrl,
        progressIntervalMilliseconds: progressIntervalMilliseconds,
        resolve: resolve,
        reject: reject)

    startFetchBlob(
      sessionConfig: sessionConfig,
      delegate: downloaderDelegate,
      reject: reject,
      fileURL: fileURL!,
      headers: headers)
  }
  // swiftlint:enable function_body_length

  func startFetchBlob(
    sessionConfig: URLSessionConfiguration,
    delegate: DownloaderDelegate,
    reject: @escaping RCTPromiseRejectBlock,
    fileURL: URL,
    headers: NSDictionary) {
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

    session.downloadTask(with: request).resume()
  }

  func isValidTargetValue(_ value: String) -> Bool {
    return BlobCourier.targetValues.contains(value)
  }

  func buildRequestDataForFileUpload(
    url: URL,
    parts: NSDictionary,
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

    var index = 0
    for (key, value) in parts {
      if let paramName = key as? String, let part = value as? [String: Any] {
        if part["type"] as? String == "file" {
          let payload = part[BlobCourier.parameterPartPayload] as? [String: String] ?? [:]
          let absoluteFilePath = payload[BlobCourier.parameterAbsoluteFilePath]!

          let fileUrl = URL(string: absoluteFilePath)!
          let filename = payload[BlobCourier.parameterFilename] ?? fileUrl.lastPathComponent
          let mimeType = payload[BlobCourier.parameterMimeType] ?? BlobCourier.defaultMimeType

          try data.addFilePart(
            part: FilePart(
              boundary: boundary,
              paramName: paramName,
              absoluteFilePath: absoluteFilePath,
              filename: filename,
              mimeType: mimeType))
          continue
        }

        let formDataValue = part[BlobCourier.parameterPartPayload] as? String ?? ""
        data.addFormDataPart(
          part: StringPart(
              boundary: boundary,
              paramName: paramName,
              value: formDataValue))
      }
    }

    if parts.count > 0 {
      data.append(string: "\r\n--\(boundary)--\r\n")
    }

    return (request, data)
  }

  func uploadBlobFromValidatedParameters(
    input: NSDictionary,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) throws {
    let taskId = (input[BlobCourier.parameterTaskId] as? String) ?? ""

    let progressIntervalMilliseconds =
      (input[BlobCourier.parameterProgressInterval] as? Int) ??
        BlobCourier.defaultProgressIntervalMilliseconds

    let url = (input[BlobCourier.parameterUrl] as? String) ?? ""

    let urlObject = URL(string: url)!

    let parts = (input[BlobCourier.parameterParts] as? NSDictionary) ?? NSDictionary()

    let returnResponse = (input[BlobCourier.parameterReturnResponse] as? Bool) ?? false

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
        (input[BlobCourier.parameterHeaders] as? NSDictionary) ??
        NSDictionary())

    let (request, fileData) = try buildRequestDataForFileUpload(url: urlObject, parts: parts, headers: headers)
    session.uploadTask(with: request, from: fileData).resume()
  }

  @objc(fetchBlob:withResolver:withRejecter:)
  func fetchBlob(
    input: NSDictionary, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock
  ) {
    do {
      try assertRequiredParameter(
        input: input, type: "String", parameterName: BlobCourier.parameterFilename)
      try assertRequiredParameter(
        input: input, type: "String", parameterName: BlobCourier.parameterTaskId)
      try assertRequiredParameter(
        input: input, type: "String", parameterName: BlobCourier.parameterUrl)

      try fetchBlobFromValidatedParameters(input: input, resolve: resolve, reject: reject)
    } catch BlobCourierErrors.BlobCourierError.requiredParameter(let parameterName) {
      BlobCourierErrors.processUnexpectedEmptyValue(reject: reject, parameterName: parameterName)
    } catch {
      BlobCourierErrors.processUnexpectedException(reject: reject, error: error)
      print("\(error)")
    }
  }

  @objc(uploadBlob:withResolver:withRejecter:)
  func uploadBlob(
    input: NSDictionary,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    do {
      try assertRequiredParameter(
        input: input, type: "NSDictionary", parameterName: BlobCourier.parameterParts)
      try assertRequiredParameter(
        input: input, type: "String", parameterName: BlobCourier.parameterTaskId)
      try assertRequiredParameter(
        input: input, type: "String", parameterName: BlobCourier.parameterUrl)

      try uploadBlobFromValidatedParameters(input: input, resolve: resolve, reject: reject)
    } catch BlobCourierErrors.BlobCourierError.requiredParameter(let parameterName) {
      BlobCourierErrors.processUnexpectedEmptyValue(reject: reject, parameterName: parameterName)
    } catch {
      BlobCourierErrors.processUnexpectedException(reject: reject, error: error)
    }
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
    append(string: "\r\n--\(part.boundary)\r\n")
    append(
      string: "Content-Disposition: form-data; name=\"\(part.paramName)\"\r\n")
    append(string: "Content-Length: \(part.value.count)\r\n")
    append(string: "\r\n")
    append(string: part.value)
  }

  mutating func addFilePart(part: FilePart) throws {
    let fileUrl = URL(string: part.absoluteFilePath)!
    let fileData = try Data(contentsOf: fileUrl)

    let maybeFileAttributes = try? FileManager.default.attributesOfItem(atPath: part.absoluteFilePath)

    append(string: "\r\n--\(part.boundary)\r\n")
    append(
      "Content-Disposition: form-data; name=\"\(part.paramName)\"; filename=\"\(part.filename)\"\r\n"
        .data(using: .utf8)!)
    append(string: "Content-Type: \(part.mimeType)\r\n")

    if let fileSize = maybeFileAttributes?[.size] {
      append(string: "Content-Length: \(fileSize)\r\n")
    }

    append(string: "\r\n")
    append(fileData)
  }
}
