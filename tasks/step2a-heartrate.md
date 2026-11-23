# 구현 요청: 심박수 추가 + 걸음 수 진단 표시

## 배경
- 실기기(Galaxy S9+, Android 10)에서 걸음 수는 0으로 나옴. Health Connect 앱에도 걸음 데이터가 없음(삼성헬스가 안 넘김). 반면 심박수는 Health Connect에 들어와 있음.
- 목적: (1) 심박수를 읽어 Health Connect → 앱 연결을 실데이터로 확인, (2) 걸음 수 원인 진단용 정보 표시.
- 범위 밖 리팩토링 금지. 기존 걸음 수 기능은 그대로 유지.

## 대상 파일
- app/src/main/AndroidManifest.xml
- app/src/main/java/com/gh/healthapp/HealthConnectManager.kt
- app/src/main/java/com/gh/healthapp/StepsViewModel.kt
- app/src/main/java/com/gh/healthapp/MainActivity.kt

## 1. 권한
- 매니페스트에 `android.permission.health.READ_HEART_RATE` 추가.
- HealthConnectManager.PERMISSIONS에 `HealthPermission.getReadPermission(HeartRateRecord::class)` 추가.
  (기존 로직상 권한이 하나라도 없으면 NeedPermission 상태가 되어 "권한 허용" 버튼이 다시 나온다. 그대로 두면 됨.)

## 2. HealthConnectManager에 함수 추가
- `data class HeartRateSummary(val latestBpm: Long?, val latestTime: Instant?, val avg: Long?, val min: Long?, val max: Long?)`
- `suspend fun heartRateToday(): HeartRateSummary`
  - 범위: 오늘 0시(기기 시간대) ~ 지금
  - 평균/최저/최고: aggregate의 HeartRateRecord.BPM_AVG / BPM_MIN / BPM_MAX
  - 최근값: readRecords(HeartRateRecord, 같은 범위, ascendingOrder = false, pageSize = 1) 결과의 첫 레코드에서 samples 중 time이 가장 늦은 샘플의 beatsPerMinute와 time
  - 데이터가 없으면 모든 필드 null
- `data class StepsDiagnostics(val recordCount: Int, val sources: List<String>)`
- `suspend fun stepsDiagnostics(days: Int = 7): StepsDiagnostics`
  - 최근 days일(오늘 포함) 범위의 StepsRecord를 readRecords로 읽어 레코드 개수와 metadata.dataOrigin.packageName 중복 제거 목록
  - pageSize 1000, 첫 페이지만 읽으면 충분 (진단용)

## 3. StepsViewModel
- Loaded 상태에 `heartRate: HeartRateSummary`, `diagnostics: StepsDiagnostics` 필드 추가하고 refresh()에서 함께 불러온다.
- 기존 refreshJob 취소 / CancellationException 재던짐 구조 유지.

## 4. 화면 (MainActivity)
- 내용이 길어지므로 Loaded 화면 전체를 하나의 LazyColumn으로 스크롤되게 바꾼다 (기존 7일 목록 포함, Modifier.weight 제거).
- 순서: 오늘 걸음 카드 → 진단 줄 → 심박수 카드 → 최근 7일 걸음 목록 → 새로고침 버튼
- 진단 줄(작은 글씨, bodySmall): "Health Connect 걸음 기록: N건 · 출처: a, b" / 0건이면 "Health Connect 걸음 기록: 0건 (삼성헬스가 아직 걸음 수를 넘기지 않음)"
- 심박수 카드:
  - 제목 "심박수 (오늘)"
  - 큰 글씨: "최근 72 bpm" + 작은 글씨 시각 "오후 3:12" (Locale.KOREAN, 기기 시간대)
  - 한 줄: "평균 70 · 최저 55 · 최고 120 bpm"
  - 데이터 없으면 "오늘 심박수 기록이 없습니다."

## 완료 조건
- PowerShell: `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug` 성공
- tasks/step2a-heartrate-done.md 에 파일별 수정 요약(3~5줄)과 빌드 결과
