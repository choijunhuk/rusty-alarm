# Personal Device Install (Free Apple ID)

이 문서는 **유료 Apple Developer Program 없이** 본인 iPhone에 Rusty Alarm을
설치해서 테스트하는 방법을 정리한다.

---

## 한계 먼저

| 항목 | 무료 Apple ID | 유료 ($99/yr) |
|------|---------------|----------------|
| 본인 기기 서명 | 7일 | 1년 |
| TestFlight 배포 | ❌ | ✅ |
| App Store 배포 | ❌ | ✅ |
| Critical Alerts (무음모드에서 울림) | ❌ | ✅ (Apple 수동 승인) |
| AlarmKit (iOS 26 정확 알람) | ❌ | ✅ (entitlement 신청) |
| App Group, HealthKit, BGTask, Widget, Watch | ✅ | ✅ |

핵심 함의:
- **알람이 무음 모드/방해금지 모드에서 안 울린다.** Critical Alerts 없이는 iOS가
  이를 그냥 알림으로 취급한다. → 시스템 시계 앱처럼 무음 우회 불가.
- **7일마다 재빌드 + 재서명 필요.** Xcode 열고 다시 Run 누르면 된다.
- **AlarmKit 코드는 자동 폴백.** `AlarmKitBridge.swift` 가 entitlement 없으면
  `UNCalendarNotificationTrigger` 로 빠진다.

---

## 1. 사전 준비

1. macOS Tahoe (26.x) + Xcode 26.5 (현재 시스템 확인 완료).
2. 본인 Apple ID로 Xcode 로그인:
   - Xcode → Settings → Accounts → `+` → Apple ID.
   - 추가 후 "Manage Certificates…" 클릭하면 자동으로 Personal Team 생성됨.
3. iPhone:
   - Settings → General → VPN & Device Management 에서 본인 Apple ID team 신뢰.
   - 케이블 연결 후 Xcode 가 인식하는지 확인.
   - iOS 17 이상 필수 (project.yml deployment target).

---

## 2. Xcode 프로젝트 열기

```sh
cd ~/Desktop/rusty-alarm/ios
open RustyAlarm.xcodeproj
```

처음 열 때 Indexer 가 한 번 돌면서 1~2분 걸린다.

---

## 3. 서명 (Signing & Capabilities)

3개 타깃 모두 동일하게 처리:

### RustyAlarm (메인 앱)
1. Project navigator → `RustyAlarm` 타깃 선택 → **Signing & Capabilities** 탭.
2. **Automatically manage signing** 체크.
3. **Team**: 본인 Apple ID (Personal Team) 선택.
4. **Bundle Identifier** 가 `com.example.rustyalarm` 으로 되어 있는지 확인.
   - 다른 사용자가 같은 ID 로 서명한 적이 있으면 충돌 발생 → 본인 도메인 prefix
     로 바꿔야 함 (예: `com.choijunhuk.rustyalarm`). 이 때 widget/watch/App Group
     도 prefix 일괄 변경 필요. 5. 항목 참고.

### RustyAlarmWidget (Widget Extension)
- 같은 Team 선택.
- Bundle ID: `com.example.rustyalarm.widget`.

### RustyAlarmWatch (Watch App)
- 같은 Team 선택.
- Bundle ID: `com.example.rustyalarm.watchkitapp`.

> 무료 Team 은 한 번에 활성 App ID 최대 3 개라는 제약이 있다. 위 3개로 정확히
> 한도를 채운다. 다른 무료 빌드를 같은 Apple ID 로 돌려놨다면 그쪽을 먼저 비워야
> 한다.

---

## 4. App Group / HealthKit

- 메인 앱 Signing & Capabilities 에 **App Groups** 가 이미 박혀 있다
  (`group.com.example.rustyalarm`). Widget / Watch 타깃도 같은 그룹이 추가된 채로
  생성됨. 만약 빨간 X 가 떠 있으면 `+ Capability → App Groups` 한 번 다시 누른다.
- **HealthKit** 도 메인 앱 entitlement 에 미리 포함됨. 처음 누를 때 권한 다이얼
  로그가 뜬다.

---

## 5. Bundle ID 변경이 필요할 때

다른 무료 계정 사용자와 ID 충돌 시:

```sh
cd ~/Desktop/rusty-alarm/ios

# 1) project.yml 내 모든 com.example.rustyalarm → com.<당신이름>.rustyalarm
# 2) Resources/Info.plist
# 3) Resources/Widget-Info.plist
# 4) Resources/Watch-Info.plist
# 5) Resources/*.entitlements 의 group.com.example.rustyalarm → group.com.<당신>.rustyalarm
# 6) Sources/Persistence/WatchSync.swift 의 App Group 문자열
# 7) Widget/NextAlarmWidget.swift 의 App Group 문자열
# 8) Sources/Persistence/BackupTask.swift 의 BGTask 식별자

xcodegen generate --spec project.yml   # 재생성
```

---

## 6. 빌드 → 설치

1. Xcode 좌측 상단 Scheme 선택: **RustyAlarm**.
2. 디바이스 드롭다운 → 연결된 iPhone 선택.
3. ⌘R (Run). 첫 실행시 iPhone 에서:
   - Settings → General → VPN & Device Management → 본인 Apple ID → "Trust" 클릭.
4. 앱 실행 후 알림 권한 / HealthKit / 위치 / 카메라 / 마이크 다이얼로그 차례로 허용.

---

## 7. 7일 만료 후 재서명

- Xcode 열고 ⌘R 한 번 더. Personal Team 인증이 갱신되며 새 7일 사이클 시작.
- 앱 데이터는 보존된다 (UserDefaults / App Group 동일 ID).

---

## 8. 알려진 동작 차이 (Android → iOS)

| Android | iOS | 비고 |
|---------|------|------|
| AlarmManager 정확 트리거 | `UNCalendarNotificationTrigger` | 무음모드 우회 불가 |
| Full-screen activity 자동 실행 | 사용자가 알림 탭해야 진입 | Apple 정책 |
| BroadcastReceiver | BGTaskScheduler | 주기 백업만 |
| Wear OS | watchOS + WatchConnectivity | applicationContext 푸시 |

---

## 9. 유료 가입 후 확장 (참고)

- $99/yr 가입 → Apple Developer Program.
- Apple 개발자 페이지에서 **Critical Alerts** entitlement 신청 (수동 검토 1~2주).
- iOS 26 출시 후 AlarmKit entitlement 도 같은 방식.
- 승인되면 `Resources/RustyAlarm.entitlements` 에 아래 추가:
  ```xml
  <key>com.apple.developer.usernotifications.critical-alerts</key>
  <true/>
  <key>com.apple.developer.alarmkit</key>
  <true/>
  ```
- 이후 `AlarmKitBridge.swift` 가 자동으로 AlarmKit path 사용.
