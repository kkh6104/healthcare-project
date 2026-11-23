# 구현 요청 (claude-1): 3단계-3 저장 구조 연결

사용할 것 (수정 금지): data/ 패키지 (HealthDatabase, HealthDao, SyncRepository, DailySummaryBuilder, LocalHealthReader). 설계 tasks/step3-design.md, API는 tasks/step3-db-done.md / step3-sync-done.md / step3-local-done.md.

## 1. HealthConnectManager
- `private val dao by lazy { HealthDatabase.get(context).dao() }`
- `val syncRepo by lazy { SyncRepository(client, dao) }`, `val summaryBuilder by lazy { DailySummaryBuilder(dao) }`, `val local by lazy { LocalHealthReader(dao) }`

## 2. StepsViewModel — 화면은 DB, Health Connect는 동기화만
- 화면 데이터(Loaded)는 **항상 LocalHealthReader로 DB에서 읽어** 만든다: stepsDaily(7) / heartRateToday() / sleep(7) / weight(30) / body(4종). 오늘 걸음 카드 = stepsDaily 첫 항목.
- ViewModel 생성 시(init): 권한·동기화와 무관하게 **DB 내용으로 즉시 Loaded 표시** (DB가 비어 있으면 빈 카드 상태 그대로).
- refresh(reason):
  1) 권한 확인(기존 기억 로직 유지). 권한 부족 → NeedPermission (DB 데이터가 있어도 동일하게 권한 화면).
  2) **순차 동기화**: STEPS → HEART_RATE → SLEEP_SESSION → WEIGHT → BODY_FAT → BONE_MASS → LEAN_BODY_MASS → BODY_WATER_MASS 각각 syncRepo.sync(type). 섹션 로그 이름 `sync:<TYPE>`, 요약 `fetched=n dates=m`.
     - 일반 실패: 다음 종류 계속, 실패 목록에 기록.
     - 한도 초과: 즉시 중단 + 남은 종류 skipped + 기존 15분 쿨다운.
  3) 성공한 종류들의 affectedDates 합집합과 stepsByDate로 summaryBuilder.rebuild(...) 1회 (실패해도 이미 성공한 부분은 반영).
  4) DB에서 다시 읽어 Loaded 갱신. 실패/건너뜀이 있으면 배너: "동기화 실패: 수면(메시지), 골질량(건너뜀)".
- refreshIfStale: 기준을 메모리 변수가 아니라 **DB의 마지막 동기화 시각(lastSyncFlow 최댓값)** 으로: 5분 안 지났으면 건너뜀. 진행 중이면 건너뜀(기존 유지).
- 기존 로그(refresh start/end, calls, section, cooldown, ignored) 형식 유지.
- 기존 직접 조회 경로(HealthConnectManager의 dailySteps/heartRateToday, SleepRepository/WeightRepository/BodyCompositionRepository.summary)는 ViewModel에서 더 이상 호출하지 않는다. **파일/함수는 삭제하지 않는다.** 걸음 진단 버튼(stepsDiagnostics)은 그대로 유지.

## 3. MainActivity
- 제목 아래 작은 글씨: "마지막 동기화: 오후 11:08" (lastSyncFlow, Locale.KOREAN) / 없으면 "아직 동기화하지 않았습니다". 동기화 중이면 "동기화 중…".
- 카드 구성/순서는 기존과 동일. 섹션별 빨간 실패 줄 대신 위 배너 하나로 표시.

## 완료
- PowerShell: `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug assembleDiag` 둘 다 성공
- tasks/step3-integrate-done.md 에 3~5줄
