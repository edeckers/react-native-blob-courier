import * as React from 'react';
import * as Progress from 'react-native-progress';

import {
  StyleSheet,
  Button,
  View,
  Text,
  PermissionsAndroid,
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

export const App = () => {
  const [downloadResult, setDownloadResult] = React.useState<BlobResponse>();
  const [progress, setProgress] = React.useState<number>();
  const [received, setReceived] = React.useState<number>();
  const [expected, setExpected] = React.useState<number>();

  React.useEffect(() => {
    const requestPermissionAndDownloadBlobAsync = async () => {
      try {
        await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE
        );
      } catch (err) {
        console.warn(err);
      }
    };

    requestPermissionAndDownloadBlobAsync();
  }, []);

  const startDownload = async () => {
    console.debug('startDownload');
    setProgress(0);
    setReceived(0);
    setExpected(0);
    const fetchedBlob = await BlobCourier.fetchBlob({
      filename: '100MB.zip',
      method: 'GET',
      useDownloadManager: false,
      url: 'http://ipv4.download.thinkbroadband.com/100MB.zip',
    } as AndroidBlobRequest).onProgress((e: any) => {
      setProgress(parseInt(e.written, 10) / e.total);
      setReceived(parseInt(e.written, 10));
      setReceived(e.written);
      setExpected(parseInt(e.total, 10));
    });

    setDownloadResult(fetchedBlob);
  };

  const startUpload = async () => {
    if (!downloadResult) {
      return;
    }
    setProgress(0);
    setReceived(0);
    setExpected(0);

    const filePath =
      downloadResult.type === BlobResponseType.Managed
        ? (downloadResult.data as BlobManagedData).fullFilePath
        : (downloadResult.data as BlobUnmanagedData).fullFilePath;

    const uploadResult = await BlobCourier.uploadBlob({
      filePath,
      method: 'POST',
      mimeType: 'text/plain',
      url: 'https://file.io',
    } as BlobUploadRequest).onProgress((e: any) => {
      setProgress(parseInt(e.written, 10) / e.total);
      setReceived(parseInt(e.written, 10));
      setReceived(e.written);
      setExpected(parseInt(e.total, 10));
    });

    console.warn(JSON.stringify(uploadResult));
  };

  return (
    <View style={styles.container}>
      <Progress.Bar progress={progress} width={200} />
      <Text>
        {received}/{expected ?? '?'}
      </Text>
      <Button title="Download" onPress={() => startDownload()} />
      <Button title="Upload" onPress={() => startUpload()} />
      <Text>Result: {JSON.stringify(downloadResult)}</Text>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
