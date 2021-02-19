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

export declare interface BlobRequestReturnResponse {
  readonly returnResponse?: boolean;
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
    BlobRequestMethod,
    AndroidFetchSettings,
    IOSFetchSettings {
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

export type TargetType = 'cache' | 'data';
export declare interface TargetSettings {
  target?: TargetType;
}
export declare interface AndroidFetchSettings {
  readonly android?: AndroidDownloadManagerToggle &
    AndroidDownloadManager &
    TargetSettings;
}

export declare interface IOSFetchSettings {
  readonly ios?: TargetSettings;
}

export declare interface BlobProgressEvent {
  readonly total: number;
  readonly written: number;
}

export declare type BlobMultipartFormData = string | { [key: string]: any };
export declare type BlobMultipartType = 'file' | 'string';
export declare type BlobMultipart = {
  payload: BlobMultipartFormData | BlobMultipartFormDataFile;
  type: BlobMultipartType;
};

export declare type BlobMultipartWithName = BlobMultipart & {
  name: string;
};

export declare type BlobNamedMultipartArray = BlobMultipartWithName[];

export declare interface BlobMultipartFormDataFile {
  readonly absoluteFilePath: string;
  readonly filename?: string;
  readonly mimeType: string;
}

export declare interface BlobUploadRequest
  extends BlobBaseRequest,
    BlobRequestMimeType,
    BlobRequestMethod,
    BlobRequestReturnResponse {
  readonly absoluteFilePath: string;
  readonly filename?: string;
  readonly multipartName?: string;
}

export declare interface BlobMultipartBaseRequest
  extends BlobBaseRequest,
    BlobRequestMethod,
    BlobRequestReturnResponse {}

export declare interface BlobMultipartArrayUploadRequest
  extends BlobMultipartBaseRequest {
  readonly parts: BlobNamedMultipartArray;
}

export declare interface BlobMultipartMapUploadRequest
  extends BlobMultipartBaseRequest {
  readonly parts: { [key: string]: BlobMultipart };
}

export declare interface BlobRequestTask {
  readonly taskId: string;
}

export declare interface BlobRequestSettings {
  readonly progressIntervalMilliseconds?: number;
  readonly signal?: AbortSignal;
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

export type BlobFetchInput = BlobFetchRequest &
  BlobRequestSettings &
  AndroidFetchSettings &
  IOSFetchSettings;

export type BlobUploadInput = BlobUploadRequest & BlobRequestSettings;
