import * as React from 'react';
import { StyleSheet, View, Text, PermissionsAndroid } from 'react-native';
import BlobCourier from 'react-native-blob-courier';
import {
  AndroidBlobRequest,
  BlobHttpResponse,
  BlobManagedResponse,
  BlobResponse,
  BlobResponseType,
  BlobUploadRequest,
} from 'src/Requests';

export const App = () => {
  const [result, setResult] = React.useState<BlobResponse>();

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
        filename: 'drop2.avi',
        method: 'GET',
        useDownloadManager: true,
        url: 'https://www.engr.colostate.edu/me/facil/dynamics/files/drop.avi',
      } as AndroidBlobRequest);

      const filePath =
        fetchedBlob.type === BlobResponseType.Managed
          ? (fetchedBlob.response as BlobManagedResponse).fullFilePath
          : (fetchedBlob.response as BlobHttpResponse).filePath;

      console.log(fetchedBlob, filePath);
      setResult(fetchedBlob);

      const x = await BlobCourier.uploadBlob({
        filePath,
        method: 'POST',
        url: 'https://file.io',
      } as BlobUploadRequest);

      console.warn('OEH LAH L', x);
    };

    requestPermissionAndDownloadBlobAsync();
  }, []);

  return (
    <View style={styles.container}>
      <Text>Result: {JSON.stringify(result)}</Text>
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
