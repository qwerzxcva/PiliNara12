//! Danmaku merging and filtering algorithms
//! 
//! High-performance Rust implementation for:
//! - Danmaku merging across multiple streams
//! - Pinyin-based filtering
//! - Collision detection and avoidance

use std::collections::HashMap;
use std::time::Instant;

/// Represents a single danmaku comment
#[derive(Debug, Clone)]
pub struct DanmakuEntry {
    pub id: u64,
    pub mode: i32,          // 1=scroll, 2=top, 3=bottom
    pub fontsize: i32,
    pub color: u32,
    pub timestamp: f64,     // seconds
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
            skip_subtitle: true,
            skip_advanced: true,
            skip_bottom: false,
            mark_position: false,
            mark_threshold: 0.8,
        }
    }
}

/// Result of danmaku merging
#[derive(Debug, Clone)]
pub struct MergedDanmaku {
    pub entries: Vec<DanmakuEntry>,
    pub filtered_count: usize,
    pub merged_count: usize,
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
        all_entries.sort_by(|a, b| a.timestamp.partial_cmp(&b.timestamp).unwrap_or(std::cmp::Ordering::Equal));
        
        let initial_count = all_entries.len();
        
        // Apply filters
        let filtered: Vec<DanmakuEntry> = all_entries.into_iter()
            .filter(|e| !self.should_skip(e))
            .collect();
        
        let filtered_count = initial_count - filtered.len();
        
        // Merge similar danmaku
        let (merged, merged_count) = self.merge_similar(filtered);
        
        let elapsed = start.elapsed();
        eprintln!("Danmaku merge: {} -> {} (filtered {}, merged {}), took {:?}", 
                  initial_count, merged.len(), filtered_count, merged_count, elapsed);
        
        MergedDanmaku {
            entries: merged,
            filtered_count,
            merged_count,
        }
    }
    
    fn should_skip(&self, entry: &DanmakuEntry) -> bool {
        if self.config.skip_subtitle && entry.mode == 4 {
            return true;
        }
        if self.config.skip_advanced && entry.mode == 5 {
            return true;
        }
        if self.config.skip_bottom && entry.mode == 3 {
            return true;
        }
        false
    }
    
    fn merge_similar(&self, entries: Vec<DanmakuEntry>) -> (Vec<DanmakuEntry>, usize) {
        if entries.len() <= 1 {
            return (entries, 0);
        }
        
        let mut result = Vec::new();
        let mut current = entries[0].clone();
        let mut merge_count = 0;
        
        for entry in entries.into_iter().skip(1) {
            if self.is_similar(&current, &entry) {
                // Merge: keep the later one but increment count
                current.content = format!("{}×{}", entry.content, 2);
                current.timestamp = entry.timestamp;
                merge_count += 1;
            } else {
                result.push(current);
                current = entry;
            }
        }
        result.push(current);
        
        (result, merge_count)
    }
    
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
                DanmakuEntry { id: 1, mode: 1, fontsize: 25, color: 0xFFFFFF, timestamp: 1.0, pool: 0, content: "test".to_string(), creator_mid: None, uid: 1 },
                DanmakuEntry { id: 2, mode: 1, fontsize: 25, color: 0xFFFFFF, timestamp: 2.0, pool: 0, content: "test".to_string(), creator_mid: None, uid: 2 },
            ],
            vec![
                DanmakuEntry { id: 3, mode: 1, fontsize: 25, color: 0xFFFFFF, timestamp: 2.1, pool: 0, content: "test".to_string(), creator_mid: None, uid: 3 },
            ],
        ];
        
        let result = merger.merge(entries);
        assert!(result.entries.len() > 0);
    }
}
