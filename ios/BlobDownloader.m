#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(BlobDownloader, NSObject)

RCT_EXTERN_METHOD(fetchBlob:(NSDictionary *)input
                 withResolver:(RCTPromiseResolveBlock)resolve
                 withRejecter:(RCTPromiseRejectBlock)reject)

@end
