import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.arborecence.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.arborecence.assistente"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-fase1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
        // For reference only; runtime key comes from EncryptedSharedPreferences.
        buildConfigField("String", "GEMINI_API_KEY_DEFAULT", "\"\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug { applicationIdSuffix = ".debug" }
    }

    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

hilt {
    enableAggregatingTask = true
}

dependencies {
    implementation(project(":core-ui"))
    implementation(project(":data-behavior"))
    implementation(project(":data-academic"))
    implementation(project(":ai-engine"))
    implementation(project(":voice"))
    implementation(project(":study"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.work.runtime)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    kspTest(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    kspTest(libs.androidx.hilt.compiler)
    implementation(libs.coroutines.android)
    implementation(libs.serialization.json)
    implementation(libs.generativeai)

    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.espresso.core)
}
