/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
const buildPathEnum = (items: string[], initial = {}) => {
  console.debug(
    `Build PathEnum type from ${items}, merged with ${JSON.stringify(initial)}`
  );

  const paths = items.reduce(
    (p, c) => ({
      ...p,
      [c]: `enum://${c}`,
    }),
    initial
  );

  console.debug('Built PathEnum type:', JSON.stringify(paths));

  return paths;
};

const AndroidPathType = {
  DCIM: '',
  Download: '',
  Movie: '',
  Music: '',
  Picture: '',
  Ringtone: '',
  SDCard: '',
};

const CommonPathType = {
  Cache: '',
  Document: '',
};

const IOSPathType = {
  MainBundle: '',
};

export type AndroidPathType = typeof AndroidPathType;
export type CommonPathType = typeof CommonPathType;
export type IOSPathType = typeof IOSPathType;

export const CommonPath = buildPathEnum(
  Object.keys(CommonPathType)
) as CommonPathType;

export const IOSPath = buildPathEnum(
  Object.keys(IOSPathType),
  CommonPath
) as IOSPathType;

export const AndroidPath = buildPathEnum(
  Object.keys(AndroidPathType),
  CommonPath
) as AndroidPathType;
