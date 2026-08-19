buildscript {
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:8.6.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24")
        classpath("org.jetbrains.kotlin:kotlin-serialization:1.9.24")
        classpath("org.jetbrains.compose:compose-gradle-plugin:1.6.11")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.51.1")
        if (file("app/google-services.json").exists()) {
            classpath("com.google.gms:google-services:4.4.4")
        }
    }
}
