//! Danmaku block-rule engine (批次L39)
//!
//! 承接 Kotlin 侧 DanmakuBlockViewModel 的关键词/正则/UID 三类屏蔽规则，
//! 下沉 CPU 密集匹配到 Rust：万条弹幕 × 数百规则时避免在主线程逐条 containsMatchIn。
//! 通过 JNI 暴露 filter(json) -> json，Kotlin 失败自动回退 DanmakuBlockViewModel.shouldBlock。

use serde::{Deserialize, Serialize};

/// 一条待过滤弹幕（与 Kotlin DanmakuEntry JSON 字段对齐，缺省兼容）
#[derive(Debug, Clone, Deserialize)]
pub struct FilterInput {
    #[serde(default)]
    pub content: String,
    #[serde(default)]
    pub uid: u64,
}

/// 屏蔽规则集（由 Kotlin 序列化 Room 中的规则传入）
#[derive(Debug, Clone, Deserialize, Serialize, Default)]
pub struct BlockRules {
    #[serde(default)]
    pub keywords: Vec<String>,
    #[serde(default)]
    pub regexes: Vec<String>, // 以字符串传入，Rust 侧编译；非法正则忽略
    #[serde(default)]
    pub uids: Vec<u64>,
}

impl BlockRules {
    /// 编译正则，非法项丢弃；构造可复用的匹配器
    pub fn compile(&self) -> CompiledRules {
        let regexes: Vec<regex::Regex> = self
            .regexes
            .iter()
            .filter_map(|r| regex::Regex::new(r).ok())
            .collect();
        CompiledRules {
            keywords: self.keywords.clone(),
            regexes,
            uids: self.uids.iter().copied().collect(),
        }
    }
}

/// 编译后的匹配器（Aho-Corasick 不可用时的多关键词 contains 也足够快）
pub struct CompiledRules {
    keywords: Vec<String>,
    regexes: Vec<regex::Regex>,
    uids: std::collections::HashSet<u64>,
}

impl CompiledRules {
    pub fn should_block(&self, content: &str, uid: u64) -> bool {
        if self.uids.contains(&uid) {
            return true;
        }
        // 关键词：先做精确 contains（B 站关键词屏蔽语义）
        for k in &self.keywords {
            if !k.is_empty() && content.contains(k.as_str()) {
                return true;
            }
        }
        for re in &self.regexes {
            if re.is_match(content) {
                return true;
            }
        }
        false
    }
}

/// 批量过滤结果：保留项下标 + 各类命中统计（供 UI 显示过滤了多少条）
#[derive(Debug, Serialize)]
pub struct FilterReport {
    pub kept_indices: Vec<usize>,
    pub blocked_total: usize,
    pub blocked_by_uid: usize,
    pub blocked_by_keyword: usize,
    pub blocked_by_regex: usize,
}

/// 过滤一批弹幕。返回应保留的下标（保持原顺序）。
pub fn filter_entries(entries: &[FilterInput], rules: &BlockRules) -> FilterReport {
    let compiled = rules.compile();
    let mut kept_indices = Vec::with_capacity(entries.len());
    let mut by_uid = 0usize;
    let mut by_kw = 0usize;
    let mut by_re = 0usize;

    for (i, e) in entries.iter().enumerate() {
        if compiled.uids.contains(&e.uid) {
            by_uid += 1;
            continue;
        }
        let mut blocked = false;
        for k in &compiled.keywords {
            if !k.is_empty() && e.content.contains(k.as_str()) {
                by_kw += 1;
                blocked = true;
                break;
            }
        }
        if !blocked {
            for re in &compiled.regexes {
                if re.is_match(&e.content) {
                    by_re += 1;
                    blocked = true;
                    break;
                }
            }
        }
        if !blocked {
            kept_indices.push(i);
        }
    }

    let blocked_total = by_uid + by_kw + by_re;
    FilterReport {
        kept_indices,
        blocked_total,
        blocked_by_uid: by_uid,
        blocked_by_keyword: by_kw,
        blocked_by_regex: by_re,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_block_by_uid() {
        let rules = BlockRules {
            uids: vec![12345],
            ..Default::default()
        };
        let entries = vec![
            FilterInput {
                content: "hello".into(),
                uid: 12345,
            },
            FilterInput {
                content: "hello".into(),
                uid: 1,
            },
        ];
        let r = filter_entries(&entries, &rules);
        assert_eq!(r.kept_indices, vec![1]);
        assert_eq!(r.blocked_by_uid, 1);
    }

    #[test]
    fn test_block_by_keyword() {
        let rules = BlockRules {
            keywords: vec!["广告".into(), "http".into()],
            ..Default::default()
        };
        let entries = vec![
            FilterInput {
                content: "点开广告链接".into(),
                uid: 1,
            },
            FilterInput {
                content: "正常弹幕".into(),
                uid: 2,
            },
            FilterInput {
                content: "看这个http://x".into(),
                uid: 3,
            },
        ];
        let r = filter_entries(&entries, &rules);
        assert_eq!(r.kept_indices, vec![1]);
        assert_eq!(r.blocked_by_keyword, 2);
    }

    #[test]
    fn test_block_by_regex() {
        let rules = BlockRules {
            regexes: vec!["\\d{4,}".into(), "[".into()], // 非法正则被忽略
            ..Default::default()
        };
        let entries = vec![
            FilterInput {
                content: "群号12345678".into(),
                uid: 1,
            },
            FilterInput {
                content: "abc".into(),
                uid: 2,
            },
        ];
        let r = filter_entries(&entries, &rules);
        assert_eq!(r.kept_indices, vec![1]);
        assert_eq!(r.blocked_by_regex, 1);
    }

    #[test]
    fn test_empty_rules_keeps_all() {
        let rules = BlockRules::default();
        let entries = vec![FilterInput {
            content: "x".into(),
            uid: 9,
        }];
        let r = filter_entries(&entries, &rules);
        assert_eq!(r.kept_indices, vec![0]);
        assert_eq!(r.blocked_total, 0);
    }
}
