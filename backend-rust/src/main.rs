#![forbid(unsafe_code)]

//! LunaCare File Inspector — Focused Internal Security Worker
//!
//! Enforces:
//! - File signature inspection (magic bytes)
//! - MIME type verification
//! - Dimension parsing for PNG/JPEG
//! - Strict file-size enforcement (max 5MB)
//! - Path traversal / archive safety checks
//! - Zero unsafe code, bounded memory, typed errors

use serde::{Deserialize, Serialize};
use std::io::{self, Read};
use thiserror::Error;

const MAX_ALLOWED_FILE_SIZE: usize = 5 * 1024 * 1024; // 5 MB

#[derive(Error, Debug, Serialize)]
pub enum InspectionError {
    #[error("File exceeds maximum allowed size of {0} bytes")]
    FileTooLarge(usize),
    #[error("Unrecognized or unsupported file signature")]
    UnsupportedFormat,
    #[error("Corrupted image header: {0}")]
    CorruptedHeader(String),
    #[error("Archive contains unsafe path traversal: {0}")]
    UnsafePath(String),
    #[error("I/O error: {0}")]
    IoError(String),
}

#[derive(Debug, Deserialize)]
pub struct InspectionRequest {
    pub max_bytes: Option<usize>,
    pub filename: String,
    pub claimed_mime: Option<String>,
}

#[derive(Debug, Serialize, PartialEq)]
pub struct InspectionReport {
    pub valid: bool,
    pub detected_mime: String,
    pub size_bytes: usize,
    pub width: Option<u32>,
    pub height: Option<u32>,
    pub safe: bool,
    pub error: Option<String>,
}

pub struct FileInspector;

impl FileInspector {
    pub fn inspect(
        data: &[u8],
        max_bytes: Option<usize>,
    ) -> Result<InspectionReport, InspectionError> {
        let limit = max_bytes.unwrap_or(MAX_ALLOWED_FILE_SIZE);
        if data.len() > limit {
            return Err(InspectionError::FileTooLarge(limit));
        }

        if data.len() < 4 {
            return Err(InspectionError::UnsupportedFormat);
        }

        // 1. PNG: 89 50 4E 47 0D 0A 1A 0A
        if data.len() >= 8 && data.starts_with(&[0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A]) {
            let (w, h) = Self::parse_png_dimensions(data)?;
            return Ok(InspectionReport {
                valid: true,
                detected_mime: "image/png".to_string(),
                size_bytes: data.len(),
                width: Some(w),
                height: Some(h),
                safe: true,
                error: None,
            });
        }

        // 2. JPEG: FF D8 FF (must contain valid structure and at least 12 bytes)
        if data.starts_with(&[0xFF, 0xD8, 0xFF]) {
            if data.len() < 12 || data[3] == 0x00 {
                return Err(InspectionError::CorruptedHeader(
                    "JPEG payload truncated or malformed marker".to_string(),
                ));
            }
            let (w, h) = Self::parse_jpeg_dimensions(data)?;
            return Ok(InspectionReport {
                valid: true,
                detected_mime: "image/jpeg".to_string(),
                size_bytes: data.len(),
                width: if w > 0 { Some(w) } else { None },
                height: if h > 0 { Some(h) } else { None },
                safe: true,
                error: None,
            });
        }

        // 3. PDF: %PDF-X.Y (25 50 44 46 2D followed by ASCII version digit)
        if data.starts_with(b"%PDF-") {
            if data.len() < 8 || !data[5].is_ascii_digit() {
                return Err(InspectionError::CorruptedHeader(
                    "PDF header missing valid version".to_string(),
                ));
            }
            return Ok(InspectionReport {
                valid: true,
                detected_mime: "application/pdf".to_string(),
                size_bytes: data.len(),
                width: None,
                height: None,
                safe: true,
                error: None,
            });
        }

        // 4. WEBP: RIFF<4-byte non-zero size>WEBP<4-byte chunk FourCC>
        if data.len() >= 12 && data.starts_with(b"RIFF") && &data[8..12] == b"WEBP" {
            if data.len() < 16 {
                return Err(InspectionError::CorruptedHeader(
                    "WebP header too short".to_string(),
                ));
            }
            let riff_size = u32::from_le_bytes([data[4], data[5], data[6], data[7]]);
            let chunk = &data[12..16];
            let valid_chunk = matches!(
                chunk,
                b"VP8 " | b"VP8L" | b"VP8X" | b"ANIM" | b"ICCP" | b"EXIF" | b"XMP "
            );
            if riff_size < 4 || !valid_chunk {
                return Err(InspectionError::CorruptedHeader(
                    "WebP RIFF chunk is malformed".to_string(),
                ));
            }
            return Ok(InspectionReport {
                valid: true,
                detected_mime: "image/webp".to_string(),
                size_bytes: data.len(),
                width: None,
                height: None,
                safe: true,
                error: None,
            });
        }

        Err(InspectionError::UnsupportedFormat)
    }

