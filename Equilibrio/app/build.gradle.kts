import java.util.Base64
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.equilibrio.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.equilibrio.app"
        minSdk = 30
        targetSdk = 34
        versionCode = 3
        versionName = "1.0.2"

        val proxyUrl = providers.gradleProperty("GEMINI_PROXY_URL").orNull ?: ""
        buildConfigField("String", "GEMINI_PROXY_URL", "\"$proxyUrl\"")
        buildConfigField("String", "GEMINI_MODEL", "\"gemini-2.5-flash\"")
    }

    signingConfigs {
        val envStorePassword = providers.environmentVariable("KEYSTORE_PASSWORD").orNull
        val envKeyAlias = providers.environmentVariable("KEY_ALIAS").orNull
        val envKeyPassword = providers.environmentVariable("KEY_PASSWORD").orNull
        val envKeystoreB64 = providers.environmentVariable("KEYSTORE_BASE64").orNull
        if (!envKeystoreB64.isNullOrBlank() && !envStorePassword.isNullOrBlank() && !envKeyAlias.isNullOrBlank() && !envKeyPassword.isNullOrBlank()) {
            val decoded = Base64.getDecoder().decode(envKeystoreB64.replace("\n", "").trim())
            val tmp = rootProject.layout.buildDirectory.file("ci-release.keystore").get().asFile
            tmp.parentFile.mkdirs()
            tmp.writeBytes(decoded)
            create("release") {
                storeFile = tmp
                storePassword = envStorePassword
                keyAlias = envKeyAlias
                keyPassword = envKeyPassword
            }
        } else if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        noCompress += "tflite"
    }

    packaging {
        resources.excludes += setOf(
            "META-INF/AL2.0",
            "META-INF/LGPL2.1",
            "META-INF/DEPENDENCIES",
            "META-INF/LICENSE*",
            "META-INF/NOTICE*",
            "META-INF/*.kotlin_module"
        )
        jniLibs.useLegacyPackaging = false
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // Tipos expostos pelas assinaturas dos repositórios de :core:data.
    // Como :core:data usa `implementation`, o consumidor precisa declarar
    // essas bibliotecas para o KSP/Hilt resolver as classes de implementação.
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.sqlite)
    implementation(libs.sqlcipher.android)
    implementation(libs.okhttp)
    implementation(libs.generativeai)
    implementation(libs.tensorflow.lite)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
