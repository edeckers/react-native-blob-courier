import Foundation

@objc(BlobDownloader)
class BlobDownloader: NSObject {

    @objc(fetch_blob:withB:withResolver:withRejecter:)
    func fetch_blob(a: Float, b: Float, resolve:RCTPromiseResolveBlock,reject:RCTPromiseRejectBlock) -> Void {
        let urlString = "https://file-examples-com.github.io/uploads/2018/04/file_example_AVI_480_750kB.avi"
        let url = URL(string: urlString)

let fileName = String((url!.lastPathComponent)) as NSString
// Create destination URL
let documentsUrl:URL =  FileManager.default.urls(for: .downloadsDirectory, in: .userDomainMask).first!
let destinationFileUrl = documentsUrl.appendingPathComponent("\(fileName)")
//Create URL to the source file you want to download
let fileURL = URL(string: urlString)
let sessionConfig = URLSessionConfiguration.default
let session = URLSession(configuration: sessionConfig)
let request = URLRequest(url:fileURL!)
let task = session.downloadTask(with: request) { (tempLocalUrl, response, error) in
    if let tempLocalUrl = tempLocalUrl, error == nil {
        // Success
        print("a")
        if let statusCode = (response as? HTTPURLResponse)?.statusCode {
            print("Successfully downloaded. Status code: \(statusCode)")
        }
        do {
            try FileManager.default.copyItem(at: tempLocalUrl, to: destinationFileUrl)
            do {
                //Show UIActivityViewController to save the downloaded file
                let contents  = try FileManager.default.contentsOfDirectory(at: destinationFileUrl, includingPropertiesForKeys: nil, options: .skipsHiddenFiles)
                for indexx in 0..<contents.count {
                    if contents[indexx].lastPathComponent == destinationFileUrl.lastPathComponent {
                        let activityViewController = UIActivityViewController(activityItems: [contents[indexx]], applicationActivities: nil)

                    }
                }
            }
            catch (let err) {
                print("error: \(err)")
            }
        } catch (let writeError) {
            print("Error creating a file \(destinationFileUrl) : \(writeError)")
        }
    } else {
        print("Error took place while downloading a file. Error description: \(error?.localizedDescription ?? "")")
    }
}
task.resume()

        resolve(a*b)
    }
}
