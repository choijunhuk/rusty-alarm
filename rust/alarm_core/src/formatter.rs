/// Formats hour and minute as "HH:MM" (24-hour, zero-padded).
pub fn format_time(hour: i32, minute: i32) -> String {
    format!("{:02}:{:02}", hour, minute)
}

/// Returns a human-readable label for repeat days.
/// Empty → "일회성", otherwise sorted day names joined by space.
pub fn get_repeat_days_label(mut repeat_days: Vec<i32>) -> String {
    if repeat_days.is_empty() {
        return "일회성".to_string();
    }
    repeat_days.sort_unstable();
    repeat_days.dedup();

    let names = ["일", "월", "화", "수", "목", "금", "토"];
    repeat_days
        .iter()
        .filter_map(|&d| names.get(d as usize).copied())
        .collect::<Vec<_>>()
        .join(" ")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn format_midnight() {
        assert_eq!(format_time(0, 0), "00:00");
    }

    #[test]
    fn format_noon() {
        assert_eq!(format_time(12, 30), "12:30");
    }

    #[test]
    fn format_single_digit() {
        assert_eq!(format_time(8, 5), "08:05");
    }

    #[test]
    fn repeat_label_empty() {
        assert_eq!(get_repeat_days_label(vec![]), "일회성");
    }

    #[test]
    fn repeat_label_weekdays() {
        assert_eq!(get_repeat_days_label(vec![1, 2, 3, 4, 5]), "월 화 수 목 금");
    }

    #[test]
    fn repeat_label_weekend() {
        assert_eq!(get_repeat_days_label(vec![0, 6]), "일 토");
    }

    #[test]
    fn repeat_label_dedup_and_sort() {
        assert_eq!(get_repeat_days_label(vec![3, 1, 1, 3]), "월 수");
    }
}
