//! PiliNara native library
//! 
//! Provides Rust implementations for:
//! - Animated WebP encoding (replacing Android Bitmap.compress)
//! - Audio normalization (dynaudnorm-style)
//! - Danmaku merging and filtering
//!
//! ## Architecture
//! 
//! Android builds use cargo-ndk or manual NDK toolchain configuration.
//! The JNI interface is in `android_entry.rs` (only compiled for Android targets).

extern crate std;

// WebP encoding module
pub mod webp;
// Audio normalization module
pub mod audio;
// Danmaku merging module
pub mod danmaku;
// Playurl DASH stream selection (stage 5)
pub mod playurl;

#[cfg(target_os = "android")]
mod android_entry;

#[cfg(not(target_os = "android"))]
pub use webp::{AnimatedWebpEncoder, WebpError};
#[cfg(not(target_os = "android"))]
pub use audio::{AudioNormalizer, AudioNormalizationConfig};
#[cfg(not(target_os = "android"))]
pub use danmaku::{DanmakuMerger, DanmakuMergeConfig, DanmakuEntry, MergedDanmaku};
