plugins {
  id(afterroot.plugins.android.library.get().pluginId)
  id(afterroot.plugins.kotlin.android.get().pluginId)
  id(afterroot.plugins.android.hilt.get().pluginId)
}

android {
  namespace = "com.afterroot.allusive2.domain"
}

dependencies {
  implementation(projects.base)
  implementation(projects.data)

  implementation(libs.kotlinx.atomicfu)

  implementation(libs.androidx.core)
  implementation(libs.androidx.lifecycle.viewmodel)
  implementation(libs.androidx.paging)

  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.firestore)
}
