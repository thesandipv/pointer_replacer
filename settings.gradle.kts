@file:Suppress("UnstableApiUsage")

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
  repositories {
    google()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
  }

  versionCatalogs {
    create("afterroot") {
      from(files("gradle/build-logic/convention.versions.toml"))
    }
  }
}

plugins {
  id("com.gradle.develocity") version "3.19.2"
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

develocity {
  buildScan {
    termsOfUseUrl = "https://gradle.com/terms-of-service"
    termsOfUseAgree = "yes"
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
  ":core:logging",
  ":core:testing",
  ":data",
  ":data:database",
  ":data:database-room",
  ":data:datastore",
  ":data:datastore-proto",
  ":data:model",
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
