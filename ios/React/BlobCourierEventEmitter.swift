// Copyright (c) Ely Deckers.
//
// This source code is licensed under the MPL-2.0 license found in the
// LICENSE file in the root directory of this source tree.
import Foundation

@objc(BlobCourierEventEmitter)
open class BlobCourierEventEmitter: RCTEventEmitter {
  public static let eventProgress = "BlobCourierProgress"

  public static var shared: BlobCourierEventEmitter?

  @objc public override static func requiresMainQueueSetup() -> Bool {
    return false
  }

  override init() {
      super.init()

      BlobCourierEventEmitter.shared = self
  }

  open override func supportedEvents() -> [String]! {
    return [BlobCourierEventEmitter.eventProgress]
  }
}
