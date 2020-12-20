/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
import { uuid } from '../Utils';
import {
  ANDROID_DOWNLOAD_MANAGER_FALLBACK_PARAMETERS,
  BLOB_COURIER_PROGRESS_EVENT_NAME,
  BLOB_FETCH_FALLBACK_PARAMETERS,
  BLOB_MULTIPART_UPLOAD_FALLBACK_PARAMETERS,
  BLOB_UPLOAD_FALLBACK_PARAMETERS,
  DEFAULT_FETCH_TARGET,
  DEFAULT_FILE_MULTIPART_FIELD_NAME,
  DEFAULT_PROGRESS_UPDATE_INTERVAL_MILLISECONDS,
} from '../Consts';
import { dict } from '../Extensions';
import { NativeEventEmitter, NativeModules } from 'react-native';
import BlobCourier from '../index';
import type { BlobMultipartUploadRequest, TargetType } from 'src/ExposedTypes';

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
  absoluteFilePath: '/path/to/some_file.txt',
  mimeType: 'plain/text',
  url: 'https://github.com/edeckers/react-native-blob-courier',
};

const DEFAULT_MULTIPART_UPLOAD_REQUEST: BlobMultipartUploadRequest = {
  parts: {
    file: {
      payload: {
        absoluteFilePath: '/path/to/some_file.txt',
        filename: undefined,
        mimeType: 'plain/text',
      },
      type: 'file',
    },
  },
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
  type in RANDOM_VALUE_GENERATORS ? RANDOM_VALUE_GENERATORS[type]() : uuid();

const createRandomObjectFromDefault = <T,>(o: T): T =>
  Object.keys(o).reduce(
    (p, c) => ({
      ...p,
      [c]: (() => {
        const m = (o as any)[c];

        if (typeof m === 'boolean') {
          return !m;
        }

        if (typeof m === 'object') {
          return createRandomObjectFromDefault(
            Object.keys(m).length === 0 ? generateValue('object') : m
          );
        }

        return generateValue(typeof m);
      })(),
    }),
    {}
  ) as T;

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
      android: {
        downloadManager: ANDROID_DOWNLOAD_MANAGER_FALLBACK_PARAMETERS,
        target: DEFAULT_FETCH_TARGET,
        useDownloadManager: false,
      },
      headers: {},
      ios: {
        target: DEFAULT_FETCH_TARGET,
      },
      method: 'GET',
      progressIntervalMilliseconds: DEFAULT_PROGRESS_UPDATE_INTERVAL_MILLISECONDS,
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

    testAsync('Fallback overrides provided `undefined` value', async () => {
      const randomAndInvertedRequest = createRandomObjectFromDefault({
        ...DEFAULT_FETCH_REQUEST,
        ...BLOB_FETCH_FALLBACK_PARAMETERS,
      });

      await BlobCourier.fetchBlob(randomAndInvertedRequest);

      const calledWithParameters = getLastMockCallFirstParameter(
        BlobCourierNative.fetchBlob
      );

      const calledWithSomeUndefinedParameter = {
        ...calledWithParameters,
        headers: undefined,
      };

      const overriddenValues = dict(calledWithSomeUndefinedParameter).intersect(
        BLOB_FETCH_FALLBACK_PARAMETERS
      );

      expect(overriddenValues.headers).toBeDefined();
    });
  });
});

