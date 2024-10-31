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
  id("com.afterroot.allusive2.android.common")
  alias(libs.plugins.google.hilt)
  alias(libs.plugins.jetbrains.kotlin.kapt)
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

  implementation(libs.hilt.hilt)
  kapt(libs.hilt.compiler)

  implementation(libs.libsu.core)
  implementation(libs.libsu.io)

  implementation("net.lingala.zip4j:zip4j:2.11.5")
}
