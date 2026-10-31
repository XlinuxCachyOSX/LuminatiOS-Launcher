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
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
}
