import React, { useState } from 'react';
import * as Progress from 'react-native-progress';

import {
  StyleSheet,
  Button,
  View,
  Text,
  PermissionsAndroid,
  Platform,
  StyleProp,
  ViewStyle,
  Switch,
} from 'react-native';
import BlobCourier, {
  BlobFetchResponse,
  BlobUploadResponse,
  BlobFetchRequest,
  BlobProgressEvent,
} from 'react-native-blob-courier';

const DEFAULT_MARGIN = 10;
const DEFAULT_PROGRESS_INTERVAL_MILLISECONDS = 200;

const styles = StyleSheet.create({
  container: {
    marginVertical: DEFAULT_MARGIN,
  },
  downloadToggle: { alignItems: 'center' },
  mainContainer: {
    alignItems: 'center',
    flex: 1,
    justifyContent: 'center',
    padding: DEFAULT_MARGIN,
  },
  header: {
    fontSize: 35,
    fontWeight: 'bold',
    textAlign: 'center',
  },
  headerContainer: {
    alignItems: 'center',
    flex: 1,
    justifyContent: 'center',
    padding: DEFAULT_MARGIN,
  },
  mainContent: { flex: 0.75 },
  mainHeader: {
    flex: 0.25,
    justifyContent: 'center',
    flexDirection: 'column',
  },
  metaBox: {
    flex: 0.25,
    width: '80%',
  },
  metaText: {
    fontSize: 12.5,
    fontWeight: 'normal',
  },
  metaLabel: {
    fontSize: 12.5,
    fontWeight: 'bold',
  },
  progress: {
    fontSize: 12.5,
  },
  progressContainer: {
    alignItems: 'center',
    flex: 1,
    justifyContent: 'center',
  },
  uploadDownloadContentBox: { flex: 0.2, alignItems: 'center' },
  uploadDownloadMetaBox: { flex: 0.25, width: '80%' },
});

const requestRequiredPermissionsOnAndroidAsync = async () => {
  if (Platform.OS !== 'android') {
    return;
  }

  try {
    await PermissionsAndroid.request(
      PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE
    );
  } catch (err) {
    console.warn(err);
  }
};

const formatBytes = (bytes: number) => bytes.toLocaleString('en-US');

interface HVProps {
  style?: StyleProp<ViewStyle>;
  title: string;
}

const HeaderView = (props: HVProps) => (
  <Text style={styles.header}>{props.title}</Text>
);

interface MVProps {
  from: string;
  style?: StyleProp<ViewStyle>;
  to: string;
}

const MetaView = (props: MVProps) => (
  <>
    <Text style={styles.metaLabel}>
      From: <Text style={styles.metaText}>{props.from}</Text>
    </Text>
    <Text style={styles.metaLabel}>
      To: <Text style={styles.metaText}>{props.to}</Text>
    </Text>
  </>
);

interface PIProps {
  value: number;
  width?: number;
  total?: number;
}

const ProgressIndicator = (props: PIProps) => (
  <>
    <Progress.Bar
      animationType={'timing'}
      indeterminate={props.total === undefined}
      indeterminateAnimationDuration={
        DEFAULT_PROGRESS_INTERVAL_MILLISECONDS * 2 // Prevent 'glitchy' animation
      }
      progress={props.total ? props.value / props.total : 0}
      useNativeDriver={true}
      width={props.width}
    />
    <Text style={styles.progress}>
      {formatBytes(props.value)}B /{' '}
      {props.total !== undefined ? formatBytes(props.total) : '?'}B
    </Text>
  </>
);

interface UDVProps {
  buttonText: string;
  from: string;
  isButtonEnabled: boolean;
  onPress: () => void;
  progress: number;
  progressTotal?: number;
  to: string;
}

const UploadDownloadView = (props: UDVProps) => (
  <>
    <View style={styles.uploadDownloadMetaBox}>
      <MetaView from={props.from} to={props.to} />
    </View>
    <View style={[styles.uploadDownloadContentBox]}>
      <ProgressIndicator value={props.progress} total={props.progressTotal} />
      <View style={styles.container}>
        <Button
          disabled={!props.isButtonEnabled}
          onPress={props.onPress}
          title={props.buttonText}
        />
      </View>
    </View>
  </>
);

interface UVProps {
  fromLocalPath: string;
  onFinished: (response: BlobUploadResponse) => void;
  toUrl: string;
}

const UploaderView = (props: UVProps) => {
  const [isUploading, setIsUploading] = useState(false);
  const [received, setReceived] = useState<number>(0);
  const [expected, setExpected] = useState<number | undefined>(0);

  const buttonText = isUploading ? 'Uploading...' : 'Start upload';

  const startUpload = async () => {
    setIsUploading(true);

    try {
      const uploadResult = await BlobCourier.settings({
        progressIntervalMilliseconds: DEFAULT_PROGRESS_INTERVAL_MILLISECONDS,
      })
        .onProgress((e: BlobProgressEvent) => {
          setReceived(e.written);
          setExpected(e.total);
        })
        // .uploadBlob({
        //   absoluteFilePath: props.fromLocalPath,
        //   method: 'POST',
        //   multipartName: 'file',
        //   mimeType: 'text/plain',
        //   returnResponse: true,
        //   url: props.toUrl,
        // });
        .uploadParts({
          method: 'POST',
          parts: {
            body: {
              value: {
                a: 'b',
                c: 42,
              },
              type: 'string',
            },
            file: {
              absoluteFilePath: props.fromLocalPath,
              mimeType: 'text/plain',
              type: 'file',
            },
          },
          returnResponse: true,
          url: props.toUrl,
        });

      props.onFinished(uploadResult);
    } catch (e) {
      console.warn(e);
    }
  };

  return (
    <UploadDownloadView
      buttonText={buttonText}
      from={props.fromLocalPath}
      isButtonEnabled={!isUploading}
      onPress={startUpload}
      progress={received}
      progressTotal={expected}
      to={props.toUrl}
    />
  );
};

