plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

tasks.register<Exec>("verifyVolume") {
    group = "verification"
    description = "Verifica metas de volume (behavior >= 3000, academic >= 2500/50 por subárea). Falha se não atendidas."
    workingDir = projectDir
    commandLine("python", "tools/verificar_volume.py")
}

