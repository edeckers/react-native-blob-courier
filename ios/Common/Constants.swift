//  Copyright (c) Ely Deckers.
// 
//  This source code is licensed under the MPL-2.0 license found in the
//  LICENSE file in the root directory of this source tree.
import Foundation

open class Constants: NSObject {
  static let downloadTypeUnmanaged  = "Unmanaged"

  static let libraryDomain  = "io.deckers.blob_courier"

  static let messageCancelRequest = "\(libraryDomain).CancelRequest"

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

  static let defaultUploadMethod = "POST"
  static let defaultMimeType = "application/octet-stream"
  static let defaultProgressIntervalMilliseconds = 500
  static let defaultTarget = targetCache
}
