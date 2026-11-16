plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "mx.acomo.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "mx.acomo.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        // Carpeta de datos generada por el recolector (GitHub Actions).
        buildConfigField(
            "String",
            "URL_DATOS",
            "\"https://raw.githubusercontent.com/JJAS-029/precios_mx/main/data/\"",
        )

        // AdMob. Por ahora son los IDs de PRUEBA oficiales de Google (muestran anuncios de ejemplo).
        // Antes de publicar: reemplazar por los IDs reales de la cuenta de AdMob.
        manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
        buildConfigField("String", "ADMOB_BANNER", "\"ca-app-pub-3940256099942544/9214589741\"")
    }

    buildTypes {
        release {
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
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
}
