/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
export declare interface BlobRequestHeaders {
  readonly headers?: { [key: string]: string };
}

export declare interface BlobRequestMethod {
  readonly method?: string;
}

export declare interface BlobRequestMimeType {
  readonly mimeType: string;
}

export declare interface BlobRequestOnProgress {
  readonly onProgress?: (e: BlobProgressEvent) => void;
}

export declare interface BlobRequestUrl {
  readonly url: string;
}

export declare interface BlobBaseRequest
  extends BlobRequestHeaders,
    BlobRequestOnProgress,
    BlobRequestUrl {}

export declare interface BlobFetchRequest
  extends BlobBaseRequest,
    BlobRequestMimeType,
    BlobRequestMethod {
  readonly filename: string;
}

export declare interface AndroidDownloadManagerToggle {
  readonly useDownloadManager?: boolean;
}

export declare interface AndroidDownloadManagerSettings {
  readonly description?: string;
  readonly enableNotifications?: boolean;
  readonly title?: string;
}

export declare interface AndroidDownloadManager {
  readonly downloadManager?: AndroidDownloadManagerSettings;
}

export declare interface AndroidSettings {
  readonly android?: AndroidDownloadManagerToggle & AndroidDownloadManager;
}

export declare interface BlobProgressEvent {
  readonly total: number;
  readonly written: number;
}

export declare interface BlobMultipartFormDataBase {
  readonly type: 'string' | 'file';
}
export declare interface BlobMultipartFormData
  extends BlobMultipartFormDataBase {
  readonly value: string | { [key: string]: any };
}

export declare interface BlobMultipartFormDataFile
  extends BlobMultipartFormDataBase {
  readonly absoluteFilePath: string;
  readonly filename?: string;
  readonly mimeType: string;
}

export declare interface BlobUploadRequest
  extends BlobBaseRequest,
    BlobRequestMimeType,
    BlobRequestMethod {
  readonly absoluteFilePath: string;
  readonly multipartName?: string;
  readonly filename?: string;
  readonly returnResponse?: boolean;
}

export declare interface BlobMultipartUploadRequest
  extends BlobBaseRequest,
    BlobRequestMethod {
  readonly parts: {
    [key: string]: BlobMultipartFormData | BlobMultipartFormDataFile;
  };
  readonly returnResponse?: boolean;
}

export declare interface BlobRequestTask {
  readonly taskId: string;
}

export declare interface BlobRequestSettings {
  readonly progressIntervalMilliseconds?: number;
}

export declare interface BlobUnmanagedHttpResponse {
  readonly headers: { [key: string]: string };
  readonly code: number;
}

export declare interface BlobFilePathData {
  readonly absoluteFilePath: string;
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

export declare interface BlobFetchResponse {
  readonly type: BlobResponseType;
  readonly data: BlobUnmanagedData | BlobManagedData;
}

export declare interface BlobUploadResponse extends BlobUnmanagedData {}
