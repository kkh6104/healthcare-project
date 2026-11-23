# step2a-heartrate 완료

## AndroidManifest.xml
- READ_HEART_RATE 권한 추가.

## HealthConnectManager.kt
- PERMISSIONS에 HeartRateRecord 읽기 권한 추가.
- HeartRateSummary, StepsDiagnostics 데이터 클래스 추가.
- heartRateToday(): 오늘 평균/최저/최고(aggregate) + 최근 샘플(readRecords, 내림차순 1건).
- stepsDiagnostics(days): 최근 days일 걸음 레코드 수와 출처 패키지(중복 제거).

## StepsViewModel.kt
- Loaded에 heartRate, diagnostics 추가, refresh()에서 함께 조회 (refreshJob/CancellationException 구조 유지).

## MainActivity.kt
- Loaded 화면을 단일 LazyColumn으로 변경(weight 제거).
- 순서: 오늘 카드 → 진단 줄 → 심박수 카드 → 최근 7일 → 새로고침 버튼.

## 빌드
assembleDebug: BUILD SUCCESSFUL (실기기 실행은 미확인)
