# Rusty Alarm — Wear OS

Wear OS 3.0+ 컴패니언. 페어링된 폰의 Rusty Alarm Android 앱이 보낸 "다음 알람" 데이터를 워치 화면에 표시한다.

## 요구 사항

- Wear OS 3.0 이상 (또는 동급 에뮬레이터)
- 페어링된 Android 폰에 **Rusty Alarm Android 앱** (이 저장소의 [`../app/`](../app/)) 이 설치되어 있어야 한다.

## 빌드

이 모듈은 Android 앱과 동일한 Android Studio 프로젝트 안에 있다.

```sh
# rusty-alarm/ 루트에서 Android Studio Open
# Run 구성 → 'wear' 타깃 선택 → Wear OS 에뮬레이터 또는 페어링된 워치
```

## 동작

- `AlarmDataListener` 가 Wearable Data Layer 채널을 구독한다.
- 폰의 `WearableDataSync` (앱 모듈) 가 다음 알람 시각/제목을 푸시한다.
- `NextAlarmStore` 가 로컬 캐시. 워치 단독 상태로도 마지막 페이로드 표시.

## 한계

- 알람 트리거 자체는 폰에서 처리. 워치는 알림 미러링.
- 챌린지 / 펫 / 리포트 UI 미지원 (워치는 단순 다음-알람 디스플레이만).

## 파일

- `src/main/java/com/example/rustyalarm/wear/MainActivity.kt` — 워치 메인 UI
- `src/main/java/com/example/rustyalarm/wear/NextAlarmStore.kt` — 로컬 캐시
- `src/main/java/com/example/rustyalarm/wear/AlarmDataListener.kt` — DataClient 콜백
