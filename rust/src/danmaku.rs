//! Danmaku merging and filtering algorithms
//!
//! High-performance Rust implementation for:
//! - Danmaku merging across multiple streams
//! - Pinyin-based filtering
//! - Collision detection and avoidance

use std::collections::HashMap;
use std::time::Instant;

/// Represents a single danmaku comment
#[derive(Debug, Clone, serde::Serialize, serde::Deserialize)]
pub struct DanmakuEntry {
    pub id: u64,
    pub mode: i32, // B站协议：1-3=滚动, 4=底部, 5=顶部, 6=逆向, 7=精准, 8=大会员
    pub fontsize: i32,
    pub color: u32,
    pub timestamp: f64, // seconds
    pub pool: i32,
    pub content: String,
    pub creator_mid: Option<u64>,
    pub uid: u64,
}

/// Configuration for danmaku merging
#[derive(Debug, Clone)]
pub struct DanmakuMergeConfig {
    pub window_seconds: f64,
    pub max_distance: f64,
    pub max_cosine: f64,
    pub use_pinyin: bool,
    pub skip_subtitle: bool,
    pub skip_advanced: bool,
    pub skip_bottom: bool,
    pub mark_position: bool,
    pub mark_threshold: f64,
}

impl Default for DanmakuMergeConfig {
    fn default() -> Self {
        Self {
            window_seconds: 5.0,
            max_distance: 1.5,
            max_cosine: 0.95,
            use_pinyin: false,
            // 审核轮189（真bug）：B站协议 mode 4=底部 5=顶部（本文件旧注释 1=scroll/2=top/3=bottom
            // 是错误语义）。原 default skip_subtitle/skip_advanced=true 会把用户底部+顶部
            // 弹幕在 Rust 合并阶段整类静默丢弃。现默认不过滤，类型开关由 Kotlin UI 控制（审核167）。
            skip_subtitle: false,
            skip_advanced: false,
            skip_bottom: false,
            mark_position: false,
            mark_threshold: 0.8,
        }
    }
}

/// Result of danmaku merging
#[derive(Debug, Clone, serde::Serialize, serde::Deserialize)]
pub struct MergedDanmaku {
    pub entries: Vec<DanmakuEntry>,
    pub filtered_count: usize,
    pub merged_count: usize,
    pub elapsed_ms: u64,
}

/// Danmaku merger processor
pub struct DanmakuMerger {
    config: DanmakuMergeConfig,
    pinyin_dict: Option<HashMap<String, String>>,
}

impl DanmakuMerger {
    pub fn new(config: DanmakuMergeConfig) -> Self {
        Self {
            config,
            pinyin_dict: None,
        }
    }

    /// Load pinyin dictionary for filtering
    pub fn load_pinyin_dict(&mut self, _dict_data: &[u8]) -> Result<(), String> {
        // Parse packed pinyin dictionary
        // Simplified for now - in production would parse the .pakku file
        self.pinyin_dict = Some(HashMap::new());
        Ok(())
    }

    /// Merge danmaku entries from multiple sources
    pub fn merge(&self, entries: Vec<Vec<DanmakuEntry>>) -> MergedDanmaku {
        let start = Instant::now();

        // Flatten and sort by timestamp
        let mut all_entries: Vec<DanmakuEntry> = entries.into_iter().flatten().collect();
        all_entries.sort_by(|a, b| {
            a.timestamp
                .partial_cmp(&b.timestamp)
                .unwrap_or(std::cmp::Ordering::Equal)
        });

        let initial_count = all_entries.len();

        // Apply filters
        let filtered: Vec<DanmakuEntry> = all_entries
            .into_iter()
            .filter(|e| !self.should_skip(e))
            .collect();

        let filtered_count = initial_count - filtered.len();

        // Merge similar danmaku
        let (merged, merged_count) = self.merge_similar(filtered);

        let elapsed = start.elapsed();
        eprintln!(
            "Danmaku merge: {} -> {} (filtered {}, merged {}), took {:?}",
            initial_count,
            merged.len(),
            filtered_count,
            merged_count,
            elapsed
        );

        MergedDanmaku {
            entries: merged,
            filtered_count,
            merged_count,
            elapsed_ms: elapsed.as_millis() as u64,
        }
    }

