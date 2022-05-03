// @ts-ignore
import type { TurboModule } from 'react-native/Libraries/TurboModule/RCTExport';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  getConstants: () => {};

  cancelRequest(input: Object): Promise<{}>;
  fetchBlob(input: Object): Promise<Object>;
  uploadBlob(input: Object): Promise<Object>;
}

export default TurboModuleRegistry.get<Spec>('BlobCourier');
