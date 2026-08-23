import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.compose.compiler)
    id("kotlin-parcelize")
}

android {
    namespace = rootProject.ext["applicationId"].toString() + ".manager"
    compileSdk = 37

    androidResources {
        noCompress += ".so"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("String", "APPLICATION_ID", "\"${rootProject.ext["applicationId"]}\"")
        applicationId = rootProject.ext["applicationId"].toString() + ".manager"
        versionCode = 1
        versionName = "1.0.0"
        minSdk = 28
        targetSdk = 36
        multiDexEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles += file("proguard-rules.pro")
        }
        debug {
            (properties["debug_flavor"] == null).also {
                isDebuggable = !it
                isMinifyEnabled = it
                isShrinkResources = it
            }
            proguardFiles += file("proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.addAll(
                "-XXLanguage:+PropertyParamAnnotationDefaultTargetMode",
                "-Xjvm-default=all"
            )
        }
    }
}

tasks.configureEach {
    if (name.startsWith("package")) {
        doLast {
            val apkDir = layout.buildDirectory.dir("outputs/apk").get().asFile
            apkDir.walkTopDown()
                .filter { it.isFile && it.extension == "apk" && it.name != "Manager.apk" }
                .forEach { apkFile ->
                    apkFile.renameTo(file("${apkFile.parent}/Manager.apk"))
                }
        }
    }
}

configurations {
    all {
        resolutionStrategy {
            exclude(group = "com.google.guava", module = "listenablefuture")
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(libs.libsu)
    implementation(libs.guava)
    implementation(libs.apksig)
    implementation(libs.dexlib2)
    implementation(libs.gson)
    implementation(libs.jsoup)
    implementation(libs.okhttp)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material3)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.ripple)
    implementation(libs.androidx.material.icons.extended)
}