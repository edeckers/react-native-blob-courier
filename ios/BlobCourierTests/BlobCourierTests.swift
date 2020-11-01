//  Copyright (c) Ely Deckers.
// 
//  This source code is licensed under the MPL-2.0 license found in the
//  LICENSE file in the root directory of this source tree.

import XCTest

@testable import BlobCourier

class BlobCourierTests: XCTestCase {
    static let defaultPromiseTimeoutSeconds: UInt32 = 10

    var sut : BlobCourier? = nil

    override func setUpWithError() throws {
        try super.setUpWithError()
        sut = BlobCourier()
    }

    override func tearDownWithError() throws {
        sut = nil
        try super.tearDownWithError()
    }

    func testAllRequiredParametersProvidedResolvesPromise() throws {
        
        let input : NSDictionary = [
          "filename": "some-filename.png",
          "taskId": "some-task-id",
          "url": "https://github.com/edeckers/react-native-blob-courier",
        ]
        let resolve: RCTPromiseResolveBlock = { (s:Any?) -> Void in XCTAssertTrue(true) }
        let reject: RCTPromiseRejectBlock = { (s:String?, t:String?, o:Error?) -> Void in XCTAssertTrue(false) }

        sut?.fetchBlob(input: input, resolve: resolve, reject: reject)

        sleep(BlobCourierTests.defaultPromiseTimeoutSeconds)
    }
}
