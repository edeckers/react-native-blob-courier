/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

// Taken from https://github.com/joltup/rn-fetch-blob/blob/master/utils/uuid.js
const createUuidPart = () => Math.random().toString(36).substring(2, 15);

export const uuid = () => `${createUuidPart()}${createUuidPart()}`;

export const prefixDict = <T extends { [key: string]: any }>(
  dict: T,
  prefix: string
) =>
  Object.keys(dict).reduce(
    (p, k) => ({
      ...p,
      [`${prefix}.${k}`]: (dict as any)[k],
    }),
    {}
  ) as T;

export const intersectObjects = (
  primary: { [key: string]: any },
  secondary: { [key: string]: any }
) =>
  [...new Set(Object.keys(primary).concat(Object.keys(secondary)))].reduce(
    (p, c) =>
      c in primary && c in secondary
        ? {
            ...p,
            [c]: primary[c] !== undefined ? primary[c] : secondary[c],
          }
        : p,
    {}
  );
