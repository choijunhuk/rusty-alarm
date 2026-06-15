# Rusty Alarm Wakeup Journey Design

## Summary

Rusty Alarm will improve as one connected wakeup journey rather than as a collection of independent features.

The journey is:

1. The user chooses a wakeup mode.
2. The app configures a sensible alarm preset.
3. The app checks whether the next alarm is operationally ready.
4. The alarm rings with platform-appropriate controls.
5. The app records the outcome.
6. The report recommends one concrete improvement that the user can apply immediately.

Core behavior remains consistent across Android, iOS, Wear OS, and Apple Watch. Platform-specific capabilities may differ when operating-system constraints or device form factors make identical behavior inappropriate.

## Product Goals

- Help users choose an alarm setup without understanding every individual option.
- Make alarm readiness visible before bedtime, not after a missed alarm.
- Turn wakeup history into a small, actionable feedback loop.
- Give watches a useful wakeup role without moving complex alarm editing onto a small screen.
- Preserve a recognizable Rusty Alarm experience across platforms while respecting platform constraints.

## Non-Goals

- A server-backed recommendation engine.
- Social, family, or remote wakeup features.
- Full feature parity across every platform.
- Replacing platform-native alarm scheduling with a cross-platform runtime.
- New challenge types during this improvement cycle.

## Target Users And Modes

Users choose one of three modes when creating or improving an alarm. They can customize the resulting alarm afterward.

### Comfortable Wakeup

Designed for users who prioritize a gentler transition out of sleep.

Default policy:

- Gradual wakeup enabled.
- Pre-alarm enabled.
- Moderate alarm volume.
- Two or three snoozes allowed.
- No mandatory challenge.
- Watch haptics may provide the first wakeup signal when available.

### On-Time Wakeup

Designed for students and workers who must reliably meet a morning schedule.

Default policy:

- Strong alarm volume.
- One snooze allowed.
- A low-friction challenge or short routine enabled.
- Readiness checks emphasized before the next alarm.
- Calendar-based time suggestions may be offered where the platform supports calendar access.

### Forced Wakeup

Designed for users who repeatedly dismiss alarms without fully waking.

Default policy:

- Strong alarm volume.
- Snooze disabled or limited to one.
- A physical or location-based challenge enabled.
- A short post-dismiss routine required.
- Watch controls do not bypass the configured challenge.

## Shared Core Contract

The platforms do not need to share one binary implementation, but they must share the same product meanings.

### Shared Inputs

- Selected wakeup mode.
- Alarm sound, vibration, volume, and gradual-wakeup settings.
- Snooze limit.
- Challenge and routine configuration.
- Alarm permission and readiness state.
- Fired, snoozed, dismissed, response-time, and challenge-completion events.

### Shared Outputs

- A mode-derived preset that remains user-editable.
- A readiness result with a score, severity, and concrete issues.
- A report containing wakeup outcomes and ranked recommendations.
- A recommendation action that can apply a supported setting change.

### Consistency Strategy

Each platform keeps native scheduling and UI code. Shared behavior is maintained with equivalent policy names and common test vectors covering representative inputs and expected outputs.

The existing Android `WakeupPresetApplier`, `AlarmReliability`, and `ReportInsightEngine`, plus the iOS `WakeupCoaching` model, are the starting points for this contract.

## Experience Design

### 1. Mode Selection

Mode selection appears during alarm creation and as an improvement action for existing alarms.

Each mode card explains:

- Who it is for.
- Which important settings it changes.
- Whether snooze or challenges are involved.

Applying a mode updates the alarm draft, then shows a short change summary before saving. Users can customize every derived setting afterward.

### 2. Readiness Check

The next-alarm surface displays one readiness state:

- Ready: no blocking issue and the alarm is reasonably configured.
- Caution: the alarm can ring, but one or more settings increase risk.
- Blocked: permissions or system settings may prevent reliable ringing.

The readiness surface prioritizes at most three issues and links each actionable issue to the relevant platform setting or in-app alarm setting.

Android evaluates exact-alarm permission, notification permission, battery restrictions, and alarm configuration. iOS evaluates notification authorization, available alarm capabilities, and alarm configuration. Watches display the phone-derived readiness status rather than attempting to reproduce phone permission diagnostics.

### 3. Ring And Watch Controls

Phones remain responsible for alarm scheduling, challenge execution, and final outcome recording.

Wear OS and Apple Watch provide:

- Next alarm status.
- Readiness summary.
- Snooze when the active mode and remaining snooze allowance permit it.
- Dismiss only when no challenge or required routine would be bypassed.

When a challenge is required, the watch directs the user to complete it on the phone. A watch command must be idempotent so repeated delivery cannot create duplicate snooze or dismiss events.

### 4. Outcome Recording

The event model records enough context to explain outcomes rather than only count them.

Required outcome dimensions:

- Alarm fired.
- Alarm dismissed.
- Alarm snoozed.
- Response time.
- Challenge type and completion.
- Wakeup mode active at firing time.
- Readiness severity active before firing.

Local event history remains the source of truth. No account or server is required.

### 5. Actionable Report

The report ranks a small number of recommendations and presents one primary next action.

