import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Properties
import java.util.TimeZone

plugins {
    id("com.android.application")
    id("com.google.devtools.ksp") version "2.3.12"
    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.brokenpip3.gymbro"
    compileSdk = 36

    val versionProps =
        Properties().apply {
            val versionPropsFile = project.file("version.properties")
            if (versionPropsFile.exists()) {
                load(versionPropsFile.inputStream())
            }
        }

    val buildTime =
        System.getenv("SOURCE_DATE_EPOCH")?.toLongOrNull()?.let { epoch ->
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            cal.time = Date(epoch * 1000L)
            "%04d-%02d-%02d".format(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH),
            )
        } ?: OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

    defaultConfig {
        applicationId = "com.brokenpip3.gymbro"
        minSdk = 26
        targetSdk = 36
        versionCode = versionProps.getProperty("VERSION_CODE", "1").toInt()
        versionName = versionProps.getProperty("VERSION_NAME", "0.1.0")
        buildConfigField("String", "BUILD_DATE", "\"$buildTime\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "x86_64")
            isUniversalApk = false
        }
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("GYMBRO_KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("GYMBRO_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("GYMBRO_KEY_ALIAS")
                keyPassword =
                    System.getenv("GYMBRO_KEY_PASSWORD")
                        ?: System.getenv("GYMBRO_KEYSTORE_PASSWORD")
                require(!storePassword.isNullOrBlank()) {
                    "GYMBRO_KEYSTORE_PASSWORD is required when GYMBRO_KEYSTORE_PATH is set"
                }
                require(!keyAlias.isNullOrBlank()) {
                    "GYMBRO_KEY_ALIAS is required when GYMBRO_KEYSTORE_PATH is set"
                }
                require(!keyPassword.isNullOrBlank()) {
                    "GYMBRO_KEY_PASSWORD or GYMBRO_KEYSTORE_PASSWORD is required when GYMBRO_KEYSTORE_PATH is set"
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            val releaseSigning = signingConfigs.findByName("release")
            if (releaseSigning?.storeFile != null) {
                signingConfig = releaseSigning
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

kotlin {
    jvmToolchain(17)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.06.00"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.navigation:navigation-compose:2.10.1")
    implementation("androidx.room:room-ktx:2.8.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    ksp("androidx.room:room-compiler:2.8.5")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.room:room-testing:2.8.5")
    testImplementation("androidx.test:core:1.7.0")
    testImplementation("androidx.navigation:navigation-testing:2.10.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    testImplementation("org.robolectric:robolectric:4.17")

    androidTestImplementation(platform("androidx.compose:compose-bom:2026.06.00"))
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
