//
// Copyright (c) Ely Deckers.
//
// This source code is licensed under the MPL-2.0 license found in the
// LICENSE file in the root directory of this source tree.
//
import Foundation

open class BlobCourierDelayedEventEmitter: NSObject {
  let taskId: String
  let eventEmitter: BlobCourierEventEmitter?
  let progressIntervalMilliseconds: Int

  var lastProgressUpdate: Date = Date()

  init(taskId: String, progressIntervalMilliseconds: Int) {
    self.taskId = taskId

    self.eventEmitter = BlobCourierEventEmitter.shared

    self.progressIntervalMilliseconds = progressIntervalMilliseconds
  }

  public func notifyBridgeOfProgress(totalBytesWritten: Int64, totalBytesExpectedToWrite: Int64) {
    let isDownloadFinished = totalBytesWritten == totalBytesExpectedToWrite
    let hasTimeoutPassed =
      Int(Date().timeIntervalSince(self.lastProgressUpdate) * 1000) > self.progressIntervalMilliseconds
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
