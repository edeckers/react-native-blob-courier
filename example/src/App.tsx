import * as React from 'react';
import { StyleSheet, View, Text, PermissionsAndroid } from 'react-native';
import BlobDownloader, {
  AndroidBlobRequest,
  AndroidPath,
} from 'react-native-blob-downloader';

export default function App() {
  const [result, setResult] = React.useState<Response>();

  console.log(AndroidPath.Download);
  React.useEffect(() => {
    const requestPermissionAndDownloadBlobAsync = async () => {
      try {
        await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE
        );
      } catch (err) {
        console.warn(err);
      }

      BlobDownloader.fetch_blob({
        filename: 'drop.avi',
        method: 'GET',
        target: AndroidPath.Download,
        useDownloadManager: false,
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
