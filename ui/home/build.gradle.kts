plugins {
  id("com.afterroot.android.library")
  id("com.afterroot.kotlin.android")
  id("com.afterroot.allusive2.android.common")
  alias(libs.plugins.google.hilt)
  alias(libs.plugins.jetbrains.kotlin.kapt)
}

android {
  namespace = "com.afterroot.allusive2.home"

  buildFeatures {
    dataBinding = true
    viewBinding = true
  }
}

dependencies {
  implementation(projects.data)
  implementation(projects.domain)
  implementation(projects.common.ui.compose)

  implementation(platform(libs.firebase.bom))
  implementation(libs.bundles.firebase)

  implementation(libs.androidx.fragment)
  implementation(libs.androidx.paging)
  implementation(libs.androidx.lifecycle.viewmodel)

  implementation(libs.bundles.coroutines)

  implementation(libs.hilt.hilt)
  implementation(libs.hilt.compose)
  kapt(libs.hilt.compiler)
}
