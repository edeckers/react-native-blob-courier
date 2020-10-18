import * as React from 'react';
import { StyleSheet, View, Text, PermissionsAndroid } from 'react-native';
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

  React.useEffect(() => {
    const requestPermissionAndDownloadBlobAsync = async () => {
      try {
        await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE
        );
      } catch (err) {
        console.warn(err);
      }

      const fetchedBlob = await BlobCourier.fetchBlob({
        filename: 'drop.avi',
        method: 'GET',
        useDownloadManager: false,
        url: 'https://www.engr.colostate.edu/me/facil/dynamics/files/drop.avi',
      } as AndroidBlobRequest).onProgress((e) => {
        console.debug('Received progress update:', JSON.stringify(e));
      });
      const filePath =
        fetchedBlob.type === BlobResponseType.Managed
          ? (fetchedBlob.data as BlobManagedData).fullFilePath
          : (fetchedBlob.data as BlobUnmanagedData).fullFilePath;

      console.log(JSON.stringify(fetchedBlob), filePath);
      setDownloadResult(fetchedBlob);

      const uploadResult = await BlobCourier.uploadBlob({
        filePath,
        method: 'POST',
        mimeType: 'text/plain',
        url: 'https://file.io',
      } as BlobUploadRequest).onProgress((e) => {
        console.debug('Received progress update:', JSON.stringify(e));
      });

      console.warn(JSON.stringify(uploadResult));
    };

    requestPermissionAndDownloadBlobAsync();
  }, []);

  return (
    <View style={styles.container}>
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
