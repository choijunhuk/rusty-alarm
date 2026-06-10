/// Analyzes raw accelerometer magnitude samples to estimate wakefulness.
///
/// The returned score is dimensionless: higher = more movement = more likely
/// to be in light sleep / awake. Threshold-based decision is made by the
/// caller (typical: `score > 0.8` triggers smart-wake).
///
/// Algorithm:
/// 1. Compute the mean of the window
/// 2. Compute the variance (mean squared deviation)
/// 3. Normalize using `score = sqrt(variance) / (1.0 + sqrt(variance))`
///    → maps [0, ∞) to [0, 1) so the threshold is intuitive
pub fn analyze_sleep_window(samples: Vec<f32>) -> f32 {
    if samples.len() < 2 {
        return 0.0;
    }
    let n = samples.len() as f32;
    let mean: f32 = samples.iter().sum::<f32>() / n;
    let variance: f32 = samples
        .iter()
        .map(|x| {
            let d = x - mean;
            d * d
        })
        .sum::<f32>()
        / n;
    let std_dev = variance.sqrt();
    // Squash to [0, 1)
    std_dev / (1.0 + std_dev)
}

/// Convenience: decide whether to wake the user given a raw window.
pub fn should_wake_now(samples: Vec<f32>, threshold: f32) -> bool {
    analyze_sleep_window(samples) >= threshold
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn empty_window_returns_zero() {
        assert_eq!(analyze_sleep_window(vec![]), 0.0);
        assert_eq!(analyze_sleep_window(vec![9.81]), 0.0);
    }

    #[test]
    fn still_phone_low_score() {
        // Phone resting flat: magnitudes hover around gravity with tiny noise
        let samples: Vec<f32> = (0..100)
            .map(|i| 9.81 + (i as f32 * 0.0001).sin() * 0.01)
            .collect();
        let score = analyze_sleep_window(samples);
        assert!(score < 0.05, "still phone should score low, got {score}");
    }

    #[test]
    fn moving_phone_high_score() {
        // Simulated tossing/turning: large swings
        let samples: Vec<f32> = (0..100)
            .map(|i| 9.81 + ((i as f32 * 0.3).sin() * 5.0))
            .collect();
        let score = analyze_sleep_window(samples);
        assert!(score > 0.5, "moving phone should score high, got {score}");
    }

    #[test]
    fn score_is_bounded_in_zero_one() {
        // Even with very large input, score stays < 1
        let samples: Vec<f32> = (0..100).map(|i| (i as f32 * 100.0)).collect();
        let score = analyze_sleep_window(samples);
        assert!(score >= 0.0 && score < 1.0, "score out of bounds: {score}");
    }

    #[test]
    fn should_wake_uses_threshold() {
        let still: Vec<f32> = vec![9.81; 100];
        let moving: Vec<f32> = (0..100).map(|i| 9.81 + (i as f32).sin() * 3.0).collect();
        assert!(!should_wake_now(still, 0.5));
        assert!(should_wake_now(moving, 0.5));
    }
}
