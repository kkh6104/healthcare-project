# 구현 요청 (claude-1): 요청 한도 초과 원인 진단

## 배경
수면·몸무게 추가 후부터 Health Connect "rate limited request quota has been exceeded" 오류가 계속 발생 (10분 이상 기다린 뒤 1회 요청해도 발생). 심박수까지만 있을 때는 정상이었다. 어느 호출이 원인인지 확인하려고 한다.

## 수정 파일
- StepsViewModel.kt, MainActivity.kt, HealthConnectManager.kt, 새 파일 HcLog.kt 허용. SleepRepository.kt/WeightRepository.kt는 **로깅 래핑 외에는** 로직 변경 금지.

## 1. 호출 로깅 (HcLog.kt)
- `suspend fun <T> hcCall(name: String, count: (T) -> Int? = { null }, block: suspend () -> T): T`
  - 시작/끝을 Log.i("HCDEBUG", ...)로 기록: `[name] ok 123ms count=45` 또는 `[name] FAIL 12ms ExceptionClass: message` (cause 체인 첫 2개 클래스명도 포함). 실패 시 예외를 그대로 다시 던짐.
- **모든 Health Connect 호출**(getGrantedPermissions, aggregate, aggregateGroupByPeriod, readRecords 등. Sleep/Weight Repository 안의 readRecords 포함)을 hcCall로 감싼다. readRecords는 count에 records.size.
- refresh 시작 시 `Log.i("HCDEBUG", "=== refresh start ===")`.

## 2. 섹션별 독립 로딩
- Loaded 상태의 각 섹션(걸음 7일, 심박, 수면, 몸무게)을 `Section<T>` 형태(성공 데이터 또는 실패 메시지)로 바꾸고, refresh에서 **순서대로 하나씩**(동시 실행 금지) 불러오되 하나가 실패해도 다음 섹션을 계속 시도한다.
  - 단, 한도 초과(quota/rate limit) 오류가 나면 그 즉시 남은 섹션은 시도하지 않고 "건너뜀(한도 초과)"로 표시 + 기존 15분 쿨다운 시작.
- 화면: 실패한 섹션 카드 자리에 "OO 불러오기 실패: 메시지"를 작은 빨간 글씨로. 성공한 섹션은 기존 카드 그대로.
- 기존 동작(권한 확인 기억, 보관 데이터 유지+배너, 쿨다운, 진단 버튼, refreshIfStale 5분)은 유지. 섹션 구조에 맞게만 조정.

## 완료 조건
- assembleDebug 성공 (PowerShell, JAVA_HOME = C:\Program Files\Android\Android Studio\jbr)
- tasks/diag-ratelimit-done.md 에 요약 3~5줄 + 감싼 호출 이름 목록
