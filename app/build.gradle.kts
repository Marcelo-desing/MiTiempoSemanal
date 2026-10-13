plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.senati.mitiemposemanal"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.senati.mitiemposemanal.mallccarima"
        minSdk = 24
        targetSdk = 36
        versionCode = 4
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true // Agrega esta línea
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}