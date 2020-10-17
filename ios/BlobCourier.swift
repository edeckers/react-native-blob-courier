/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
 import Foundation

@objc(BlobCourier)
class BlobCourier: NSObject {
  static let ERROR_MISSING_REQUIRED_PARAM = "ERROR_MISSING_REQUIRED_PARAM"
  static let ERROR_UNEXPECTED_EXCEPTION = "ERROR_UNEXPECTED_EXCEPTION"

  enum BlobCourierError: Error {
    case withMessage(code: String, message: String)
  }

  static let PARAM_FILENAME = "filename"
  static let PARAM_FILE_PATH = "filePath"
  static let PARAM_METHOD = "method"
  static let PARAM_URL = "url"

  static let DEFAULT_METHOD = "GET"

  static let REQUIRED_PARAMETER_PROCESSOR = [
    "Boolean": { (input: NSDictionary, parameterName: String) in return input[parameterName]! },
    "String": { (input: NSDictionary, parameterName: String) in return input[parameterName]! },
  ]

  func assertRequiredParameter(input: NSDictionary, type: String, parameterName: String) throws {
    let maybeValue = try
      (BlobCourier.REQUIRED_PARAMETER_PROCESSOR[type] ?? { (_, _) in
        throw BlobCourierError.withMessage(
          code: BlobCourier.ERROR_MISSING_REQUIRED_PARAM,
          message:
            "No processor defined for type `\(type)`, valid options: \(BlobCourier.REQUIRED_PARAMETER_PROCESSOR.keys as! [String])"
        )
      })(input, parameterName)

    if maybeValue == nil {
      throw BlobCourierError.withMessage(
        code: BlobCourier.ERROR_MISSING_REQUIRED_PARAM,
        message: "`\(parameterName)` is a required parameter of type `\(type)`")
    }
  }

  func fetchBlobFromValidatedParameters(
    input: NSDictionary, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock
  ) throws {
    let url = (input[BlobCourier.PARAM_URL] as? String) ?? ""

    let urlObject = URL(string: url)

    let filename = (input[BlobCourier.PARAM_FILENAME] as? String) ?? ""

    let documentsUrl: URL = try FileManager.default.url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: false)
    let destinationFileUrl = documentsUrl.appendingPathComponent(filename)

    let fileURL = URL(string: url)
    let sessionConfig = URLSessionConfiguration.default
    let session = URLSession(configuration: sessionConfig)
    let request = URLRequest(url: fileURL!)

    let task = session.downloadTask(with: request) { (location, response, error) in
      if error == nil {
       let result : NSDictionary = [
         "type": "Http",
         "response": [
           "filePath": "\(destinationFileUrl)",
           "response": [
             "code": 200
           ]
         ],
       ]

        if let statusCode = (response as? HTTPURLResponse)?.statusCode {
          print("Successfully downloaded. Status code: \(statusCode)")
          do {
            try? FileManager.default.removeItem(at: destinationFileUrl)
            try FileManager.default.copyItem(at: location!, to: destinationFileUrl)
            print("Successfully moved file to \(destinationFileUrl)")
            resolve(result)
          } catch (let writeError) {
            let error = NSError(domain: "io.deckers.blob_courier", code: 0, userInfo: [NSLocalizedDescriptionKey: "teh error."])
            print("Error creating a file \(destinationFileUrl) : \(writeError)")
            reject("nonono", "bbbb", error)
          }
         }
     } else {
        print(
          "Error took place while downloading a file. Error description: \(error?.localizedDescription ?? "")"
        )

        let error = NSError(domain: "io.deckers.blob_courier", code: 0, userInfo: [NSLocalizedDescriptionKey: "teh error."])

        reject("nonono", "bbbb", error)
      }
    }
    task.resume()
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
    let url = (input[BlobCourier.PARAM_URL] as? String) ?? ""

    let urlObject = URL(string: url)!

    let filePath = (input[BlobCourier.PARAM_FILE_PATH] as? String) ?? ""

    let filePathObject = URL(string: filePath)!

    let sessionConfig = URLSessionConfiguration.default
    let session = URLSession(configuration: sessionConfig)

    let (request, fileData) = buildRequestDataForFileUpload(url: urlObject, fileUrl: filePathObject)

    let task = session.uploadTask(with: request, from: fileData) { (data, response, error) in
        let error0 = NSError(domain: "io.deckers.blob_courier", code: 0, userInfo: [NSLocalizedDescriptionKey: "teh error."])

      if error == nil {
        if let statusCode = (response as? HTTPURLResponse)?.statusCode {
          print("Successfully uploaded Status code: \(statusCode)")
          let rawResponse = String(data: data!, encoding: String.Encoding.utf8)
          resolve(rawResponse)
          return
        }

        reject("nonono", "aaaa", error0)
     } else {
        print(
          "Error took place while uploading a file. Error description: \(error?.localizedDescription ?? "")"
        )
        reject("nonono", error?.localizedDescription, error)
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
        input: input, type: "String", parameterName: BlobCourier.PARAM_FILENAME)
      try assertRequiredParameter(
        input: input, type: "String", parameterName: BlobCourier.PARAM_URL)

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
        input: input, type: "String", parameterName: BlobCourier.PARAM_FILE_PATH)
      try assertRequiredParameter(
        input: input, type: "String", parameterName: BlobCourier.PARAM_URL)

      try uploadBlobFromValidatedParameters(input: input, resolve: resolve, reject: reject)
    } catch {
      print("\(error)")
    }
  }
}
