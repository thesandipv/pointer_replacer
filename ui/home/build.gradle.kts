plugins {
    id(afterroot.plugins.android.library.get().pluginId)
    id(afterroot.plugins.kotlin.android.get().pluginId)
    id(afterroot.plugins.allusive2.android.common.get().pluginId)
    id(afterroot.plugins.android.hilt.get().pluginId)
}

android {
    namespace = "com.afterroot.allusive2.home"

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
    implementation(libs.androidx.lifecycle.viewmodel)

    implementation(libs.bundles.coroutines)

    implementation(libs.hilt.compose)
}
