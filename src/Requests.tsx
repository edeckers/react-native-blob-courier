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

export declare interface BlobUploadRequest {
  readonly filePath: string;
  readonly headers?: Headers;
  readonly method?: string;
  readonly url: string;
}

export declare interface BlobManagedResponse {
  readonly result: string;
  readonly fullFilePath: string;
}

export declare interface BlobHttpResponse {
  readonly fullFilePath: string;
  readonly code: number;
}

export enum BlobResponseType {
  Managed,
  Unmanaged,
}
export declare interface BlobResponse {
  readonly type: BlobResponseType;
  readonly response: BlobHttpResponse | BlobManagedResponse;
}
