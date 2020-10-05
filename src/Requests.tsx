export declare interface BlobRequest {
  readonly filename: string;
  readonly headers?: Headers;
  readonly method?: string;
  readonly url: string;
  readonly target: string;
}

export declare interface AndroidBlobRequest extends BlobRequest {
  readonly useDownloadManager: boolean;
}
