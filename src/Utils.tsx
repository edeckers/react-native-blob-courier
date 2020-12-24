/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

import type { BlobNamedMultipartArray, BlobMultipart } from './ExposedTypes';

// Taken from https://github.com/joltup/rn-fetch-blob/blob/master/utils/uuid.js
const createUuidPart = () => Math.random().toString(36).substring(2, 15);

export const uuid = () => `${createUuidPart()}${createUuidPart()}`;

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
  ) as { [key: string]: any };

export const unionObjects = (
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
        : {
            ...p,
            [c]: c in primary ? primary[c] : secondary[c],
          },
    {}
  ) as { [key: string]: any };

export const fallbackObjects = (
  primary: { [key: string]: any },
  fallback: { [key: string]: any }
): { [key: string]: any } => {
  const primaryAndFallbackUnified = unionObjects(primary, fallback);

  const allUniqueKeys = [...new Set(Object.keys(primaryAndFallbackUnified))];

  return allUniqueKeys.reduce((p, c) => {
    if (typeof fallback[c] === 'object') {
      const childWithFallback = fallbackObjects(
        typeof primary[c] === 'object' ? primary[c] : {}, // Fallback object is leading, so either primary is an object or we're ignoring it
        fallback[c]
      );

      return {
        ...p,
        [c]: childWithFallback,
      };
    }

    return {
      ...p,
      [c]: primary[c] !== undefined ? primary[c] : fallback[c],
    };
  }, {});
};

export const convertMappedMultipartsWithSymbolizedKeysToArray = (mappedParts: {
  [key: string]: BlobMultipart;
}): BlobNamedMultipartArray => {
  const symbolKeys = Object.getOwnPropertySymbols(mappedParts);

  return symbolKeys.map((symbolName: any) => ({
    ...mappedParts[symbolName],
    name: Symbol.keyFor(symbolName) ?? '',
  }));
};

export const sanitizeMappedMultiparts = (mapped: {
  [key: string]: BlobMultipart;
}) => {
  const regularKeys = Object.keys(mapped);
  const symbolKeys = Object.getOwnPropertySymbols(mapped);

  const receivedRegularKeys = regularKeys.length > 0;
  const receivedSymbolKeys = symbolKeys.length > 0;

  const receivedMixedKeys = receivedRegularKeys && receivedSymbolKeys;

  const receivedInvalidSymbols =
    receivedSymbolKeys &&
    symbolKeys.map(Symbol.keyFor).filter((_) => _ === undefined).length > 0;

  if (receivedMixedKeys) {
    throw new Error('Either use all Symbols or all Strings as multipart keys');
  }

  if (receivedInvalidSymbols) {
    throw new Error('Use Symbol.for when using Symbols as key for multiparts');
  }

  return regularKeys.length > 0
    ? regularKeys.reduce(
        (p, c) => ({
          ...p,
          [Symbol.for(c)]: mapped[c],
        }),
        {}
      )
    : mapped;
};
