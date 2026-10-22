plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.paceghost.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.paceghost.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 14
        versionName = "1.9.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("paceghost.jks")
            storePassword = "paceghost123"
            keyAlias = "paceghost"
            keyPassword = "paceghost123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
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
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // AndroidX Core & Lifecycle
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
    implementation("androidx.activity:activity-compose:1.9.2")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Jetpack Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // OsmDroid for offline/online OpenStreetMap
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // Room Database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Google Play Services Location
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}

tasks.register("checkLocalization") {
    group = "verification"
    description = "Verifies that production source code in src/main contains no untranslated non-English text"
    doLast {
        val pattern = Regex("[\\u0400-\\u04FF]")
        val mainDir = file("src/main")
        var violations = 0
        mainDir.walkTopDown().filter { it.extension in listOf("kt", "xml") }.forEach { file ->
            file.readLines().forEachIndexed { lineIdx, line ->
                if (pattern.containsMatchIn(line)) {
                    logger.error("Localization error in ${file.name}:${lineIdx + 1}: ${line.trim()}")
                    violations++
                }
            }
        }
        if (violations > 0) {
            throw GradleException("Found $violations non-English strings in src/main. PaceGhost UI and strings must be strictly English.")
        }
    }
}

tasks.register("checkAndroidCompatibility") {
    group = "verification"
    description = "Enforces Android runtime compatibility: checks for banned regex flags (?U) and ViewModel constructors"
    doLast {
        val mainDir = file("src/main")
        var violations = 0

        // 1. Check for banned regex flags that crash Android ICU runtime
        val bannedRegexPattern = Regex("""\(\?[a-zA-Z]*U[a-zA-Z]*\)""")
        mainDir.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            file.readLines().forEachIndexed { idx, line ->
                if (bannedRegexPattern.containsMatchIn(line)) {
                    logger.error("Android ICU incompatibility in ${file.name}:${idx + 1}: Flag (?U) is not supported on Android runtime: ${line.trim()}")
                    violations++
                }
            }
        }

        // 2. Check for AndroidViewModel classes with default arguments lacking @JvmOverloads
        mainDir.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            val text = file.readText()
            if (text.contains("AndroidViewModel") && text.contains("class ") && text.contains("=")) {
                val classRegex = Regex("""class\s+([A-Za-z0-9_]+)\s*(?:@([A-Za-z0-9_]+)\s*)?\(([^)]*=[^)]*)\)\s*:\s*AndroidViewModel""")
                classRegex.findAll(text).forEach { match ->
                    val className = match.groupValues[1]
                    val annotation = match.groupValues[2]
                    if (annotation != "JvmOverloads" && !text.contains("@JvmOverloads constructor")) {
                        logger.error("ViewModel reflection incompatibility in ${file.name}: class $className has default constructor parameters but is missing @JvmOverloads for AndroidViewModelFactory")
                        violations++
                    }
                }
            }
        }

        if (violations > 0) {
            throw GradleException("Found $violations Android compatibility violations. Fix them before building.")
        }
    }
}

tasks.named("preBuild") {
    dependsOn("checkLocalization")
    dependsOn("checkAndroidCompatibility")
}
