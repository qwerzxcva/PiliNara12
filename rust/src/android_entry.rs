//! Android FFI entry points for pilinara-native
//!
//! JNI bindings for:
//! - WebP encoding
//! - Audio normalization
//! - Danmaku merging

use jni::objects::{JByteArray, JClass, JObject, JPrimitiveArray, JString};
use jni::sys::jint;
use jni::JNIEnv;
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
    use crate::audio::{AudioNormalizationConfig, AudioNormalizer};

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
    env.set_byte_array_region(&jbytes, 0, &output_bytes)
        .unwrap();
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

    let u8_buf: Vec<u8> = buf.iter().map(|&b| b as u8).collect();
    match merger.load_pinyin_dict(&u8_buf) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

// Helper type for boolean in JNI
type jboolean = i32;

// ============================================================================
// Playurl DASH stream selection (stage 5)
// ============================================================================

/// 输入 playurl 响应 JSON，返回选中的 {video, audio, duration} JSON；失败返回 null。
#[no_mangle]
pub extern "C" fn Java_com_example_pilinara_PlayUrlNativeLib_selectStreams<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    body: JString<'a>,
    target_qn: jint,
) -> JString<'a> {
    let input: String = match env.get_string(&body) {
        Ok(s) => s.into(),
        Err(_) => return JObject::null().into(),
    };
    match crate::playurl::select_streams(&input, target_qn as i64) {
        Ok(result) => match env.new_string(result) {
            Ok(s) => s.into(),
            Err(_) => JObject::null().into(),
        },
        Err(_) => JObject::null().into(),
    }
}

// ============================================================================
// Danmaku block-rule filtering (batch L39): JSON in / JSON out
// 输入: entries=[{content,uid}...], rules={keywords:[],regexes:[],uids:[]}
// 输出: {kept_indices:[...], blocked_total, blocked_by_uid, blocked_by_keyword, blocked_by_regex}
// ============================================================================
#[no_mangle]
pub extern "C" fn Java_com_example_pilinara_DanmakuNativeLib_nativeFilterBlock<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    entries_json: JString<'a>,
    rules_json: JString<'a>,
) -> JString<'a> {
    let entries_s: String = match env.get_string(&entries_json) {
        Ok(s) => s.into(),
        Err(_) => return JObject::null().into(),
    };
    let rules_s: String = match env.get_string(&rules_json) {
        Ok(s) => s.into(),
        Err(_) => return JObject::null().into(),
    };
    let entries: Vec<crate::dmfilter::FilterInput> = match serde_json::from_str(&entries_s) {
        Ok(v) => v,
        Err(_) => return JObject::null().into(),
    };
    let rules: crate::dmfilter::BlockRules = match serde_json::from_str(&rules_s) {
        Ok(r) => r,
        Err(_) => return JObject::null().into(),
    };
    let report = crate::dmfilter::filter_entries(&entries, &rules);
    match env.new_string(serde_json::to_string(&report).unwrap_or_default()) {
        Ok(s) => s.into(),
        Err(_) => JObject::null().into(),
    }
}

// ============================================================================
// Danmaku merge (batch L15): JSON in / JSON out
// 输入: [[{id,mode,fontsize,color,timestamp,pool,content,uid}...], ...] 多源弹幕
// 输出: {entries:[...], filtered_count, merged_count, elapsed_ms}；失败返回 null
// ============================================================================
#[no_mangle]
pub extern "C" fn Java_com_example_pilinara_DanmakuNativeLib_merge<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    merger_ptr: i64,
    sources_json: JString<'a>,
) -> JString<'a> {
    let input: String = match env.get_string(&sources_json) {
        Ok(s) => s.into(),
        Err(_) => return JObject::null().into(),
    };
    if merger_ptr == 0 {
        return JObject::null().into();
    }
    let merger = unsafe { &*(merger_ptr as *const crate::danmaku::DanmakuMerger) };
    let parsed: Result<Vec<Vec<crate::danmaku::DanmakuEntry>>, _> = serde_json::from_str(&input);
    let sources = match parsed {
        Ok(v) => v,
        Err(_) => return JObject::null().into(),
    };
    let merged = merger.merge(sources);
    let result = serde_json::json!({
        "entries": merged.entries,
        "filtered_count": merged.filtered_count,
        "merged_count": merged.merged_count,
        "elapsed_ms": merged.elapsed_ms,
    });
    match env.new_string(result.to_string()) {
        Ok(s) => s.into(),
        Err(_) => JObject::null().into(),
    }
}
