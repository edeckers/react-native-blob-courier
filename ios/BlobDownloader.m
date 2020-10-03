#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(BlobDownloader, NSObject)

RCT_EXTERN_METHOD(fetch_blob:(float)a withB:(float)b
                 withResolver:(RCTPromiseResolveBlock)resolve
                 withRejecter:(RCTPromiseRejectBlock)reject)

@end
