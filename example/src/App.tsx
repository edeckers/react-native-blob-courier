import * as React from 'react';
import { StyleSheet, View, Text, PermissionsAndroid } from 'react-native';
import BlobCourier from 'react-native-blob-courier';
import type { AndroidBlobRequest } from 'src/Requests';

export default function App() {
  const [result, setResult] = React.useState<Response>();

  React.useEffect(() => {
    const requestPermissionAndDownloadBlobAsync = async () => {
      try {
        await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE
        );
      } catch (err) {
        console.warn(err);
      }

      BlobCourier.fetchBlob({
        filename: 'drop2.avi',
        method: 'GET',
        useDownloadManager: true,
        url: 'https://www.engr.colostate.edu/me/facil/dynamics/files/drop.avi',
      } as AndroidBlobRequest).then(setResult);
    };

    requestPermissionAndDownloadBlobAsync();
  }, []);

  return (
    <View style={styles.container}>
      <Text>Result: {JSON.stringify(result)}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
