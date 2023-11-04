//  Copyright (c) Ely Deckers.
// 
//  This source code is licensed under the MPL-2.0 license found in the
//  LICENSE file in the root directory of this source tree.
import Foundation

struct SendParameters {
  let absoluteFilePath: String
  let headers: NSDictionary
  let method: String
  let progressIntervalMilliseconds: Int
  let returnResponse: Bool
  let taskId: String
  let url: URL

  init(
    absoluteFilePath: String,
    headers: NSDictionary,
    method: String,
    progressIntervalMilliseconds: Int,
    returnResponse: Bool,
    taskId: String,
    url: URL) {
    self.absoluteFilePath = absoluteFilePath
    self.headers = headers
    self.method = method
    self.progressIntervalMilliseconds = progressIntervalMilliseconds
    self.returnResponse = returnResponse
    self.taskId = taskId
    self.url = url
  }
}