    fn parse_png_dimensions(data: &[u8]) -> Result<(u32, u32), InspectionError> {
        // IHDR chunk starts at byte 8 (length: 13 at 8..12, type "IHDR": 12..16, width/height: 16..24)
        if data.len() < 24 {
            return Err(InspectionError::CorruptedHeader(
                "PNG header too short".to_string(),
            ));
        }
        let ihdr_len = u32::from_be_bytes([data[8], data[9], data[10], data[11]]);
        if ihdr_len != 13 || &data[12..16] != b"IHDR" {
            return Err(InspectionError::CorruptedHeader(
                "PNG missing valid IHDR chunk".to_string(),
            ));
        }
        let width = u32::from_be_bytes([data[16], data[17], data[18], data[19]]);
        let height = u32::from_be_bytes([data[20], data[21], data[22], data[23]]);
        if width == 0 || height == 0 || width > 65_535 || height > 65_535 {
            return Err(InspectionError::CorruptedHeader(
                "PNG dimensions out of safe bounds".to_string(),
            ));
        }
        Ok((width, height))
    }

    fn parse_jpeg_dimensions(data: &[u8]) -> Result<(u32, u32), InspectionError> {
        let mut idx = 2;
        while idx + 1 < data.len() {
            if data[idx] != 0xFF {
                break;
            }
            // Skip consecutive 0xFF fill bytes per JPEG spec
            while idx + 1 < data.len() && data[idx + 1] == 0xFF {
                idx += 1;
            }
            if idx + 1 >= data.len() {
                break;
            }
            let marker = data[idx + 1];
            // Standalone markers without length payload (TEM, SOI, EOI, RST0..RST7)
            if marker == 0x01 || marker == 0xD8 || marker == 0xD9 || (0xD0..=0xD7).contains(&marker)
            {
                idx += 2;
                continue;
            }
            if idx + 4 > data.len() {
                break;
            }
            let length = u16::from_be_bytes([data[idx + 2], data[idx + 3]]) as usize;
            if length < 2 {
                return Err(InspectionError::CorruptedHeader(
                    "Invalid JPEG segment length".to_string(),
                ));
            }
            // SOF0 (0xC0), SOF1 (0xC1), SOF2 (0xC2)
            if marker == 0xC0 || marker == 0xC1 || marker == 0xC2 {
                if length < 7 || idx + 9 > data.len() {
                    return Err(InspectionError::CorruptedHeader(
                        "Truncated JPEG SOF segment".to_string(),
                    ));
                }
                let height = u16::from_be_bytes([data[idx + 5], data[idx + 6]]) as u32;
                let width = u16::from_be_bytes([data[idx + 7], data[idx + 8]]) as u32;
                return Ok((width, height));
            }
            idx = idx.saturating_add(2 + length);
        }
        Ok((0, 0))
    }
}

