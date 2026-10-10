use std::env;

fn main() {
    println!("cargo:rerun-if-changed=build.rs");

    // Detect Android NDK target
    let target = env::var("TARGET").unwrap_or_default();

    if target.contains("android") {
        // Link against system libraries
        println!("cargo:rustc-link-lib=dylib=android");
        println!("cargo:rustc-link-lib=dylib=c");
        println!("cargo:rustc-link-lib=dylib=log");

        // Only build for ARM64
        println!("cargo:rustc-env=TARGET_ARCH=arm64-v8a");
    }
}
