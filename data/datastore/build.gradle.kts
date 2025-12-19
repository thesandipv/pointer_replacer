plugins {
  id(afterroot.plugins.android.library.get().pluginId)
  id(afterroot.plugins.kotlin.android.get().pluginId)
  id(afterroot.plugins.android.hilt.get().pluginId)
  id(afterroot.plugins.allusive2.android.common.get().pluginId)
}

android {
  namespace = "com.afterroot.allusive2.datastore"

  defaultConfig {
    consumerProguardFiles("consumer-rules.pro")
  }
}

dependencies {
  api(libs.androidx.datastore)
  api(projects.core.logging)
  api(projects.data.datastoreProto)
  implementation(projects.data.model)
}
