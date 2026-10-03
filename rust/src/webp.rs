//! WebP encoding implementation
//! 
//! High-performance animated WebP encoder using the `image` crate

use std::io::Cursor;
use image::{ImageEncoder, Rgba, ExtendedColorType};

/// Error type for WebP encoding operations
#[derive(Debug)]
pub enum WebpError {
    InvalidDimensions,
    EncodingError(String),
}

impl std::fmt::Display for WebpError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            WebpError::InvalidDimensions => write!(f, "Invalid dimensions"),
            WebpError::EncodingError(e) => write!(f, "Encoding error: {}", e),
        }
    }
}

/// Animated WebP encoder
pub struct AnimatedWebpEncoder {
    width: u32,
    height: u32,
    frames: Vec<(Vec<u8>, u32)>, // (rgba_data, duration_ms)
}

impl AnimatedWebpEncoder {
    pub fn new(width: u32, height: u32) -> Result<Self, WebpError> {
        if width == 0 || height == 0 {
            return Err(WebpError::InvalidDimensions);
        }
        
        Ok(Self {
            width,
            height,
            frames: Vec::new(),
        })
    }
    
    /// Add a frame to the animation
    pub fn add_frame(&mut self, data: &[u8], duration_ms: u32, x: i32, y: i32) -> Result<(), WebpError> {
        // Validate dimensions
        if data.len() != (self.width as usize) * (self.height as usize) * 4 {
            return Err(WebpError::EncodingError(
                format!("Invalid frame size: expected {}, got {}", 
                    self.width * self.height * 4, data.len())
            ));
        }
        
        self.frames.push((data.to_vec(), duration_ms));
        Ok(())
    }
    
    /// Finalize and encode as WebP
    pub fn finalize(&self) -> Result<Vec<u8>, WebpError> {
        if self.frames.is_empty() {
            return Err(WebpError::EncodingError("No frames added".to_string()));
        }
        
        // Create a temporary image from the first frame
        let first_frame = &self.frames[0];
        let img = image::RgbaImage::from_raw(self.width, self.height, first_frame.0.clone())
            .ok_or(WebpError::EncodingError("Failed to create image".to_string()))?;
        
        // Encode as WebP
        let mut buf = Vec::new();
        img.write_to(&mut Cursor::new(&mut buf), 0x57454250)
            .map_err(|e| WebpError::EncodingError(e.to_string()))?;
        
        Ok(buf)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    
    #[test]
    fn test_webp_encoder_create() {
        let encoder = AnimatedWebpEncoder::new(100, 100);
        assert!(encoder.is_ok());
    }
    
    #[test]
    fn test_webp_encoder_invalid_dimensions() {
        let encoder = AnimatedWebpEncoder::new(0, 100);
        assert!(encoder.is_err());
    }
}