interface MDTProps {
  onValueChange: () => void;
  value?: boolean;
}
const ManagedDownloadToggle = (props: MDTProps) => (
  <View style={styles.downloadToggle}>
    <Text>
      Enable managed
      {Platform.OS !== 'android' ? ' (disabled: Android only)' : ''}
    </Text>
    <Switch
      disabled={Platform.OS !== 'android'}
      onValueChange={props.onValueChange}
      value={props.value}
    />
  </View>
);

interface DVProps {
  filename: string;
  fromUrl: string;
  mimeType?: string;
  onFinished: (response: BlobFetchResponse) => void;
}

const DownloaderView = (props: DVProps) => {
  const [isDownloading, setIsDownloading] = useState(false);
  const [useAndroidDownloadManager, setUseAndroidDownloadManager] = useState(
    false
  );
  const [received, setReceived] = useState<number>(0);
  const [expected, setExpected] = useState<number | undefined>(0);

  const buttonText = isDownloading ? 'Downloading...' : 'Start download';

  const startDownload = async () => {
    setIsDownloading(true);

    const req0 = {
      filename: props.filename,
      method: 'GET',
      mimeType: 'text/plain',
      url: props.fromUrl,
    } as BlobFetchRequest;

    try {
      const reqSettings = BlobCourier.settings({
        progressIntervalMilliseconds: DEFAULT_PROGRESS_INTERVAL_MILLISECONDS,
      }).onProgress((e: BlobProgressEvent) => {
        const maybeTotal = e.total > 0 ? e.total : undefined;

        setReceived(e.written);
        setExpected(maybeTotal);
      });

      const withDownloadManager = useAndroidDownloadManager
        ? reqSettings.useDownloadManagerOnAndroid({
            enableNotifications: true,
          })
        : reqSettings;

      const fetchedResult = await withDownloadManager.fetchBlob(req0);

      setTimeout(
        () => props.onFinished(fetchedResult),
        DEFAULT_PROGRESS_INTERVAL_MILLISECONDS * 2 // Allow progress indicator to finish / prevent 'glitchy' ui
      );
    } catch (e) {
      console.warn(e);
    }
  };

  return (
    <>
      <UploadDownloadView
        buttonText={buttonText}
        from={props.fromUrl}
        isButtonEnabled={!isDownloading}
        onPress={startDownload}
        progress={received}
        progressTotal={expected}
        to={props.filename}
      />
      <ManagedDownloadToggle
        onValueChange={() =>
          setUseAndroidDownloadManager(!useAndroidDownloadManager)
        }
        value={useAndroidDownloadManager}
      />
    </>
  );
};

interface FVProps {
  onRetry: () => void;
}

const FinishedView = (props: FVProps) => (
  <View style={styles.container}>
    <Text>Finished!</Text>
    <Button title="Retry" onPress={props.onRetry} />
  </View>
);

type Route = 'download' | 'upload' | 'finished';

export const App = () => {
  const [downloadedFilePath, setDownloadedFilePath] = React.useState<string>();
  const [route, setRoute] = React.useState<Route>('download');

  React.useEffect(() => {
    requestRequiredPermissionsOnAndroidAsync();
  }, []);

  const onDownloadCompleted = (downloadResult: BlobFetchResponse) => {
    setDownloadedFilePath(downloadResult.data.absoluteFilePath);

    setRoute('upload');
  };

  const onUploadCompleted = () => {
    setRoute('finished');
  };

  const onRetry = () => {
    setDownloadedFilePath(undefined);

    setRoute('download');
  };

  const routeToViewMapping = {
    download: (
      <DownloaderView
        fromUrl="http://ipv4.download.thinkbroadband.com/5MB.zip"
        filename="5MB.zip"
        onFinished={onDownloadCompleted}
      />
    ),
    upload: (
      <UploaderView
        fromLocalPath={downloadedFilePath ?? ''}
        onFinished={onUploadCompleted}
        // toUrl="https://file.io"
        toUrl="http://192.168.0.103/uploader"
      />
    ),
    finished: <FinishedView onRetry={onRetry} />,
  };

  const ContentView = (cvProps: { route: Route }) =>
    routeToViewMapping[cvProps.route];

  return (
    <View style={styles.mainContainer}>
      <View style={styles.mainHeader}>
        <HeaderView
          style={styles.headerContainer}
          title="React Native Blob Courier"
        />
      </View>
      <View style={styles.mainContent}>
        <ContentView route={route} />
      </View>
    </View>
  );
};
