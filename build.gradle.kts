plugins {
    id("com.android.application") version "9.3.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20" apply false
    id("com.google.devtools.ksp") version "2.2.20-2.0.2" apply false
}

allprojects {
    group = "com.jdluu.leafline"
    version = "0.0.1-SNAPSHOT"
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}
