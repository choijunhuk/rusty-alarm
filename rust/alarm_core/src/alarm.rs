/// Returns true if hour and minute are in valid range.
pub fn validate_alarm_time(hour: i32, minute: i32) -> bool {
    (0..=23).contains(&hour) && (0..=59).contains(&minute)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn valid_times() {
        assert!(validate_alarm_time(0, 0));
        assert!(validate_alarm_time(23, 59));
        assert!(validate_alarm_time(12, 30));
        assert!(validate_alarm_time(8, 0));
    }

    #[test]
    fn invalid_times() {
        assert!(!validate_alarm_time(24, 0));
        assert!(!validate_alarm_time(-1, 0));
        assert!(!validate_alarm_time(12, 60));
        assert!(!validate_alarm_time(12, -1));
    }
}
