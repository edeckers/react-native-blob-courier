/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
import { intersectObjects, prefixDict } from './Utils';

declare global {
  interface Object {
    /**
     * @param secondary contains fallback values for `undefined`
     * @returns an intersection of both objects
     */
    intersect(this: Object, secondary: Object): Object;
    prefixKeys(this: Object, prefix: string): Object;
  }
}

// eslint-disable-next-line no-extend-native
Object.prototype.intersect = function (this: Object, secondary: Object) {
  return intersectObjects(this, secondary);
};
// eslint-disable-next-line no-extend-native
Object.prototype.prefixKeys = function (this: Object, prefix: string) {
  return prefixDict(this, prefix);
};
