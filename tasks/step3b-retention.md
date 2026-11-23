# 구현 요청 (claude-1): 심박 1시간 정리 + 하루 요약 규칙

## 규칙
- 수정 허용: data/HealthDao.kt, data/DailySummaryBuilder.kt, data/LocalHealthReader.kt, data/SyncRepository.kt, StepsViewModel.kt, 새 파일. **data/SamsungCsvParser.kt 및 그 테스트는 claude-2가 만드는 중이니 건드리지 말 것.**
- DB 스키마(엔티티 컬럼) 변경 없음 (version 1 유지).

## 1. 심박 1시간 정리 (새 파일 data/HeartRateRetention.kt)
- `class HeartRateRetention(private val dao: HealthDao)` / `suspend fun run(now: Instant = Instant.now(), keepDays: Long = 90): Int` (정리한 원본 수 반환)
- 대상: type=HEART_RATE, startTime < (now - 90일), **extraJson에 "n"이 없는 행(=아직 정리 안 된 원본)**.
- (source, 시각의 기기 시간대 기준 1시간 구간)별로 묶어 1행 생성: start=구간 시작, end=구간 끝, value=평균, extraJson={"min":..,"max":..,"n":개수}, externalId="hourly:${source}:${구간시작 epoch ms}", originPackage=그룹 내 첫 값.
  - 원본에 extraJson min/max가 있으면(CSV의 1시간 행) 그 값도 min/max 계산에 반영.
- 하나의 트랜잭션에서: 묶음 행 upsert → 원본 삭제. DAO에 필요한 쿼리 추가(@Transaction 사용).
- ViewModel: refresh(동기화) 끝에 1회 실행, 로그 `retention: rolled=<n>`.

## 2. 하루 요약 규칙 (DailySummaryBuilder)
- 출처 우선순위: 날짜·지표마다 **source=health_connect 행이 있으면 그것만**, 없으면 samsung_csv 행 사용 (심박, 수면, 체중, 체성분 모두).
- 심박: 행의 extraJson에 min/max/n 있으면 활용. avg는 n 가중 평균(n 없으면 1로), min/max는 행의 min/max(없으면 value).
- 걸음: stepsByDate(HC aggregate)와 그 날짜 STEPS 중 source=samsung_csv 행의 value를 비교해 **큰 값**. stepsByDate에 없는 날짜도 CSV 행이 있으면 그 값 (HC는 과거 날짜를 0으로 주기 때문).
- `suspend fun rebuildAll()`: DB에 기록이 있는 모든 날짜를 다시 계산 (CSV 가져오기 후 사용). 날짜 목록은 DAO 쿼리로(type별 min/max startTime 등).

## 3. LocalHealthReader 반영
- heartRateToday / 그 외 원본을 직접 읽는 곳도 같은 출처 우선순위 적용 (오늘은 HC 행이 있으면 HC만).
- 심박 최근값은 원본(정리 안 된) 행 기준.

## 완료
- PowerShell: `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug` 성공 (동시 빌드 lock 시 30초 후 재시도)
- tasks/step3b-retention-done.md 에 요약 + 추가한 DAO 함수 시그니처
