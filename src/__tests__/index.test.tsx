/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
import { uuid } from '../Utils';
import {
  BLOB_COURIER_PROGRESS_EVENT_NAME,
  BLOB_FETCH_FALLBACK_PARAMETERS,
  BLOB_UPLOAD_FALLBACK_PARAMETERS,
} from '../Consts';
import { dict } from '../Extensions';
import { NativeEventEmitter, NativeModules } from 'react-native';
import BlobCourier from '../index';

const {
  BlobCourier: BlobCourierNative,
  BlobCourierEventEmitter,
} = NativeModules;

const NATIVE_EVENT_EMITTER_SINGLETON = new NativeEventEmitter(
  BlobCourierEventEmitter
);

const DEFAULT_FETCH_REQUEST = {
  filename: 'some_filename.ext',
  mimeType: 'plain/text',
  url: 'https://github.com/edeckers/react-native-blob-courier',
};

const DEFAULT_UPLOAD_REQUEST = {
  filePath: 'some_filename.ext',
  mimeType: 'plain/text',
  url: 'https://github.com/edeckers/react-native-blob-courier',
};

const RANDOM_VALUE_GENERATORS: { [key: string]: () => any } = {
  boolean: () => Math.random() >= 0.5,
  function: () => {
    /* noop */
  },
  number: () => Math.random(),
  object: () => ({
    [uuid()]: uuid(),
  }),
  string: () => uuid(),
};

const getLastMockCallParameters = (m: any) =>
  m.mock.calls[m.mock.calls.length - 1];
const getLastMockCallFirstParameter = (m: any) =>
  getLastMockCallParameters(m)[0];

const generateValue = (type: string) =>
  type in RANDOM_VALUE_GENERATORS ? RANDOM_VALUE_GENERATORS[type]() : undefined;

const createRandomObjectFromDefault = function <T>(o: T) {
  return Object.keys(o).reduce(
    (p, c) => ({
      ...p,
      [c]:
        typeof (o as any)[c] === 'boolean'
          ? !(o as any)[c]
          : generateValue(typeof (o as any)[c]),
    }),
    {}
  ) as T;
};

const verifyPropertyExistsAndIsDefined = (o: any, key: string) => {
  expect(o).toHaveProperty(key);
  expect(o[key]).toBeDefined();
};

const testAsync = (name: string, testableFunction: () => Promise<void>) => {
  test(name, async (done) => {
    try {
      await testableFunction();

      done();
    } catch (e) {
      done(e);
    }
  });
};

jest.mock(
  'react-native/Libraries/EventEmitter/NativeEventEmitter',
  () => require('../__mocks__/NativeEventEmitter.mock').default
);

jest.mock('react-native/Libraries/BatchedBridge/NativeModules', () => ({
  BlobCourier: {
    fetchBlob: jest.fn(),
    uploadBlob: jest.fn(),
  },
  BlobCourierEventEmitter: {
    addListener: jest.fn(),
    removeListeners: jest.fn(),
  },
  PlatformConstants: {},
}));

beforeEach(() => {
  BlobCourierNative.fetchBlob.mockReset();
  BlobCourierNative.uploadBlob.mockReset();
  (BlobCourierEventEmitter as any).addListener.mockReset();
});

describe('Given fallback parameters are provided through a constant', () => {
  test('Fetch constant should not accidentally be changed', () => {
    expect(BLOB_FETCH_FALLBACK_PARAMETERS).toStrictEqual({
      androidDownloadManager: {},
      headers: {},
      method: 'GET',
      useAndroidDownloadManager: false,
    });
  });

  test('Upload constant should not accidentally be changed', () => {
    expect(BLOB_UPLOAD_FALLBACK_PARAMETERS).toStrictEqual({
      headers: {},
      method: 'POST',
      returnResponse: false,
    });
  });
});

