import buildhelp.setupLibraryModule

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

setupLibraryModule(publish = true, document = false)

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.paging)

    implementation(libs.gson)
    implementation(libs.androidx.media)
    implementation(libs.exoplayer.core)
//    implementation(libs.exoplayer.ui)
    implementation(libs.exoplayer.mediasession)

//    implementation(libs.glide.runtime)
//    kapt(libs.glide.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.espresso)
}
