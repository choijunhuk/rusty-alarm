# Wakeup Success Loop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Android wakeup success loop by making readiness issues actionable on the main screen, previewing alarm draft reliability in the editor, and making report recommendations explain their cause and effect.

**Architecture:** Extend existing pure domain objects first, then wire Compose screens to those objects. `AlarmReliability` owns readiness issue/action classification, `WakeupPresetApplier` owns preset mutations, and `ReportInsightEngine` owns weekly recommendation copy.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Android settings intents, JUnit 4, existing Room/repository/viewmodel patterns.

## Global Constraints

- Do not add new dependencies.
- Exclude release automation, TestFlight, Critical Alerts entitlement application, and build pipeline work.
- Keep Korean-first copy.
- Preserve existing alarm scheduling and preset mutation behavior unless tests require a targeted extension.
- Keep watch behavior snapshot/status based; do not add watch dismiss/control bypasses.

---

## File Structure

- Modify `app/src/main/java/com/example/rustyalarm/alarm/AlarmReliability.kt`: add issue action kinds, preset suggestions, and primary issue ranking.
- Modify `app/src/test/java/com/example/rustyalarm/alarm/AlarmReliabilityTest.kt`: cover action classification and draft guidance.
- Modify `app/src/main/java/com/example/rustyalarm/viewmodel/ReportInsightEngine.kt`: add cause/result fields to actionable insights.
- Modify `app/src/test/java/com/example/rustyalarm/viewmodel/ReportInsightEngineTest.kt`: cover cause/result copy and preset mapping.
- Modify `app/src/main/java/com/example/rustyalarm/ui/screens/AlarmListScreen.kt`: add readiness action panel and settings/edit/test-alarm routing.
- Modify `app/src/main/java/com/example/rustyalarm/ui/screens/AlarmEditScreen.kt`: add draft success preview and preset impact copy.

## Task 1: Domain Readiness Actions

**Files:**
- Modify: `app/src/main/java/com/example/rustyalarm/alarm/AlarmReliability.kt`
- Test: `app/src/test/java/com/example/rustyalarm/alarm/AlarmReliabilityTest.kt`

**Interfaces:**
- Produces: `ReliabilityActionKind`, `ReliabilityIssue.actionKind`, `AlarmReliability.primaryIssue(issues: List<ReliabilityIssue>): ReliabilityIssue?`, `AlarmReliability.recommendedPreset(alarm: Alarm): WakeupPreset?`
- Consumes: existing `PermissionsStatus`, `Alarm`, `ChallengeType`, `WakeupPreset`

- [ ] **Step 1: Write failing tests**

Add tests asserting notification/exact/battery issues map to settings action kinds, alarm configuration issues map to edit action kind, and a weak alarm recommends `WakeupPreset.FORCED`.

- [ ] **Step 2: Run red test**

Run: `./gradlew :app:testDebugUnitTest --tests com.example.rustyalarm.alarm.AlarmReliabilityTest`

Expected: compile failure for missing `ReliabilityActionKind`, `actionKind`, and `recommendedPreset`.

- [ ] **Step 3: Implement minimal domain changes**

Add the enum and fields in `AlarmReliability.kt`, update existing issue construction with action kinds, add primary issue ranking that prefers blocking permission issues, and add recommended preset mapping.

- [ ] **Step 4: Run green test**

Run: `./gradlew :app:testDebugUnitTest --tests com.example.rustyalarm.alarm.AlarmReliabilityTest`

Expected: tests pass.

## Task 2: Report Insight Cause And Result Copy

**Files:**
- Modify: `app/src/main/java/com/example/rustyalarm/viewmodel/ReportInsightEngine.kt`
- Test: `app/src/test/java/com/example/rustyalarm/viewmodel/ReportInsightEngineTest.kt`

**Interfaces:**
- Produces: `WakeupInsight.cause: String?`, `WakeupInsightAction.expectedResult: String`
- Consumes: existing `WakeupInsightInput`, `WakeupPreset`

- [ ] **Step 1: Write failing tests**

Add tests asserting high-snooze insight exposes a cause containing the snooze percentage and an expected result mentioning one snooze, and no-challenge insight explains that a dismiss gate will be added.

