import com.android.build.gradle.internal.lint.AndroidLintAnalysisTask
import com.android.build.gradle.internal.lint.LintModelWriterTask

plugins {
    id(afterroot.plugins.android.library.get().pluginId)
    id(afterroot.plugins.kotlin.android.get().pluginId)
    id(afterroot.plugins.allusive2.android.common.get().pluginId)
    id(afterroot.plugins.android.hilt.get().pluginId)
}

apply(from = "$rootDir/gradle/create-zip.gradle")

android {
  namespace = "com.afterroot.allusive2.magisk"

  buildFeatures {
    dataBinding = true
    viewBinding = true
  }
}
dependencies {
  implementation(projects.data)

  implementation(libs.materialdialogs.core)

  implementation(libs.androidx.constraintLayout)
  implementation(libs.androidx.lifecycle.runtime)
  implementation(libs.androidx.lifecycle.livedata)
  implementation(libs.androidx.lifecycle.extensions)

  implementation(libs.google.material)

  implementation(platform(libs.firebase.bom))
  implementation(libs.bundles.firebase)
  implementation(libs.bundles.coroutines)

    implementation(libs.libsu.core)
    implementation(libs.libsu.io)

  implementation("net.lingala.zip4j:zip4j:2.11.5")
}

tasks.whenTaskAdded {
  if (this is LintModelWriterTask || this is AndroidLintAnalysisTask) {
    this.mustRunAfter("createEmptyModuleZip")
  }
}
