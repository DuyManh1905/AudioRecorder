plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.chaquo.python")
}


android {
    namespace = "com.duymanh.audiorecorder"
    compileSdk = 34

    viewBinding {
        enable = true
    }

    defaultConfig {
        applicationId = "com.duymanh.audiorecorder"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            // On Apple silicon, you can omit x86_64.
            abiFilters  += listOf("arm64-v8a", "x86_64","armeabi-v7a")
        }
    }

    flavorDimensions += "pyVersion"
    productFlavors {
        create("py38") { dimension = "pyVersion" }
        create("py39") { dimension = "pyVersion" }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

}

chaquopy {
    defaultConfig {
        pyc {
            src = false
        }
        pip {
            install ("scipy")
            install ("soundfile")
            install ("librosa==0.9.2")
            install ("resampy==0.3.1")
        }
    }
    sourceSets {
        sourceSets {
            getByName("main") {
                srcDir("src/main/python")
            }
        }
    }
    productFlavors {
        getByName("py38") { version = "3.8" }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    implementation ("com.google.android.material:material:latest_version")


    implementation("androidx.room:room-runtime:2.5.2")
    kapt("androidx.room:room-compiler:2.5.2")
    implementation("androidx.room:room-ktx:2.5.2")

    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.15.0")

    implementation("com.arthenica:ffmpeg-kit-full:6.0-2")

}