- [ ] **Step 2: Run red test**

Run: `./gradlew :app:testDebugUnitTest --tests com.example.rustyalarm.viewmodel.ReportInsightEngineTest`

Expected: compile failure for missing `cause` and `expectedResult`.

- [ ] **Step 3: Implement insight copy**

Extend data classes and fill cause/result strings for actionable insights while preserving existing ids and preset mappings.

- [ ] **Step 4: Run green test**

Run: `./gradlew :app:testDebugUnitTest --tests com.example.rustyalarm.viewmodel.ReportInsightEngineTest`

Expected: tests pass.

## Task 3: Main Screen Readiness Panel

**Files:**
- Modify: `app/src/main/java/com/example/rustyalarm/ui/screens/AlarmListScreen.kt`

**Interfaces:**
- Consumes: `ReliabilityIssue.actionKind`, `AlarmReliability.primaryIssue`, existing `vm.quickAlarm(minutes)`, `onEditAlarm(alarm)`
- Produces: compact readiness panel under the next-alarm hero

- [ ] **Step 1: Wire domain data**

Build a `ReliabilityDiagnostic` from current permissions, enabled alarm count, and next alarm in `AlarmListScreen`.

- [ ] **Step 2: Add readiness card**

Render headline, score/level, primary issue detail, and a primary action button. Permission actions open existing `Permissions` settings intents. Alarm configuration actions call `onEditAlarm(nextAlarm)`. No-enabled-alarm action calls `vm.quickAlarm(5)`.

- [ ] **Step 3: Verify UI compiles**

Run: `./gradlew :app:compileDebugKotlin`

Expected: compile succeeds.

## Task 4: Alarm Editor Draft Preview

**Files:**
- Modify: `app/src/main/java/com/example/rustyalarm/ui/screens/AlarmEditScreen.kt`

**Interfaces:**
- Consumes: `AlarmReliability.alarmIssues(alarm)`, `AlarmReliability.recommendedPreset(alarm)`, `WakeupPresetApplier.profile(alarm)`, existing `vm.applyWakeupPreset(preset)`
- Produces: save-adjacent draft readiness preview and preset impact button

- [ ] **Step 1: Add preview section**

Show current draft profile, first alarm issue if any, and strengths based on pure domain functions.

- [ ] **Step 2: Add preset action**

When `recommendedPreset` is non-null, show a button that applies it and a short text describing the expected change.

- [ ] **Step 3: Verify UI compiles**

Run: `./gradlew :app:compileDebugKotlin`

Expected: compile succeeds.

## Task 5: Report UI Cause And Effect

**Files:**
- Modify: `app/src/main/java/com/example/rustyalarm/ui/screens/ReportScreen.kt`

**Interfaces:**
- Consumes: `WakeupInsight.cause`, `WakeupInsight.action.expectedResult`
- Produces: cause/result copy in the "다음 개선 액션" card

- [ ] **Step 1: Render cause/result**

For each insight, show `cause` when present and show `expectedResult` above the apply button for actionable insights.

- [ ] **Step 2: Verify UI compiles**

Run: `./gradlew :app:compileDebugKotlin`

Expected: compile succeeds.

## Task 6: Final Verification

**Files:**
- Validate modified Android files.

- [ ] **Step 1: Run focused tests**

Run: `./gradlew :app:testDebugUnitTest --tests com.example.rustyalarm.alarm.AlarmReliabilityTest --tests com.example.rustyalarm.viewmodel.ReportInsightEngineTest --tests com.example.rustyalarm.alarm.WakeupPresetApplierTest`

Expected: focused tests pass.

- [ ] **Step 2: Run broader Android checks**

Run: `./gradlew :app:lintDebug :app:assembleDebug`

Expected: lint and debug build pass. If the local environment blocks Android SDK execution, record the exact blocker and run the strongest available focused tests.

## Self-Review

- Spec coverage: readiness actions, editor preview, report cause/effect, and exclusion of release work are each mapped to tasks.
- Placeholder scan: no task uses placeholder requirements.
- Type consistency: domain names match existing Kotlin naming and are introduced before UI tasks consume them.
