import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// IDs de PRUEBA oficiales de Google: se usan en desarrollo y como respaldo.
val admobAppIdPrueba = "ca-app-pub-3940256099942544~3347511713"
val admobBannerPrueba = "ca-app-pub-3940256099942544/9214589741"
// IDs reales de AdMob para la versión publicada (en gradle.properties; no son secretos).
val admobAppId: String = providers.gradleProperty("admob.appId").orNull ?: admobAppIdPrueba
val admobBanner: String = providers.gradleProperty("admob.banner").orNull ?: admobBannerPrueba

// Firma para Google Play: android/keystore.properties (NO se sube a git). Ver diseno/PUBLICAR.md.
val firma = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

android {
    namespace = "mx.acomo.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "mx.acomo.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "0.3.0"

        // Carpeta de datos generada por el recolector (GitHub Actions).
        buildConfigField(
            "String",
            "URL_DATOS",
            "\"https://raw.githubusercontent.com/JJAS-029/precios_mx/main/data/\"",
        )
    }

    signingConfigs {
        if (firma.containsKey("storeFile")) {
            create("play") {
                storeFile = rootProject.file(firma.getProperty("storeFile"))
                storePassword = firma.getProperty("storePassword")
                keyAlias = firma.getProperty("keyAlias")
                keyPassword = firma.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // En desarrollo siempre anuncios de prueba (tocar anuncios reales propios viola las reglas de AdMob).
            manifestPlaceholders["admobAppId"] = admobAppIdPrueba
            buildConfigField("String", "ADMOB_BANNER", "\"$admobBannerPrueba\"")
        }
        release {
            manifestPlaceholders["admobAppId"] = admobAppId
            buildConfigField("String", "ADMOB_BANNER", "\"$admobBanner\"")
            signingConfig = signingConfigs.findByName("play")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.services.ads)
    implementation(libs.androidx.work.runtime)
    implementation(libs.billing.ktx)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
}
