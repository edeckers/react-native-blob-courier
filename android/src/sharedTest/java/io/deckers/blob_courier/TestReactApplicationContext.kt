/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier

import android.content.Context
import com.facebook.react.bridge.CatalystInstance
import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.JavaScriptContextHolder
import com.facebook.react.bridge.JavaScriptModule
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.UIManager
import com.facebook.react.bridge.WritableNativeMap
import com.facebook.react.turbomodule.core.interfaces.CallInvokerHolder

class TestReactApplicationContext(context: Context) : ReactApplicationContext(context) {
  override fun <T : JavaScriptModule> getJSModule(jsInterface: Class<T>): T {
    throw UnsupportedOperationException("Not available in tests")
  }

  override fun <T : NativeModule> hasNativeModule(nativeModuleInterface: Class<T>): Boolean = false

  override fun getNativeModules(): Collection<NativeModule> = emptyList()

  override fun <T : NativeModule> getNativeModule(nativeModuleInterface: Class<T>): T? = null

  override fun getNativeModule(moduleName: String): NativeModule? = null

  override fun getCatalystInstance(): CatalystInstance {
    throw UnsupportedOperationException("Not available in tests")
  }

  override fun hasActiveCatalystInstance(): Boolean = false

  override fun hasActiveReactInstance(): Boolean = false

  override fun hasCatalystInstance(): Boolean = false

  override fun hasReactInstance(): Boolean = false

  override fun destroy() {}

  override fun handleException(e: Exception) {
    throw e
  }

  override fun isBridgeless(): Boolean = false

  override fun getJavaScriptContextHolder(): JavaScriptContextHolder? = null

  override fun getJSCallInvokerHolder(): CallInvokerHolder? = null

  override fun getFabricUIManager(): UIManager? = null

  override fun getSourceURL(): String? = null

  override fun registerSegment(segmentId: Int, path: String, callback: Callback) {}
}
