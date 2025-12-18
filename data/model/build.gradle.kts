plugins {
    id(afterroot.plugins.android.library.get().pluginId)
    id(afterroot.plugins.kotlin.android.get().pluginId)
    id(afterroot.plugins.allusive2.android.common.get().pluginId)
}

android {
    namespace = "com.afterroot.allusive2.data.model"
}

dependencies {
    implementation(projects.ards)

    implementation(libs.androidx.room.common)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)

    implementation(libs.kotlinx.datetime)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
}
