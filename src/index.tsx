/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
import { NativeModules } from 'react-native';
import type { AndroidBlobRequest, BlobRequest } from './Requests';

type BlobDownloaderType = {
  fetchBlob(input: AndroidBlobRequest | BlobRequest): Promise<Response>;
};

const { BlobDownloader } = NativeModules;

export default BlobDownloader as BlobDownloaderType;
export * from './Paths';
export * from './Requests';
