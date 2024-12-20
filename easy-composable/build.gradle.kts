plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    val composeCompilerVersion: String by project

    namespace = "gr.sppzglou.easy.composable"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    composeOptions {
        kotlinCompilerExtensionVersion = composeCompilerVersion
    }
}

dependencies {
    implementation("androidx.activity:activity-compose:1.9.3")

    //COMPOSE
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material3:material3")

    //CameraX
    val cameraX = "1.4.0"
    implementation("androidx.camera:camera-camera2:$cameraX")
    implementation("androidx.camera:camera-lifecycle:$cameraX")
    implementation("androidx.camera:camera-view:$cameraX")
    implementation("androidx.camera:camera-video:$cameraX")

    //Glide
    implementation("com.github.bumptech.glide:compose:1.0.0-beta01")
    //sliders
    implementation("com.github.krottv:compose-sliders:0.1.4")
    //In-app update
    implementation("com.google.android.play:app-update-ktx:2.1.0")
    //Video payer
    val player = "1.4.1"
    implementation("androidx.media3:media3-exoplayer:$player")
    implementation("androidx.media3:media3-ui:$player")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.github.sppzglou"
                artifactId = "easy.composable"
            }
        }
    }
}