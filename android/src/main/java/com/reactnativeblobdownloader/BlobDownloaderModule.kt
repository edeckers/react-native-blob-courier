package com.reactnativeblobdownloader

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import java.io.File

class BlobDownloaderModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    val sureSure = reactContext;

    override fun getName(): String {
        return "BlobDownloader"
    }

    // Example method
    // See https://facebook.github.io/react-native/docs/native-modules-android
    @ReactMethod
    fun multiply(a: Int, b: Int, promise: Promise) {
        val uri = Uri.parse("https://file-examples-com.github.io/uploads/2018/04/file_example_AVI_480_750kB.avi");

        val downloadManager = sureSure.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager;

        val directory = File(Environment.DIRECTORY_DOWNLOADS)

        val request = DownloadManager.Request(uri).apply {
            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                    .setAllowedOverRoaming(false)
                    .setTitle("TEH TITLE")
                    .setVisibleInDownloadsUi(true)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDescription("")
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                            "test.mp4");
                    //.setDestinationInExternalPublicDir(
                    //        directory.toString()
                    //)
        }

        downloadManager.enqueue(request);

        // ...

        // Uri uri = Uri.parse("https://file-examples-com.github.io/uploads/2018/04/file_example_AVI_480_750kB.avi");

        // DownloadManager.Request request = new DownloadManager.Request(uri);
        // request.setTitle("My File");
        // request.setDescription("Downloading");
        // request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        // request.setVisibleInDownloadsUi(false);
        // request.setDestinationUri(Uri.parse("file://" + folderName + "/myfile.mp3"));

        // downloadmanager.enqueue(request);

        promise.resolve(a * b)
    }
}
