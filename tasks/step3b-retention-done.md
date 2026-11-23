# step3b-retention 완료
- 새 파일 data/HeartRateRetention.kt: run(now, keepDays=90)이 90일 지난 심박 원본(extraJson에 "n" 없음)을 (출처, 기기 시간대 1시간)별 1행으로 묶음(avg/min/max/n, externalId="hourly:<source>:<구간시작ms>") → 한 트랜잭션에서 upsert 후 원본 삭제, 정리한 원본 수 반환. 순수 함수 rollUpHourly/extraNumber는 internal. ViewModel은 동기화 끝에 1회 실행하고 "retention: rolled=n" 로그(실패해도 동기화 결과엔 영향 없음). HealthConnectManager.retention 추가.
- DailySummaryBuilder: 날짜·지표별로 health_connect 행이 있으면 그것만, 없으면 samsung_csv 사용(심박/수면/체중/체성분). 심박은 n 가중 평균, min/max는 행의 min/max 반영. 걸음은 stepsByDate(없으면 기존 값)와 그날 CSV 걸음 합 중 큰 값(buildDailySummary에 기본값 있는 csvSteps 파라미터 추가). rebuildAll() 추가(DB 기록의 최소~최대 날짜 전체 재계산).
- LocalHealthReader: heartRateToday/수면/몸무게/체성분에 같은 출처 우선순위(날짜별) 적용, 심박 최근값은 선택된 행 기준.
- DB 스키마 변경 없음(version 1). 빌드: assembleDebug BUILD SUCCESSFUL (단위 테스트 미작성, 첫 빌드는 DAO 쿼리 문자열 이스케이프 오류로 실패 후 수정)

## 추가한 DAO 함수 (HealthDao)
- suspend fun rawRecordsBefore(type: String, before: Long): List<HealthRecordEntity>
- suspend fun deleteRecordsByIds(ids: List<Long>)
- @Transaction suspend fun replaceRecords(upserts: List<HealthRecordEntity>, deleteIds: List<Long>)
- suspend fun minRecordTime(): Long?
- suspend fun maxRecordTime(): Long?
