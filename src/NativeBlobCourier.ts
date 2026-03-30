/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  cancelRequest(input: Object): Promise<Object>;
  fetchBlob(input: Object): Promise<Object>;
  uploadBlob(input: Object): Promise<Object>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('BlobCourier');
