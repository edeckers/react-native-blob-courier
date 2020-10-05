#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(BlobDownloader, NSObject)

RCT_EXTERN_METHOD(fetchBlob:(NSString)url
                 withResolver:(RCTPromiseResolveBlock)resolve
                 withRejecter:(RCTPromiseRejectBlock)reject)

@end
