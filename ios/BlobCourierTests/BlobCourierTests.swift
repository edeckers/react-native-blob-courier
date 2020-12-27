//  Copyright (c) Ely Deckers.
// 
//  This source code is licensed under the MPL-2.0 license found in the
//  LICENSE file in the root directory of this source tree.

import XCTest

import Embassy
import EnvoyAmbassador

import MimeParser

@testable import BlobCourier

// swiftlint:disable type_body_length
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

    // swiftlint:disable function_body_length
    func testUploadMultipartMessageIsValid() throws {
        var result = (false, "Unknown")
        let input: NSDictionary = [
          "filename": "some-filename.png",
          "taskId": "some-task-id",
          "url": "https://github.com/edeckers/react-native-blob-courier"
        ]

        let expectedParts = ["test", "file"]

        let router = Router()
        router["/api/v2/users"] =
          DelayResponse(DataResponse(
            statusCode: 201,
            statusMessage: "Created",
            contentType: "text/plain",
            headers: []) { response -> Data in

            if let input = response["swsgi.input"] as? SWSGIInput {
              let contentType = response["CONTENT_TYPE"]

              DataReader.read(input) { data in
                let body = String(data: data, encoding: .utf8) ?? ""

                let httpMessage = "Content-Type: \(contentType!)\r\n\(body)"
                let bodyStartsWithBoundary = body.hasPrefix("--")
                if (!bodyStartsWithBoundary) {
                  result = (false, "Body must start with boundary")
                  return
                }

                do {
                  let parsedMessage = try MimeParser().parse(httpMessage)
                  if case .mixed(let mimes) = parsedMessage.content {
                    let actualParts: [String] = mimes.reduce(into: []) { acc, mime in
                      if let value = mime.header.contentDisposition?.parameters["name"] {
                        acc.append(value)
                      }
                    }

                    if actualParts.sorted() != expectedParts.sorted() {
                      result = (false, "Did not receive all expected parts")
                    } else if actualParts != expectedParts {
                      result = (false, "Received parts order differs from provided order")
                    } else {
                      result = (true, "Success")
                    }
                  }
                } catch {
                  result = (false, error.localizedDescription)
                }
              }
            }

            return Data("".utf8)
          })

        let httpServer = EmbeddedHttpServer(withRouter: router)

        let reject: RCTPromiseRejectBlock = { (_: String?, _: String?, error: Error?) -> Void in
          result = (false, error?.localizedDescription ?? "") }
        let resolveUpload: RCTPromiseResolveBlock = { (_: Any?) -> Void in print("")  }
        let resolve: RCTPromiseResolveBlock = { (response: Any?) -> Void in
            let dict = response as? NSDictionary ?? [:]
            let data = dict["data"] as? NSDictionary ?? [:]

            try? httpServer.start()

            self.sut?.uploadBlob(input: [
             "parts": [
               [
                 "name": "test",
                 "payload": "SOME_TEST_STRING",
                 "type": "string"
               ],
               [
                 "name": "file",
                 "payload": [
                    "absoluteFilePath": data["absoluteFilePath"] ?? "",
                    "mimeType": "image/png"
                 ],
                 "type": "file"
               ]
             ],
             "taskId": data["taskId"] ?? "",
             "url": "http://localhost:12345/api/v2/users"
           ], resolve: resolveUpload, reject: reject)
        }

        sut?.fetchBlob(input: input, resolve: resolve, reject: reject)

        sleep(BlobCourierTests.defaultPromiseTimeoutSeconds)
        httpServer.stop()

        XCTAssertTrue(result.0)
    }
    // swiftlint:enable function_body_length

    func testAllRequiredFetchParametersProvidedResolvesPromise() throws {
        var result = (false, "Unknown")
        let input: NSDictionary = [
          "filename": "some-filename.png",
          "taskId": "some-task-id",
          "url": "https://github.com/edeckers/react-native-blob-courier"
        ]
        let resolve: RCTPromiseResolveBlock = { (_: Any?) -> Void in result = (true, "Success") }
        let reject: RCTPromiseRejectBlock = { (_: String?, _: String?, error: Error?) -> Void in
          result = (false, error?.localizedDescription ?? "") }

        sut?.fetchBlob(input: input, resolve: resolve, reject: reject)

        sleep(BlobCourierTests.defaultPromiseTimeoutSeconds)
        XCTAssertTrue(result.0)
    }

    func testMissingRequiredFetchParametersRejectsPromise() throws {
        var result = (false, "Unknown")
        let input: NSDictionary = [
          "taskId": "some-task-id",
          "url": "https://github.com/edeckers/react-native-blob-courier"
        ]
        let resolve: RCTPromiseResolveBlock = { (_: Any?) -> Void in
           result = (false, "Resolved, but expected reject") }
        let reject: RCTPromiseRejectBlock = { (_: String?, _: String?, _: Error?) -> Void in
           result = (true, "Success") }

        sut?.fetchBlob(input: input, resolve: resolve, reject: reject)

        sleep(BlobCourierTests.defaultPromiseTimeoutSeconds)
        XCTAssertTrue(result.0)
    }

    func testAllRequiredUploadParametersProvidedResolvesPromise() throws {
        var result = (false, "Unknown")
        let input: NSDictionary = [
          "filename": "some-filename.png",
          "taskId": "some-task-id",
          "url": "https://github.com/edeckers/react-native-blob-courier"
        ]
        let reject: RCTPromiseRejectBlock = { (_: String?, _: String?, error: Error?) -> Void in
          result = (false, error?.localizedDescription ?? "") }
        let resolveUpload: RCTPromiseResolveBlock = { (_: Any?) -> Void in
          result = (true, "Success") }
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
        XCTAssertTrue(result.0)
    }

    func testMissingRequiredUploadParametersRejectsPromise() throws {
        var result = (false, "Unknown")
        let input: NSDictionary = [
          "filename": "some-filename.png",
          "taskId": "some-task-id",
          "url": "https://github.com/edeckers/react-native-blob-courier"
        ]
        let reject: RCTPromiseRejectBlock = { (_: String?, _: String?, _: Error?) -> Void in
          result = (true, "Success") }
        let resolveUpload: RCTPromiseResolveBlock = { (_: Any?) -> Void in
          result = (false, "Resoved, but expected reject") }
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
        XCTAssertTrue(result.0)
    }

    func testValidTargetParametersResolvesPromise() throws {
        var result = (false, "Unknown")
        for target in ["cache", "data"] {
          let input: NSDictionary = [
            "filename": "some-filename.png",
            "ios": [
              "target": target
            ],
            "taskId": "some-task-id",
            "url": "https://github.com/edeckers/react-native-blob-courier"
          ]
          let resolve: RCTPromiseResolveBlock = { (_: Any?) -> Void in result = (true, "Success") }
          let reject: RCTPromiseRejectBlock = { (_: String?, _: String?, error: Error?) -> Void in
            result = (false, error?.localizedDescription ?? "") }

          sut?.fetchBlob(input: input, resolve: resolve, reject: reject)

          sleep(BlobCourierTests.defaultPromiseTimeoutSeconds)
        XCTAssertTrue(result.0)
        }
    }

    func testInvalidTargetParametersRejectsPromise() throws {
        var result = (false, "Unknown")
        let target = "SOME_INVALID_VALUE"

        let input: NSDictionary = [
          "filename": "some-filename.png",
          "ios": [
            "target": target
          ],
          "taskId": "some-task-id",
          "url": "https://github.com/edeckers/react-native-blob-courier"
        ]
        let resolve: RCTPromiseResolveBlock = { (_: Any?) -> Void in
          result = (false, "Resolved, but expected reject") }
        let reject: RCTPromiseRejectBlock = { (_: String?, _: String?, _: Error?) -> Void in
          result = (true, "Success") }

        sut?.fetchBlob(input: input, resolve: resolve, reject: reject)

        sleep(BlobCourierTests.defaultPromiseTimeoutSeconds)
        XCTAssertTrue(result.0)
    }

    func testUnreachableFetchServerRejectsPromise() throws {
        var result = (false, "Unknown")

        let input: NSDictionary = [
          "filename": "some-filename.png",
          "taskId": "some-task-id",
          "url": "http://127.0.0.1:12345"
        ]
        let resolve: RCTPromiseResolveBlock = { (_: Any?) -> Void in result = (false, "Resolved, but expected reject") }
        let reject: RCTPromiseRejectBlock = { (_, _, _) -> Void in result = (true, "Success") }

        sut?.fetchBlob(input: input, resolve: resolve, reject: reject)

        sleep(BlobCourierTests.defaultPromiseTimeoutSeconds)
        XCTAssertTrue(result.0)
    }

    func testUploadOfNonExistentFileRejectsPromise() throws {
        var result = (false, "Unknown")

        let resolve: RCTPromiseResolveBlock = { (_: Any?) -> Void in result = (false, "Resolved, but expected reject") }
        let reject: RCTPromiseRejectBlock = { (_, _, _) -> Void in result = (true, "Success") }

        self.sut?.uploadBlob(input: [
         "parts": [
           [
             "name": "file",
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
        XCTAssertTrue(result.0)
    }

    func testNonExistingFetchUrlResolvesPromise() throws {
        var result = (false, "Unknown")

        let input: NSDictionary = [
          "filename": "some-filename.png",
          "taskId": "some-task-id",
          "url": "https://github.com/edeckers/this-does-not-exist"
        ]
        let resolve: RCTPromiseResolveBlock = { (_: Any?) -> Void in result = (true, "Success") }
        let reject: RCTPromiseRejectBlock = { (_: String?, _: String?, error: Error?) -> Void in
          result = (false, error?.localizedDescription ?? "") }

        sut?.fetchBlob(input: input, resolve: resolve, reject: reject)

        sleep(BlobCourierTests.defaultPromiseTimeoutSeconds)
        XCTAssertTrue(result.0)
    }

    func testNonExistingUploadUrlResolvesPromise() throws {
        var result = (false, "Unknown")

        let input: NSDictionary = [
          "filename": "some-filename.png",
          "taskId": "some-task-id",
          "url": "https://github.com/edeckers/react-native-blob-courier"
        ]
        let reject: RCTPromiseRejectBlock = { (_: String?, _: String?, error: Error?) -> Void in
          result = (false, error?.localizedDescription ?? "") }
        let resolveUpload: RCTPromiseResolveBlock = { (_: Any?) -> Void in
          result = (true, "Success") }
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
        XCTAssertTrue(result.0)
    }
 }
