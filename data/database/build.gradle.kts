plugins {
    id(afterroot.plugins.android.library.get().pluginId)
    id(afterroot.plugins.kotlin.android.get().pluginId)
    id(afterroot.plugins.allusive2.android.common.get().pluginId)
}

android {
    namespace = "com.afterroot.allusive2.database"

    defaultConfig {
        testInstrumentationRunner = "com.afterroot.allusive2.core.testing.AllusiveTestRunner"
    }
}

dependencies {
    implementation(projects.data.model)
    implementation(libs.androidx.paging.common)
}
