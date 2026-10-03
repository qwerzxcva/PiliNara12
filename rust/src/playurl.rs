//! playurl DASH 流选择逻辑（stage ⑤）
//!
//! 输入：Bilibili playurl 响应 JSON（x/player/wbi/playurl）
//! 输出：选中的 video/audio 流 JSON `{ "video": {...}, "audio": {...} }`
//! 规则：video 按 目标qn 优先 → 编码(avc优先,省电) → bandwidth 最高；audio 按 bandwidth 最高。

use serde::{Deserialize, Serialize};
use serde_json::Value;

#[derive(Deserialize, Serialize, Debug, Clone, Default)]
pub struct Stream {
    #[serde(default)]
    pub id: i64,
    #[serde(default, rename = "baseUrl")]
    pub base_url: String,
    #[serde(default, rename = "backupUrl")]
    pub backup_url: Vec<String>,
    #[serde(default)]
    pub codecs: String,
    #[serde(default)]
    pub bandwidth: i64,
    #[serde(default)]
    pub width: i32,
    #[serde(default)]
    pub height: i32,
    #[serde(default, rename = "frameRate")]
    pub frame_rate: String,
    #[serde(default)]
    pub size: i64,
}

#[derive(Deserialize, Debug, Default)]
pub struct Dash {
    #[serde(default)]
    pub duration: i64,
    #[serde(default, rename = "minBufferTime")]
    pub min_buffer_time: f64,
    #[serde(default)]
    pub video: Vec<Stream>,
    #[serde(default)]
    pub audio: Vec<Stream>,
}

/// 从 playurl 响应 JSON 中选出最优流组合。
/// target_qn: 期望清晰度（如 80=1080P）；0 = 不限，取最高。
pub fn select_streams(body: &str, target_qn: i64) -> Result<String, String> {
    let v: Value = serde_json::from_str(body).map_err(|e| format!("bad json: {e}"))?;
    if v["code"].as_i64().unwrap_or(-1) != 0 {
        return Err(format!("api error code={}", v["code"]));
    }
    let dash: Dash = serde_json::from_value(v["data"]["dash"].clone())
        .map_err(|e| format!("no dash: {e}"))?;

    // ---- video：qn 优先，其次编码偏好（avc > hevc > 其他），最后 bandwidth ----
    fn codec_rank(c: &str) -> i64 {
        if c.starts_with("avc") {
            0
        } else if c.starts_with("hevc") || c.starts_with("hvc") {
            1
        } else {
            2
        }
    }

    let mut videos = dash.video.clone();
    videos.sort_by(|a, b| {
        let qa = (a.id - target_qn).abs();
        let qb = (b.id - target_qn).abs();
        qa.cmp(&qb)
            .then(codec_rank(&a.codecs).cmp(&codec_rank(&b.codecs)))
            .then(b.bandwidth.cmp(&a.bandwidth))
    });
    let best_video = videos
        .first()
        .ok_or_else(|| "no video stream".to_string())?;

    // ---- audio：bandwidth 最高 ----
    let best_audio = dash
        .audio
        .iter()
        .max_by_key(|s| s.bandwidth)
        .ok_or_else(|| "no audio stream".to_string())?;

    let out = serde_json::json!({
        "video": best_video,
        "audio": best_audio,
        "duration": dash.duration,
    });
    Ok(out.to_string())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn selects_target_qn_and_avc() {
        let body = r#"{"code":0,"data":{"dash":{
            "duration":100,"video":[
                {"id":80,"baseUrl":"http://v/hevc","codecs":"hevc","bandwidth":2000000},
                {"id":64,"baseUrl":"http://v/avc64","codecs":"avc1","bandwidth":3000000},
                {"id":80,"baseUrl":"http://v/avc80","codecs":"avc1","bandwidth":1500000}],
            "audio":[{"baseUrl":"http://a/low","bandwidth":64000},
                     {"baseUrl":"http://a/high","bandwidth":192000}]}}}"#;
        let out = select_streams(body, 80).unwrap();
        let v: Value = serde_json::from_str(&out).unwrap();
        assert_eq!(v["video"]["baseUrl"], "http://v/avc80");
        assert_eq!(v["audio"]["baseUrl"], "http://a/high");
    }

    #[test]
    fn falls_back_to_closest_qn() {
        let body = r#"{"code":0,"data":{"dash":{
            "video":[{"id":32,"baseUrl":"http://v/32","codecs":"avc1","bandwidth":800000}],
            "audio":[{"baseUrl":"http://a","bandwidth":64000}]}}}"#;
        let out = select_streams(body, 116).unwrap();
        let v: Value = serde_json::from_str(&out).unwrap();
        assert_eq!(v["video"]["baseUrl"], "http://v/32");
    }

    #[test]
    fn rejects_api_error() {
        assert!(select_streams(r#"{"code":-404}"#, 80).is_err());
    }
}
