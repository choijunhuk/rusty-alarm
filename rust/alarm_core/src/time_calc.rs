use chrono::{DateTime, Datelike, Duration, Local, LocalResult, TimeZone, Timelike};

/// Calculates the next alarm trigger timestamp (milliseconds since epoch).
///
/// - If `repeat_days` is empty: one-time alarm, returns next occurrence
///   (today if the time is still future, otherwise tomorrow).
/// - If `repeat_days` is non-empty: returns the earliest future slot among
///   the specified weekdays (0=Sun … 6=Sat).
pub fn calculate_next_alarm_timestamp(
    current_millis: i64,
    hour: i32,
    minute: i32,
    repeat_days: Vec<i32>,
) -> i64 {
    let now: DateTime<Local> = match Local.timestamp_millis_opt(current_millis) {
        LocalResult::Single(t) => t,
        _ => Local::now(),
    };

    // Candidate time: same day as `now`, at alarm h:m:00
    let candidate = now
        .with_hour(hour as u32)
        .and_then(|t| t.with_minute(minute as u32))
        .and_then(|t| t.with_second(0))
        .and_then(|t| t.with_nanosecond(0));

    let candidate = match candidate {
        Some(t) => t,
        None => return current_millis + 60_000, // fallback: 1 minute from now
    };

    if repeat_days.is_empty() {
        // One-time: today if future, else tomorrow
        return if candidate.timestamp_millis() > current_millis {
            candidate.timestamp_millis()
        } else {
            (candidate + Duration::days(1)).timestamp_millis()
        };
    }

    // Repeating: find minimum days ahead for any of the specified weekdays
    // chrono weekday: Mon=0..Sun=6 — convert to 0=Sun..6=Sat
    let chrono_weekday = now.weekday().num_days_from_sunday() as i32; // 0=Sun

    let min_days = repeat_days
        .iter()
        .map(|&target_day| {
            let diff = target_day - chrono_weekday;
            match diff {
                d if d > 0 => d,
                0 if candidate.timestamp_millis() > current_millis => 0,
                d => 7 + d, // wrap around the week (handles 0 same-day-past and negative)
            }
        })
        .min()
        .unwrap_or(1);

    (candidate + Duration::days(min_days as i64)).timestamp_millis()
}

#[cfg(test)]
mod tests {
    use super::*;
    use chrono::TimeZone;

    fn ts(year: i32, month: u32, day: u32, h: u32, m: u32) -> i64 {
        chrono::Local
            .with_ymd_and_hms(year, month, day, h, m, 0)
            .unwrap()
            .timestamp_millis()
    }

    #[test]
    fn one_time_future_today() {
        // Current: 08:00, alarm: 09:00 → same day
        let now = ts(2024, 1, 15, 8, 0); // Monday
        let result = calculate_next_alarm_timestamp(now, 9, 0, vec![]);
        let expected = ts(2024, 1, 15, 9, 0);
        assert_eq!(result, expected);
    }

    #[test]
    fn one_time_past_today_schedules_tomorrow() {
        // Current: 10:00, alarm: 08:00 → next day
        let now = ts(2024, 1, 15, 10, 0);
        let result = calculate_next_alarm_timestamp(now, 8, 0, vec![]);
        let expected = ts(2024, 1, 16, 8, 0);
        assert_eq!(result, expected);
    }

    #[test]
    fn repeat_next_weekday() {
        // Current: Monday 2024-01-15 08:00
        // Alarm: 07:00 on Wednesday (3) and Friday (5)
        // Wednesday is 2 days ahead, Friday is 4 days ahead → Wednesday wins
        let now = ts(2024, 1, 15, 8, 0); // Monday = weekday 1 (0=Sun)
        let result = calculate_next_alarm_timestamp(now, 7, 0, vec![3, 5]);
        let expected = ts(2024, 1, 17, 7, 0); // Wednesday
        assert_eq!(result, expected);
    }

    #[test]
    fn repeat_same_day_future() {
        // Current: Monday 08:00, alarm 09:00 on Monday (1) → today
        let now = ts(2024, 1, 15, 8, 0);
        let result = calculate_next_alarm_timestamp(now, 9, 0, vec![1]);
        let expected = ts(2024, 1, 15, 9, 0);
        assert_eq!(result, expected);
    }

    #[test]
    fn repeat_same_day_past_wraps_to_next_week() {
        // Current: Monday 10:00, alarm 09:00 on Monday (1) → next Monday
        let now = ts(2024, 1, 15, 10, 0);
        let result = calculate_next_alarm_timestamp(now, 9, 0, vec![1]);
        let expected = ts(2024, 1, 22, 9, 0);
        assert_eq!(result, expected);
    }

    #[test]
    fn result_is_always_in_the_future() {
        let now = ts(2024, 6, 10, 12, 0);
        for h in 0..24_i32 {
            for day in 0..7_i32 {
                let result = calculate_next_alarm_timestamp(now, h, 0, vec![day]);
                assert!(
                    result > now,
                    "Expected future timestamp for hour={h}, day={day}"
                );
            }
        }
    }
}
