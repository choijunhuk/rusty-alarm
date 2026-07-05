# Rusty Alarm 🦀

알람 앱 멀티 플랫폼 포트폴리오 — Android · iOS · Wear OS.

[![Latest Release](https://img.shields.io/github/v/release/choijunhuk/rusty-alarm?label=Download&style=for-the-badge)](https://github.com/choijunhuk/rusty-alarm/releases/latest)

---

## 📥 빠른 다운로드 (사용자용)

### 📱 Android 폰 → APK 직접 설치

가장 쉬운 방법. 폰 브라우저에서 한 번에 받기:

1. 폰에서 [**최신 Release 페이지**](https://github.com/choijunhuk/rusty-alarm/releases/latest) 열기.
2. `rusty-alarm-android-v1.0.0.apk` 탭 → 다운로드.
3. 처음이면 **설정 → 보안 → "출처를 알 수 없는 앱" 허용** (브라우저 또는 파일 관리자 항목).
4. 다운로드된 APK 파일 탭 → "설치".
5. 첫 실행시 권한 다이얼로그 전부 허용 (알람, 알림, 위치, 카메라, 마이크).

**최소 요구사항**: Android 8.0 (API 26) 이상.

> 디버그 서명이라 Play Store 자동 업데이트 안 됨. 새 버전은 Release 페이지에서 다시 받아야 한다.

### ⌚ Wear OS 워치 → 페어링 폰 통해 사이드로드

워치 앱은 페어링된 폰에 Android 앱이 먼저 설치되어 있어야 한다.

1. 위의 Android 앱부터 설치.
2. [`rusty-alarm-wear-v1.0.0.apk`](https://github.com/choijunhuk/rusty-alarm/releases/latest) 다운.
3. 워치 디버깅 켜기: 워치 설정 → 시스템 → 정보 → 빌드 번호 7번 탭 → 개발자 옵션 → ADB 디버깅 ON + Wi-Fi/Bluetooth 디버깅 ON.
4. 컴퓨터에서:
   ```sh
   # 폰을 통한 워치 디버깅 (가장 흔함)
   adb forward tcp:4444 tcp:5555      # 폰이 워치로 포워드
   adb connect localhost:4444
   adb install rusty-alarm-wear-v1.0.0.apk
   ```
   또는 Wi-Fi 디버깅으로 워치에 직접 연결:
   ```sh
   adb connect <워치_IP>:5555
   adb install rusty-alarm-wear-v1.0.0.apk
   ```

**최소 요구사항**: Wear OS 3.0 이상.

### 🍎 iOS / iPadOS → Xcode 직접 빌드 (무료 Apple ID)

iOS는 사전 빌드 IPA 배포가 불가능하다 (Personal Team 서명은 본인 기기 UDID 만 통과). 본인 Mac + iPhone 만 있으면 5분 안에 설치 가능:

1. **소스 받기**: `git clone https://github.com/choijunhuk/rusty-alarm.git`
2. **사전 도구**:
   ```sh
   brew install xcodegen
   ```
3. **프로젝트 생성**:
   ```sh
   cd rusty-alarm/ios
   xcodegen generate
   open RustyAlarm.xcodeproj
   ```
4. **Xcode 에서**:
   - 좌측 RustyAlarm 타깃 → Signing & Capabilities → Team 에 본인 Apple ID 선택 (Personal Team).
   - Widget 타깃도 동일 처리.
   - iPhone 연결 → 좌측 상단 디바이스 드롭다운에서 본인 iPhone 선택 → ⌘R.
5. **iPhone 에서**: 설정 → 일반 → VPN 및 기기 관리 → 본인 Apple ID → "신뢰".

**최소 요구사항**: macOS Tahoe (26.x) + Xcode 26+, iOS 17.0 이상.

자세한 단계 + 7일 재서명 사이클 + 무료 ID 한계는 **[`ios/INSTALL_PERSONAL_DEVICE.md`](ios/INSTALL_PERSONAL_DEVICE.md)** 참고.

---

## 🔧 개발자용 — 소스에서 빌드

세 플랫폼 전부 한 저장소에 있다. 본인이 작업할 플랫폼만 빌드하면 된다.

### Android + Wear (Kotlin · Rust JNI · Gradle)

```sh
# 사전: Rust + Android NDK + cargo-ndk
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
rustup target add aarch64-linux-android x86_64-linux-android
cargo install cargo-ndk

# 1. Rust JNI 라이브러리 (도메인 로직)
cd rust/alarm_core
cargo ndk -t arm64-v8a -t x86_64 -o ../../app/src/main/jniLibs build --release

# 2. APK 빌드
cd ../..
./gradlew :app:assembleDebug          # Android 폰
./gradlew :wear:assembleDebug         # Wear OS

# APK 위치
# app/build/outputs/apk/debug/app-debug.apk
# wear/build/outputs/apk/debug/wear-debug.apk
```

또는 Android Studio 에서 `rusty-alarm/` 폴더 Open → Run 'app' / Run 'wear'.

> Rust 라이브러리 없이도 앱은 실행된다. `RustAlarmCore.isAvailable == false` 면 Kotlin 폴백.

### iOS (SwiftUI · XcodeGen)

```sh
cd ios
brew install xcodegen
xcodegen generate
open RustyAlarm.xcodeproj
```

상세는 위 [iOS 다운로드 섹션](#-ios--ipados--xcode-직접-빌드-무료-apple-id) 참고.

---

## 폴더 구조

```
rusty-alarm/
├── app/        # Android (Kotlin + Compose) — Rust JNI 도메인 로직 호출
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
| 기상 모드 (편안한/지각 방지/강제) | ✅ | ✅ | 상태 표시 |
| 리포트 추천 즉시 적용 | ✅ | ✅ | ❌ |
| 다음 알람 준비 상태 | ✅ | ✅ | ✅ |

---

## 기술 스택 요약

| 영역 | Android | iOS |
|------|---------|-----|
| UI | Jetpack Compose + Material 3 | SwiftUI |
| 알람 예약 | `AlarmManager.setExactAndAllowWhileIdle` | `UNCalendarNotificationTrigger` (+ iOS 26 AlarmKit) |
| 백그라운드 | `BroadcastReceiver` · `WorkManager` | `BGTaskScheduler` |
| 로컬 저장 | Room (SQLite) | UserDefaults + App Group |
| 도메인 로직 | Rust crate via JNI | SwiftUI 네이티브 (Rust 미사용) |

## 기상 여정

알람 편집에서 사용 목적에 맞는 기상 모드를 선택할 수 있다.

- **편안한 기상**: 미리알림, 단계적 알람, 최대 3회 스누즈로 부드럽게 깨운다.
- **지각 방지**: 낮은 부담의 타이핑 챌린지와 1회 스누즈로 일정 준수를 돕는다.
- **강제 기상**: 챌린지, 높은 음량, 기상 루틴으로 무의식적인 알람 해제를 막는다.

주간 리포트는 스누즈 비율과 반응 시간을 바탕으로 다음 활성 알람에 적용할 수 있는 모드를 제안한다. Wear OS와 Apple Watch에는 다음 알람 준비 상태와 폰 챌린지 필요 여부가 표시된다.

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
- [x] Hilt 의존성 주입
- [x] Compose UI 테스트 (Robolectric — `./gradlew :app:testDebugUnitTest`)
- [ ] iOS 유료 가입 후 TestFlight 베타 채널
- [ ] iOS Critical Alerts entitlement 신청 (Apple 수동 검토)
