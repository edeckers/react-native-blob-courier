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
  AndroidBlobRequest,
  BlobFilePathData,
  BlobResponse,
  BlobUploadRequest,
} from 'react-native-blob-courier';

const DEFAULT_MARGIN = 10;

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

const ProgressIndicator = (props: PIProps) => {
  const progress = props.total ? props.value / props.total : 0;

  return (
    <>
      <Progress.Bar
        animationType={'timing'}
        indeterminate={props.total === undefined}
        progress={progress}
        useNativeDriver={true}
        width={props.width}
      />
      <Text style={styles.progress}>
        {formatBytes(props.value)}B /{' '}
        {props.total !== undefined ? formatBytes(props.total) : '?'}B
      </Text>
    </>
  );
};

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
  onFinished: (response: BlobResponse) => void;
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
      const uploadResult = await BlobCourier.uploadBlob({
        filePath: props.fromLocalPath,
        method: 'POST',
        mimeType: 'text/plain',
        url: props.toUrl,
      } as BlobUploadRequest).onProgress((e: any) => {
        setReceived(parseInt(e.written, 10));
        setExpected(parseInt(e.total, 10));
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
    <Text>Enable managed</Text>
    <Switch onValueChange={props.onValueChange} value={props.value} />
  </View>
);

interface DVProps {
  filename: string;
  fromUrl: string;
  onFinished: (response: BlobResponse) => void;
}

const DownloaderView = (props: DVProps) => {
  const [isDownloading, setIsDownloading] = useState(false);
  const [useDownloadManager, setUseDownloadManager] = useState(false);
  const [received, setReceived] = useState<number>(0);
  const [expected, setExpected] = useState<number | undefined>(0);

  const buttonText = isDownloading ? 'Downloading...' : 'Start download';

  const startDownload = async () => {
    setIsDownloading(true);

    try {
      const fetchedResult = await BlobCourier.fetchBlob({
        filename: props.filename,
        headers: {
          A: 'B',
        },
        method: 'GET',
        url: props.fromUrl,
        useDownloadManager: useDownloadManager,
      } as AndroidBlobRequest).onProgress((e: any) => {
        const serializedMaybeTotal = parseInt(e.total, 10);
        const maybeTotal =
          serializedMaybeTotal > 0 ? serializedMaybeTotal : undefined;

        setReceived(parseInt(e.written, 10));
        setExpected(maybeTotal);
      });

      props.onFinished(fetchedResult);
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
        onValueChange={() => setUseDownloadManager(!useDownloadManager)}
        value={useDownloadManager}
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

  const onDownloadCompleted = (downloadResult: BlobResponse) => {
    setDownloadedFilePath(
      (downloadResult.data as BlobFilePathData).fullFilePath ?? ''
    );

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
        toUrl="https://file.io"
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
