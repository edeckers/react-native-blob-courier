//  Copyright (c) Ely Deckers.
// 
//  This source code is licensed under the MPL-2.0 license found in the
//  LICENSE file in the root directory of this source tree.
import Foundation

struct UploadParameters {
  let headers: NSDictionary
  let parts: NSArray
  let progressIntervalMilliseconds: Int
  let returnResponse: Bool
  let taskId: String
  let url: URL

  init(
    headers: NSDictionary,
    parts: NSArray,
    progressIntervalMilliseconds: Int,
    returnResponse: Bool,
    taskId: String,
    url: URL) {
    self.headers = headers
    self.parts = parts
    self.progressIntervalMilliseconds = progressIntervalMilliseconds
    self.returnResponse = returnResponse
    self.taskId = taskId
    self.url = url
  }
}
