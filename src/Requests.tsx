/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
export declare interface BlobRequestHeaders {
  readonly headers?: { [key: string]: string };
}
export declare interface BlobRequest extends BlobRequestHeaders {
  readonly filename: string;
  readonly method?: string;
  readonly url: string;
}

export declare interface AndroidBlobRequest extends BlobRequestHeaders {
  readonly useDownloadManager: boolean;
}

export declare interface BlobUploadRequest extends BlobRequestHeaders {
  readonly filePath: string;
  readonly method?: string;
  readonly url: string;
}

export declare interface BlobRequestTask {
  readonly taskId: string;
}

export declare interface BlobUnmanagedHttpResponse {
  readonly headers: { [key: string]: string };
  readonly code: number;
}

export declare interface BlobFilePathData {
  readonly fullFilePath: string;
}

export declare interface BlobManagedData extends BlobFilePathData {
  readonly result: string;
}

export declare interface BlobUnmanagedData extends BlobFilePathData {
  readonly response: BlobUnmanagedHttpResponse;
}

export enum BlobResponseType {
  Managed,
  Unmanaged,
}

export declare interface BlobResponse {
  readonly type: BlobResponseType;
  readonly data: BlobUnmanagedData | BlobManagedData;
}
