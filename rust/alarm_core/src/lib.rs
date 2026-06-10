mod alarm;
mod formatter;
mod time_calc;

// Re-export pure Rust API for use in tests / other Rust code
pub use alarm::validate_alarm_time;
pub use formatter::{format_time, get_repeat_days_label};
pub use time_calc::calculate_next_alarm_timestamp;

// ── JNI bindings ─────────────────────────────────────────────
// Class: com.example.rustyalarm.rust.RustAlarmCore
// Each `external fun` in the Kotlin object maps to one function here.

use jni::objects::JClass;
use jni::sys::{jboolean, jint, jintArray, jlong, jstring};
use jni::JNIEnv;

#[no_mangle]
pub extern "C" fn Java_com_example_rustyalarm_rust_RustAlarmCore_nativeValidateAlarmTime(
    _env: JNIEnv<'_>,
    _class: JClass<'_>,
    hour: jint,
    minute: jint,
) -> jboolean {
    alarm::validate_alarm_time(hour, minute) as jboolean
}

#[no_mangle]
pub extern "C" fn Java_com_example_rustyalarm_rust_RustAlarmCore_nativeFormatTime(
    env: JNIEnv<'_>,
    _class: JClass<'_>,
    hour: jint,
    minute: jint,
) -> jstring {
    let result = formatter::format_time(hour, minute);
    env.new_string(result)
        .expect("Failed to create Java string")
        .into_raw()
}

#[no_mangle]
pub extern "C" fn Java_com_example_rustyalarm_rust_RustAlarmCore_nativeCalculateNextAlarmTimestamp(
    env: JNIEnv<'_>,
    _class: JClass<'_>,
    current_timestamp_millis: jlong,
    hour: jint,
    minute: jint,
    repeat_days: jintArray,
) -> jlong {
    let arr = unsafe { jni::objects::JIntArray::from_raw(repeat_days) };
    let len = env.get_array_length(&arr).unwrap_or(0) as usize;
    let mut buf = vec![0i32; len];
    if len > 0 {
        let _ = env.get_int_array_region(&arr, 0, &mut buf);
    }
    time_calc::calculate_next_alarm_timestamp(current_timestamp_millis, hour, minute, buf)
}

#[no_mangle]
pub extern "C" fn Java_com_example_rustyalarm_rust_RustAlarmCore_nativeGetRepeatDaysLabel(
    env: JNIEnv<'_>,
    _class: JClass<'_>,
    repeat_days: jintArray,
) -> jstring {
    let arr = unsafe { jni::objects::JIntArray::from_raw(repeat_days) };
    let len = env.get_array_length(&arr).unwrap_or(0) as usize;
    let mut buf = vec![0i32; len];
    if len > 0 {
        let _ = env.get_int_array_region(&arr, 0, &mut buf);
    }
    let result = formatter::get_repeat_days_label(buf);
    env.new_string(result)
        .expect("Failed to create Java string")
        .into_raw()
}
