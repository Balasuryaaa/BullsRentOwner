plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services") // Firebase Plugin
    id("kotlin-kapt") // Kotlin Annotation Processing (Required for Glide)
}

android {
    namespace = "com.example.bullsrentowner"
    compileSdk = 34 // Changed from 35 to 34 (current latest)

    defaultConfig {
        applicationId = "com.example.bullsrentowner"
        minSdk = 24
        targetSdk = 34 // Changed from 35 to 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17 // ✅ Ensures Java 17 support
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17" // ✅ Matches Java version
    }
}

dependencies {
    // ✅ AndroidX & Material UI
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // ✅ ViewPager Dots Indicator
    implementation("com.tbuonomo:dotsindicator:5.0")

    // ✅ Firebase BoM (Updated)
    implementation(platform("com.google.firebase:firebase-bom:32.8.0"))

    // ✅ Firebase Services
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // ✅ Google Play Services (Auth)
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // ✅ Kotlin Coroutines (Firebase integration)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // ✅ Glide (Image Loading)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.github.bumptech.glide:okhttp3-integration:4.16.0")

    // ✅ Testing Libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
