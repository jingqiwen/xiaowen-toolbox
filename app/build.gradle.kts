import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.jisuanyusuiji.toolbox"
    compileSdk = 36
    buildToolsVersion = "36.1.0"

    defaultConfig {
        applicationId = "com.jisuanyusuiji.toolbox"
        minSdk = 26
        targetSdk = 36
        versionCode = 17
        versionName = "0.12.0"
    }

    signingConfigs {
        // 项目自带的调试签名，避免依赖 C:\Users\<用户>\.android\debug.keystore，
        // 让新环境也能直接构建 debug APK。
        create("localDebug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        // release 签名：读取本机 release-signing.properties（已被 .gitignore 忽略）。
        // 没有该文件时自动跳过，assembleDebug 不受影响。
        val releaseProps = Properties().apply {
            val f = rootProject.file("release-signing.properties")
            if (f.exists()) f.inputStream().use { load(it) }
        }
        val releaseStore = rootProject.file(releaseProps.getProperty("storeFile") ?: "release.keystore")
        if (releaseStore.exists()) {
            create("releaseLocal") {
                storeFile = releaseStore
                storePassword = releaseProps.getProperty("storePassword") ?: ""
                keyAlias = releaseProps.getProperty("keyAlias") ?: ""
                keyPassword = releaseProps.getProperty("keyPassword") ?: ""
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("localDebug")
        }
        release {
            isMinifyEnabled = false
            if (rootProject.file("release.keystore").exists()) {
                signingConfig = signingConfigs.getByName("releaseLocal")
            }
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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // 本地二维码生成
    implementation("com.google.zxing:core:3.5.3")

    // 视频处理（Media3 Transformer）
    implementation("androidx.media3:media3-common:1.5.1")
    implementation("androidx.media3:media3-effect:1.5.1")
    implementation("androidx.media3:media3-transformer:1.5.1")

    // 镜子：CameraX 前置摄像头预览
    implementation("androidx.camera:camera-core:1.4.1")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
