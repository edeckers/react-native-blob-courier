//  Copyright (c) Ely Deckers.
// 
//  This source code is licensed under the MPL-2.0 license found in the
//  LICENSE file in the root directory of this source tree.
import Foundation

@objc(BlobCourier)
open class BlobCourier: NSObject {

 static let downloadTypeUnmanaged  = "Unmanaged"

  static let libraryDomain  = "io.deckers.blob_courier"

  static let parameterFilename = "filename"
  static let parameterAbsoluteFilePath = "absoluteFilePath"
  static let parameterHeaders = "headers"
  static let parameterMethod = "method"
  static let parameterMimeType = "mimeType"
  static let parameterParts = "parts"
  static let parameterProgressInterval = "progressIntervalMilliseconds"
  static let parameterReturnResponse = "returnResponse"
  static let parameterTaskId = "taskId"
  static let parameterUrl = "url"

  static let defaultMethod = "GET"
  static let defaultMimeType = "application/octet-stream"
  static let defaultProgressIntervalMilliseconds = 500

  func assertRequiredParameter(input: NSDictionary, type: String, parameterName: String) throws {
    let maybeValue = input[parameterName]

    if maybeValue == nil {
      throw BlobCourierErrors.BlobCourierError.withMessage(
        code: BlobCourierErrors.errorMissingRequiredParameter,
        message: "`\(parameterName)` is a required parameter of type `\(type)`")
    }
  }

  func filterHeaders(unfilteredHeaders: NSDictionary) -> NSDictionary {
    Dictionary(uniqueKeysWithValues: unfilteredHeaders
      .map { key, value in (key as? String, value as? String) }
      .filter({ $0.1 != nil }))
      .mapValues { $0! } as NSDictionary
  }

  func fetchBlobFromValidatedParameters(
    input: NSDictionary,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) throws {
    let taskId = (input[BlobCourier.parameterTaskId] as? String) ?? ""

    let progressIntervalMilliseconds =
      (input[BlobCourier.parameterProgressInterval] as? Int) ??
        BlobCourier.defaultProgressIntervalMilliseconds

    let url = (input[BlobCourier.parameterUrl] as? String) ?? ""

    let filename = (input[BlobCourier.parameterFilename] as? String) ?? ""

    let headers =
      filterHeaders(unfilteredHeaders:
        (input[BlobCourier.parameterHeaders] as? NSDictionary) ??
        NSDictionary())

    let documentsUrl: URL =
      try FileManager.default.url(
        for: .documentDirectory,
        in: .userDomainMask,
        appropriateFor: nil,
        create: false)
    let destinationFileUrl = documentsUrl.appendingPathComponent(filename)

    let fileURL = URL(string: url)
    let sessionConfig = URLSessionConfiguration.default
    let downloaderDelegate =
      DownloaderDelegate(
        taskId: taskId,
        destinationFileUrl: destinationFileUrl,
        progressIntervalMilliseconds: progressIntervalMilliseconds,
        resolve: resolve,
        reject: reject)

    let session =
     URLSession(configuration: sessionConfig, delegate: downloaderDelegate, delegateQueue: nil)

    var request = URLRequest(url: fileURL!)
    for (key, value) in headers {
      if let headerKey = key as? String, let headerValue = value as? String {
        request.setValue(
          headerValue,
          forHTTPHeaderField: headerKey)
      }
    }

    session.downloadTask(with: request).resume()
  }

  func buildRequestDataForFileUpload(url: URL, parts: NSDictionary, headers: NSDictionary) -> (URLRequest, Data) {
    // https://igomobile.de/2020/06/16/swift-upload-a-file-with-multipart-form-data-in-ios-using-uploadtask-and-urlsession/
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

    for (key, value) in parts {
      if let paramName = key as? String, let part = value as? [String: String] {
        if part["type"] == "file" {
          let absoluteFilePath = part[BlobCourier.parameterAbsoluteFilePath]!

          let fileUrl = URL(string: absoluteFilePath)!
          let filename = part[BlobCourier.parameterFilename] ?? fileUrl.lastPathComponent
          let mimeType = part[BlobCourier.parameterMimeType] ?? BlobCourier.defaultMimeType

          data.addFilePart(
            boundary: boundary,
            paramName: paramName,
            absoluteFilePath: absoluteFilePath,
            filename: filename,
            mimeType: mimeType)
          continue
        }

        let formDataValue = part["value"] ?? ""
        data.addFormDataPart(boundary: boundary, paramName: paramName, value: formDataValue)
      }
    }

    request.setValue(String(data.count), forHTTPHeaderField: "Content-Length")

    return (request, data)
  }

  func uploadBlobFromValidatedParameters(
    input: NSDictionary, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock
  ) throws {
    print("Start uploadBlobFromValidatedParameters")
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

    let (request, fileData) = buildRequestDataForFileUpload(url: urlObject, parts: parts, headers: headers)

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
    } catch {
      BlobCourierErrors.processUnexpectedEmptyValue(reject: reject, parameterName: "TEST")
      print("\(error)")
    }
  }

  @objc(uploadBlob:withResolver:withRejecter:)
  func uploadBlob(
    input: NSDictionary,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    print("Start uploadBlob")
    do {
      try assertRequiredParameter(
        input: input, type: "NSDictionary", parameterName: BlobCourier.parameterParts)
      try assertRequiredParameter(
        input: input, type: "String", parameterName: BlobCourier.parameterTaskId)
      try assertRequiredParameter(
        input: input, type: "String", parameterName: BlobCourier.parameterUrl)

      try uploadBlobFromValidatedParameters(input: input, resolve: resolve, reject: reject)
    } catch {
      BlobCourierErrors.processUnexpectedEmptyValue(reject: reject, parameterName: "TEST")
      print("\(error)")
    }
  }
}

extension Data {
  mutating func append(string: String) {
    if let data = string.data(using: .utf8) {
      append(data)
    }
  }

  mutating func addFormDataPart(boundary: String, paramName: String, value: String) {
    append(string: "\r\n--\(boundary)\r\n")
    append(
      string: "Content-Disposition: form-data; name=\"\(paramName)\"\r\n")
    append(string: value)
    append(string: "\r\n--\(boundary)--\r\n")
  }

  mutating func addFilePart(
    boundary: String,
    paramName: String,
    absoluteFilePath: String,
    filename: String,
    mimeType: String) {

    let fileUrl = URL(string: absoluteFilePath)!
    let fileData = try? Data(contentsOf: fileUrl)

    append(string: "\r\n--\(boundary)\r\n")
    append(
      "Content-Disposition: form-data; name=\"\(paramName)\"; filename=\"\(filename)\"\r\n"
        .data(using: .utf8)!)
    append(string: "Content-Type: \(mimeType)\r\n\r\n")
    append(fileData!)
    append(string: "\r\n--\(boundary)--\r\n")
  }
}
