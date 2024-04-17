/*
 * Copyright (C) 2016-2020 Sandip Vaghela
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

import java.util.Properties

pluginManagement {
    includeBuild("gradle/build-logic")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    val properties = readProperties(file("private.properties"))

    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }

        maven {
            name = "github-afterroot-utils"
            url = uri("https://maven.pkg.github.com/afterroot/utils")
            credentials {
                username = properties.getProperty("gpr.user") ?: System.getenv("GHUSERNAME")
                password = properties.getProperty("gpr.key") ?: System.getenv("GHTOKEN")
            }
        }
    }
}

plugins {
    id("com.gradle.enterprise") version "3.17.2"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
        publishAlways()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "pointer-replacer"

include(
    ":allusive",
    ":ards",
    ":arro",
    ":base",
    ":common:ui:compose",
    ":common:ui:resources",
    ":data",
    ":domain",
    ":ui:home",
    ":ui:magisk",
    ":ui:repo",
    ":utils",
)

project(":ards").projectDir = file("ards/lib") // AfterROOT Data Structure
project(":arro").projectDir = file("arro/app") // Allusive RRO
project(":utils").projectDir = file("utils/lib") // AfterROOT Utils

fun readProperties(propertiesFile: File): Properties {
    if (!propertiesFile.exists()) {
        return Properties()
    }
    return Properties().apply {
        propertiesFile.inputStream().use { fis -> load(fis) }
    }
}