describe('Given a fluent fetch request', () => {
  describe('And settings are provided', () => {
    testAsync(
      'The native module is called with all required values and the provided settings',
      async () => {
        const progressIntervalMilliseconds = DEFAULT_PROGRESS_UPDATE_INTERVAL_MILLISECONDS;

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

  describe('And a target is provided', () => {
    const targets = ['cache', 'data'] as TargetType[];
    for (const target of targets) {
      testAsync(
        `The native module is called with the provided target '${target}'`,
        async () => {
          const android = { target };
          const ios = { target };
          const expectedParameters = {
            ...DEFAULT_FETCH_REQUEST,
            android,
            ios,
          };

          await BlobCourier.fetchBlob(expectedParameters);

          const calledWithParameters = getLastMockCallFirstParameter(
            BlobCourierNative.fetchBlob
          );

          expect(calledWithParameters.android.target).toEqual(
            expectedParameters.android.target
          );
          expect(calledWithParameters.ios.target).toEqual(
            expectedParameters.ios.target
          );
        }
      );
    }
  });

  describe('And no target is provided', () => {
    testAsync(
      `The native module is called with fallback '${DEFAULT_FETCH_TARGET}'`,
      async () => {
        const expectedParameters = {
          ...DEFAULT_FETCH_REQUEST,
          android: {
            target: DEFAULT_FETCH_TARGET,
          },
          ios: {
            target: DEFAULT_FETCH_TARGET,
          },
        };

        await BlobCourier.fetchBlob(DEFAULT_FETCH_REQUEST);

        const calledWithParameters = getLastMockCallFirstParameter(
          BlobCourierNative.fetchBlob
        );

        expect(calledWithParameters.android.target).toEqual(
          expectedParameters.android.target
        );
        expect(calledWithParameters.ios.target).toEqual(
          expectedParameters.ios.target
        );
      }
    );
  });

  describe('And a progress updater callback is provided', () => {
    testAsync(
      'The native module is called with all required values and the provided callback',
      async () => {
        await BlobCourier.settings({})
          .onProgress(() => {
            /* noop */
          })
          .fetchBlob(DEFAULT_FETCH_REQUEST);

        expect(BlobCourierEventEmitter.addListener).toHaveBeenCalled();
      }
    );

    describe('And Android Download Manager settings are provided', () => {
      testAsync(
        'The native module is called with all required values and the provided download manager settings',
        async () => {
          const downloadManager = {
            description: uuid(),
            enableNotifications: true,
            title: uuid(),
          };

          await BlobCourier.onProgress(() => {})
            .useDownloadManagerOnAndroid(downloadManager)
            .fetchBlob(DEFAULT_FETCH_REQUEST);

          const calledWithParameters = getLastMockCallFirstParameter(
            BlobCourierNative.fetchBlob
          );

          const expectedParameters = {
            ...DEFAULT_FETCH_REQUEST,
            android: {
              downloadManager,
              target: DEFAULT_FETCH_TARGET,
              useDownloadManager: true,
            },
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
        const downloadManager = {
          description: uuid(),
          enableNotifications: true,
          title: uuid(),
        };

        await BlobCourier.useDownloadManagerOnAndroid(
          downloadManager
        ).fetchBlob(DEFAULT_FETCH_REQUEST);

        const calledWithParameters = getLastMockCallFirstParameter(
          BlobCourierNative.fetchBlob
        );

        const expectedParameters = {
          ...DEFAULT_FETCH_REQUEST,
          android: {
            downloadManager,
            target: DEFAULT_FETCH_TARGET,
            useDownloadManager: true,
          },
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

      const calledWithParameters = getLastMockCallFirstParameter(
        BlobCourierNative.uploadBlob
      );

      const parameterIntersection = dict(calledWithParameters).intersect(
        DEFAULT_MULTIPART_UPLOAD_REQUEST
      );

      expect(parameterIntersection).toEqual(DEFAULT_MULTIPART_UPLOAD_REQUEST);
      verifyPropertyExistsAndIsDefined(calledWithParameters, 'taskId');
    }
  );

  testAsync('Missing multipartNames are generated', async () => {
    await BlobCourier.uploadBlob(DEFAULT_UPLOAD_REQUEST);

    const calledWithParameters = getLastMockCallFirstParameter(
      BlobCourierNative.uploadBlob
    );

    verifyPropertyExistsAndIsDefined(
      calledWithParameters.parts,
      DEFAULT_FILE_MULTIPART_FIELD_NAME
    );
  });

  testAsync('Provided multipartNames are used', async () => {
    const multipartName = RANDOM_VALUE_GENERATORS.string();

    await BlobCourier.uploadBlob({
      ...DEFAULT_UPLOAD_REQUEST,
      multipartName,
    });

    const calledWithParameters = getLastMockCallFirstParameter(
      BlobCourierNative.uploadBlob
    );

    verifyPropertyExistsAndIsDefined(calledWithParameters.parts, multipartName);
  });

  testAsync('JSON multi-part payloads are serialized', async () => {
    await BlobCourier.uploadParts({
      ...DEFAULT_UPLOAD_REQUEST,
      parts: {
        some_json_part: {
          type: 'string',
          payload: { a: 1 },
        },
      },
    });

    const calledWithParameters = getLastMockCallFirstParameter(
      BlobCourierNative.uploadBlob
    );

    expect(typeof calledWithParameters.parts.some_json_part.payload).toEqual(
      'string'
    );
  });

  testAsync(
    'The native module is called with all required multi part-values',
    async () => {
      const progressIntervalMilliseconds = Math.random();
      await BlobCourier.uploadParts(DEFAULT_MULTIPART_UPLOAD_REQUEST);

      const calledWithParameters = getLastMockCallFirstParameter(
        BlobCourierNative.uploadBlob
      );

      const expectedParameters = {
        ...DEFAULT_MULTIPART_UPLOAD_REQUEST,
        progressIntervalMilliseconds,
      };

      expect(dict(calledWithParameters).intersect(expectedParameters)).toEqual(
        expectedParameters
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
        BLOB_MULTIPART_UPLOAD_FALLBACK_PARAMETERS
      );

      expect(Object.keys(overriddenValues).sort()).toEqual(
        Object.keys(BLOB_MULTIPART_UPLOAD_FALLBACK_PARAMETERS).sort()
      );

      Object.keys(overriddenValues).forEach((k) => {
        expect((overriddenValues as any)[k]).not.toEqual(
          (BLOB_MULTIPART_UPLOAD_FALLBACK_PARAMETERS as any)[k]
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
          ...DEFAULT_MULTIPART_UPLOAD_REQUEST,
          progressIntervalMilliseconds,
        };

        expect(
          dict(calledWithParameters).intersect(expectedParameters)
        ).toEqual(expectedParameters);

        verifyPropertyExistsAndIsDefined(calledWithParameters, 'taskId');
      }
    );

    describe('And a progress updater is provided provided', () => {
      testAsync(
        'The native module is called with all required multi part-values and the provided settings',
        async () => {
          const progressIntervalMilliseconds = Math.random();
          await BlobCourier.onProgress(() => {
            /* noop */
          }).uploadParts(DEFAULT_MULTIPART_UPLOAD_REQUEST);

          const calledWithParameters = getLastMockCallFirstParameter(
            BlobCourierNative.uploadBlob
          );

          const expectedParameters = {
            ...DEFAULT_MULTIPART_UPLOAD_REQUEST,
            progressIntervalMilliseconds,
          };

          expect(
            dict(calledWithParameters).intersect(expectedParameters)
          ).toEqual(expectedParameters);

          verifyPropertyExistsAndIsDefined(calledWithParameters, 'taskId');
        }
      );
    });

    testAsync(
      'The native module is called with all required multi part-values and the provided settings',
      async () => {
        const progressIntervalMilliseconds = Math.random();
        await BlobCourier.settings({
          progressIntervalMilliseconds,
        }).uploadParts(DEFAULT_MULTIPART_UPLOAD_REQUEST);

        const calledWithParameters = getLastMockCallFirstParameter(
          BlobCourierNative.uploadBlob
        );

        const expectedParameters = {
          ...DEFAULT_MULTIPART_UPLOAD_REQUEST,
          progressIntervalMilliseconds,
        };

        expect(
          dict(calledWithParameters).intersect(expectedParameters)
        ).toEqual(expectedParameters);

        verifyPropertyExistsAndIsDefined(calledWithParameters, 'taskId');
      }
    );

    describe('And Android Download Manager settings are provided', () => {
      testAsync(
        'The native module is called with all required values and the provided download manager settings',
        async () => {
          const downloadManager = {
            description: uuid(),
            enableNotifications: true,
            title: uuid(),
          };

          const progressIntervalMilliseconds = Math.random();
          await BlobCourier.settings({
            progressIntervalMilliseconds,
          })
            .useDownloadManagerOnAndroid(downloadManager)
            .fetchBlob(DEFAULT_FETCH_REQUEST);

          const calledWithParameters = getLastMockCallFirstParameter(
            BlobCourierNative.fetchBlob
          );

          const expectedParameters = {
            ...DEFAULT_FETCH_REQUEST,
            android: {
              useDownloadManager: true,
              downloadManager,
              target: DEFAULT_FETCH_TARGET,
            },
          };

          expect(expectedParameters).toEqual(
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
