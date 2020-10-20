import * as React from 'react';
import * as Progress from 'react-native-progress';

import {
  StyleSheet,
  View,
  Text,
  PermissionsAndroid,
  Button,
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
    const fetchedBlob = await BlobCourier.fetchBlob({
      filename: '5MB.zip',
      method: 'GET',
      useDownloadManager: true,
      url: 'http://ipv4.download.thinkbroadband.com/5MB.zip',
    } as AndroidBlobRequest).onProgress((e: any) => {
      setProgress(parseInt(e.written, 10) / 5242880);
      setReceived(parseInt(e.written, 10));
      setExpected(parseInt(e.total, 10));
    });

    setDownloadResult(fetchedBlob);
  };

  const startUpload = async () => {
    if (!downloadResult) {
      return;
    }

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
      console.log(JSON.stringify(e));
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
