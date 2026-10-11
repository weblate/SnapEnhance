import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.ByteArrayOutputStream
import java.util.Locale

interface InjectedExecOps { @get:Inject val execOps: ExecOperations }

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.compose.compiler)
    id("kotlin-parcelize")
}

android {
    namespace = rootProject.ext["applicationId"].toString()
    compileSdk = 37

    buildFeatures {
        aidl = true
        compose = true
    }

    defaultConfig {
        applicationId = rootProject.ext["applicationId"].toString()
        versionCode = rootProject.ext["appVersionCode"].toString().toInt()
        versionName = rootProject.ext["appVersionName"].toString()
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
            // Temp go back to non minify, android.nonFinalResIds causing issue.
            val isCustomDebug = properties["debug_flavor"] != null
            isDebuggable = isCustomDebug
            isMinifyEnabled = !isCustomDebug
            isShrinkResources = !isCustomDebug
            proguardFiles += file("proguard-rules.pro")
        }
    }

    flavorDimensions += "abi"

    packaging {
        jniLibs {
            excludes += "**/*_neon.so"
        }

        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "DebugProbesKt.bin",
                "okhttp3/internal/publicsuffix/**",
                "META-INF/*.version",
                "META-INF/services/**",
                "META-INF/*.kotlin_builtins",
                "META-INF/*.kotlin_module"
            )
        }
    }

    // noinspection ChromeOsAbiSupport
    productFlavors {
        create("core") {
            dimension = "abi"
        }

        create("armv8") {
            ndk { abiFilters += "arm64-v8a" }
            dimension = "abi"
        }

        create("armv7") {
            ndk { abiFilters += "armeabi-v7a" }
            dimension = "abi"
        }

        create("all") {
            ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
            dimension = "abi"
        }
    }

    properties["debug_flavor"]?.let { targetFlavor ->
        productFlavors.find { it.name == targetFlavor.toString() }?.isDefault = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    //noinspection WrongGradleMethod
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

androidComponents {
    onVariants(selector().withFlavor("abi", "core")) { variant ->
        variant.packaging.jniLibs.apply {
            pickFirsts.set(listOf("**/lib${rootProject.ext["buildHash"]}.so"))
            excludes.set(listOf("**/*.so"))
        }
    }
}

tasks.configureEach {
    if (name.startsWith("package") && name.endsWith("Debug")) {
        doLast {
            val apkDir = layout.buildDirectory.dir("outputs/apk").get().asFile
            apkDir.walkTopDown()
                .filter { it.isFile && it.extension == "apk" && !it.name.startsWith("snapenhance") && it.name != "Core.apk" }
                .forEach { apkFile ->
                    val parentFolder = apkFile.parentFile.name
                    val isCore = parentFolder.contains("core", ignoreCase = true) || apkFile.name.contains("core", ignoreCase = true)
                    val newName = if (isCore) {
                        "Core.apk"
                    } else {
                        "snapenhance_${rootProject.ext["appVersionName"]}-${parentFolder}.apk"
                    }
                    apkFile.renameTo(file("${apkFile.parent}/$newName"))
                }
        }
    }
}

dependencies {
    fun fullImplementation(dependencyNotation: Any) {
        compileOnly(dependencyNotation)
        for (flavorName in listOf("armv8", "armv7", "all")) {
            dependencies.add("${flavorName}Implementation", dependencyNotation)
        }
    }

    implementation(project(":core"))
    implementation(project(":common"))
    implementation(libs.androidx.documentfile)
    implementation(libs.gson)
    implementation(libs.smart.exception.java)
    implementation(files("libs/ffmpeg-kit-full-gpl-6.0-2.LTS.aar"))
    implementation(libs.osmdroid.android)
    implementation(libs.rhino)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.androidliquidglass)
    implementation(libs.capsule)
    fullImplementation(platform(libs.androidx.compose.bom))
    fullImplementation(libs.bcprov.jdk18on)
    fullImplementation(libs.androidx.navigation.compose)
    fullImplementation(libs.androidx.material.icons.core)
    fullImplementation(libs.androidx.material.ripple)
    fullImplementation(libs.androidx.material.icons.extended)
    fullImplementation(libs.androidx.material3)
    fullImplementation(libs.coil.compose)
    fullImplementation(libs.coil.video)
    fullImplementation(libs.colorpicker.compose)
    fullImplementation(libs.androidx.ui.tooling.preview)
    fullImplementation(libs.haze)
    fullImplementation(libs.haze.materials)

    properties["debug_flavor"]?.let {
        debugImplementation(libs.androidx.ui.tooling)
    }
}

afterEvaluate {
    val debugFlavor = properties["debug_flavor"]?.toString()
    val packageName = properties["debug_package_name"]?.toString() ?: rootProject.ext["applicationId"].toString()

    if (debugFlavor != null) {
        val taskName = "install${debugFlavor.uppercase(Locale.getDefault())}Debug"
        val execOps = objects.newInstance<InjectedExecOps>().execOps

        tasks.findByName(taskName)?.doLast {
            runCatching {
                val output = ByteArrayOutputStream()
                execOps.exec {
                    commandLine("adb", "devices")
                    standardOutput = output
                }
                val devices = output.toString().lines().drop(1).mapNotNull { line ->
                    line.split("\t").firstOrNull()?.takeIf { it.isNotBlank() }
                }
                devices.forEach { device ->
                    execOps.exec { commandLine("adb", "-s", device, "shell", "am", "force-stop", packageName) }
                    execOps.exec { commandLine("adb", "-s", device, "shell", "monkey", "-p", packageName, "-c", "android.intent.category.LAUNCHER", "1") }
                }
            }
        }
    }
}

properties["debug_flavor"]?.let {
    configurations.all {
        exclude(group = "androidx.profileinstaller", module = "profileinstaller")
    }
}