    fn should_skip(&self, entry: &DanmakuEntry) -> bool {
        // 审核轮189：按 B站协议 mode 语义（1-3 滚动 / 4 底部 / 5 顶部 / 6 逆向 / 7 精准）。
        // 旧实现把 4 当"字幕"、5 当"高级"、3 当"底部"，与协议不符 → 误删用户弹幕。
        if self.config.skip_subtitle && entry.mode == 8 {
            return true; // 大会员专属弹幕
        }
        if self.config.skip_advanced && entry.mode == 7 {
            return true; // 精准定位弹幕（特殊模式）
        }
        if self.config.skip_bottom && entry.mode == 4 {
            return true; // 底部弹幕
        }
        false
    }

    fn merge_similar(&self, entries: Vec<DanmakuEntry>) -> (Vec<DanmakuEntry>, usize) {
        if entries.len() <= 1 {
            return (entries, 0);
        }

        let mut result = Vec::new();
        let mut current = entries[0].clone();
        let mut run = 1usize; // 审核轮190：累积计数（原来硬编码 2，最多只能合并 2 条）
        let mut merge_count = 0;

        for entry in entries.into_iter().skip(1) {
            // 审核轮190：先按"原始内容"判断相似（current.content 可能已被加上 ×N 后缀，
            // 旧实现直接比较导致第 3 条起永远无法合并）
            if self.is_similar_raw(&current, &entry) {
                run += 1;
                current.content = format!("{}×{}", entry.content, run);
                current.timestamp = entry.timestamp;
                merge_count += 1;
            } else {
                result.push(current);
                current = entry;
                run = 1;
            }
        }
        result.push(current);

        (result, merge_count)
    }

    /// 审核轮190：剥离合并后缀 "×N" 后的原始内容
    fn raw_content(s: &str) -> &str {
        match s.rfind('×') {
            Some(i) if s[i + '×'.len_utf8()..].chars().all(|c| c.is_ascii_digit()) => &s[..i],
            _ => s,
        }
    }

    fn is_similar_raw(&self, a: &DanmakuEntry, b: &DanmakuEntry) -> bool {
        if Self::raw_content(&a.content) != Self::raw_content(&b.content) {
            return false;
        }
        if (b.timestamp - a.timestamp) > self.config.window_seconds || b.timestamp < a.timestamp {
            return false;
        }
        if a.mode != b.mode {
            return false;
        }
        true
    }

    #[allow(dead_code)]
    fn is_similar(&self, a: &DanmakuEntry, b: &DanmakuEntry) -> bool {
        if a.content != b.content {
            return false;
        }
        if (b.timestamp - a.timestamp) > self.config.window_seconds {
            return false;
        }
        if a.mode != b.mode {
            return false;
        }
        true
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_danmaku_merge() {
        let config = DanmakuMergeConfig::default();
        let merger = DanmakuMerger::new(config);

        let entries = vec![
            vec![
                DanmakuEntry {
                    id: 1,
                    mode: 1,
                    fontsize: 25,
                    color: 0xFFFFFF,
                    timestamp: 1.0,
                    pool: 0,
                    content: "test".to_string(),
                    creator_mid: None,
                    uid: 1,
                },
                DanmakuEntry {
                    id: 2,
                    mode: 1,
                    fontsize: 25,
                    color: 0xFFFFFF,
                    timestamp: 2.0,
                    pool: 0,
                    content: "test".to_string(),
                    creator_mid: None,
                    uid: 2,
                },
            ],
            vec![DanmakuEntry {
                id: 3,
                mode: 1,
                fontsize: 25,
                color: 0xFFFFFF,
                timestamp: 2.1,
                pool: 0,
                content: "test".to_string(),
                creator_mid: None,
                uid: 3,
            }],
        ];

        let result = merger.merge(entries);
        assert!(!result.entries.is_empty());
    }
}
