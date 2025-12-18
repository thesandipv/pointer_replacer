plugins {
  id(afterroot.plugins.android.library.get().pluginId)
  id(afterroot.plugins.kotlin.android.get().pluginId)
  id(afterroot.plugins.android.compose.get().pluginId)
  id(afterroot.plugins.android.hilt.get().pluginId)
  id(afterroot.plugins.allusive2.android.common.get().pluginId)

  id("kotlin-parcelize")
}

android {
  namespace = "com.afterroot.allusive2.data"
  buildFeatures.buildConfig = true
}

ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
  arg("room.incremental", "true")
}

dependencies {
  api(projects.ards)
  api(projects.core.logging)
  implementation(projects.data.model)
  implementation(projects.data.datastore)

  implementation(libs.androidx.paging)
  implementation(libs.androidx.preference)

  implementation(libs.bundles.coroutines)

  implementation(platform(libs.firebase.bom))
  implementation(libs.bundles.firebase)

  api(libs.androidx.room.room)
  api(libs.androidx.room.runtime)
  ksp(libs.androidx.room.compiler)

  api(libs.okhttp.okhttp)
  api(libs.okhttp.logging)
  api(libs.retrofit.retrofit)
  api(libs.retrofit.gson)

  implementation(libs.materialdialogs.core)
}
