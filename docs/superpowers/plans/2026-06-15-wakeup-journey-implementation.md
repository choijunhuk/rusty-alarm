# Wakeup Journey Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Connect wakeup modes, readiness, actionable coaching, and safe watch behavior across Android, iOS, Wear OS, and Apple Watch.

**Architecture:** Keep wakeup decisions in pure policy types and leave scheduling, persistence, and device communication in existing platform adapters. Android remains the reference implementation; iOS mirrors the same mode and recommendation meanings. Watches receive a compact readiness/control snapshot and never bypass phone challenges or routines.

**Tech Stack:** Kotlin, Jetpack Compose, Room, JUnit 4, Wearable Data Layer, Swift, SwiftUI, WatchConnectivity, XcodeGen

---

### Task 1: Define Android Wakeup Mode And Watch Policy

**Files:**
- Modify: `app/src/main/java/com/example/rustyalarm/alarm/WakeupPresetApplier.kt`
- Create: `app/src/main/java/com/example/rustyalarm/alarm/WatchControlPolicy.kt`
- Modify: `app/src/test/java/com/example/rustyalarm/alarm/WakeupPresetApplierTest.kt`
- Create: `app/src/test/java/com/example/rustyalarm/alarm/WatchControlPolicyTest.kt`

- [ ] Add failing tests that require the three user-facing modes to produce comfortable, on-time, and forced-wakeup settings.
- [ ] Run `./gradlew :app:testDebugUnitTest --tests '*WakeupPresetApplierTest'` and confirm the new assertions fail.
- [ ] Rename the policy-facing presets and implement the minimum setting changes needed by the tests.
- [ ] Add failing tests that reject watch dismiss when a challenge or routine exists and limit watch snooze by remaining allowance.
- [ ] Run `./gradlew :app:testDebugUnitTest --tests '*WatchControlPolicyTest'` and confirm the class is missing.
- [ ] Implement pure `WatchControlPolicy` decisions and rerun both test classes.

### Task 2: Make Android Report Recommendations Actionable

**Files:**
- Modify: `app/src/main/java/com/example/rustyalarm/viewmodel/ReportInsightEngine.kt`
- Modify: `app/src/main/java/com/example/rustyalarm/viewmodel/ReportViewModel.kt`
- Modify: `app/src/main/java/com/example/rustyalarm/ui/screens/ReportScreen.kt`
- Modify: `app/src/main/java/com/example/rustyalarm/ui/navigation/AppNavigation.kt`
- Modify: `app/src/test/java/com/example/rustyalarm/viewmodel/ReportInsightEngineTest.kt`

- [ ] Add failing tests for recommendation action types and mapped wakeup modes.
- [ ] Run `./gradlew :app:testDebugUnitTest --tests '*ReportInsightEngineTest'` and confirm the action assertions fail.
- [ ] Add a pure recommendation action model and map high snooze, no challenge, and slow response insights to safe actions.
- [ ] Pass alarm repository/navigation callbacks into the report screen.
- [ ] Add an apply button that previews the target mode and applies it to the next enabled alarm only after confirmation.
- [ ] Rerun focused tests and compile Android.

### Task 3: Surface Android Readiness And Mode Meaning

**Files:**
- Modify: `app/src/main/java/com/example/rustyalarm/ui/screens/AlarmEditScreen.kt`
- Modify: `app/src/main/java/com/example/rustyalarm/ui/screens/AlarmListScreen.kt`
- Modify: `app/src/main/java/com/example/rustyalarm/wear/WearSync.kt`
- Modify: `wear/src/main/java/com/example/rustyalarm/wear/AlarmDataListener.kt`
- Modify: `wear/src/main/java/com/example/rustyalarm/wear/NextAlarmStore.kt`
- Modify: `wear/src/main/java/com/example/rustyalarm/wear/MainActivity.kt`

- [ ] Replace technical preset wording with comfortable, on-time, and forced-wakeup mode wording.
- [ ] Push the next alarm's readiness label and safe watch-control flags through the existing Data Layer snapshot.
- [ ] Render readiness and permitted action guidance on Wear OS.
- [ ] Run `./gradlew :app:testDebugUnitTest :app:assembleDebug :wear:assembleDebug`.

### Task 4: Mirror Shared Policy And Actionable Coaching On iOS

**Files:**
- Modify: `ios/Sources/Models/WakeupCoaching.swift`
- Modify: `ios/Sources/Views/AlarmEditView.swift`
- Modify: `ios/Sources/Views/ReportView.swift`
- Modify: `ios/Sources/Persistence/AlarmStore.swift`

- [ ] Rename iOS presets to the same three user-facing modes and mirror Android policy values.
- [ ] Add recommendation action metadata equivalent to Android.
- [ ] Let the report preview and apply a recommended mode to the next enabled alarm.
- [ ] Keep all changes local and preserve existing Codable defaults.
- [ ] Generate the Xcode project and build the iOS app without signing.

### Task 5: Add Apple Watch Readiness And Safe-Control Guidance

**Files:**
- Modify: `ios/Sources/Persistence/WatchSync.swift`
- Modify: `ios/WatchApp/RustyWatchApp.swift`

- [ ] Add readiness and safe-control fields to the latest-wins application context.
- [ ] Render the readiness state and whether snooze/dismiss must happen on the phone.
- [ ] Build the watch target through the generated project.

### Task 6: Cross-Platform Verification And Documentation

**Files:**
- Modify: `README.md`

- [ ] Run Android unit tests, lint, phone build, and Wear OS build.
- [ ] Run Rust core tests.
- [ ] Generate and build the iOS and watch targets without signing.
- [ ] Update the README feature matrix and explain the three wakeup modes.
- [ ] Run `git diff --check` and review changed files for accidental `.idea/gradle.xml` inclusion.
