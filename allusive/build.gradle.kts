import com.afterroot.gradle.readProperties

plugins {
  id(afterroot.plugins.android.application.get().pluginId)
  id(afterroot.plugins.kotlin.android.get().pluginId)
  id(afterroot.plugins.android.compose.get().pluginId)
  id(afterroot.plugins.android.hilt.get().pluginId)
  id(afterroot.plugins.allusive2.android.common.get().pluginId)

  alias(libs.plugins.firebase.crashlytics)
  alias(libs.plugins.google.gms)
  alias(libs.plugins.google.ksp)
  alias(libs.plugins.jetbrains.kotlin.kapt) // TODO Remove after removing data-binding

  id(libs.plugins.google.ossLic.get().pluginId)
  id("androidx.navigation.safeargs.kotlin")
  id("kotlin-parcelize")
}

val ci by extra { System.getenv("CI") == "true" }

android {
  namespace = "com.afterroot.allusive2"

  buildFeatures {
    viewBinding = true
    dataBinding = true
    buildConfig = true
  }

  defaultConfig {
    manifestPlaceholders += mapOf(
      "hostName" to "afterroot.web.app",
      "pathPrefix" to "/apps/pointer-replacer/launch",
    )
    applicationId = "com.afterroot.allusive2"
    versionCode = rootProject.extra["versionCode"] as Int
    versionName = rootProject.extra["versionName"].toString()
    vectorDrawables {
      useSupportLibrary = true
    }
    multiDexEnabled = true

    resourceConfigurations.addAll(listOf("en"))
  }

  val keystoreProperties = readProperties(rootProject.file("keystore.properties"))

  signingConfigs {
    create("allusive") {
      storeFile = rootProject.file("release/keystore.jks")
      storePassword =
        keystoreProperties["storePassword"] as String? ?: System.getenv("SIGN_STORE_PW")
      keyAlias = "allusive"
      keyPassword = keystoreProperties["keyPassword"] as String? ?: System.getenv("SIGN_KEY_PW")
    }
  }

  buildTypes {
    release {
      isShrinkResources = true
      isMinifyEnabled = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs["allusive"]
    }
    debug {
      extra["alwaysUpdateBuildId"] = false

      // applicationIdSuffix = ".debug"
      versionNameSuffix = "-beta"

      signingConfig = signingConfigs["allusive"]

      isMinifyEnabled = false
    }
  }

  lint.abortOnError = false

  packaging.resources.excludes +=
    setOf(
      "META-INF/proguard/*",
      "/*.properties",
      "fabric/*.properties",
      "META-INF/*.properties",
      "META-INF/LICENSE*.md",
      "META-INF/**/previous-compilation-data.bin",
    )
}

dependencies {
  implementation(projects.data)
  implementation(projects.ui.home)
  implementation(projects.ui.magisk)
  implementation(projects.ui.repo)

  implementation(libs.kotlin.stdLib)

  implementation(libs.androidx.appCompat)
  implementation(libs.androidx.activity)
  implementation(libs.androidx.billing)
  implementation(libs.androidx.cardView)
  implementation(libs.androidx.constraintLayout)
  implementation(libs.androidx.core)
  implementation(libs.androidx.core.splash)
  implementation(libs.androidx.fragment)
  implementation(libs.bundles.lifecycle)
  implementation(libs.androidx.multiDex)
  implementation(libs.androidx.navigation.fragment)
  implementation(libs.androidx.navigation.ui)
  implementation(libs.androidx.paging)
  implementation(libs.androidx.palette)
  implementation(libs.androidx.preference)
  implementation(libs.androidx.recyclerView)
  implementation(libs.androidx.supportV13)
  implementation(libs.androidx.supportV4)
  implementation(libs.androidx.vectorDrawable)
  implementation(libs.androidx.work)

  implementation(libs.androidx.room.room)
  implementation(libs.androidx.room.runtime)
  ksp(libs.androidx.room.compiler)
  androidTestImplementation(libs.androidx.room.test)

  implementation(libs.materialdialogs.input)
  implementation(libs.materialdialogs.core)
  implementation(libs.materialdialogs.bottomSheets)
  implementation(libs.materialdialogs.color)

  implementation(libs.firebase.ui.auth)
  implementation(libs.firebase.ui.firestore)
  implementation(libs.firebase.ui.storage)

  ksp(libs.glide.ksp)

  implementation(libs.google.ossLic)
  implementation(libs.google.material)
  implementation(libs.google.ads)
  implementation(platform(libs.firebase.bom))
  implementation(libs.bundles.firebase)
  implementation(libs.google.play.review)

  implementation(libs.google.gson)

  implementation(libs.commonsIo)
  compileOnly((files("libs/api-82.jar")))

  implementation(libs.bundles.coroutines)

  testImplementation(libs.test.junit)
  androidTestImplementation(libs.androidx.test.junitExt)
  androidTestImplementation(libs.androidx.test.espresso)

  testImplementation(libs.androidx.test.core)

  implementation(libs.okhttp.okhttp)
  implementation(libs.fastScroll)
}
