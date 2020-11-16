/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
import { uuid } from '../Utils';
import {
  BLOB_FETCH_FALLBACK_PARAMETERS,
  BLOB_UPLOAD_FALLBACK_PARAMETERS,
} from '../Consts';
import { dict } from '../Extensions';
import { NativeModules } from 'react-native';
import BlobCourier from '../index';
const {
  BlobCourier: BlobCourierNative,
  BlobCourierEventEmitter,
} = NativeModules;

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

beforeEach(() => {
  BlobCourierNative.fetchBlob.mockReset();
  BlobCourierNative.uploadBlob.mockReset();
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
        const progressIntervalMilliseconds = Math.random();
        await BlobCourier.settings({ progressIntervalMilliseconds })
          .onProgress(() => {
            /* noop */
          })
          .fetchBlob(DEFAULT_FETCH_REQUEST);

        const calledWithParameters = getLastMockCallFirstParameter(
          BlobCourierNative.fetchBlob
        );

        expect(BlobCourierEventEmitter.addListener).toHaveBeenCalled();

        verifyPropertyExistsAndIsDefined(calledWithParameters, 'taskId');
      }
    );

    testAsync('Progress updates are handled', async () => {
      await BlobCourier.onProgress(() => {
        /* noop */
      }).fetchBlob(DEFAULT_FETCH_REQUEST);

      const calledWithParameters = getLastMockCallFirstParameter(
        BlobCourierNative.fetchBlob
      );

      expect(BlobCourierEventEmitter.addListener).toHaveBeenCalled();

      verifyPropertyExistsAndIsDefined(calledWithParameters, 'taskId');
    });
  });

  describe('And Android Download Manager settings are provided', () => {
    testAsync(
      'The native module is called with all required values and the provided download manager settings',
      async () => {
        const androidSettings = {
          description: uuid(),
          enableNotifications: true,
          title: uuid(),
        };

        await BlobCourier.useDownloadManagerOnAndroid(
          androidSettings
        ).fetchBlob(DEFAULT_FETCH_REQUEST);

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
  });

  describe('And a progress updater callback is provided', () => {
    testAsync(
      'The native module is called with all required values and the provided callback',
      async () => {
        await BlobCourier.onProgress(() => {
          /* noop */
        }).uploadBlob(DEFAULT_UPLOAD_REQUEST);

        const calledWithParameters = getLastMockCallFirstParameter(
          BlobCourierNative.uploadBlob
        );

        expect(BlobCourierEventEmitter.addListener).toHaveBeenCalled();

        verifyPropertyExistsAndIsDefined(calledWithParameters, 'taskId');
      }
    );

    testAsync('Progress updates are handled', async () => {
      await BlobCourier.onProgress(() => {
        /* noop */
      }).uploadBlob(DEFAULT_UPLOAD_REQUEST);

      const calledWithParameters = getLastMockCallFirstParameter(
        BlobCourierNative.uploadBlob
      );

      expect(BlobCourierEventEmitter.addListener).toHaveBeenCalled();

      verifyPropertyExistsAndIsDefined(calledWithParameters, 'taskId');
    });
  });
});
