require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "react-native-blob-courier"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]

  s.platforms    = { :ios => "10.0" }
  s.source       = { :git => "https://github.com/edeckers/react-native-blob-courier.git", :tag => "#{s.version}" }

  
  s.source_files = "ios/*.{h,m,mm,swift}", "ios/Cancel/*.{h,m,swift}", "ios/Common/*.{h,m,swift}", "ios/Fetch/*.{h,m,swift}", "ios/Progress/*.{h,m,swift}", "ios/React/*.{h,m,swift}", "ios/Upload/*.{h,m,swift}"

  s.dependency "React"
end
