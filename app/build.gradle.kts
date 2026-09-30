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
        // Pre-release: 0.0.x signals pre-alpha; MINOR bumps (0.1.0+) start
        // when the first release ships, per the CHANGELOG versioning policy.
        versionName = "0.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            // Release signing (#32): credentials never live in this repo. Set
            // them in ~/.gradle/gradle.properties (or CI secrets):
            //   LEAFLINE_STORE_FILE=/absolute/path/to/leafline-release.jks
            //   LEAFLINE_STORE_PASSWORD=...
            //   LEAFLINE_KEY_ALIAS=leafline
            //   LEAFLINE_KEY_PASSWORD=...
            // When the properties are absent the release build stays unsigned,
            // which is what F-Droid/Acres recipe builds produce anyway (they
            // sign with their own keys at packaging time).
            val storeFile = (project.findProperty("LEAFLINE_STORE_FILE") as? String)
                ?.takeIf { it.isNotBlank() }
            if (storeFile != null) {
                this.storeFile = project.file(storeFile)
                storePassword = project.findProperty("LEAFLINE_STORE_PASSWORD") as? String
                keyAlias = project.findProperty("LEAFLINE_KEY_ALIAS") as? String
                keyPassword = project.findProperty("LEAFLINE_KEY_PASSWORD") as? String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Only attach the signing config once it has actually been
            // populated, so a credential-less environment still builds an
            // unsigned release APK instead of failing.
            signingConfigs.findByName("release")
                ?.takeIf { it.storeFile != null }
                ?.let { signingConfig = it }
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
    testImplementation("app.cash.turbine:turbine:1.2.1")
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
    implementation("org.readium.kotlin-toolkit:readium-navigator-media-tts:$readiumVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.work:work-runtime-ktx:2.11.2")

    val okhttpVersion = "4.12.0"
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    testImplementation("com.squareup.okhttp3:mockwebserver:$okhttpVersion")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}