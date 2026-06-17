# Wakeup Success Loop Design

## Goal

Improve Rusty Alarm's daily-use value by connecting alarm readiness, alarm editing, and weekly report recommendations into one wakeup success loop. Release and portfolio automation work is intentionally out of scope.

## User Experience

The Android main alarm screen should make the next alarm's reliability understandable and actionable. When the next alarm or device state has risks, the hero area shows the most important issue and exposes a compact readiness action panel. Permission issues open the correct Android settings screen. Alarm configuration issues open the next alarm for editing. If there is no enabled alarm, the action creates a short test alarm path through existing quick-alarm behavior.

The Android alarm editor should preview the current alarm's success profile before saving. Users should see whether the current draft is good, needs caution, or is risky, and should be able to apply a recommended wakeup preset from the same area. Presets must describe the concrete changes they make, such as snooze limits, challenge gates, volume, gradual wakeup, and pre-alarm reminders.

The weekly report should explain recommendations as a cause-and-effect action. Each insight should include the observed reason, the suggested preset, and the expected change to the next enabled alarm. Applying a recommendation should remain the existing `applyPresetToNextEnabled()` action so report changes and main-screen readiness improve through the same domain logic.

## Architecture

Keep the domain logic pure and testable. Extend `AlarmReliability` with action categories and draft-level guidance, extend `ReportInsightEngine` with reason/result copy, and keep preset mutation in `WakeupPresetApplier`.

UI should consume these domain objects instead of recomputing readiness rules. Android is the implementation target for this pass because it already has permission intents and tests around the relevant domain logic. iOS release entitlement work, TestFlight, and build pipeline changes are excluded.

## Components

- `AlarmReliability`: classifies readiness issues, maps them to user actions, and ranks which issues should be shown first.
- `WakeupPresetApplier`: remains the single mutation point for comfortable, on-time, and forced wakeup presets.
- `ReportInsightEngine`: emits recommendation title, detail, action label, cause, and expected result copy.
- `AlarmListScreen`: displays a readiness action panel and routes permission/configuration/test-alarm actions.
- `AlarmEditScreen`: displays draft readiness preview and preset impact copy near save.
- Unit tests: lock domain behavior before UI wiring.

## Error Handling

Settings intents should use `runCatching` at the call site so unsupported device settings do not crash the app. Missing enabled alarms should show a clear "create test alarm" action rather than applying report presets to nothing. Report actions keep the existing no-enabled-alarm message.

## Testing

Add unit tests for readiness action classification and report insight action copy before implementation. Then run Android unit tests for `AlarmReliabilityTest`, `ReportInsightEngineTest`, and `WakeupPresetApplierTest`. Finish with Android lint/build if the code compiles far enough in the current local environment.
