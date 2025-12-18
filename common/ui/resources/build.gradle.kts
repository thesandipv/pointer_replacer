plugins {
  id(afterroot.plugins.android.library.get().pluginId)
  id(afterroot.plugins.kotlin.android.get().pluginId)
}

android {
  namespace = "com.afterroot.allusive2.resources"
}

dependencies {
  api(libs.google.material)
  implementation(libs.androidx.preference)
  implementation(libs.androidx.core.splash)
  implementation(libs.materialdialogs.core)
}
