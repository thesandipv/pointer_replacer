plugins {
  id(afterroot.plugins.android.library.get().pluginId)
  id(afterroot.plugins.kotlin.android.get().pluginId)
  id(afterroot.plugins.android.compose.get().pluginId)
  id(afterroot.plugins.android.hilt.get().pluginId)
  id(afterroot.plugins.allusive2.android.common.get().pluginId)
  alias(libs.plugins.jetbrains.kotlin.android)
  alias(libs.plugins.jetbrains.kotlin.compose)
}

android {
  namespace = "com.afterroot.allusive2.settings"

  buildFeatures {
    dataBinding = true
    viewBinding = true
  }
}

dependencies {
  implementation(projects.data)
}
