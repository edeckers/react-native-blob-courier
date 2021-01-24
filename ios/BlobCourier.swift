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

      let result: Result<NSDictionary, Error> =
        try BlobDownloader.fetchBlobFromValidatedParameters(input: input)

      switch result {
      case .success(let success):
        resolve(success)
      case .failure(let error):
        reject("FIX_THIS_CODE", "FIX_THIS_MESSAGE", nil) // FIXME ED Process error
      }
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

      let result = try BlobUploader.uploadBlobFromValidatedParameters(input: input)

      switch result {
      case .success(let success):
        resolve(success)
      case .failure(let error):
        reject("FIX_THIS_CODE", "FIX_THIS_MESSAGE", nil) // FIXME ED Process error
      }
     } catch Errors.BlobCourierError.requiredParameter(let parameterName) {
      Errors.processUnexpectedEmptyValue(reject: reject, parameterName: parameterName)
    } catch {
      Errors.processUnexpectedException(reject: reject, error: error)
    }
  }
}
