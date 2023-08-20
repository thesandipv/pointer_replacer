plugins {
    id("com.afterroot.android.library")
    id("com.afterroot.kotlin.android")
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "com.afterroot.allusive2.domain"
}

dependencies {
    implementation(project(":base"))
    implementation(projects.data)

    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.paging)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)

    implementation(libs.hilt.hilt)
    kapt(libs.hilt.compiler)
}