describe('Given a regular fetch request', () => {
  testAsync(
    'The native module is called with the same parameters',
    async () => {
      await BlobCourier.fetchBlob(DEFAULT_FETCH_REQUEST);

      expect(BlobCourierNative.fetchBlob).toHaveBeenCalledWith(
        expect.objectContaining(DEFAULT_FETCH_REQUEST)
      );

      const calledWithParameters = getLastMockCallFirstParameter(
        BlobCourierNative.fetchBlob
      );

      verifyPropertyExistsAndIsDefined(calledWithParameters, 'taskId');
    }
  );

  describe('And no optional parameters are provided', () => {
    testAsync(
      'The native module is called with fallback parameters',
      async () => {
        await BlobCourier.fetchBlob(DEFAULT_FETCH_REQUEST);

        const calledWithParameters = getLastMockCallFirstParameter(
          BlobCourierNative.fetchBlob
        );

        expect(calledWithParameters).toMatchObject(
          BLOB_FETCH_FALLBACK_PARAMETERS
        );
        verifyPropertyExistsAndIsDefined(calledWithParameters, 'taskId');
      }
    );
  });

  describe('And all optional parameters are provided', () => {
    testAsync('The provided values override the fallbacks', async () => {
      const randomAndInvertedRequest = createRandomObjectFromDefault({
        ...DEFAULT_FETCH_REQUEST,
        ...BLOB_FETCH_FALLBACK_PARAMETERS,
      });

      await BlobCourier.fetchBlob(randomAndInvertedRequest);

      const calledWithParameters = getLastMockCallFirstParameter(
        BlobCourierNative.fetchBlob
      );

      const overriddenValues = dict(calledWithParameters).intersect(
        BLOB_FETCH_FALLBACK_PARAMETERS
      );

      expect(Object.keys(overriddenValues).sort()).toEqual(
        Object.keys(BLOB_FETCH_FALLBACK_PARAMETERS).sort()
      );

      Object.keys(overriddenValues).forEach((k) => {
        expect((overriddenValues as any)[k]).not.toEqual(
          (BLOB_FETCH_FALLBACK_PARAMETERS as any)[k]
        );
      });
      verifyPropertyExistsAndIsDefined(calledWithParameters, 'taskId');
    });
  });
});

describe('Given a fluent fetch request', () => {
  describe('And settings are provided', () => {
    testAsync(
      'The native module is called with all required values and the provided settings',
      async () => {
        const progressIntervalMilliseconds = Math.random();
        await BlobCourier.settings({
          progressIntervalMilliseconds,
        }).fetchBlob(DEFAULT_FETCH_REQUEST);

        const calledWithParameters = getLastMockCallFirstParameter(
          BlobCourierNative.fetchBlob
        );

        const expectedParameters = {
          ...DEFAULT_FETCH_REQUEST,
          progressIntervalMilliseconds,
        };

        expect(expectedParameters).toMatchObject(
          dict(calledWithParameters).intersect(expectedParameters)
        );
        verifyPropertyExistsAndIsDefined(calledWithParameters, 'taskId');
      }
    );
  });

  describe('And a progress updater callback is provided', () => {
    testAsync(
      'The native module is called with all required values and the provided callback',
      async () => {
        await BlobCourier.onProgress(() => {
          /* noop */
        }).fetchBlob(DEFAULT_FETCH_REQUEST);

        expect(BlobCourierEventEmitter.addListener).toHaveBeenCalled();
      }
    );

    describe('And Android Download Manager settings are provided', () => {
      testAsync(
        'The native module is called with all required values and the provided download manager settings',
        async () => {
          const androidDownloadManager = {
            description: uuid(),
            enableNotifications: true,
            title: uuid(),
          };

          await BlobCourier.onProgress(() => {})
            .useDownloadManagerOnAndroid(androidDownloadManager)
            .fetchBlob(DEFAULT_FETCH_REQUEST);

          const calledWithParameters = getLastMockCallFirstParameter(
            BlobCourierNative.fetchBlob
          );

          const expectedParameters = {
            ...DEFAULT_FETCH_REQUEST,
            useAndroidDownloadManager: true,
            androidDownloadManager,
          };

          const parameterIntersection = dict(calledWithParameters).intersect(
            expectedParameters
          );

          expect(expectedParameters).toEqual(parameterIntersection);
          expect(BlobCourierEventEmitter.addListener).toHaveBeenCalled();
          verifyPropertyExistsAndIsDefined(calledWithParameters, 'taskId');
        }
      );
    });
  });

  describe('And Android Download Manager settings are provided', () => {
    testAsync(
      'The native module is called with all required values and the provided download manager settings',
      async () => {
        const androidDownloadManager = {
          description: uuid(),
          enableNotifications: true,
          title: uuid(),
        };

        await BlobCourier.useDownloadManagerOnAndroid(
          androidDownloadManager
        ).fetchBlob(DEFAULT_FETCH_REQUEST);

        const calledWithParameters = getLastMockCallFirstParameter(
          BlobCourierNative.fetchBlob
        );

        const expectedParameters = {
          ...DEFAULT_FETCH_REQUEST,
          useAndroidDownloadManager: true,
          androidDownloadManager,
        };

        const parameterIntersection = dict(calledWithParameters).intersect(
          expectedParameters
        );

        expect(expectedParameters).toEqual(parameterIntersection);
        verifyPropertyExistsAndIsDefined(calledWithParameters, 'taskId');
      }
    );
  });
});

