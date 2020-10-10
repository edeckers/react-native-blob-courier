/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
export declare interface BlobRequest {
  readonly filename: string;
  readonly headers?: Headers;
  readonly method?: string;
  readonly url: string;
}

export declare interface AndroidBlobRequest {
  readonly useDownloadManager: boolean;
}
