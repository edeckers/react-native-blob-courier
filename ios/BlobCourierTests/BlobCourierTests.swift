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
                 "payload": [
                    "absoluteFilePath": data["absoluteFilePath"] ?? "",
                    "mimeType": "image/png"
                 ],
                 "type": "file"
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
                 "payload": [
                   "absoluteFilePath": data["absoluteFilePath"] ?? "",
                   "mimeType": "image/png"
                 ],
                 "type": "file"
               ]
             ],
             "taskId": data["taskId"] ?? ""
           ], resolve: resolveUpload, reject: reject)
        }

        sut?.fetchBlob(input: input, resolve: resolve, reject: reject)

        sleep(BlobCourierTests.defaultPromiseTimeoutSeconds)
    }

    func testValidTargetParametersResolvesPromise() throws {
        for target in ["cache", "data"] {
          let input: NSDictionary = [
            "filename": "some-filename.png",
            "ios": [
              "target": target
            ],
            "taskId": "some-task-id",
            "url": "https://github.com/edeckers/react-native-blob-courier"
          ]
          let resolve: RCTPromiseResolveBlock = { (_: Any?) -> Void in XCTAssertTrue(true) }
          let reject: RCTPromiseRejectBlock = { (_: String?, _: String?, _: Error?) -> Void in XCTAssertTrue(false) }

          sut?.fetchBlob(input: input, resolve: resolve, reject: reject)

          sleep(BlobCourierTests.defaultPromiseTimeoutSeconds)
        }
    }

    func testInvalidTargetParametersRejectsPromise() throws {
        let target = "SOME_INVALID_VALUE"

        let input: NSDictionary = [
          "filename": "some-filename.png",
          "ios": [
            "target": target
          ],
          "taskId": "some-task-id",
          "url": "https://github.com/edeckers/react-native-blob-courier"
        ]
        let resolve: RCTPromiseResolveBlock = { (_: Any?) -> Void in XCTAssertTrue(false) }
        let reject: RCTPromiseRejectBlock = { (_: String?, _: String?, _: Error?) -> Void in XCTAssertTrue(true) }

        sut?.fetchBlob(input: input, resolve: resolve, reject: reject)

        sleep(BlobCourierTests.defaultPromiseTimeoutSeconds)
    }

    func testUnreachableFetchServerRejectsPromise() throws {
        let input: NSDictionary = [
          "filename": "some-filename.png",
          "taskId": "some-task-id",
          "url": "http://127.0.0.1:12345"
        ]
        let resolve: RCTPromiseResolveBlock = { (_: Any?) -> Void in XCTAssertTrue(false) }
        let reject: RCTPromiseRejectBlock = { (_, _, _) -> Void in XCTAssertTrue(true) }

        sut?.fetchBlob(input: input, resolve: resolve, reject: reject)

        sleep(BlobCourierTests.defaultPromiseTimeoutSeconds)
    }

    func testUploadOfNonExistentFileRejectsPromise() throws {
        let resolve: RCTPromiseResolveBlock = { (_: Any?) -> Void in XCTAssertTrue(false) }
        let reject: RCTPromiseRejectBlock = { (_, _, _) -> Void in XCTAssertTrue(true) }

        self.sut?.uploadBlob(input: [
         "parts": [
           "file": [
             "payload": [
                "absoluteFilePath": "/this/path/does/not/exist.png",
                "mimeType": "image/png"
             ],
             "type": "file"
           ]
         ],
         "taskId": "SOME_TASK_ID",
         "url": "https://file.io"
        ], resolve: resolve, reject: reject)

        sleep(BlobCourierTests.defaultPromiseTimeoutSeconds)
    }

    func testNonExistingFetchUrlResolvesPromise() throws {
        let input: NSDictionary = [
          "filename": "some-filename.png",
          "taskId": "some-task-id",
          "url": "https://github.com/edeckers/this-does-not-exist"
        ]
        let resolve: RCTPromiseResolveBlock = { (_: Any?) -> Void in XCTAssertTrue(true) }
        let reject: RCTPromiseRejectBlock = { (_: String?, _: String?, _: Error?) -> Void in
          XCTAssertTrue(false) }

        sut?.fetchBlob(input: input, resolve: resolve, reject: reject)

        sleep(BlobCourierTests.defaultPromiseTimeoutSeconds)
    }

    func testNonExistingUploadUrlResolvesPromise() throws {
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
                 "payload": [
                    "absoluteFilePath": data["absoluteFilePath"] ?? "",
                    "mimeType": "image/png"
                 ],
                 "type": "file"
               ]
             ],
             "taskId": data["taskId"] ?? "",
             "url": "https://github.com/edeckers/this-does-not-exist"
           ], resolve: resolveUpload, reject: reject)
        }

        sut?.fetchBlob(input: input, resolve: resolve, reject: reject)

        sleep(BlobCourierTests.defaultPromiseTimeoutSeconds)
    }
 }
