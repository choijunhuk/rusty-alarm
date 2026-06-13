# Rusty Alarm 🦀

알람 앱 멀티 플랫폼 포트폴리오 — Android · iOS · Wear OS.

---

## 어떤 버전이 필요한가요?

| 플랫폼 | 폴더 | 설치 가이드 | 상태 |
|--------|------|-------------|------|
| 📱 **Android** | [`app/`](app/) · [`rust/`](rust/) | [Android 설치 →](#-android-설치) | Production ready (Kotlin + Rust JNI) |
| 🍎 **iOS / iPadOS** | [`ios/`](ios/) | [iOS 설치 가이드 →](ios/INSTALL_PERSONAL_DEVICE.md) | SwiftUI 포트, 무료 Apple ID 지원 |
| ⌚ **Wear OS** | [`wear/`](wear/) | [Wear OS 설치 →](#-wear-os-설치) | Android companion, WearableDataSync |

각 폴더에 들어 있는 코드는 독립적으로 빌드된다. 셋 다 받을 필요 없이 본인이 쓰는 플랫폼만 골라서 빌드하면 된다.

---

## 📱 Android 설치

대상: Android 7.0 (API 24) 이상.

### 사전 준비

```bash
# Rust + cargo-ndk (도메인 로직 JNI 라이브러리)
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
cargo install cargo-ndk
```

### 빌드

```bash
# 1. Rust JNI 라이브러리 빌드
cd rust/alarm_core
cargo ndk -t arm64-v8a -t x86_64 -o ../../app/src/main/jniLibs build

# 2. Android Studio에서 rusty-alarm/ 폴더 Open → Run 'app'
```

> Rust 라이브러리 없이도 실행은 된다. `RustAlarmCore.isAvailable == false` 면 Kotlin 폴백.

### 권한 (Android 12+)

- **알람 및 리마인더** (SCHEDULE_EXACT_ALARM): 설정 → 앱 → Rusty Alarm.
- **알림** (POST_NOTIFICATIONS, Android 13+): 최초 실행시 다이얼로그.

자세한 권한 매트릭스는 [`app/README.md`](app/) 참고 (없으면 본 README 하단 Android 상세 섹션).

---

## 🍎 iOS 설치

대상: iOS 17.0 이상.

### 빠른 시작

```bash
cd ios
brew install xcodegen        # 처음 한 번만
xcodegen generate            # RustyAlarm.xcodeproj 생성
open RustyAlarm.xcodeproj
```

Xcode 에서:
1. **Signing & Capabilities** → 본인 Apple ID Personal Team 선택 (메인 + Widget 타깃).
2. iPhone 연결 → ⌘R.

자세한 단계 + 무료 Apple ID 한계 + 7일 재서명 사이클은 **[`ios/INSTALL_PERSONAL_DEVICE.md`](ios/INSTALL_PERSONAL_DEVICE.md)** 참고.

> iOS는 무음모드 우회가 불가능하다 (Critical Alerts entitlement은 $99 유료 + Apple 수동 승인). 코드는 iOS 26 AlarmKit 듀얼 지원 (`Sources/Persistence/AlarmKitBridge.swift`).

---

## ⌚ Wear OS 설치

대상: Wear OS 3.0 이상. Android 컴패니언 앱과 페어링 필요.

### 빌드

```bash
# Android 앱과 동일한 Android Studio 프로젝트 안에서:
# Run 구성 → wear 타깃 선택 → Wear OS 에뮬레이터 또는 워치 선택
```

페어링된 폰에 Android 앱이 먼저 설치되어 있어야 다음 알람 데이터를 받는다 (`WearableDataSync`).

---

## 폴더 구조

```
rusty-alarm/
├── app/        # Android (Kotlin + Compose) — Rust JNI 도메인 로직
├── ios/        # iOS (SwiftUI) — Widget + WatchApp 포함
├── wear/       # Wear OS (Kotlin + Compose for Wear)
├── rust/       # 공유 도메인 로직 (cargo crate)
└── README.md   # ← 지금 보는 파일
```

---

## 기능 매트릭스

| 기능 | Android | iOS | Wear OS |
|------|---------|-----|---------|
| 알람 CRUD + 반복 | ✅ | ✅ | 보기 전용 |
| 정확 알람 (무음 우회) | ✅ AlarmManager | △ Critical Alerts 필요 | n/a |
| 챌린지 (수학/타이핑/흔들기/스쿼트) | ✅ | ✅ | ❌ |
| 챌린지 (사진/QR/위치/걷기) | ✅ | ✅ | ❌ |
| 테트리스 챌린지 | ✅ Compose Canvas | ✅ SwiftUI Canvas | ❌ |
| 음성 인식 챌린지 | ✅ ko-KR | ✅ ko-KR | ❌ |
| YouTube 알람 BGM | ✅ WebView | ✅ WKWebView | ❌ |
| 펫 + 스킨 + 경험치 | ✅ | ✅ | ❌ |
| 주간 리포트 + 30일 히트맵 | ✅ | ✅ | ❌ |
| 잠금화면 위젯 | ✅ Glance | ✅ WidgetKit | n/a |
| 컴패니언 워치 | ✅ Wearable Data Layer | ✅ WatchConnectivity | 본체 |
| 백업 (주간 자동) | ✅ WorkManager | ✅ BGTaskScheduler | ❌ |

---

## 기술 스택 요약

| 영역 | Android | iOS |
|------|---------|-----|
| UI | Jetpack Compose + Material 3 | SwiftUI |
| 알람 예약 | `AlarmManager.setExactAndAllowWhileIdle` | `UNCalendarNotificationTrigger` (+ iOS 26 AlarmKit) |
| 백그라운드 | `BroadcastReceiver` · `WorkManager` | `BGTaskScheduler` |
| 로컬 저장 | Room (SQLite) | UserDefaults + App Group |
| 도메인 로직 | Rust crate via JNI | SwiftUI 네이티브 (Rust 미사용) |

---

## Android 권한 상세

### Android 12 이상 (API 31+) — SCHEDULE_EXACT_ALARM
- 시스템 알람 앱 외 → 명시적 권한 필요. `canScheduleExactAlarms()` 체크 후 분기.

### Android 13 이상 (API 33+) — POST_NOTIFICATIONS
- 앱 최초 실행시 다이얼로그.

### Android 14 이상 (API 34+)
- `USE_EXACT_ALARM` 권한은 시계/알람 카테고리 앱에 자동 부여 (Play Store 심사 필요).

---

## 향후 개선 예정

- [ ] UniFFI 활성화 (현재는 UDL + 문서만)
- [ ] Hilt 의존성 주입
- [ ] Compose UI 테스트
- [ ] iOS 유료 가입 후 TestFlight 베타 채널
- [ ] iOS Critical Alerts entitlement 신청 (Apple 수동 검토)
