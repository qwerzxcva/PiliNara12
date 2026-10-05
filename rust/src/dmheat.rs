//! Danmaku heat-map / "高能进度条" (批次L42)
//!
//! 承接 B 站进度条下方的弹幕密度热力曲线：把整片弹幕按时间分桶统计密度，
//! 归一化到 0..1 供 Kotlin 绘制曲线。数千~数万条弹幕的 O(n) 分桶下沉 Rust，
//! 避免在主线程逐条计算。

use serde::{Deserialize, Serialize};

/// 输入：一条弹幕的时间（毫秒）+ 权重（普通弹幕=1，高级/置顶可加权）
#[derive(Debug, Clone, Deserialize)]
pub struct HeatPoint {
    #[serde(default)]
    pub t: f64, // 毫秒
    #[serde(default = "one")]
    pub w: f64, // 权重
}

fn one() -> f64 {
    1.0
}

/// 输出：分桶密度（已归一化 0..1）+ 峰值下标，供 UI 直接绘制
#[derive(Debug, Serialize)]
pub struct HeatMap {
    pub buckets: Vec<f64>,
    pub peak_index: usize,
    pub peak_value: f64,
    pub total: f64,
}

/// 计算密度热力曲线。
/// * `duration_ms` 视频总时长（毫秒，<=0 时用弹幕最大时间兜底）
/// * `bucket_count` 分桶数（与进度条像素宽度同量级，通常 100~300）
pub fn compute_heat(points: &[HeatPoint], duration_ms: f64, bucket_count: usize) -> HeatMap {
    if bucket_count == 0 {
        return HeatMap {
            buckets: vec![],
            peak_index: 0,
            peak_value: 0.0,
            total: 0.0,
        };
    }
    let max_t = points.iter().map(|p| p.t).fold(0.0_f64, f64::max);
    let span = if duration_ms > 0.0 {
        duration_ms
    } else {
        max_t
    };
    if span <= 0.0 {
        return HeatMap {
            buckets: vec![0.0; bucket_count],
            peak_index: 0,
            peak_value: 0.0,
            total: 0.0,
        };
    }

    let mut raw = vec![0.0_f64; bucket_count];
    for p in points {
        if p.t < 0.0 {
            continue;
        }
        let idx = ((p.t / span) * bucket_count as f64) as usize;
        let idx = idx.min(bucket_count - 1);
        raw[idx] += if p.w > 0.0 { p.w } else { 1.0 };
    }

    // 归一化
    let peak = raw.iter().cloned().fold(0.0_f64, f64::max);
    let total: f64 = raw.iter().sum();
    let buckets: Vec<f64> = if peak > 0.0 {
        raw.iter().map(|v| v / peak).collect()
    } else {
        raw.clone()
    };
    let peak_index = raw
        .iter()
        .enumerate()
        .max_by(|a, b| a.1.partial_cmp(b.1).unwrap_or(std::cmp::Ordering::Equal))
        .map(|(i, _)| i)
        .unwrap_or(0);

    HeatMap {
        buckets,
        peak_index,
        peak_value: peak,
        total,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_uniform_distribution() {
        // 10 条均匀分布到 10 桶 → 每桶 1，归一化后全 1
        let pts: Vec<HeatPoint> = (0..10)
            .map(|i| HeatPoint {
                t: (i as f64) * 1000.0,
                w: 1.0,
            })
            .collect();
        let hm = compute_heat(&pts, 10_000.0, 10);
        assert_eq!(hm.buckets.len(), 10);
        assert!(hm.buckets.iter().all(|v| (*v - 1.0).abs() < 1e-9));
        assert_eq!(hm.total, 10.0);
    }

    #[test]
    fn test_peak_detection() {
        let mut pts: Vec<HeatPoint> = (0..10)
            .map(|i| HeatPoint {
                t: (i as f64) * 1000.0,
                w: 1.0,
            })
            .collect();
        // 在第 5 桶堆 5 条
        for _ in 0..5 {
            pts.push(HeatPoint { t: 5500.0, w: 1.0 });
        }
        let hm = compute_heat(&pts, 10_000.0, 10);
        assert_eq!(hm.peak_index, 5);
        assert!((hm.peak_value - 6.0).abs() < 1e-9);
        assert!((hm.buckets[5] - 1.0).abs() < 1e-9); // 归一化后峰值为 1
    }

    #[test]
    fn test_empty_and_zero_duration() {
        let hm = compute_heat(&[], 0.0, 8);
        assert_eq!(hm.buckets.len(), 8);
        assert_eq!(hm.total, 0.0);
    }

    #[test]
    fn test_duration_fallback_to_max_time() {
        // duration 未知 → 用最大时间兜底，最后一条应落在最后一桶
        let pts = vec![
            HeatPoint { t: 0.0, w: 1.0 },
            HeatPoint { t: 9000.0, w: 1.0 },
        ];
        let hm = compute_heat(&pts, 0.0, 10);
        assert!(hm.buckets[9] > 0.0);
    }
}
