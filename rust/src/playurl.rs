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
    // 普通视频: data.dash；番剧(pgc): result.dash —— 两者结构同构，兼容取用
    let dash_node = if v["data"]["dash"].is_object() {
        v["data"]["dash"].clone()
    } else {
        v["result"]["dash"].clone()
    };
    let dash: Dash = serde_json::from_value(dash_node).map_err(|e| format!("no dash: {e}"))?;

    // ---- video：qn 优先，其次编码偏好（默认 avc > hevc > 其他；审核220 支持 _preferCodec 覆盖）----
    let prefer = v["_preferCodec"].as_str().unwrap_or("avc").to_lowercase();
    let codec_rank = |c: &str| -> i64 {
        let fam = if c.starts_with("avc") {
            "avc"
        } else if c.starts_with("hev") || c.starts_with("hvc") || c.starts_with("h265") {
            "hevc"
        } else if c.contains("av01") || c.contains("av1") {
            "av1"
        } else {
            "other"
        };
        if fam == prefer {
            0
        } else {
            match fam {
                "avc" => 1,
                "hevc" => 2,
                "av1" => 3,
                _ => 4,
            }
        }
    };

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

    // ---- audio：审核219 音质目标（B站音频流 id：30251 Hi-Res无损 > 30280 192k > 30232 132k > 30216 64k）----
    // 设置页传入的 targetAudioQn 经 body 顶层 "_targetAudioQn" 传递（0=默认取最高音质）。
    // Flutter defaultAudioQa 对齐；未指定时维持旧行为（bandwidth 最高）。
    let target_audio = v["_targetAudioQn"].as_i64().unwrap_or(0);
    fn audio_qn_rank(id: i64) -> i64 {
        match id {
            30251 => 4, // Hi-Res
            30280 => 3, // 192k
            30232 => 2, // 132k
            30216 => 1, // 64k
            _ => 0,
        }
    }
    let best_audio = match target_audio {
        0 => dash.audio.iter().max_by_key(|s| s.bandwidth),
        qn => dash
            .audio
            .iter()
            .min_by_key(|s| {
                let r = audio_qn_rank(s.id);
                let qn_r = audio_qn_rank(qn);
                (
                    if r > 0 { (qn_r - r).abs() } else { i64::MAX },
                    std::cmp::Reverse(r),
                    std::cmp::Reverse(s.bandwidth),
                )
            })
            .filter(|_| dash.audio.iter().any(|s| audio_qn_rank(s.id) > 0))
            .or_else(|| dash.audio.iter().max_by_key(|s| s.bandwidth)),
    }
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
    fn selects_target_audio_qn() {
        // 审核219：_targetAudioQn=30216（64k）时选 30216 而非带宽最高的 30280
        let body = r#"{"code":0,"_targetAudioQn":30216,"data":{"dash":{
            "video":[{"id":80,"codecs":"avc1","bandwidth":1000,"baseUrl":"http://v/80"}],
            "audio":[{"id":30280,"bandwidth":320000,"baseUrl":"http://a/192k"},
                     {"id":30216,"bandwidth":64000,"baseUrl":"http://a/64k"}],
            "duration":1}}}"#;
        let out = select_streams(body, 80).unwrap();
        let v: Value = serde_json::from_str(&out).unwrap();
        assert_eq!(v["audio"]["id"], 30216);
    }

    #[test]
    fn defaults_to_highest_audio_bandwidth() {
        // 未传 _targetAudioQn → 维持旧行为（bandwidth 最高）
        let body = r#"{"code":0,"data":{"dash":{
            "video":[{"id":80,"codecs":"avc1","bandwidth":1000,"baseUrl":"http://v/80"}],
            "audio":[{"id":30216,"bandwidth":64000,"baseUrl":"http://a/64k"},
                     {"id":30280,"bandwidth":320000,"baseUrl":"http://a/192k"}],
            "duration":1}}}"#;
        let out = select_streams(body, 80).unwrap();
        let v: Value = serde_json::from_str(&out).unwrap();
        assert_eq!(v["audio"]["id"], 30280);
    }

    #[test]
    fn prefers_hevc_when_requested() {
        // 审核220：_preferCodec=hevc 时优先 hevc（默认会选 avc）
        let body = r#"{"code":0,"_preferCodec":"hevc","data":{"dash":{
            "video":[{"id":80,"codecs":"avc1.640032","bandwidth":1000,"baseUrl":"http://v/avc"},
                     {"id":80,"codecs":"hev1.1.6","bandwidth":1000,"baseUrl":"http://v/hev"}],
            "audio":[{"id":30280,"bandwidth":320000,"baseUrl":"http://a/192k"}],
            "duration":1}}}"#;
        let out = select_streams(body, 80).unwrap();
        let v: Value = serde_json::from_str(&out).unwrap();
        assert!(v["video"]["codecs"].as_str().unwrap().starts_with("hev"));
    }

    #[test]
    fn rejects_api_error() {
        assert!(select_streams(r#"{"code":-404}"#, 80).is_err());
    }
}
