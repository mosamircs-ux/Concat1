import java.io.File

plugins {
    id("com.android.application")
}

val appVersionName: String = (findProperty("versionName") as String?) ?: "0.2.11"
val appVersionCode: Int = (findProperty("versionCode") as String?)?.toIntOrNull() ?: 2011
val targetAbi: String = (findProperty("abi") as String?) ?: "arm64-v8a"

val admobAppId: String = System.getenv("ADMOB_APP_ID")?.takeIf { it.isNotBlank() }
    ?: (findProperty("ADMOB_APP_ID") as String?)
    ?: "ca-app-pub-4969792934066333~9936075974"

val admobBannerId: String = System.getenv("ADMOB_BANNER_ID")?.takeIf { it.isNotBlank() }
    ?: (findProperty("ADMOB_BANNER_ID") as String?)
    ?: "ca-app-pub-3940256099942544/6300978111"

val admobInterstitialId: String = System.getenv("ADMOB_INTERSTITIAL_ID")?.takeIf { it.isNotBlank() }
    ?: (findProperty("ADMOB_INTERSTITIAL_ID") as String?)
    ?: "ca-app-pub-3940256099942544/1033173712"

android {
    namespace = "com.mosamircs.videoeditor"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mosamircs.videoeditor"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName

        ndk {
            abiFilters += targetAbi
        }

        manifestPlaceholders["ADMOB_APP_ID"] = admobAppId
        buildConfigField("String", "ADMOB_BANNER_ID", "\"$admobBannerId\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$admobInterstitialId\"")
    }

    signingConfigs {
        create("release") {
            val ksPath = System.getenv("ANDROID_KEYSTORE_PATH")
            if (!ksPath.isNullOrBlank() && File(ksPath).exists()) {
                storeFile = File(ksPath)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("ANDROID_KEY_ALIAS") ?: "videoeditor"
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
                    ?.takeIf { it.isNotBlank() }
                    ?: (System.getenv("ANDROID_KEYSTORE_PASSWORD") ?: "")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val ksPath = System.getenv("ANDROID_KEYSTORE_PATH")
            signingConfig = if (!ksPath.isNullOrBlank() && File(ksPath).exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        // Uncompressed & 16 KB page-aligned native libs for Android 15 / Google Play compliance.
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {
    // Real Google Mobile Ads SDK (AdMob) + Google UMP (GDPR/EEA consent)
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    implementation("com.google.android.ump:user-messaging-platform:3.1.0")
}
