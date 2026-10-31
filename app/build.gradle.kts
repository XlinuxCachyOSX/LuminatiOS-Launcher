plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.luminati.launcher"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.luminati.launcher"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation("androidx.recyclerview:recyclerview:1.1.0")
}
