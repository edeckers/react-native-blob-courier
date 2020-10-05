/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(BlobDownloader, NSObject)

RCT_EXTERN_METHOD(fetchBlob:(NSDictionary *)input
                 withResolver:(RCTPromiseResolveBlock)resolve
                 withRejecter:(RCTPromiseRejectBlock)reject)

@end
