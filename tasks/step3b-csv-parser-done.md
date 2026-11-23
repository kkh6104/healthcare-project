# step3b-csv-parser 완료
- 새 파일: data/SamsungCsvParser.kt, app/src/test/java/com/gh/healthapp/data/SamsungCsvParserTest.kt (기존 파일 수정 없음).
- Android 의존 없는 순수 Kotlin. RFC4180 CSV 직접 구현, 1행 메타 건너뛰기, 헤더는 마지막 '.' 뒤 접미사로 조회(끝 빈 컬럼 무시), 시각은 UTC로 해석하고 time_offset을 zoneOffsetSec에 저장, 2000년 이전/지금+1일 이후는 버리고 dropped 집계.
- extraJson은 org.json 대신 문자열로 직접 생성(JVM 단위 테스트에서 org.json이 동작하지 않기 때문).
- 테스트: assembleDebug testDebugUnitTest 성공 (합성 CSV 6건: kindOf, 따옴표, 심박 잘못된 연도, 걸음 최댓값, 체중 다중 레코드, 수면 단계).

## 공개 API
- enum class CsvKind { PEDOMETER_DAY_SUMMARY, HEART_RATE, WEIGHT, SLEEP, SLEEP_STAGE }
- data class CsvImportResult(records: List<HealthRecordEntity>, countsByType: Map<String, Int>, dropped: Int, warnings: List<String>)
- object SamsungCsvParser {
  fun kindOf(fileName: String): CsvKind?
  fun parsePedometerDaySummary(reader: Reader, zone: ZoneId, now: Instant): CsvImportResult
  fun parseHeartRate(reader: Reader, now: Instant): CsvImportResult
  fun parseWeight(reader: Reader, now: Instant): CsvImportResult
  fun parseSleep(sleep: Reader, stages: Reader?, now: Instant): CsvImportResult }
