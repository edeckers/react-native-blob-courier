//  Copyright (c) Ely Deckers.
// 
//  This source code is licensed under the MPL-2.0 license found in the
//  LICENSE file in the root directory of this source tree.
import Foundation

open class RequestCanceller: NSObject {
  static func cancelRequestFromValidatedParameters(parameters: CancelParameters) ->
    Result<NSDictionary, BlobCourierError> {

    NotificationCenter.default.post(
      name: Notification.Name(rawValue: Constants.messageCancelRequest),
      object: nil,
      userInfo: ["taskId": parameters.taskId])

    return .success([:])
  }
}
