//
// Copyright (c) Ely Deckers.
//
// This source code is licensed under the MPL-2.0 license found in the
// LICENSE file in the root directory of this source tree.
//
import Foundation

@objc(BlobCourierDelayedEventEmitter)
open class BlobCourierDelayedEventEmitter: NSObject {
  let taskId: String
  let eventEmitter: BlobCourierEventEmitter?
  let updateTimeoutMilliseconds: Int

  var lastProgressUpdate: Date = Date()

  init(taskId: String, updateTimeoutMilliseconds: Int = 200) {
    self.taskId = taskId

    self.eventEmitter = BlobCourierEventEmitter.shared

    self.updateTimeoutMilliseconds = updateTimeoutMilliseconds
  }

  public func notifyBridgeOfProgress(totalBytesWritten: Int64, totalBytesExpectedToWrite: Int64) {
    let isDownloadFinished = totalBytesWritten == totalBytesExpectedToWrite
    let hasTimeoutPassed =
      Int(Date().timeIntervalSince(self.lastProgressUpdate) * 1000) > self.updateTimeoutMilliseconds
    let shouldUpdate = isDownloadFinished || hasTimeoutPassed

    if !shouldUpdate {
      return
    }

    self.eventEmitter?.sendEvent(
     withName: BlobCourierEventEmitter.eventProgress,
     body: ["taskId": self.taskId, "total": totalBytesExpectedToWrite, "written": totalBytesWritten])
    self.lastProgressUpdate = Date()
  }
}
