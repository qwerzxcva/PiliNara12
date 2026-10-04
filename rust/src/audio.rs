//! Audio normalization implementation
//!
//! Simplified audio normalization for stream processing

/// Configuration for audio normalization
#[derive(Debug, Clone)]
pub struct AudioNormalizationConfig {
    pub target_level: f32,
    pub max_gain: f32,
    pub look_ahead_ms: u32,
}

impl Default for AudioNormalizationConfig {
    fn default() -> Self {
        Self {
            target_level: -23.0, // LUFS
            max_gain: 12.0,
            look_ahead_ms: 200,
        }
    }
}

/// Audio normalizer
pub struct AudioNormalizer {
    config: AudioNormalizationConfig,
}

impl AudioNormalizer {
    pub fn new(config: AudioNormalizationConfig) -> Self {
        Self { config }
    }

    /// Normalize i16 PCM audio data
    pub fn normalize_i16(&self, samples: &[i16], channels: usize) -> Vec<i16> {
        if samples.is_empty() || channels == 0 {
            return samples.to_vec();
        }

        // Calculate RMS level
        let rms = self.calculate_rms(samples, channels);

        // Calculate gain factor
        let target_rms = self.rms_for_level(-23.0); // -23 LUFS
        let mut gain = target_rms / rms;

        // Apply max gain limit
        gain = gain.min(self.config.max_gain);

        // Apply gain
        samples
            .iter()
            .map(|&s| (s as f32 * gain).clamp(-32768.0, 32767.0) as i16)
            .collect()
    }

    fn calculate_rms(&self, samples: &[i16], _channels: usize) -> f32 {
        if samples.is_empty() {
            return 0.0;
        }

        let sum_sq: f32 = samples.iter().map(|&s| (s as f32).powi(2)).sum();

        (sum_sq / samples.len() as f32).sqrt()
    }

    fn rms_for_level(&self, level_db: f32) -> f32 {
        // Convert LUFS to RMS
        // Reference: -23 LUFS ≈ 0.0708 RMS
        10.0f32.powf(level_db / 20.0) * 0.707
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_normalize_mono() {
        let config = AudioNormalizationConfig::default();
        let normalizer = AudioNormalizer::new(config);

        let input = vec![1000i16; 100];
        let output = normalizer.normalize_i16(&input, 1);

        assert_eq!(output.len(), input.len());
        assert!(!output.is_empty());
    }

    #[test]
    fn test_normalize_stereo() {
        let config = AudioNormalizationConfig::default();
        let normalizer = AudioNormalizer::new(config);

        let input = vec![1000i16; 200]; // 100 samples * 2 channels
        let output = normalizer.normalize_i16(&input, 2);

        assert_eq!(output.len(), input.len());
    }
}
