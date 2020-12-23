import Foundation
import XCTest

import Embassy
import EnvoyAmbassador

class EmbeddedHttpServer {
  var eventLoop: EventLoop!

  var eventLoopThreadCondition: NSCondition!
  var eventLoopThread: Thread!

  var server: DefaultHTTPServer!

  var tehRouter: Router!

  init(withRouter router: Router) {
    tehRouter = router
  }

  public func start() throws {
    eventLoop = try SelectorEventLoop(selector: try KqueueSelector())

    server = DefaultHTTPServer(eventLoop: eventLoop, port: 12345, app: tehRouter.app)

    try server.start()

    eventLoopThreadCondition = NSCondition()
    eventLoopThread = Thread(target: self, selector: #selector(runEventLoop), object: nil)
    eventLoopThread.start()
  }

  public func stop() {
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
