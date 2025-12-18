plugins {
  id(afterroot.plugins.android.library.get().pluginId)
  id(afterroot.plugins.kotlin.android.get().pluginId)
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
  api(libs.kotlinx.coroutines.core)

  implementation(projects.common.ui.resources)

  api(libs.glide.glide)
  ksp(libs.glide.ksp)

  implementation(libs.androidx.activity)
  implementation(libs.firebase.firestore)

  implementation(libs.firebase.ui.firestore)
  implementation(libs.firebase.ui.storage)

  implementation(libs.materialdialogs.core)

  api(libs.timber)
  api(projects.utils)
}
