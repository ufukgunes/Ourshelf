import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

val appVersionName = "0.3.0"

// --- API anahtarları -------------------------------------------------------------------------
// local.properties'ten (yerel) veya ortam değişkenlerinden (CI) okunur ve derleme sırasında ortak koda
// `Secrets` nesnesi olarak üretilir. Böylece Android ve iOS aynı mekanizmayı kullanır; anahtarlar repoya girmez.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun secret(name: String): String =
    (localProperties.getProperty(name) ?: System.getenv(name) ?: "").trim()

val secretValues = mapOf(
    "TMDB_API_KEY" to secret("TMDB_API_KEY"),
    "IGDB_PROXY_URL" to secret("IGDB_PROXY_URL"),
    "IGDB_PROXY_KEY" to secret("IGDB_PROXY_KEY"),
    "VERSION_NAME" to appVersionName,
)

val generateSecrets by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/secrets/kotlin")
    inputs.property("values", secretValues)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("com/ourshelf/app/Secrets.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            buildString {
                appendLine("// Bu dosya derleme sırasında üretilir; elle düzenlemeyin.")
                appendLine("package com.ourshelf.app")
                appendLine()
                appendLine("internal object Secrets {")
                secretValues.forEach { (key, value) ->
                    val escaped = value.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$")
                    appendLine("    const val $key: String = \"$escaped\"")
                }
                appendLine("}")
            },
        )
    }
}

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.addAll(
            "kotlin.time.ExperimentalTime",
            "kotlin.uuid.ExperimentalUuidApi",
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "androidx.compose.animation.ExperimentalSharedTransitionApi",
            "kotlinx.coroutines.ExperimentalCoroutinesApi",
            "kotlinx.coroutines.FlowPreview",
        )
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateSecrets)
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.animation)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(libs.compose.material.icons.extended)
                implementation(libs.jb.navigation.compose)
                implementation(libs.jb.lifecycle.viewmodel.compose)
                implementation(libs.jb.lifecycle.runtime.compose)

                implementation(libs.room.runtime)
                implementation(libs.datastore.preferences)

                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)
                implementation(libs.koin.compose.viewmodel.navigation)

                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.serialization.json)

                implementation(libs.coil.compose)
                implementation(libs.coil.network.ktor)

                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.okio)
            }
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.android)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            // Android'de sistem SQLite'ı (FTS4 + unicode61) yeterli; iOS'ta paketlenmiş sürüm kullanılır.
            implementation(libs.sqlite.bundled)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        androidUnitTest.dependencies {
            implementation(libs.junit)
            implementation(libs.robolectric)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.test.ext.junit)
            implementation(libs.androidx.compose.ui.test.junit4)
        }
    }
}

android {
    namespace = "com.ourshelf.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ourshelf.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            // R8 Faz D'de (yayın hazırlığı) açılacak.
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

room {
    schemaDirectory("$projectDir/schemas")
}

// KSP (Room), ortak kaynak setindeki üretilmiş dosyaları da okur; üreteçlerden sonra çalışmalı.
tasks.matching { it.name.startsWith("ksp") }.configureEach {
    dependsOn(generateSecrets)
    dependsOn(
        tasks.matching {
            it.name == "generateComposeResClass" ||
                it.name.startsWith("generateResourceAccessors") ||
                it.name.startsWith("generateActualResourceCollectors") ||
                it.name.startsWith("generateExpectResourceCollectors")
        },
    )
}

compose.resources {
    packageOfResClass = "com.ourshelf.app.resources"
    publicResClass = false
}
