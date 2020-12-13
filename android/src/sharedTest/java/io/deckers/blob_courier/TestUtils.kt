/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier

import android.os.Build
import java.lang.reflect.Method

object TestUtils {
  private const val vmRuntimeClassName = "dalvik.system.VMRuntime"
  private const val getDeclaredMethodMethodName = "getDeclaredMethod"
  private const val getRuntimeMethodName = "getRuntime"
  private const val setHiddenApiExemptionsMethodName = "setHiddenApiExemptions"

  fun circumventHiddenApiExemptionsForMockk() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
      return
    }

    // https://github.com/mockk/mockk/pull/442
    try {
      val vmRuntimeClass = Class.forName(vmRuntimeClassName)
      val getDeclaredMethod = Class::class.java.getDeclaredMethod(
        getDeclaredMethodMethodName,
        String::class.java,
        arrayOf<Class<*>>()::class.java
      ) as Method
      val getRuntime = getDeclaredMethod(
        vmRuntimeClass,
        getRuntimeMethodName,
        null
      ) as Method
      val setHiddenApiExemptions = getDeclaredMethod(
        vmRuntimeClass,
        setHiddenApiExemptionsMethodName,
        arrayOf(arrayOf<String>()::class.java)
      ) as Method

      setHiddenApiExemptions(getRuntime(null), arrayOf("L"))
    } catch (ex: Exception) {
      throw Exception("Could not set up hiddenApiExemptions")
    }
  }
}
