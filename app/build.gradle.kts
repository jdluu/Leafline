plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.jdluu.leafline"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jdluu.leafline"
        // SDK pinning decision (#31): min 26 covers Android 8.0+ (the README's
        // stated floor, including most e-ink readers); target 36 tracks the
        // current platform. Revisit only with a concrete device requirement.
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Release signing (#32): credentials never live in this repo.
            // Set these in ~/.gradle/gradle.properties (or CI secrets):
            //   LEAFLINE_STORE_FILE=/absolute/path/to/leafline-release.jks
            //   LEAFLINE_STORE_PASSWORD=...
            //   LEAFLINE_KEY_ALIAS=leafline
            //   LEAFLINE_KEY_PASSWORD=...
            // Unsigned release builds still work without them.
            signingConfigs.findByName("release")?.let { config ->
                val storeFile = project.findProperty("LEAFLINE_STORE_FILE") as? String
                if (storeFile != null) {
                    config.storeFile = file(storeFile)
                    config.storePassword = project.findProperty("LEAFLINE_STORE_PASSWORD") as? String
                    config.keyAlias = project.findProperty("LEAFLINE_KEY_ALIAS") as? String
                    config.keyPassword = project.findProperty("LEAFLINE_KEY_PASSWORD") as? String
                }
            }
        }
    }

    signingConfigs {
        create("release") {
            // Populated from gradle properties above when present; the build
            // falls back to an unsigned release APK otherwise, which is what
            // F-Droid/Acres recipe builds produce anyway (they sign with
            // their own keys at packaging time).
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures { compose = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    val roomVersion = "2.8.4"

    implementation(platform("androidx.compose:compose-bom:2025.10.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.core:core-splashscreen:1.2.0-alpha02")
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    implementation("androidx.fragment:fragment-ktx:1.8.9")

    debugImplementation("androidx.compose.ui:ui-tooling")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation("androidx.test:runner:1.6.1")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.room:room-testing:$roomVersion")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.test.ext:junit:1.2.1")

    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("com.google.code.gson:gson:2.10.1")
    ksp("androidx.room:room-compiler:$roomVersion")

    val readiumVersion = "3.3.0"
    implementation("org.readium.kotlin-toolkit:readium-shared:$readiumVersion")
    implementation("org.readium.kotlin-toolkit:readium-streamer:$readiumVersion")
    implementation("org.readium.kotlin-toolkit:readium-navigator:$readiumVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.work:work-runtime-ktx:2.11.2")

    val okhttpVersion = "4.12.0"
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    testImplementation("com.squareup.okhttp3:mockwebserver:$okhttpVersion")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}