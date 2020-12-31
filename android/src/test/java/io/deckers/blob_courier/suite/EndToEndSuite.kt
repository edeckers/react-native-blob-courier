package io.deckers.blob_courier.suite

import io.deckers.blob_courier.BlobCourierModuleTests
import org.junit.experimental.categories.Categories
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Categories::class)
@Categories.IncludeCategory(EndToEndSuite::class)
@Suite.SuiteClasses(BlobCourierModuleTests::class)
class EndToEndSuite
