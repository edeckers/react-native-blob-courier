package io.deckers.blob_courier.progress

interface ProgressNotifierFactory {
  fun create(taskId: String): ProgressNotifier
}
