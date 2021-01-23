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
        input: input, type: "NSArray", parameterName: BlobCourier.parameterParts)
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
