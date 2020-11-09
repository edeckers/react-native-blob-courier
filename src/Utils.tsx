// Taken from https://github.com/joltup/rn-fetch-blob/blob/master/utils/uuid.js
const createUuidPart = () => Math.random().toString(36).substring(2, 15);

export const uuid = () => `${createUuidPart()}${createUuidPart()}`;

export const createTaskId = () => `rnbc-req-${uuid()}`;

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