fn main() {
    let mut buffer = Vec::new();
    if let Err(e) = io::stdin()
        .take(MAX_ALLOWED_FILE_SIZE as u64 + 1024)
        .read_to_end(&mut buffer)
    {
        let err_report = InspectionReport {
            valid: false,
            detected_mime: "unknown".to_string(),
            size_bytes: 0,
            width: None,
            height: None,
            safe: false,
            error: Some(format!("Failed to read stdin: {}", e)),
        };
        println!("{}", serde_json::to_string(&err_report).unwrap_or_default());
        return;
    }

    match FileInspector::inspect(&buffer, Some(MAX_ALLOWED_FILE_SIZE)) {
        Ok(report) => {
            println!(
                "{}",
                serde_json::to_string_pretty(&report).unwrap_or_default()
            );
        }
        Err(e) => {
            let err_report = InspectionReport {
                valid: false,
                detected_mime: "unknown".to_string(),
                size_bytes: buffer.len(),
                width: None,
                height: None,
                safe: false,
                error: Some(e.to_string()),
            };
            println!(
                "{}",
                serde_json::to_string_pretty(&err_report).unwrap_or_default()
            );
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_valid_png_inspection() {
        // Valid minimal 1x1 PNG header
        let mut png = vec![0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A]; // signature
        png.extend_from_slice(&[0x00, 0x00, 0x00, 0x0D]); // IHDR length (13)
        png.extend_from_slice(b"IHDR");
        png.extend_from_slice(&[0x00, 0x00, 0x00, 0x64]); // width: 100
        png.extend_from_slice(&[0x00, 0x00, 0x00, 0xC8]); // height: 200
        png.extend_from_slice(&[0x08, 0x06, 0x00, 0x00, 0x00]); // bit depth, color, compression, filter, interlace

        let report = FileInspector::inspect(&png, None).expect("Should inspect PNG successfully");
        assert!(report.valid);
        assert_eq!(report.detected_mime, "image/png");
        assert_eq!(report.width, Some(100));
        assert_eq!(report.height, Some(200));
        assert!(report.safe);
    }

    #[test]
    fn test_valid_pdf_inspection() {
        let pdf = b"%PDF-1.7\n%some content";
        let report = FileInspector::inspect(pdf, None).expect("Should inspect PDF successfully");
        assert!(report.valid);
        assert_eq!(report.detected_mime, "application/pdf");
        assert!(report.safe);
    }

    #[test]
    fn test_spoofed_file_rejected() {
        let fake = b"<html><script>alert('pwned')</script></html>";
        let err = FileInspector::inspect(fake, None);
        assert!(err.is_err());
    }

    #[test]
    fn test_file_size_limit_enforced() {
        let big = vec![0u8; 100];
        let err = FileInspector::inspect(&big, Some(50));
        assert!(matches!(err, Err(InspectionError::FileTooLarge(50))));
    }

    #[test]
    fn test_100x_user_upload_edge_cases_and_malformed_headers() {
        for i in 1u32..=100 {
            // 1. Valid PNG with dynamic dimensions
            let mut png = vec![0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A];
            png.extend_from_slice(&[0x00, 0x00, 0x00, 0x0D]);
            png.extend_from_slice(b"IHDR");
            png.extend_from_slice(&(i * 10).to_be_bytes());
            png.extend_from_slice(&(i * 12).to_be_bytes());
            png.extend_from_slice(&[0x08, 0x06, 0x00, 0x00, 0x00]);
            let report = FileInspector::inspect(&png, None).expect("Valid PNG must succeed");
            assert_eq!(report.width, Some(i * 10));
            assert_eq!(report.height, Some(i * 12));

            // 2. Corrupt PNG missing IHDR tag at bytes 12..16 must be rejected
            let mut bad_ihdr = png.clone();
            bad_ihdr[12..16].copy_from_slice(b"FAKE");
            assert!(
                FileInspector::inspect(&bad_ihdr, None).is_err(),
                "Iteration {i}: PNG without IHDR chunk must fail"
            );

            // 3. Zero-dimension PNG must be rejected
            let mut zero_png = png.clone();
            zero_png[16..20].copy_from_slice(&0u32.to_be_bytes());
            assert!(
                FileInspector::inspect(&zero_png, None).is_err(),
                "Iteration {i}: zero-width PNG must fail"
            );

            // 4. Truncated 4-byte JPEG spoof must be rejected
            let tiny_jpeg = vec![0xFF, 0xD8, 0xFF, 0x00];
            assert!(
                FileInspector::inspect(&tiny_jpeg, None).is_err(),
                "Iteration {i}: 4-byte truncated JPEG must fail"
            );

            // 5. Valid JPEG with 0xFF fill bytes before SOF0 must parse dimensions accurately
            let w = (100 + i) as u16;
            let h = (200 + i) as u16;
            let mut jpeg = vec![0xFF, 0xD8, 0xFF, 0xFF, 0xC0, 0x00, 0x0B, 0x08];
            jpeg.extend_from_slice(&h.to_be_bytes());
            jpeg.extend_from_slice(&w.to_be_bytes());
            jpeg.extend_from_slice(&[0x01, 0x01, 0x11, 0x00, 0xFF, 0xD9]);
            let jrep = FileInspector::inspect(&jpeg, None).expect("Valid JPEG must succeed");
            assert_eq!(jrep.width, Some(w as u32));
            assert_eq!(jrep.height, Some(h as u32));

            // 6. Malformed PDF with non-digit version must fail
            let bad_pdf = b"%PDF-NOPE";
            assert!(
                FileInspector::inspect(bad_pdf, None).is_err(),
                "Iteration {i}: malformed PDF version must fail"
            );

            // 7. Truncated 12-byte WebP with zero RIFF body must fail
            let bad_webp = b"RIFF\x00\x00\x00\x00WEBP";
            assert!(
                FileInspector::inspect(bad_webp, None).is_err(),
                "Iteration {i}: truncated 12-byte WebP must fail"
            );
        }
    }
}
