//  Copyright (c) Ely Deckers.
// 
//  This source code is licensed under the MPL-2.0 license found in the
//  LICENSE file in the root directory of this source tree.
import Foundation

struct CancelParameters {
  let taskId: String

  init(taskId: String) {
    self.taskId = taskId
  }
}
