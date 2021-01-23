//  Copyright (c) Ely Deckers.
// 
//  This source code is licensed under the MPL-2.0 license found in the
//  LICENSE file in the root directory of this source tree.
import Foundation

@objc(BlobCourier)
open class BlobCourier: NSObject {
  @objc(fetchBlob:withResolver:withRejecter:)
  func fetchBlob(
    input: NSDictionary, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock
  ) {
    do {
      try Errors.assertRequiredParameter(
        input: input, type: "String", parameterName: Constants.parameterFilename)
      try Errors.assertRequiredParameter(
        input: input, type: "String", parameterName: Constants.parameterTaskId)
      try Errors.assertRequiredParameter(
        input: input, type: "String", parameterName: Constants.parameterUrl)

      let result: NSDictionary = try BlobDownloader.fetchBlobFromValidatedParameters(input: input, reject: reject)

      return resolve(result)
    } catch Errors.BlobCourierError.requiredParameter(let parameterName) {
      Errors.processUnexpectedEmptyValue(reject: reject, parameterName: parameterName)
    } catch {
      Errors.processUnexpectedException(reject: reject, error: error)
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
      try Errors.assertRequiredParameter(
        input: input, type: "NSArray", parameterName: Constants.parameterParts)
      try Errors.assertRequiredParameter(
        input: input, type: "String", parameterName: Constants.parameterTaskId)
      try Errors.assertRequiredParameter(
        input: input, type: "String", parameterName: Constants.parameterUrl)

      try BlobUploader.uploadBlobFromValidatedParameters(input: input, resolve: resolve, reject: reject)
    } catch Errors.BlobCourierError.requiredParameter(let parameterName) {
      Errors.processUnexpectedEmptyValue(reject: reject, parameterName: parameterName)
    } catch {
      Errors.processUnexpectedException(reject: reject, error: error)
    }
  }
}
