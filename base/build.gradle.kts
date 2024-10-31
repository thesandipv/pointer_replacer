/*
 * Copyright (C) 2016-2023 Sandip Vaghela
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
  id("com.afterroot.android.library")
  id("com.afterroot.kotlin.android")
  alias(libs.plugins.jetbrains.kotlin.kapt)
  alias(libs.plugins.google.ksp)
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
}
