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

// Top-level build file where you can add configuration options common to all sub-projects/modules.

import com.afterroot.gradle.readProperties
import dagger.hilt.android.plugin.HiltExtension

buildscript {
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath(libs.google.pluginOssLic)
        classpath(libs.androidx.navigation.pluginSafeArgs)
    }
}

plugins {
    id(afterroot.plugins.root.get().pluginId)

    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.lint) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.google.gms) apply false
    alias(libs.plugins.google.hilt) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.jetbrains.kotlin.multiplatform) apply false
    alias(libs.plugins.jetbrains.kotlin.kapt) apply false
    alias(libs.plugins.jetbrains.kotlin.serialization) apply false
    alias(libs.plugins.google.ksp) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
}

val versionProperties = readProperties(from = rootProject.file("version.properties"))

val major = libs.versions.major.get().toInt()
val minor = libs.versions.minor.get().toInt()
val patch = versionProperties["patch"].toString().toInt()
val versionCode: Int by extra { libs.versions.minSdk.get().toInt() * 10000000 + major * 10000 + minor * 100 + patch }
val versionName: String by extra { "$major.$minor.$patch" }

println("- INFO: Build version code: $versionCode")

subprojects {
    plugins.withId(rootProject.libs.plugins.google.hilt.get().pluginId) {
        extensions.getByType<HiltExtension>().enableAggregatingTask = true
    }
    plugins.withId(rootProject.libs.plugins.jetbrains.kotlin.kapt.get().pluginId) {
        extensions.getByType<org.jetbrains.kotlin.gradle.plugin.KaptExtension>().apply {
            correctErrorTypes = true
            useBuildCache = true
        }
    }
}

task("incrementPatch") {
    doLast {
        versionProperties["patch"] = (patch + 1).toString()
        versionProperties.store(rootProject.file("version.properties").writer(), null)
        println("-INFO: Patch changed from $patch to ${versionProperties["patch"]}")
    }
}
