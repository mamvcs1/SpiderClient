plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.spiderclient"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.spiderclient"
        minSdk = 23
        targetSdk = 28
        versionCode = maxOf(2, System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 2)
        versionName = "1.0.${System.getenv("GITHUB_RUN_NUMBER") ?: "1"}"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }


    signingConfigs {
        create("stable") {
            val keystoreFile = rootProject.file("spider-release.jks")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("SPIDER_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("SPIDER_KEY_ALIAS") ?: "spiderclient"
                keyPassword = System.getenv("SPIDER_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            val keystoreFile = rootProject.file("spider-release.jks")
            if (keystoreFile.exists()) {
                signingConfig = signingConfigs.getByName("stable")
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.7.0")
}
