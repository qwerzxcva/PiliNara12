// Rust build integration for PiliNara (ARM64 only)
// This script is sourced by android/app/build.gradle.kts

import org.gradle.api.tasks.Exec
import java.io.File

// Cargo/Rust configuration
val rustProjectDir = file("..")
val cargoTargetDir = file("$buildDir/rust-target")

// Android ABI targets - ONLY ARM64
val androidAbis = listOf(
    "arm64-v8a" to "aarch64-linux-android"
)

// NDK version
val ndkVersion = project.ext.has("ndkVersion") ? project.ext.get("ndkVersion") : "27.3.13750724"

// Task: build Rust library for ARM64 only
tasks.register<Exec>("cargoBuildAndroid") {
    group = "rust"
    description = "Build Rust native library for arm64-v8a"
    
    val ndkHome = System.getenv("ANDROID_NDK_HOME") ?: findNdkHome()
    val toolchainRoot = File(ndkHome, "toolchains/llvm/prebuilt/linux-x86_64")
    
    for ((abi, target) in androidAbis) {
        val outputDir = file("$buildDir/intermediates/rust/release/$abi")
        
        doLast {
            // Set environment for cross-compilation
            environment("TARGET", target)
            environment("CROSS_COMPILE", "${toolchainRoot}/bin/${target.replace("-linux-android", "21-")}")
            environment("NDK_HOME", ndkHome)
            
            // Build with cargo-ndk
            val cargoCmd = arrayOf(
                "cargo", "ndk",
                "-t", "arm64-v8a",
                "-o", "${project.rootDir.absolutePath}/android/app/src/main/jniLibs".toString(),
                "build",
                "--release"
            )
            
            workingDir = rustProjectDir
            executable = findCargoExecutable()
            
            val result = exec {
                commandLine(*cargoCmd)
                standardOutput = System.out
                errorOutput = System.err
            }
        }
    }
}

fun findCargoExecutable(): String {
    val home = System.getenv("HOME")
    return "$home/.cargo/bin/cargo"
}

fun findNdkHome(): String {
    val localProperties = File("local.properties")
    if (localProperties.exists()) {
        val props = java.util.Properties()
        localProperties.inputStream().use { props.load(it) }
        val sdkHome = props.getProperty("sdk.dir")
        if (sdkHome != null) {
            val ndkPaths = listOf(
                "$sdkHome/ndk/${project.ext.get("ndkVersion")}",
                "$sdkHome/ndk/bundle",
                "$sdkHome/ndk/latest"
            )
            for (path in ndkPaths) {
                if (File(path).exists()) return path
            }
        }
    }
    return System.getenv("ANDROID_NDK_HOME") ?: ""
}
