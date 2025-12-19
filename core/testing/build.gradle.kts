plugins {
  id(afterroot.plugins.android.library.get().pluginId)
  id(afterroot.plugins.kotlin.android.get().pluginId)
  id(afterroot.plugins.android.compose.get().pluginId)
  id(afterroot.plugins.android.hilt.get().pluginId)
  id(afterroot.plugins.allusive2.android.common.get().pluginId)
}

android {
  namespace = "com.afterroot.allusive2.core.testing"
}

dependencies {
  api(libs.accompanist.testharness)
  api(libs.androidx.activity.compose)
  api(libs.androidx.compose.ui.test)
  api(libs.androidx.test.core)
  api(libs.androidx.test.espresso)
  api(libs.androidx.test.junitExt)
  api(libs.androidx.test.rules)
  api(libs.androidx.test.runner)
  api(libs.hilt.testing)
  api(libs.kotlinx.coroutines.test)
  api(libs.test.junit)
  api(libs.test.robolectric)

  debugApi(libs.androidx.compose.ui.testManifest)

  implementation(projects.data)
}
