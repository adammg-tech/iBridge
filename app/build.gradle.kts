plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.adammg.ibridge"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.adammg.ibridge"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val storeFileProp = project.findProperty("MYAPP_RELEASE_STORE_FILE") as String?
            if (!storeFileProp.isNullOrBlank()) {
                storeFile = file(storeFileProp)
                storePassword = project.findProperty("MYAPP_RELEASE_STORE_PASSWORD") as String?
                keyAlias = project.findProperty("MYAPP_RELEASE_KEY_ALIAS") as String?
                keyPassword = project.findProperty("MYAPP_RELEASE_KEY_PASSWORD") as String?
            }
        }
    }

    buildTypes {
        release {
            val storeFileProp = project.findProperty("MYAPP_RELEASE_STORE_FILE") as String?
            if (!storeFileProp.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
            optimization {
                enable = false
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}