/*
 * Copyright (C) 2016-2021 Sandip Vaghela
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
  // id("com.afterroot.android.compose")

  alias(libs.plugins.jetbrains.kotlin.kapt)
  alias(libs.plugins.google.ksp)
}

android {
  namespace = "com.afterroot.allusive2.data"
}

ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
  arg("room.incremental", "true")
}

dependencies {
  api(projects.ards)

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

  implementation(libs.hilt.hilt)
  kapt(libs.hilt.compiler)

  implementation(libs.materialdialogs.core)
}
