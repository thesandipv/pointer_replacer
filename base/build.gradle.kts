plugins {
  id(afterroot.plugins.android.library.get().pluginId)
  id(afterroot.plugins.android.hilt.get().pluginId)
}

android {
  namespace = "com.afterroot.allusive2.base"

  buildFeatures.buildConfig = true

  defaultConfig {
    val commitHash = providers.exec {
      commandLine("git", "rev-parse", "--short", "HEAD")
    }.standardOutput.asText.get()

    val commit = System.getenv("COMMIT_ID") ?: commitHash.trim()
    buildConfigField("String", "COMMIT_ID", "\"$commit\"")
    buildConfigField("int", "VERSION_CODE", "${rootProject.extra["versionCode"]}")
    buildConfigField("String", "VERSION_NAME", "\"${rootProject.extra["versionName"]}\"")
  }
}

dependencies {
  api(libs.glide.glide)
  api(libs.kotlinx.coroutines.core)
  api(libs.kotlinx.serialization)
  api(libs.timber)

  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.storage)
  implementation(libs.firebase.ui.storage)

  api(projects.utils)
  implementation(projects.common.ui.resources)

  ksp(libs.glide.ksp)
}
