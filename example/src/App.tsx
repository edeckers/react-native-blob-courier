import * as React from 'react';
import { StyleSheet, View, Text } from 'react-native';
import BlobDownloader from 'react-native-blob-downloader';

export default function App() {
  const [result, setResult] = React.useState<boolean | undefined>();

  React.useEffect(() => {
    BlobDownloader.fetch_blob(
      'https://file-examples-com.github.io/uploads/2018/04/file_example_AVI_480_750kB.avi'
    ).then(setResult);
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
