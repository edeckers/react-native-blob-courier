// Copyright (c) Ely Deckers.
//
// This source code is licensed under the MPL-2.0 license found in the
// LICENSE file in the root directory of this source tree.
import Foundation
import React

open class BlobCourierEventEmitter: RCTEventEmitter {
  public static let eventProgress = "BlobCourierProgress"

  public static var shared: BlobCourierEventEmitter?

  override init() {
      super.init()

      BlobCourierEventEmitter.shared = self
  }

  open override func supportedEvents() -> [String]! {
    return [BlobCourierEventEmitter.eventProgress]
  }
}
