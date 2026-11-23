# 구현 요청 (claude-2): 삼성헬스 CSV 해석기

## 규칙
- **새 파일만 만든다** (claude-1이 동시에 data/HealthDao.kt, DailySummaryBuilder.kt, SyncRepository.kt 등을 수정 중). Android API(Context, Uri) 의존 없이 `java.io.Reader`를 받는 순수 Kotlin으로.
- 엔티티는 data/HealthRecordEntity.kt 그대로 사용 (RecordType, RecordSource.SAMSUNG_CSV).
- JVM 단위 테스트 작성: app/src/test/java/com/gh/healthapp/data/SamsungCsvParserTest.kt (합성 CSV 문자열로. **실제 개인 데이터 파일을 테스트에 넣지 말 것**). `.\gradlew.bat testDebugUnitTest` 통과.
- 빌드: PowerShell `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug testDebugUnitTest` (동시 빌드 lock 시 30초 후 재시도)

## 실제 파일 형식 (분석 결과, 반드시 반영)
- 1번째 줄: 메타데이터 (예: `com.samsung.health.weight,7013005,13`) → 건너뜀
- 2번째 줄: 헤더. **끝에 빈 컬럼**이 있을 수 있고, 컬럼명에 접두어가 붙을 수 있음 (예: `com.samsung.health.heart_rate.start_time`). → 컬럼은 **마지막 '.' 뒤 이름(접미사)** 으로 찾기.
- 값에 쉼표/따옴표가 있을 수 있으니 RFC4180 따옴표 처리하는 CSV 파서를 직접 구현(외부 라이브러리 추가 금지).
- 시각 문자열 `yyyy-MM-dd HH:mm:ss.SSS` 는 **UTC**. time_offset 컬럼(예: `UTC+0900`)이 있으면 zoneOffsetSec로 저장. 검증 예: 파일의 `2026-11-18 21:04:24.217` + UTC+0900 = 한국 2026-11-19 06:04.
- 2000년 이전 또는 (지금 + 1일) 이후 시각 행은 **버림** (실제 파일에 2092년, 2103년 같은 잘못된 심박 6건 있음). 버린 건수 집계.

## 파일별 매핑 (파일명 접두어 → 결과)
1. `com.samsung.shealth.tracker.pedometer_day_summary` → STEPS (하루 1행)
   - day_time 값(예 `2026-11-20 00:00:00.000`)의 **날짜 부분을 그대로 로컬 날짜로** 사용 (UTC 변환하지 않음).
   - 같은 날짜에 기기(deviceuuid)별 여러 행 → **step_count 최댓값 1개**만.
   - start = 그 날짜 기기 시간대 0시, end = 다음날 0시, value = 최댓값, unit "count", externalId = "day:yyyy-MM-dd", originPackage = source_package_name.
2. `com.samsung.shealth.tracker.heart_rate` → HEART_RATE (1시간 단위 행)
   - start_time/end_time(UTC), value = heart_rate, extraJson = {"min":min,"max":max} (값 있을 때), externalId = datauuid, originPackage = pkg_name.
3. `com.samsung.health.weight` → 한 행에서 최대 4개 레코드 (값이 비었으면 생략)
   - WEIGHT=weight(kg), BODY_FAT=body_fat(%), LEAN_BODY_MASS=fat_free_mass(kg), BODY_WATER_MASS=total_body_water(kg). start=end=start_time. externalId = "${datauuid}:${type}". originPackage = pkg_name (예 com.matin.android.atflee).
4. `com.samsung.shealth.sleep` + `com.samsung.health.sleep_stage` → SLEEP_SESSION
   - sleep 파일: start_time/end_time(UTC, 접미사로 찾기), datauuid. 
   - sleep_stage 파일: sleep_id(= sleep의 datauuid), start_time, end_time, stage 코드 (40001 깸, 40002 얕은, 40003 깊은, 40004 렘).
   - 단계가 있으면: deep/light/rem/awake 분 합계, value(잠든 분) = 전체 - awake. 없으면 value = (end-start) 분, 단계 생략.
   - extraJson = {"deep":..,"light":..,"rem":..,"awake":..} (null 생략), externalId = datauuid.
   - 시작~끝이 0분 이하이거나 24시간 초과면 버림.

## 공개 API (data/SamsungCsvParser.kt)
```kotlin
data class CsvImportResult(val records: List<HealthRecordEntity>, val countsByType: Map<String, Int>, val dropped: Int, val warnings: List<String>)
object SamsungCsvParser {
    // 파일명(확장자 포함)으로 어떤 파서인지 판별. 지원하지 않으면 null
    fun kindOf(fileName: String): CsvKind?
    fun parsePedometerDaySummary(reader: Reader, zone: ZoneId, now: Instant): CsvImportResult
    fun parseHeartRate(reader: Reader, now: Instant): CsvImportResult
    fun parseWeight(reader: Reader, now: Instant): CsvImportResult
    fun parseSleep(sleep: Reader, stages: Reader?, now: Instant): CsvImportResult
}
enum class CsvKind { PEDOMETER_DAY_SUMMARY, HEART_RATE, WEIGHT, SLEEP, SLEEP_STAGE }
```
- 모든 레코드 source = RecordSource.SAMSUNG_CSV.

## 완료
- tasks/step3b-csv-parser-done.md 에 요약 + 공개 API + 테스트 결과
