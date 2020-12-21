package io.deckers.blob_courier.upload

import android.net.Uri

data class FilePart(
  val absoluteFilePath: Uri,
  val filename: String,
  val mimeType: String
) : PartPayload
