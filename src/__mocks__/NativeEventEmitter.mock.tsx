/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
import { NativeModules } from 'react-native';

const { BlobCourierEventEmitter } = NativeModules;

const NativeEventEmitter = jest.requireActual(
  'react-native/Libraries/EventEmitter/NativeEventEmitter'
);

const emitterInstance = new NativeEventEmitter(BlobCourierEventEmitter);

export default function myTest() {
  return emitterInstance;
}
