plugins {
    id(afterroot.plugins.android.library.get().pluginId)
    id(afterroot.plugins.kotlin.android.get().pluginId)
    id(afterroot.plugins.android.hilt.get().pluginId)
    id(afterroot.plugins.allusive2.android.common.get().pluginId)

    alias(libs.plugins.google.ksp)
}

android {
    namespace = "com.afterroot.allusive2.database.room"

    defaultConfig {
        testInstrumentationRunner = "com.afterroot.allusive2.core.testing.ERPTestRunner"
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {
    implementation(projects.data)
    implementation(projects.data.model)
    implementation(projects.data.database)

    implementation(libs.androidx.room.room)
    implementation(libs.androidx.room.paging)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    implementation(libs.bundles.coroutines)

    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization)

    implementation(libs.androidx.paging.common)

    androidTestImplementation(projects.core.testing)
}
