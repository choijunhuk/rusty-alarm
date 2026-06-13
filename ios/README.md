# Rusty Alarm — iOS

SwiftUI version of the alarm app with full feature parity to the Android
counterpart, including challenges, pet system, weekly report, widgets,
Apple Watch companion, and background backups.

## Layout

```
ios/
├── Sources/
│   ├── App/RustyAlarmApp.swift                   ← @main entry
│   ├── Models/
│   │   ├── Alarm.swift                           ← + qrRequiredValue + geofence
│   │   ├── ChallengeType.swift                   ← squat + tetris
│   │   ├── Pet.swift
│   │   └── AlarmEvent.swift
│   ├── Persistence/
│   │   ├── AlarmStore.swift
│   │   ├── PetStore.swift
│   │   ├── EventStore.swift                      ← weekly report + heatmap
│   │   ├── AlarmScheduler.swift                  ← UNUserNotificationCenter
│   │   ├── WatchSync.swift                       ← WatchConnectivity + App Group
│   │   └── BackupTask.swift                      ← BGTaskScheduler weekly snapshot
│   ├── Views/
│   │   ├── RootTabView.swift                     ← 알람/펫/리포트
│   │   ├── AlarmListView.swift
│   │   ├── AlarmEditView.swift
│   │   ├── AlarmRingView.swift                   ← challenge runner + routine
│   │   ├── PetView.swift
│   │   ├── ReportView.swift
│   │   ├── Components/YouTubePlayerView.swift
│   │   └── Challenges/
│   │       ├── MathChallengeView.swift
│   │       ├── TypingChallengeView.swift
│   │       ├── ShakeChallengeView.swift          ← CoreMotion
│   │       ├── SquatChallengeView.swift          ← CoreMotion
│   │       ├── VoiceChallengeView.swift          ← Speech ko-KR
│   │       ├── QrChallengeView.swift             ← AVCaptureMetadataOutput
│   │       ├── PhotoChallengeView.swift          ← AVCaptureSession
│   │       ├── LocationChallengeView.swift       ← CLLocationManager
│   │       ├── StepChallengeView.swift           ← HealthKit
│   │       └── TetrisChallengeView.swift         ← SwiftUI Canvas
├── Widget/
│   └── NextAlarmWidget.swift                     ← WidgetKit + Lock-screen support
├── WatchApp/
│   └── RustyWatchApp.swift                       ← watchOS face
└── Resources/Info.plist                          ← permissions + BGTask + audio mode
```

## Setup (Xcode multi-target)

1. **Main iOS app target**
   - Create the project (`App ▸ iOS`), drag `ios/Sources/` into it.
   - Replace `Info.plist` with `ios/Resources/Info.plist` (or merge keys).
   - In *Signing & Capabilities* add **App Groups** = `group.com.example.rustyalarm`
     (shared by widget + watch + main app).
   - Enable **Background Modes**: Audio, Background processing.

2. **Widget Extension target** (`File ▸ New ▸ Target ▸ Widget Extension`)
   - Drop `ios/Widget/NextAlarmWidget.swift` in this target.
   - Enable the same App Group.

3. **Watch App target** (`File ▸ New ▸ Target ▸ Watch App` paired with the
   main app)
   - Drop `ios/WatchApp/RustyWatchApp.swift` in.
   - Enable the same App Group.

4. **HealthKit capability** for the main app (step-count challenge).

5. **BGTaskScheduler identifier** — already declared in Info.plist:
   `com.example.rustyalarm.backup`. The app calls `Backup.register()` at
   launch; iOS will fire `doWork` roughly weekly when the device is idle.

6. Run on a real device (simulators can't reliably fire local notifications
   or use HealthKit / CoreMotion).

## Features in v3

| Feature                          | Status |
|----------------------------------|--------|
| Alarm CRUD + repeat / one-shot   | ✅ |
| Local notifications              | ✅ (Critical Alerts entitlement optional) |
| Routine checklist gate           | ✅ |
| Math / Typing / Shake / Voice    | ✅ |
| Squat (CoreMotion)               | ✅ |
| QR scan (AVCaptureMetadataOutput)| ✅ |
| Photo (AVCaptureSession)         | ✅ |
| Location (CLLocationManager)     | ✅ |
| Step count (HealthKit)           | ✅ |
| Tetris (SwiftUI Canvas)          | ✅ |
| YouTube BGM (WKWebView IFrame)   | ✅ |
| Pet + happiness + skins          | ✅ |
| Weekly report + 30d heatmap      | ✅ |
| WidgetKit lock-screen widget     | ✅ |
| Apple Watch companion            | ✅ |
| Weekly backup (BGTaskScheduler)  | ✅ |

## Known caveats

- iOS won't auto-launch the alarm screen on notification fire; the user has
  to tap the notification. Apple does not expose this to third-party apps.
  *Critical Alerts* entitlement (manual Apple review) is the closest you can
  get to Android's full-screen alarm.
- HealthKit step counter relies on the user actually moving in the
  measurement window (default: from `start` view appearance to dismiss).
- Tetris UI is functional but visually plainer than the Android version
  (no shaders / particle effects).
