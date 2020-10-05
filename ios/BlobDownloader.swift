import Foundation

@objc(BlobDownloader)
class BlobDownloader: NSObject {
    static let ERROR_MISSING_REQUIRED_PARAM = "ERROR_MISSING_REQUIRED_PARAM"
    static let ERROR_INVALID_TARGET_PARAM_ENUM = "ERROR_INVALID_TARGET_PARAM_ENUM"
    static let ERROR_UNEXPECTED_EXCEPTION = "ERROR_UNEXPECTED_EXCEPTION"

    enum BlobDownloaderError: Error {
        case withMessage(code:String, message:String)
    }

    static let PARAM_FILENAME = "filename"
    static let PARAM_METHOD = "method"
    static let PARAM_TARGET = "target"
    static let PARAM_URL = "url"
    static let PARAM_USE_DOWNLOAD_MANAGER = "useDownloadManager"

    static let TARGET_PARAM_ENUM_PREFIX = "enum://"

    static let DEFAULT_METHOD = "GET"

    static let REQUIRED_PARAMETER_PROCESSOR = [
      "Boolean": { (input: NSDictionary, parameterName: String) in return input[parameterName]! },
      "String": { (input: NSDictionary, parameterName: String) in return input[parameterName]! }
    ]

    static let predefinedPaths:NSDictionary = [
      "CACHE": FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first!,
      "DOCUMENT": FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!,
      "MAINBUNDLE": FileManager.default.urls(for: .applicationDirectory, in: .userDomainMask).first!
    ]

    func stripEnumPrefix(path: String) -> String { return path.replacingOccurrences(of: BlobDownloader.TARGET_PARAM_ENUM_PREFIX, with: "") }

    func assertPathEnum(pathEnum: String) throws {
        let cleanedPathEnum = stripEnumPrefix(path: pathEnum)

        if (BlobDownloader.predefinedPaths[cleanedPathEnum.uppercased()] == nil) {
            throw BlobDownloaderError.withMessage(code:BlobDownloader.ERROR_INVALID_TARGET_PARAM_ENUM, message:"Unknown enum `\(cleanedPathEnum)`")
        }
    }

    func isEnum(pathEnum: String) -> Bool { return pathEnum.hasPrefix(BlobDownloader.TARGET_PARAM_ENUM_PREFIX) }

    func parsePathEnum(pathEnum: String) throws -> String {
        let cleanedPathEnum = stripEnumPrefix(path: pathEnum);

        try assertPathEnum(pathEnum: cleanedPathEnum)

        return BlobDownloader.predefinedPaths[cleanedPathEnum.uppercased()] as! String
    }

    func assertTargetParam(value: String) throws {
        if (isEnum(pathEnum: value)) {
            try assertPathEnum(pathEnum: value)
        }
    }

    func assertRequiredParameter(input: NSDictionary, type: String, parameterName: String) throws {
        let maybeValue = try (BlobDownloader.REQUIRED_PARAMETER_PROCESSOR[type] ?? { (_,_) in throw BlobDownloaderError.withMessage(code: BlobDownloader.ERROR_MISSING_REQUIRED_PARAM, message: "No processor defined for type `\(type)`, valid options: \(BlobDownloader.REQUIRED_PARAMETER_PROCESSOR.keys as! [String])") })(input, parameterName)

        if (maybeValue == nil) {
             throw BlobDownloaderError.withMessage(code: BlobDownloader.ERROR_MISSING_REQUIRED_PARAM, message: "`\(parameterName)` is a required parameter of type `\(type)`")
        }
    }

    func fetchBlobFromValidatedParameters(input: NSDictionary, resolve:RCTPromiseResolveBlock,reject:RCTPromiseRejectBlock) throws {
        let url = (input[BlobDownloader.PARAM_URL] as? String) ?? ""
        
        let urlObject = URL(string: url)

        let filename = (input[BlobDownloader.PARAM_FILENAME] as? String) ?? ""

        let documentsUrl:URL =  FileManager.default.urls(for: .downloadsDirectory, in: .userDomainMask).first!
        let destinationFileUrl = documentsUrl.appendingPathComponent("\(filename)")

        let fileURL = URL(string: url)
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

        resolve(true)
    }

    @objc(fetchBlob:withResolver:withRejecter:)
    func fetchBlob(input: NSDictionary, resolve:RCTPromiseResolveBlock,reject:RCTPromiseRejectBlock) -> Void {
        do {
            try assertRequiredParameter(input: input, type: "String", parameterName: BlobDownloader.PARAM_FILENAME)
            try assertRequiredParameter(input: input, type: "String", parameterName: BlobDownloader.PARAM_TARGET)
            try assertRequiredParameter(input: input, type: "String", parameterName: BlobDownloader.PARAM_URL)
            let target = (input[BlobDownloader.PARAM_TARGET] as? String) ?? ""
            try assertTargetParam(value: target)

            try fetchBlobFromValidatedParameters(input: input, resolve: resolve, reject: reject)
        } catch {
            print("\(error)")
        }
    }
}
