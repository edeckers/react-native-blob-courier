//  Copyright (c) Ely Deckers.
// 
//  This source code is licensed under the MPL-2.0 license found in the
//  LICENSE file in the root directory of this source tree.
import Foundation

@objc(BlobCourier)
open class BlobCourier: NSObject {
  @objc static func requiresMainQueueSetup() -> Bool {
    return false
  }

  @objc(cancelRequest:withResolver:withRejecter:)
  func cancelRequest(
    input: NSDictionary, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock
  ) {
    DispatchQueue.global(qos: .background).async {
      do {
        let errorOrParameters = CancelParameterFactory.fromInput(input: input)

        if case .failure(let error) = errorOrParameters { reject(error.code, error.message, error.error) }
        guard case .success(let parameters) = errorOrParameters else { return }

        let result = RequestCanceller.cancelRequestFromValidatedParameters(parameters: parameters)

        switch result {
        case .success(let success):
          resolve(success)
        case .failure(let error):
          reject(error.code, error.message, error.error)
        }
      } catch {
        let unexpectedError = Errors.createUnexpectedError(error: error)

        reject(unexpectedError.code, unexpectedError.message, unexpectedError.error)
      }
    }
  }

  @objc(fetchBlob:withResolver:withRejecter:)
  func fetchBlob(
    input: NSDictionary, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock
  ) {
    DispatchQueue.global(qos: .background).async {
      do {
        let errorOrParameters = DownloaderParameterFactory.fromInput(input: input)

        if case .failure(let error) = errorOrParameters { reject(error.code, error.message, error.error) }
        guard case .success(let parameters) = errorOrParameters else { return }

        let result = BlobDownloader.fetchBlobFromValidatedParameters(parameters: parameters)

        switch result {
        case .success(let success):
          resolve(success)
        case .failure(let error):
          reject(error.code, error.message, error.error)
        }
      } catch {
        let unexpectedError = Errors.createUnexpectedError(error: error)

        reject(unexpectedError.code, unexpectedError.message, unexpectedError.error)
      }
    }
  }

  @objc(uploadBlob:withResolver:withRejecter:)
  func uploadBlob(
    input: NSDictionary,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    DispatchQueue.global(qos: .background).async {
      do {
        let errorOrParameters = UploaderParameterFactory.fromInput(input: input)

        if case .failure(let error) = errorOrParameters { reject(error.code, error.message, error.error) }
        guard case .success(let parameters) = errorOrParameters else { return }

        let result = BlobUploader.uploadBlobFromValidatedParameters(parameters: parameters)

        switch result {
        case .success(let success):
          resolve(success)
        case .failure(let error):
          reject(error.code, error.message, error.error)
        }
      } catch {
        let unexpectedError = Errors.createUnexpectedError(error: error)

        reject(unexpectedError.code, unexpectedError.message, unexpectedError.error)
      }
    }
  }
}
