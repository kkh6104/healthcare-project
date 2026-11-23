# 구현 요청 (claude-1): 3단계 검수 지적 4건 수정

검수 원문: tasks/review-step3-result.md

## 1. syncedUntil 갱신 시점 (SyncRepository)
- syncedUntil은 **그 종류의 모든 작업(레코드 조회+저장, STEPS는 stepsDaily 집계까지)이 성공한 뒤에만** 갱신. 중간에 예외가 나면 갱신하지 않고 예외를 그대로 던진다.

## 2. 5페이지 제한에 걸렸을 때 (HcLog.kt readAllPages + SyncRepository)
- readAllPages가 "다 못 읽었는지"를 알려 주도록: 새 함수 `readAllPagesResult(...)`(같은 인자) → `data class PagedResult<T>(val records: List<T>, val truncated: Boolean)` 추가. 기존 readAllPages는 내부적으로 이걸 써서 records만 반환(시그니처 유지).
- SyncRepository는 readAllPagesResult 사용. 요청은 ascendingOrder = true(오래된 것부터).
  - truncated=false → 기존대로 syncedUntil = now
  - truncated=true → syncedUntil = **받은 레코드 중 가장 늦은 시각**(순간 측정은 time, 구간은 endTime, 심박은 마지막 샘플 time). 다음 동기화가 거기서(-1일 겹침) 이어서 받음. Log.w("HCDEBUG", "sync(<type>) truncated, syncedUntil=<시각>") 기록.
  - SyncResult에 `truncated: Boolean` 추가. ViewModel은 truncated 종류가 있으면 배너에 "OO: 데이터가 많아 일부만 받았습니다. 새로고침하면 이어서 받습니다." 추가.

## 3. 권한 예외 시에도 요약 재계산 (StepsViewModel)
- SecurityException으로 NeedPermission으로 가기 전에, 그때까지 성공한 종류의 summaryBuilder.rebuild(...)를 먼저 실행(그 자체 실패는 로그만).

## 4. 시작 직후 로딩 화면 방지 (StepsViewModel)
- refresh는 **어떤 경우에도 화면을 Loading으로 바꾸지 않는다** (이미 Loaded/초기 DB 표시가 있으면 유지). 동기화 진행 표시는 기존 "동기화 중…" 텍스트로만.
- init의 DB 초기 로드 결과가 refresh 진행 여부와 상관없이 반드시 화면에 반영되게 (refresh가 이를 덮어쓰거나 버리지 않게).
- 앱 최초 실행 등 DB가 비어 있어도 Loading이 아니라 빈 카드 + "동기화 중…".

## 완료
- PowerShell: `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug assembleDiag` 둘 다 성공
- tasks/fix-step3-done.md 에 항목별 1~2줄