Examples:

- High snooze rate: apply On-Time Wakeup or reduce snooze allowance.
- Slow response time: enable pre-alarm or gradual wakeup.
- No challenge completions with frequent snoozes: apply a low-friction challenge.
- Blocked readiness: open the missing permission or system setting.

Every recommendation must be one of:

- Directly applicable to the selected alarm.
- A deep link to a relevant system setting.
- Informational when the platform cannot safely automate the change.

The app shows a preview of setting changes before applying a mode or multi-setting recommendation.

## Platform Responsibilities

### Android

- Primary reference implementation for shared policies.
- Exact-alarm, notification, battery-optimization, and full-screen intent readiness.
- Full mode application, challenge execution, actionable reports, and calendar suggestions.
- Wear OS command handling through the existing Wearable Data Layer.

### iOS

- Native notification and AlarmKit scheduling when available.
- Equivalent mode, readiness, and report policy behavior.
- Critical Alerts remain an optional capability that depends on Apple approval.
- Calendar suggestions use EventKit only after explicit user permission.
- Apple Watch communication uses WatchConnectivity.

### Wear OS

- Displays next alarm and phone-derived readiness.
- Provides permitted snooze and dismiss commands.
- Never bypasses required phone challenges or routines.
- Surfaces command delivery failure and lets the user retry.

### Apple Watch

- Mirrors the Wear OS role using native SwiftUI and WatchConnectivity.
- Provides permitted snooze and dismiss commands.
- Never bypasses required phone challenges or routines.

## Architecture And Boundaries

### Policy Layer

Pure, testable policy code decides:

- Mode-derived alarm changes.
- Readiness severity and ranked issues.
- Report recommendations and their supported actions.
- Whether a watch command is permitted.

Policy code must not open system settings, schedule alarms, or mutate storage directly.

### Platform Adapter Layer

Platform adapters:

- Collect current permission and capability state.
- Apply approved changes to the alarm draft or saved alarm.
- Open system settings.
- Send and receive watch commands.
- Schedule and reschedule alarms.

### Presentation Layer

UI surfaces render policy outputs and collect explicit user confirmation before multi-setting changes. They do not duplicate policy decisions.

## Error Handling

- If a recommendation cannot be applied, preserve the current alarm and explain the failed setting.
- If only part of a platform action succeeds, report the remaining issue and recompute readiness.
- If a watch command times out, show that the phone did not confirm it and allow retry.
- If calendar permission is denied, keep manual mode selection fully usable.
- If event context is unavailable for older records, reports continue using existing metrics without fabricating values.
- Import, migration, or policy-version changes must preserve existing alarms and default missing fields conservatively.

## Privacy And Safety

- All wakeup history and recommendations remain local.
- Calendar data is used only to suggest wakeup times and is not persisted beyond what is necessary for the chosen alarm.
- Watch commands cannot weaken a configured challenge or routine.
- Readiness wording distinguishes a risk estimate from a guarantee that an operating system will deliver an alarm.

## Testing Strategy

### Shared Policy Vectors

Maintain matching scenarios across Android and iOS for:

- Each mode's derived settings.
- Ready, caution, and blocked diagnostics.
- Recommendation ranking.
- Watch command permission rules.

### Android

- Unit tests for policy behavior and legacy alarm defaults.
- Room migration tests for new event context fields.
- Integration tests for recommendation actions and rescheduling.
- Compose tests for mode preview, readiness actions, and report actions.
- Wear Data Layer tests for idempotent command handling.

### iOS

- Unit tests for equivalent coaching policy behavior.
- Persistence migration tests.
- UI tests for mode preview, readiness actions, and report actions.
- WatchConnectivity tests with duplicated and failed commands.

### End-To-End Scenarios

- Create an alarm using each mode and verify the resulting settings.
- Resolve a blocking readiness issue and verify the status is recomputed.
- Fire, snooze, and dismiss an alarm, then verify the report action.
- Send a permitted watch snooze and confirm exactly one recorded event.
- Attempt watch dismiss with a required challenge and verify it is rejected.

## Delivery Sequence

1. Define and test the shared policy contract and event context.
2. Connect Android mode, readiness, and actionable report flows.
3. Match the policy behavior and flows on iOS.
4. Add safe Wear OS and Apple Watch command handling.
5. Add optional calendar-based suggestions.
6. Run cross-platform scenario verification and update user-facing documentation.

Each stage must leave the affected platform in a working, testable state.

## Success Criteria

- A new user can select a suitable mode and understand its important effects.
- The next alarm clearly communicates ready, caution, or blocked status.
- A report recommendation can change a relevant alarm setting without manual navigation.
- Watches can perform permitted actions without bypassing phone safety rules.
- Android and iOS produce equivalent policy outcomes for the shared test vectors.
- Existing alarms continue to load, schedule, and ring after migration.

## Remaining Risks

- iOS alarm delivery and Critical Alerts remain constrained by Apple capabilities and approval.
- Calendar-based suggestions introduce permission and time-zone edge cases.
- Watch command delivery can be delayed or unavailable when the phone is disconnected.
- Existing large UI files increase change risk and may require small responsibility-focused extractions during implementation.
