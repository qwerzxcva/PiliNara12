use std::env;

fn main() {
    println!("cargo:rerun-if-changed=build.rs");

    // Detect Android NDK target
    let target = env::var("TARGET").unwrap_or_default();

    if target.contains("android") {
        // Get NDK toolchain path
        let ndk_home = env::var("NDK_HOME")
            .unwrap_or_else(|_| env::var("ANDROID_NDK_HOME").unwrap_or_default());

        if !ndk_home.is_empty() {
            println!("cargo:rustc-link-search={}/toolchains/llvm/prebuilt/linux-x86_64/lib64/clang/17.0.2/lib/aarch64-linux-android/21/", ndk_home);
        }

        // Link against system libraries
        println!("cargo:rustc-link-lib=dylib=android");
        println!("cargo:rustc-link-lib=dylib=c");
        println!("cargo:rustc-link-lib=dylib=log");

        // Only build for ARM64
        println!("cargo:rustc-env=TARGET_ARCH=arm64-v8a");
    }
}
