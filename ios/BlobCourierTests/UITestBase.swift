import Foundation
import XCTest

import Embassy
import EnvoyAmbassador

import MimeParser

class UITestBase: XCTestCase {
  var eventLoop: EventLoop!
  var server: DefaultHTTPServer!

  var eventLoopThreadCondition: NSCondition!
  var eventLoopThread: Thread!

  override func setUp() {
    super.setUp()
    setupWebApp()
  }

  // setup the Embassy web server for testing
  private func setupWebApp() {
    eventLoop = try! SelectorEventLoop(selector: try! KqueueSelector())

    let router = Router()
    let mimeParser = MimeParser()
    server = DefaultHTTPServer(eventLoop: eventLoop, port: 12345, app: router.app)
    
    router["/api/v2/users"] = DelayResponse(JSONResponse(handler: { r -> Any in
        let input = r["swsgi.input"] as! SWSGIInput
        let contentType = r["CONTENT_TYPE"]
        print(contentType)
           

        DataReader.read(input) { data in
          let text = String(data: data, encoding: .utf8) ?? ""
          print(text)
          do {
            let xs = try mimeParser.parse("Content-Type: \(contentType!)\r\n\(text)")
            print(xs)
          } catch {
            print("FDKJSDJFKSDLFJSKFSKSJFKSFJDSFKSDLFJSDKJFLSJDFSDF")
            print(error)
          }
        }

        return [
            ["id": "01", "name": "john"],
            ["id": "02", "name": "tom"]
        ]
    }))
    
    // Start HTTP server to listen on the port
    try! server.start()

    eventLoopThreadCondition = NSCondition()
    eventLoopThread = Thread(target: self, selector: #selector(runEventLoop), object: nil)
    eventLoopThread.start()
  }

  override func tearDown() {
    super.tearDown()
    server.stopAndWait()
    eventLoopThreadCondition.lock()
    eventLoop.stop()

    let calendar = Calendar.current
    let date = calendar.date(byAdding: .minute, value: 1, to: Date())

    while eventLoop.running {
      if !eventLoopThreadCondition.wait(until: date!) {
        fatalError("Join eventLoopThread timeout")
      }
    }
  }

  @objc private func runEventLoop() {
    eventLoop.runForever()
    eventLoopThreadCondition.lock()
    eventLoopThreadCondition.signal()
    eventLoopThreadCondition.unlock()
  }
}
