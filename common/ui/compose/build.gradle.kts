plugins {
  id(afterroot.plugins.android.library.get().pluginId)
  id(afterroot.plugins.kotlin.android.get().pluginId)
  id(afterroot.plugins.android.compose.get().pluginId)
  id(afterroot.plugins.allusive2.android.common.get().pluginId)
}

android {
  namespace = "com.afterroot.allusive2.compose"
}

dependencies {
  implementation(projects.data)

  api(libs.androidx.lifecycle.runtime)
}