describe('Given a regular upload request', () => {
  testAsync(
    'The native module is called with the same parameters',
    async () => {
      await BlobCourier.uploadBlob(DEFAULT_UPLOAD_REQUEST);

      expect(BlobCourierNative.uploadBlob).toHaveBeenCalledWith(
        expect.objectContaining(DEFAULT_UPLOAD_REQUEST)
      );

      const calledWithParameters = getLastMockCallFirstParameter(
        BlobCourierNative.uploadBlob
      );

      verifyPropertyExistsAndIsDefined(calledWithParameters, 'taskId');
    }
  );

  describe('And no optional parameters are provided', () => {
    testAsync(
      'The native module is called with fallback parameters',
      async () => {
        await BlobCourier.uploadBlob(DEFAULT_UPLOAD_REQUEST);

        const calledWithParameters = getLastMockCallFirstParameter(
          BlobCourierNative.uploadBlob
        );

        expect(calledWithParameters).toMatchObject(
          BLOB_UPLOAD_FALLBACK_PARAMETERS
        );
        verifyPropertyExistsAndIsDefined(calledWithParameters, 'taskId');
      }
    );
  });

  describe('And all optional parameters are provided', () => {
    testAsync('The provided values override the fallbacks', async () => {
      const randomAndInvertedRequest = createRandomObjectFromDefault({
        ...DEFAULT_UPLOAD_REQUEST,
        ...BLOB_UPLOAD_FALLBACK_PARAMETERS,
      });

      await BlobCourier.uploadBlob(randomAndInvertedRequest);

      const calledWithParameters = getLastMockCallFirstParameter(
        BlobCourierNative.uploadBlob
      );

      const overriddenValues = dict(calledWithParameters).intersect(
        BLOB_UPLOAD_FALLBACK_PARAMETERS
      );

      expect(Object.keys(overriddenValues).sort()).toEqual(
        Object.keys(BLOB_UPLOAD_FALLBACK_PARAMETERS).sort()
      );

      Object.keys(overriddenValues).forEach((k) => {
        expect((overriddenValues as any)[k]).not.toEqual(
          (BLOB_UPLOAD_FALLBACK_PARAMETERS as any)[k]
        );
      });
      verifyPropertyExistsAndIsDefined(calledWithParameters, 'taskId');
    });
  });
});

