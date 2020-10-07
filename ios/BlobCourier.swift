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
    input: NSDictionary, resolve: RCTPromiseResolveBlock, reject: RCTPromiseRejectBlock
  ) throws {
    let url = (input[BlobCourier.PARAM_URL] as? String) ?? ""

    let urlObject = URL(string: url)

    let filename = (input[BlobCourier.PARAM_FILENAME] as? String) ?? ""

    let documentsUrl: URL = FileManager.default.urls(for: .downloadsDirectory, in: .userDomainMask)
      .first!
    let destinationFileUrl = documentsUrl.appendingPathComponent("\(filename)")

    let fileURL = URL(string: url)
    let sessionConfig = URLSessionConfiguration.default
    let session = URLSession(configuration: sessionConfig)
    let request = URLRequest(url: fileURL!)

    let task = session.downloadTask(with: request) { (tempLocalUrl, response, error) in
      if let tempLocalUrl = tempLocalUrl, error == nil {
        if let statusCode = (response as? HTTPURLResponse)?.statusCode {
          print("Successfully downloaded. Status code: \(statusCode)")
        }

        do {
          try FileManager.default.copyItem(at: tempLocalUrl, to: destinationFileUrl)
       } catch (let writeError) {
          print("Error creating a file \(destinationFileUrl) : \(writeError)")
        }
      } else {
        print(
          "Error took place while downloading a file. Error description: \(error?.localizedDescription ?? "")"
        )
      }
    }
    task.resume()

    resolve(true)
  }

  @objc(fetchBlob:withResolver:withRejecter:)
  func fetchBlob(
    input: NSDictionary, resolve: RCTPromiseResolveBlock, reject: RCTPromiseRejectBlock
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
}
