//! Android FFI entry points for pilinara-native
//! 
//! JNI bindings for:
//! - WebP encoding
//! - Audio normalization
//! - Danmaku merging

use jni::JNIEnv;
use jni::objects::{JClass, JByteArray, JString, JObject, JPrimitiveArray};
use jni::sys::jint;
use std::collections::HashMap;

// ============================================================================
// WebP Native Library
// ============================================================================

#[no_mangle]
pub extern "C" fn Java_com_example_pilinara_WebpNativeLib_create(
    _env: JNIEnv<'_>,
    _class: JClass<'_>,
    width: jint,
    height: jint,
) -> i64 {
    use crate::webp::AnimatedWebpEncoder;
    match AnimatedWebpEncoder::new(width as u32, height as u32) {
        Ok(encoder) => Box::into_raw(Box::new(encoder)) as i64,
        Err(_) => -1,
    }
}

#[no_mangle]
pub extern "C" fn Java_com_example_pilinara_WebpNativeLib_addFrame(
    _env: JNIEnv<'_>,
    _class: JClass<'_>,
    encoder_ptr: i64,
    _data: JByteArray<'_>,
    duration_ms: jint,
    x: jint,
    y: jint,
) -> jint {
    let _encoder = unsafe { &mut *(encoder_ptr as *mut crate::webp::AnimatedWebpEncoder) };
    let _ = duration_ms;
    let _ = x;
    let _ = y;
    0
}

#[no_mangle]
pub extern "C" fn Java_com_example_pilinara_WebpNativeLib_finalize<'a>(
    env: JNIEnv<'a>,
    _class: JClass<'_>,
    encoder_ptr: i64,
) -> JByteArray<'a> {
    let encoder = unsafe { Box::from_raw(encoder_ptr as *mut crate::webp::AnimatedWebpEncoder) };
    match encoder.finalize() {
        Ok(bytes) => {
            let jbytes = env.new_byte_array(bytes.len() as i32).unwrap();
            let mut buf = vec![0; bytes.len()];
            for (i, &b) in bytes.iter().enumerate() {
                buf[i] = b as i8;
            }
            env.set_byte_array_region(&jbytes, 0, &buf).unwrap();
            jbytes
        }
        Err(_) => env.new_byte_array(0).unwrap(),
    }
}

// ============================================================================
// Audio Native Library
// ============================================================================

#[no_mangle]
pub extern "C" fn Java_com_example_pilinara_AudioNativeLib_normalize<'a>(
    env: JNIEnv<'a>,
    _class: JClass<'_>,
    input: JByteArray<'a>,
    channels: jint,
) -> JByteArray<'a> {
    use crate::audio::{AudioNormalizer, AudioNormalizationConfig};
    
    let len = match env.get_array_length(&input) {
        Ok(l) => l as usize,
        Err(_) => return env.new_byte_array(0).unwrap(),
    };
    
    let mut buf = vec![0; len];
    env.get_byte_array_region(&input, 0, &mut buf).unwrap();
    
    let input_samples: Vec<i16> = buf
        .chunks_exact(2)
        .map(|c| i16::from_le_bytes([c[0] as u8, c[1] as u8]))
        .collect();
    
    let normalizer = AudioNormalizer::new(AudioNormalizationConfig::default());
    let output = normalizer.normalize_i16(&input_samples, channels as usize);
    
    let mut output_bytes = vec![0; output.len() * 2];
    for (i, &s) in output.iter().enumerate() {
        let b = s.to_le_bytes();
        output_bytes[i * 2] = b[0] as i8;
        output_bytes[i * 2 + 1] = b[1] as i8;
    }
    
    let jbytes = env.new_byte_array(output_bytes.len() as i32).unwrap();
    env.set_byte_array_region(&jbytes, 0, &output_bytes).unwrap();
    jbytes
}

// ============================================================================
// Danmaku Native Library
// ============================================================================

/// Create a new DanmakuMerger instance
#[no_mangle]
pub extern "C" fn Java_com_example_pilinara_DanmakuNativeLib_create(
    _env: JNIEnv<'_>,
    _class: JClass<'_>,
    window_seconds: f64,
    max_distance: f64,
    max_cosine: f64,
    use_pinyin: jboolean,
) -> i64 {
    use crate::danmaku::{DanmakuMergeConfig, DanmakuMerger};
    
    let config = DanmakuMergeConfig {
        window_seconds,
        max_distance,
        max_cosine,
        use_pinyin: use_pinyin != 0,
        ..Default::default()
    };
    
    let merger = DanmakuMerger::new(config);
    Box::into_raw(Box::new(merger)) as i64
}

/// Free DanmakuMerger instance
#[no_mangle]
pub extern "C" fn Java_com_example_pilinara_DanmakuNativeLib_destroy(
    _env: JNIEnv<'_>,
    _class: JClass<'_>,
    merger_ptr: i64,
) {
    if merger_ptr != 0 {
        unsafe {
            let _ = Box::from_raw(merger_ptr as *mut crate::danmaku::DanmakuMerger);
        }
    }
}

/// Load pinyin dictionary
#[no_mangle]
pub extern "C" fn Java_com_example_pilinara_DanmakuNativeLib_loadPinyinDict<'a>(
    env: JNIEnv<'a>,
    _class: JClass<'_>,
    merger_ptr: i64,
    dict_data: JByteArray<'a>,
) -> jint {
    let merger = unsafe { &mut *(merger_ptr as *mut crate::danmaku::DanmakuMerger) };
    
    let len = match env.get_array_length(&dict_data) {
        Ok(l) => l as usize,
        Err(_) => return -1,
    };
    
    let mut buf = vec![0; len];
    if let Err(_) = env.get_byte_array_region(&dict_data, 0, &mut buf) {
        return -1;
    }
    
    match merger.load_pinyin_dict(&buf) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

// Helper type for boolean in JNI
type jboolean = i32;
