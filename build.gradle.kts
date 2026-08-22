plugins {
    id("com.android.application") version "9.3.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20" apply false
}

allprojects {
    group = "com.jdluu.leafline"
    version = "0.1.0-SNAPSHOT"
}

val clean by tasks.registering(Delete::class) {
    delete(layout.buildDirectory)
}
