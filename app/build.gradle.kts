import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.application")
    kotlin("android")
}

val APP_VERSION_NAME: String by project
val APP_VERSION_CODE: String by project
val APP_ID: String by project

android {
    compileSdk = libs.versions.compile.sdk.version.get().toInt()

    defaultConfig {
        // --- UBAHAN 1: KITA TURUNKAN KE ANDROID 7 (Nougat) ---
        // Biar hampir semua HP bisa instal
        minSdk = 24 
        
        namespace = APP_ID
        applicationId = APP_ID
        versionCode = APP_VERSION_CODE.toInt()
        versionName = APP_VERSION_NAME
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // --- UBAHAN 2: BUAT APK UNIVERSAL (SEMUA HP) ---
    splits {
        abi {
            isEnable = true
            reset()
            // Kita masukkan kedua tipe CPU (32-bit dan 64-bit)
            include("armeabi-v7a", "arm64-v8a") 
            // Kita set true biar jadi 1 file APK yang isinya lengkap
            isUniversalApk = true 
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false 
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    lint {
        warningsAsErrors = false 
        abortOnError = false
        disable.add("GradleDependency")
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // Library bawaan template
    implementation(projects.libraryAndroid)
    implementation(projects.libraryCompose)
    implementation(projects.libraryKotlin)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraint.layout)
    implementation(libs.androidx.core.ktx)
    implementation("androidx.recyclerview:recyclerview:1.3.2") 

    testImplementation(libs.junit)

    // --- LIBRARY DOWNLOADER & STREAMER ---
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media3:media3-ui:1.2.0")
    implementation("androidx.media3:media3-common:1.2.0")

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.ext.junit.ktx)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.espresso.core)
}