// Versiones en gradle/libs.versions.toml. AGP 9 trae Kotlin integrado; el plugin
// kotlin-android se declara solo para fijar la versión de Kotlin del classpath.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
