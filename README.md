# Rusty Alarm 🦀

Android 알람 앱 — UI/시스템은 **Kotlin + Jetpack Compose**, 도메인 로직은 **Rust crate**로 분리한 포트폴리오 프로젝트.

---

## 주요 기능

- 알람 추가 / 수정 / 삭제
- 반복 요일 설정 (일~토)
- 5분 스누즈
- 진동 설정
- 활성화 / 비활성화 토글
- 기기 재부팅 후 알람 자동 복구
- Material 3 다크 테마

---

## 기술 스택

| 영역 | 기술 |
|------|------|
| UI | Kotlin · Jetpack Compose · Material 3 |
| 알람 예약 | Android AlarmManager (setExactAndAllowWhileIdle) |
| 백그라운드 | BroadcastReceiver · BootReceiver |
| 알림 | NotificationManager |
| 로컬 저장 | Room (SQLite) |
| 도메인 로직 | Rust (chrono) |
| 언어 브리지 | JNI (`jni` crate) |
| 빌드 | Gradle (Kotlin DSL) · Cargo · cargo-ndk |

---

## 왜 Kotlin + Rust 구조인가?

| 역할 | 이유 |
|------|------|
| Android 시스템 API | AlarmManager, BroadcastReceiver 등은 Android SDK 없이는 불가 → Kotlin |
| 시간 계산 / 포매팅 | 플랫폼 독립적 순수 로직 → Rust (`cargo test` 로 빠른 검증) |
| 타입 안전성 | Rust의 Result/Option으로 경계값 처리 강제 |
| 포트폴리오 | 멀티 언어 JNI 연동 경험 시연 |

---

## 폴더 구조

```
rusty-alarm/
├── app/src/main/java/com/example/rustyalarm/
│   ├── alarm/          # 도메인 + 시스템 (Scheduler, Receiver, DB)
│   ├── ui/
│   │   ├── navigation/ # 네비게이션 그래프
│   │   ├── screens/    # 3개 화면 (List / Edit / Ring)
│   │   ├── components/ # AlarmCard, DaySelector, TimePickerSection
│   │   └── theme/      # Material 3 다크 테마
│   ├── viewmodel/      # AlarmListViewModel, AlarmEditViewModel
│   └── rust/           # RustAlarmCore.kt (JNI 브리지)
└── rust/alarm_core/
    └── src/
        ├── lib.rs       # JNI 진입점
        ├── alarm.rs     # validate_alarm_time
        ├── time_calc.rs # calculate_next_alarm_timestamp
        └── formatter.rs # format_time, get_repeat_days_label
```

---

## 실행 방법 (전체)

### 1. 사전 준비

```bash
# Rust 설치
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh

# Android 크로스 컴파일 타겟 추가
rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android i686-linux-android

# cargo-ndk 설치
cargo install cargo-ndk
```

### 2. Rust 빌드 (JNI .so 생성)

```bash
cd rust/alarm_core

# Debug 빌드 (에뮬레이터용)
cargo ndk -t arm64-v8a -t x86_64 -o ../../app/src/main/jniLibs build

# Release 빌드 (실기기 배포용)
cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 -o ../../app/src/main/jniLibs build --release
```

빌드 후 `app/src/main/jniLibs/` 아래에 `.so` 파일이 생성됩니다:
```
app/src/main/jniLibs/
├── arm64-v8a/libalarm_core.so
├── armeabi-v7a/libalarm_core.so
└── x86_64/libalarm_core.so
```

### 3. Rust 단위 테스트

```bash
cd rust/alarm_core
cargo test
# 결과: 15 passed; 0 failed
```

### 4. Android Studio에서 실행

1. Android Studio → **Open** → `rusty-alarm/` 폴더 선택
2. Rust `.so` 파일이 위의 단계에서 이미 생성되어 있어야 합니다
3. **Run 'app'** (▶) → 에뮬레이터 또는 실기기 선택

> **참고:** Rust 라이브러리 없이도 앱이 실행됩니다.
> `RustAlarmCore.isAvailable == false`이면 Kotlin 폴백 구현으로 동작합니다.

---

## 알람 권한 설명

### Android 12 이상 (API 31+) — SCHEDULE_EXACT_ALARM

- 시스템 알람 앱 외의 앱은 **정확한 알람** 사용 시 권한 필요
- 앱은 `canScheduleExactAlarms()` 체크 후:
  - 권한 있음 → `setExactAndAllowWhileIdle` 사용
  - 권한 없음 → `setAndAllowWhileIdle` 폴백 (오차 수 분 가능)
- 권한 부여 방법: **설정 → 앱 → Rusty Alarm → 알람 및 리마인더 → 허용**

### Android 13 이상 (API 33+) — POST_NOTIFICATIONS

- 앱 최초 실행 시 알림 권한 요청 다이얼로그 표시
- 거부 시 알람이 예약되어도 알림이 표시되지 않음
- 권한 부여 방법: **설정 → 앱 → Rusty Alarm → 알림 → 허용**

### Android 14 이상 (API 34+) 주의사항

- `USE_EXACT_ALARM`: 시계/알람 카테고리 앱에게 자동 부여되는 권한
  (일반 앱은 Play Store 심사 필요)
- `SCHEDULE_EXACT_ALARM`과 `USE_EXACT_ALARM` 중 하나라도 있으면 정확한 알람 사용 가능
- 실기기 테스트 시 반드시 권한 허용 확인

---

## GitHub 업로드

```bash
cd rusty-alarm
git init
git add .
git commit -m "feat: initial rusty alarm app"
git branch -M main
git remote add origin https://github.com/USERNAME/rusty-alarm.git
git push -u origin main
```

> `USERNAME`을 본인의 GitHub 계정명으로 변경하세요.

---

## 향후 개선 예정

- [ ] 알람 사운드 선택 (RingtoneManager)
- [ ] UniFFI로 JNI 보일러플레이트 제거
- [ ] 위젯 (Glance API)
- [ ] Wear OS 연동
- [ ] 알람 통계 (언제 끄는지, 스누즈 횟수)
- [ ] Hilt 의존성 주입 적용
- [ ] Espresso / Compose UI 테스트
