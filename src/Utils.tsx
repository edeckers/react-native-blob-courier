// Taken from https://github.com/joltup/rn-fetch-blob/blob/master/utils/uuid.js
const createUuidPart = () => Math.random().toString(36).substring(2, 15);

export const uuid = () => `${createUuidPart()}${createUuidPart()}`;
