//  Copyright (c) Ely Deckers.
// 
//  This source code is licensed under the MPL-2.0 license found in the
//  LICENSE file in the root directory of this source tree.
import Foundation

open class CancelController: NSObject {
    static func registerCancelObserver(session: URLSession, taskId: String) -> NSObjectProtocol? {
    return NotificationCenter.default.addObserver(
      forName: Notification.Name(rawValue: Constants.messageCancelRequest),
      object: nil,
      queue: nil) { notification in
        guard let data = notification.userInfo as? [String: String] else { return }
        guard let needleId = data["taskId"] else { return }

        if needleId != taskId {
          print("Not cancelling task (id=\(taskId),needleId=\(needleId))")
          return
        }

        print("Cancelling task (id=\(taskId))")

        DispatchQueue.global(qos: .background).async {
          session.invalidateAndCancel()
          print("Cancelled task (id=\(taskId))")
        }
      }
  }
}
