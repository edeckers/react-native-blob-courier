/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
import { intersectObjects } from './Utils';

export const dict = (o: { [key: string]: any }) => ({
  /**
   * @param secondary contains fallback values for `undefined`
   * @returns an intersection of both objects
   */
  intersect: (secondary: { [key: string]: any }) =>
    intersectObjects(o, secondary),
});
