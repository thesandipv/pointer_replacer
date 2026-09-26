import com.android.build.gradle.internal.lint.AndroidLintAnalysisTask
import com.android.build.gradle.internal.lint.LintModelWriterTask

plugins {
  id(afterroot.plugins.android.library.get().pluginId)
  id(afterroot.plugins.allusive2.android.common.get().pluginId)
  id(afterroot.plugins.android.hilt.get().pluginId)
}

tasks.register<Zip>("createEmptyModuleZip") {
  group = "build"
  description = "Creates Empty Magisk Module Zip"
  archiveFileName.set("empty-module.zip")
  destinationDirectory.set(layout.projectDirectory.dir("src/main/assets"))
  from(layout.projectDirectory.dir("module/empty-module"))
}

tasks.register<Zip>("createRroModuleZip") {
  group = "build"
  description = "Creates RRO Magisk Module Zip"
  archiveFileName.set("rro-module-2.zip")
  destinationDirectory.set(layout.projectDirectory.dir("src/main/assets"))
  from(layout.projectDirectory.dir("module/rro-module"))
}

val createMagiskModuleZips = tasks.register("createMagiskModuleZips") {
  group = "build"
  description = "Creates all Magisk Module Zips"
  dependsOn("createEmptyModuleZip", "createRroModuleZip")
}

tasks.matching { it.name.matches(Regex("generate.*Assets")) }.configureEach {
  dependsOn(createMagiskModuleZips)
}

tasks.whenTaskAdded {
  if (this is LintModelWriterTask || this is AndroidLintAnalysisTask) {
    this.mustRunAfter("createMagiskModuleZips")
  }
}

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

  implementation("net.lingala.zip4j:zip4j:2.11.6")
}