describe('Given a fluent upload request', () => {
  describe('And settings are provided', () => {
    testAsync(
      'The native module is called with all required values and the provided settings',
      async () => {
        const progressIntervalMilliseconds = Math.random();
        await BlobCourier.settings({
          progressIntervalMilliseconds,
        }).uploadBlob(DEFAULT_UPLOAD_REQUEST);

        const calledWithParameters = getLastMockCallFirstParameter(
          BlobCourierNative.uploadBlob
        );

        const expectedParameters = {
          ...DEFAULT_FETCH_REQUEST,
          progressIntervalMilliseconds,
        };

        expect(expectedParameters).toMatchObject(
          dict(calledWithParameters).intersect(expectedParameters)
        );
        verifyPropertyExistsAndIsDefined(calledWithParameters, 'taskId');
      }
    );

    describe('And Android Download Manager settings are provided', () => {
      testAsync(
        'The native module is called with all required values and the provided download manager settings',
        async () => {
          const androidSettings = {
            description: uuid(),
            enableNotifications: true,
            title: uuid(),
          };

          const progressIntervalMilliseconds = Math.random();
          await BlobCourier.settings({
            progressIntervalMilliseconds,
          })
            .useDownloadManagerOnAndroid()
            .fetchBlob(DEFAULT_FETCH_REQUEST);

          const calledWithParameters = getLastMockCallFirstParameter(
            BlobCourierNative.fetchBlob
          );

          const expectedParameters = {
            ...DEFAULT_FETCH_REQUEST,
            useAndroidDownloadManager: true,
            androidSettings,
          };

          expect(expectedParameters).toMatchObject(
            dict(calledWithParameters).intersect(expectedParameters)
          );
          verifyPropertyExistsAndIsDefined(calledWithParameters, 'taskId');
        }
      );
    });
  });
});

describe('Given a progress updater callback is provided', () => {
  testAsync('Progress updates are handled', async () => {
    const p0 = jest.fn();

    const r0 = BlobCourier.onProgress(p0).uploadBlob(DEFAULT_UPLOAD_REQUEST);

    const { taskId } = getLastMockCallFirstParameter(
      BlobCourierNative.uploadBlob
    );

    const TEST_EVENT_PAYLOAD = {
      taskId,
      written: 42,
      total: 314,
    };

    NATIVE_EVENT_EMITTER_SINGLETON.emit(
      BLOB_COURIER_PROGRESS_EVENT_NAME,
      TEST_EVENT_PAYLOAD
    );

    const expectedArgument = (({ written, total }) => ({
      written,
      total,
    }))(TEST_EVENT_PAYLOAD);

    expect(p0).toBeCalledTimes(1);
    expect(p0).toHaveBeenCalledWith(expectedArgument);

    await r0;
  });

  testAsync('It only fires for this particular instance', async () => {
    const p0 = jest.fn();

    const r0 = BlobCourier.onProgress(p0).uploadBlob(DEFAULT_UPLOAD_REQUEST);

    const { taskId } = getLastMockCallFirstParameter(
      BlobCourierNative.uploadBlob
    );

    const TEST_EVENT_PAYLOAD = {
      written: 42,
      total: 314,
    };

    const SUCCESS_TEST_EVENT_PAYLOAD = {
      taskId,
      ...TEST_EVENT_PAYLOAD,
    };

    const FAILED_TEST_EVENT_PAYLOAD = {
      taskId: uuid(),
      ...TEST_EVENT_PAYLOAD,
    };

    NATIVE_EVENT_EMITTER_SINGLETON.emit(
      BLOB_COURIER_PROGRESS_EVENT_NAME,
      FAILED_TEST_EVENT_PAYLOAD
    );

    NATIVE_EVENT_EMITTER_SINGLETON.emit(
      BLOB_COURIER_PROGRESS_EVENT_NAME,
      SUCCESS_TEST_EVENT_PAYLOAD
    );

    expect(p0).toBeCalledTimes(1);

    await r0;
  });
});
