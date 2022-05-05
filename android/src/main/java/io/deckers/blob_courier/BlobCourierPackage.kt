/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier

import android.util.Log
import com.facebook.react.TurboReactPackage
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.NativeModule
import io.deckers.blob_courier.common.LIBRARY_NAME
import com.facebook.react.module.model.ReactModuleInfoProvider
import com.facebook.react.module.model.ReactModuleInfo
import java.util.HashMap

private val TAG = "BlobCourier"

class BlobCourierPackage : TurboReactPackage() {
    override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? {
        return TurboBlobCourierModule(reactContext)

        // return if (name == LIBRARY_NAME) {
        //     TurboBlobCourierModule(reactContext)
        // } else {
        //     null
        // }
    }

    override fun getReactModuleInfoProvider(): ReactModuleInfoProvider {
      return ReactModuleInfoProvider {
            val moduleInfos: MutableMap<String, ReactModuleInfo> = HashMap()
            // val isTurboModule: Boolean = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED
            val isTurboModule = true
            moduleInfos[LIBRARY_NAME] = ReactModuleInfo(
                LIBRARY_NAME,
                LIBRARY_NAME,
                false,  // canOverrideExistingModule
                false,  // needsEagerInit
                true,  // hasConstants
                false,  // isCxxModule
                isTurboModule // isTurboModule
            )
            moduleInfos
        }
    }
}
