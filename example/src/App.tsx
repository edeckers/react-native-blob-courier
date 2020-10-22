import React, { useState } from 'react';
import * as Progress from 'react-native-progress';

import {
  StyleSheet,
  Button,
  View,
  Text,
  PermissionsAndroid,
  Platform,
} from 'react-native';
import BlobCourier from 'react-native-blob-courier';
import {
  AndroidBlobRequest,
  BlobUnmanagedData,
  BlobManagedData,
  BlobResponse,
  BlobResponseType,
  BlobUploadRequest,
} from 'react-native-blob-courier';

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
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

interface PIProps {
  value: number;
  width?: number;
  total?: number;
}

const ProgressIndicator = (props: PIProps) => {
  const progress = props.total ? props.value / props.total : 0;

  return (
    <>
      <Progress.Bar progress={progress} width={props.width} />
      <Text>
        {props.value}/{props.total ?? '?'}
      </Text>
    </>
  );
};

interface UVProps {
  fromLocalPath: string;
  onFinished: (response: BlobResponse) => void;
  toUrl: string;
}

const UploaderView = (props: UVProps) => {
  const [received, setReceived] = useState<number>(0);
  const [expected, setExpected] = useState<number>(0);

  const startUpload = async () => {
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
  };

  return (
    <>
      <ProgressIndicator value={received} total={expected} />
      <Button title="Start upload" onPress={() => startUpload()} />
    </>
  );
};

interface DVProps {
  filename: string;
  fromUrl: string;
  onFinished: (response: BlobResponse) => void;
}

const DownloaderView = (props: DVProps) => {
  const [received, setReceived] = useState<number>(0);
  const [expected, setExpected] = useState<number>(0);

  const startDownload = async () => {
    const fetchedResult = await BlobCourier.fetchBlob({
      filename: props.filename,
      method: 'GET',
      url: props.fromUrl,
      useDownloadManager: false,
    } as AndroidBlobRequest).onProgress((e: any) => {
      setReceived(parseInt(e.written, 10));
      setExpected(parseInt(e.total, 10));
    });

    props.onFinished(fetchedResult);
  };

  return (
    <>
      <ProgressIndicator value={received} total={expected} />
      <Button title="Start download" onPress={() => startDownload()} />
    </>
  );
};

export const App = () => {
  const [downloadedFilePath, setDownloadedFilePath] = React.useState<string>();

  React.useEffect(() => {
    requestRequiredPermissionsOnAndroidAsync();
  }, []);

  const onDownloadCompleted = (downloadResult: BlobResponse) => {
    const theFilePath =
      downloadResult.type === BlobResponseType.Managed
        ? (downloadResult.data as BlobManagedData).fullFilePath
        : (downloadResult.data as BlobUnmanagedData).fullFilePath;

    setDownloadedFilePath(theFilePath);
  };

  return (
    <View style={styles.container}>
      {!downloadedFilePath ? (
        <DownloaderView
          fromUrl="http://ipv4.download.thinkbroadband.com/100MB.zip"
          filename="100MB.zip"
          onFinished={onDownloadCompleted}
        />
      ) : (
        <UploaderView
          fromLocalPath={downloadedFilePath}
          onFinished={() => {}}
          toUrl="https://file.io"
        />
      )}
    </View>
  );
};
