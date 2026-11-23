# 구현 요청 (claude-1): 수면·몸무게 연결

이미 만들어진 모듈: SleepRepository.kt/SleepSection.kt (tasks/step2b-sleep-done.md), WeightRepository.kt/WeightSection.kt (tasks/step2b-weight-done.md). 이 4개 파일은 수정하지 말고 사용만 한다.

## 수정 파일
- AndroidManifest.xml: `android.permission.health.READ_SLEEP`, `android.permission.health.READ_WEIGHT` 추가
- HealthConnectManager.kt:
  - PERMISSIONS에 SleepSessionRecord, WeightRecord 읽기 권한 추가
  - `val sleep by lazy { SleepRepository(client) }`, `val weight by lazy { WeightRepository(client) }` 추가 (client는 private 유지)
- StepsViewModel.kt: Loaded에 `sleep: SleepSummary`, `weight: WeightSummary` 추가, refresh()에서 함께 조회 (기존 refreshJob/CancellationException 구조 유지)
- MainActivity.kt:
  - 화면 제목을 "걸음 수" → "내 건강 데이터"
  - Loaded LazyColumn 순서: 오늘 걸음 카드 → 진단 줄 → 심박수 카드 → **SleepCard** → **WeightCard** → 최근 7일 걸음 목록 → 새로고침
  - NeedPermission 안내 문구: "걸음 수, 심박수, 수면, 몸무게를 읽으려면 Health Connect 권한이 필요합니다."
  - PrivacyPolicyActivity 안내 문구도 "걸음 수, 심박수, 수면, 몸무게 데이터"로 수정

## 완료 조건
- assembleDebug 성공 (JAVA_HOME = Android Studio jbr), tasks/step2b-integrate-done.md 에 요약 3~5줄
