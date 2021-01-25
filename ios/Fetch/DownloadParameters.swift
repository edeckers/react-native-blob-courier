//  Copyright (c) Ely Deckers.
// 
//  This source code is licensed under the MPL-2.0 license found in the
//  LICENSE file in the root directory of this source tree.
import Foundation

struct DownloadParameters {
  let filename: String
  let headers: NSDictionary
  let progressIntervalMilliseconds: Int
  let targetDirectory: URL
  let taskId: String
  let url: URL

  init(
    filename: String,
    headers: NSDictionary,
    progressIntervalMilliseconds: Int,
    targetDirectory: URL,
    taskId: String,
    url: URL) {
    self.filename = filename
    self.headers = headers
    self.progressIntervalMilliseconds = progressIntervalMilliseconds
    self.targetDirectory = targetDirectory
    self.taskId = taskId
    self.url = url
  }
}
