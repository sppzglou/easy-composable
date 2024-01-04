buildscript {
    val kotlinVersion: String by project

    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        //Gradle
        classpath("com.android.tools.build:gradle:8.1.4")
        //Compose
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}

allprojects {
    repositories {
        google()
        maven("https://jitpack.io")
        mavenCentral()
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}