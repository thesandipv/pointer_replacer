plugins {
    id(afterroot.plugins.android.library.get().pluginId)
    id(afterroot.plugins.kotlin.android.get().pluginId)
    id(afterroot.plugins.android.hilt.get().pluginId)
    id(afterroot.plugins.allusive2.android.common.get().pluginId)
}

android {
    namespace = "com.afterroot.allusive2.core.logging"
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.timber)
}
