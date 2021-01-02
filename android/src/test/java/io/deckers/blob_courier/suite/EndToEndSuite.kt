/**
 * Copyright (c) Ely Deckers.
 *
 * This source code is licensed under the MPL-2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
package io.deckers.blob_courier.suite

import io.deckers.blob_courier.BlobCourierModuleTests
import io.deckers.blob_courier.category.EndToEnd
import org.junit.experimental.categories.Categories
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Categories::class)
@Categories.IncludeCategory(EndToEnd::class)
@Suite.SuiteClasses(BlobCourierModuleTests::class)
class EndToEndSuite
