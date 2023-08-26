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
    id("com.afterroot.android.compose")
    id("com.afterroot.allusive2.android.common")
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "com.afterroot.allusive2.repo"

    buildFeatures {
        dataBinding = true
        viewBinding = true
    }
}

dependencies {
    implementation(projects.data)
    implementation(projects.domain)
    implementation(projects.common.ui.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase)

    implementation(libs.androidx.fragment)
    implementation(libs.androidx.paging)
    implementation(libs.androidx.paging)
    implementation(libs.androidx.lifecycle.viewmodel)

    implementation(libs.bundles.coroutines)

    implementation(libs.hilt.hilt)
    implementation(libs.hilt.compose)
    kapt(libs.hilt.compiler)

    implementation(libs.coil)
}
