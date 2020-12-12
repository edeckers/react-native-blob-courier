//  Copyright (c) Ely Deckers.
// 
//  This source code is licensed under the MPL-2.0 license found in the
//  LICENSE file in the root directory of this source tree.

import XCTest

@testable import BlobCourier

class BlobCourierTests: XCTestCase {
    static let defaultPromiseTimeoutSeconds: UInt32 = 10

    var sut: BlobCourier?

    override func setUpWithError() throws {
        try super.setUpWithError()
        sut = BlobCourier()
    }

    override func tearDownWithError() throws {
        sut = nil
        try super.tearDownWithError()
    }

    func testAllRequiredFetchParametersProvidedResolvesPromise() throws {
        let input: NSDictionary = [
          "filename": "some-filename.png",
          "taskId": "some-task-id",
          "url": "https://github.com/edeckers/react-native-blob-courier"
        ]
        let resolve: RCTPromiseResolveBlock = { (_: Any?) -> Void in XCTAssertTrue(true) }
        let reject: RCTPromiseRejectBlock = { (_: String?, _: String?, _: Error?) -> Void in
          XCTAssertTrue(false) }

        sut?.fetchBlob(input: input, resolve: resolve, reject: reject)

        sleep(BlobCourierTests.defaultPromiseTimeoutSeconds)
    }

    func testMissingRequiredFetchParametersRejectsPromise() throws {
        let input: NSDictionary = [
          "taskId": "some-task-id",
          "url": "https://github.com/edeckers/react-native-blob-courier"
        ]
        let resolve: RCTPromiseResolveBlock = { (_: Any?) -> Void in XCTAssertTrue(false) }
        let reject: RCTPromiseRejectBlock = { (_: String?, _: String?, _: Error?) -> Void in XCTAssertTrue(true) }

        sut?.fetchBlob(input: input, resolve: resolve, reject: reject)

        sleep(BlobCourierTests.defaultPromiseTimeoutSeconds)
    }

    func testAllRequiredUploadParametersProvidedResolvesPromise() throws {
        let input: NSDictionary = [
          "filename": "some-filename.png",
          "taskId": "some-task-id",
          "url": "https://github.com/edeckers/react-native-blob-courier"
        ]
        let reject: RCTPromiseRejectBlock = { (_: String?, _: String?, _: Error?) -> Void in XCTAssertTrue(false) }
        let resolveUpload: RCTPromiseResolveBlock = { (_: Any?) -> Void in XCTAssertTrue(true) }
        let resolve: RCTPromiseResolveBlock = { (response: Any?) -> Void in
            let dict = response as? NSDictionary ?? [:]
            let data = dict["data"] as? NSDictionary ?? [:]

            self.sut?.uploadBlob(input: [
             "parts": [
               "file": [
                 "absoluteFilePath": data["absoluteFilePath"] ?? "",
                 "mimeType": "image/png"
               ]
             ],
             "taskId": data["taskId"] ?? "",
             "url": "https://file.io"
           ], resolve: resolveUpload, reject: reject)
        }

        sut?.fetchBlob(input: input, resolve: resolve, reject: reject)

        sleep(BlobCourierTests.defaultPromiseTimeoutSeconds)
    }

    func testMissingRequiredUploadParametersRejectsPromise() throws {
        let input: NSDictionary = [
          "filename": "some-filename.png",
          "taskId": "some-task-id",
          "url": "https://github.com/edeckers/react-native-blob-courier"
        ]
        let reject: RCTPromiseRejectBlock = { (_: String?, _: String?, _: Error?) -> Void in XCTAssertTrue(true) }
        let resolveUpload: RCTPromiseResolveBlock = { (_: Any?) -> Void in XCTAssertTrue(false) }
        let resolve: RCTPromiseResolveBlock = { (response: Any?) -> Void in
            let dict = response as? NSDictionary ?? [:]
            let data = dict["data"] as? NSDictionary ?? [:]

            self.sut?.uploadBlob(input: [
             "parts": [
               "file": [
                 "absoluteFilePath": data["absoluteFilePath"] ?? "",
                 "mimeType": "image/png"
               ]
             ],
             "taskId": data["taskId"] ?? ""
           ], resolve: resolveUpload, reject: reject)
        }

        sut?.fetchBlob(input: input, resolve: resolve, reject: reject)

        sleep(BlobCourierTests.defaultPromiseTimeoutSeconds)
    }
}
