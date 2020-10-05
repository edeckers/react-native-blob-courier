package io.deckers.blob_downloader

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.Environment.getExternalStoragePublicDirectory
import com.facebook.common.internal.ImmutableMap
import com.facebook.react.bridge.*
import com.facebook.react.modules.network.OkHttpClientProvider

import java.io.File
import java.lang.reflect.Type
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Okio


class BlobDownloaderModule(val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    val ERROR_MISSING_REQUIRED_PARAM = "ERROR_MISSING_REQUIRED_PARAM"
    val ERROR_INVALID_TARGET_PARAM_ENUM = "ERROR_INVALID_TARGET_PARAM_ENUM"
    val ERROR_UNEXPECTED_EXCEPTION = "ERROR_UNEXPECTED_EXCEPTION"

    val PARAM_FILENAME = "filename"
    val PARAM_METHOD = "method"
    val PARAM_TARGET = "target"
    val PARAM_URL = "url"
    val PARAM_USE_DOWNLOAD_MANAGER = "useDownloadManager"

    val TARGET_PARAM_ENUM_PREFIX = "enum://"

    val DEFAULT_METHOD = "GET"

    override fun getName(): String {
        return "BlobDownloader"
    }

    val predefinedPaths = mapOf(
            "DCIM" to getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "DOCUMENT" to getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "DOWNLOAD" to getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    )

    class BlobDownloaderError(open val code: String, message: String) : Throwable(message) {
    }

    fun stripEnumPrefix(path: String): String = path.replaceFirst(TARGET_PARAM_ENUM_PREFIX, "")

    fun assertPathEnum(pathEnum: String) {
        val cleanedPathEnum = stripEnumPrefix(pathEnum)

        if (!predefinedPaths.containsKey(cleanedPathEnum.toUpperCase())) {
            throw BlobDownloaderError(ERROR_INVALID_TARGET_PARAM_ENUM, "Unknown enum `${cleanedPathEnum}`")
        }
    }

    fun isEnum(pathEnum: String) = pathEnum.startsWith(TARGET_PARAM_ENUM_PREFIX, ignoreCase = true)

    fun parsePathEnum(pathEnum: String): File? {
        val cleanedPathEnum = stripEnumPrefix(pathEnum);

        assertPathEnum(cleanedPathEnum)

        return predefinedPaths.get(cleanedPathEnum.toUpperCase())
    }

    fun buildTargetFromPathOrEnum(pathOrEnum: String): String =
            if (isEnum(pathOrEnum)) parsePathEnum(pathOrEnum).toString() else pathOrEnum


    val REQUIRED_PARAMETER_PROCESSOR = ImmutableMap.of(
            Boolean::class.java.toString(), { input: ReadableMap, parameterName: String -> input.getBoolean(parameterName) },
            String::class.java.toString(), { input: ReadableMap, parameterName: String -> input.getString(parameterName) }
    )


    fun assertTargetParam(target: String) {
        if (isEnum(target)) {
            assertPathEnum(target)
        }
    }

    fun assertRequiredParameter(input: ReadableMap, type: Type, parameterName: String) {
        val maybeValue = REQUIRED_PARAMETER_PROCESSOR.getOrDefault(type.toString()) { _, _ -> throw Exception("No processor defined for type `${type}`, valid options: ${REQUIRED_PARAMETER_PROCESSOR.keys.joinToString(", ")}") }(input, parameterName)

        if (maybeValue == null) {
            throw BlobDownloaderError(ERROR_MISSING_REQUIRED_PARAM, "`${parameterName}` is a required parameter of type `${type}`")
        }
    }

    fun fetchBlobUsingDownloadManager(uri: Uri, targetPath: String, filename: String?) {
        val downloadManager = reactContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val request = DownloadManager.Request(uri).apply {
            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                    .setAllowedOverRoaming(true)
                    .setVisibleInDownloadsUi(true)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(targetPath, filename)
        }

        downloadManager.enqueue(request)
    }

    fun fetchBlobWithoutDownloadManager(uri: Uri, targetPath: String, filename: String, method: String) {
        val fullTargetPath = File(targetPath, filename)

        val okHttpClient = OkHttpClientProvider.getOkHttpClient()

        val request = Request.Builder().method(method, null).url(uri.toString()).build()

        okHttpClient.newCall(request).execute().use { response ->
            response.body()?.source().use { source ->
                Okio.buffer(Okio.sink(fullTargetPath)).use { sink ->
                    sink.writeAll(source)
                }
            }
        }
    }

    fun fetchBlobFromValidatedParameters(input: ReadableMap, promise: Promise) {
        val filename = input.getString(PARAM_FILENAME) ?: ""
        val uri = Uri.parse(input.getString(PARAM_URL))
        val targetPathOrEnum = input.getString(PARAM_TARGET) ?: ""
        val target = buildTargetFromPathOrEnum(targetPathOrEnum)

        val useDownloadManager =
                input.hasKey(PARAM_USE_DOWNLOAD_MANAGER) &&
                        input.getBoolean(PARAM_USE_DOWNLOAD_MANAGER)

        if (useDownloadManager) {
            fetchBlobUsingDownloadManager(uri, target, filename)
            promise.resolve(true)
            return
        }

        val method = input.getString(PARAM_METHOD) ?: DEFAULT_METHOD

        fetchBlobWithoutDownloadManager(uri, target, filename, method)
        promise.resolve(true)
    }

    @ReactMethod
    fun fetchBlob(input: ReadableMap, promise: Promise) {
        try {
            assertRequiredParameter(input, String::class.java, PARAM_FILENAME)
            assertRequiredParameter(input, String::class.java, PARAM_TARGET)
            assertRequiredParameter(input, String::class.java, PARAM_URL)

            assertTargetParam(input.getString(PARAM_TARGET) ?: "")

            fetchBlobFromValidatedParameters(input, promise)
        } catch (e: BlobDownloaderError) {
            promise.reject(e.code, e.message)
            return
        } catch (e: Exception) {
            promise.reject(ERROR_UNEXPECTED_EXCEPTION, "An unexpected exception occurred: ${e.message}")
            return
        }
    }
}
