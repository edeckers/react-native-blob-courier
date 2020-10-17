/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
import { NativeModules } from 'react-native';
import type {
  AndroidBlobRequest,
  BlobRequest,
  BlobResponse,
  BlobUploadRequest,
} from './Requests';

type BlobCourierType = {
  fetchBlob(input: AndroidBlobRequest | BlobRequest): Promise<BlobResponse>;
  uploadBlob(input: BlobUploadRequest): Promise<BlobResponse>;
};

const { BlobCourier } = NativeModules;

export default BlobCourier as BlobCourierType;

export * from './Requests';
