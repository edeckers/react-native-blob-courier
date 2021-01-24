//
//  Copyright (c) Ely Deckers.
// 
//  This source code is licensed under the MPL-2.0 license found in the
//  LICENSE file in the root directory of this source tree.
import Foundation

struct DownloadParameters {
  let filename: String
  let taskId: String
  let url: String

  init(filename: String, taskId: String, url: String) {
    self.filename = filename
    self.taskId = taskId
    self.url = url
  }
}
