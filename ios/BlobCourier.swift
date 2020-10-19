/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
import Foundation

@objc(BlobCourier)
open class BlobCourier: NSObject {

 static let DOWNLOAD_TYPE_UNMANAGED  = "Unmanaged"

  static let LIBRARY_DOMAIN  = "io.deckers.blob_courier"

  static let PARAMETER_FILENAME = "filename"
  static let PARAMETER_FILE_PATH = "filePath"
  static let PARAMETER_METHOD = "method"
  static let PARAMETER_TASK_ID = "taskId"
  static let PARAMETER_URL = "url"

  static let DEFAULT_METHOD = "GET"

  static let REQUIRED_PARAMETER_PROCESSOR = [
    "Boolean": { (input: NSDictionary, parameterName: String) in return input[parameterName]! },
    "String": { (input: NSDictionary, parameterName: String) in return input[parameterName]! },
  ]

  func assertRequiredParameter(input: NSDictionary, type: String, parameterName: String) throws {
    let maybeValue = try
      (BlobCourier.REQUIRED_PARAMETER_PROCESSOR[type] ?? { (_, _) in
        throw BlobCourierErrors.BlobCourierError.withMessage(
          code: BlobCourierErrors.ERROR_MISSING_REQUIRED_PARAMETER,
          message:
            "No processor defined for type `\(type)`, valid options: \(BlobCourier.REQUIRED_PARAMETER_PROCESSOR.keys as! [String])"
        )
      })(input, parameterName)

    if maybeValue == nil {
      throw BlobCourierErrors.BlobCourierError.withMessage(
        code: BlobCourierErrors.ERROR_MISSING_REQUIRED_PARAMETER,
        message: "`\(parameterName)` is a required parameter of type `\(type)`")
    }
  }

  func processUnexpectedException(
    reject: @escaping RCTPromiseRejectBlock, e: NSError?
  ) {
    reject(BlobCourierErrors.ERROR_UNEXPECTED_EXCEPTION, "An unexpected exception occurred: \(e?.localizedDescription ?? "")", e)
  }

  func processUnexpectedEmptyValue(
    reject: @escaping RCTPromiseRejectBlock, parameterName: String
  ) {
    reject(BlobCourierErrors.ERROR_UNEXPECTED_EMPTY_VALUE, "Parameter `\(parameterName)` cannot be empty.", nil)
  }

  func fetchBlobFromValidatedParameters(
    input: NSDictionary, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock
  ) throws {
    let taskId = (input[BlobCourier.PARAMETER_TASK_ID] as? String) ?? ""

    let url = (input[BlobCourier.PARAMETER_URL] as? String) ?? ""

    let urlObject = URL(string: url)

    let filename = (input[BlobCourier.PARAMETER_FILENAME] as? String) ?? ""

    let documentsUrl: URL = try FileManager.default.url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: false)
    let destinationFileUrl = documentsUrl.appendingPathComponent(filename)

    let fileURL = URL(string: url)
    let sessionConfig = URLSessionConfiguration.default
    let downloaderDelegate = DownloaderDelegate(taskId: taskId, destinationFileUrl: destinationFileUrl, resolve: resolve, reject: reject)
    let session = URLSession(configuration: sessionConfig, delegate: downloaderDelegate, delegateQueue: nil)
    let request = URLRequest(url: fileURL!)

    session.downloadTask(with: request).resume()
  }

  func buildRequestDataForFileUpload(url:URL, fileUrl:URL) -> (URLRequest, Data) {
    // https://igomobile.de/2020/06/16/swift-upload-a-file-with-multipart-form-data-in-ios-using-uploadtask-and-urlsession/

    let boundary = UUID().uuidString
             
    var request = URLRequest(url: url)
    request.httpMethod = "POST"

    request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")

    let fileName = fileUrl.lastPathComponent
    let mimetype = "application/octet-stream"
    let paramName = "file"
    let fileData = try? Data(contentsOf: fileUrl)

    var data = Data()

    data.append("\r\n--\(boundary)\r\n".data(using: .utf8)!)
    data.append("Content-Disposition: form-data; name=\"\(paramName)\"; filename=\"\(fileName)\"\r\n".data(using: .utf8)!)
    data.append("Content-Type: \(mimetype)\r\n\r\n".data(using: .utf8)!)
    data.append(fileData!)
    data.append("\r\n--\(boundary)--\r\n".data(using: .utf8)!)

    request.setValue(String(data.count), forHTTPHeaderField: "Content-Length")

    return (request, data)
  }

  func uploadBlobFromValidatedParameters(
    input: NSDictionary, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock
  ) throws {
    print("Start uploadBlobFromValidatedParameters")
    let url = (input[BlobCourier.PARAMETER_URL] as? String) ?? ""

    let urlObject = URL(string: url)!

    let filePath = (input[BlobCourier.PARAMETER_FILE_PATH] as? String) ?? ""

    let filePathObject = URL(string: filePath)!

    let taskId = (input[BlobCourier.PARAMETER_TASK_ID] as? String) ?? ""

    let sessionConfig = URLSessionConfiguration.default
    let session = URLSession(configuration: sessionConfig)

    let (request, fileData) = buildRequestDataForFileUpload(url: urlObject, fileUrl: filePathObject)

    let task = session.uploadTask(with: request, from: fileData) { (data, response, error) in
      if error == nil {
        if let statusCode = (response as? HTTPURLResponse)?.statusCode {
          let rawResponse = String(data: data!, encoding: String.Encoding.utf8)

          let result : NSDictionary = [
            "type": BlobCourier.DOWNLOAD_TYPE_UNMANAGED,
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

        self.processUnexpectedException(reject: reject, e: noStatusCodeError)
     } else {
        print(
          "Error took place while uploading a file. Error description: \(error?.localizedDescription ?? "")"
        )
        self.processUnexpectedException(reject: reject, e: error as NSError?)
      }
    }
    task.resume()
  }

  @objc(fetchBlob:withResolver:withRejecter:)
  func fetchBlob(
    input: NSDictionary, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock
  ) {
    do {
      try assertRequiredParameter(
        input: input, type: "String", parameterName: BlobCourier.PARAMETER_FILENAME)
      try assertRequiredParameter(
        input: input, type: "String", parameterName: BlobCourier.PARAMETER_TASK_ID)
      try assertRequiredParameter(
        input: input, type: "String", parameterName: BlobCourier.PARAMETER_URL)

      try fetchBlobFromValidatedParameters(input: input, resolve: resolve, reject: reject)
    } catch {
      print("\(error)")
    }
  }

  @objc(uploadBlob:withResolver:withRejecter:)
  func uploadBlob(
    input: NSDictionary, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock
  ) {
    print("Start uploadBlob")
    do {
      try assertRequiredParameter(
        input: input, type: "String", parameterName: BlobCourier.PARAMETER_FILE_PATH)
      try assertRequiredParameter(
        input: input, type: "String", parameterName: BlobCourier.PARAMETER_TASK_ID)
      try assertRequiredParameter(
        input: input, type: "String", parameterName: BlobCourier.PARAMETER_URL)


      try uploadBlobFromValidatedParameters(input: input, resolve: resolve, reject: reject)
    } catch {
      print("\(error)")
    }
  }
}
