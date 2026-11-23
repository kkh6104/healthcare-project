# fix-step3 완료
1. SyncRepository: syncedUntil은 레코드 조회·저장(STEPS는 stepsDaily 집계까지)이 모두 성공한 뒤에만 갱신. 중간 예외는 갱신 없이 그대로 던짐.
2. HcLog.kt: PagedResult(records, truncated)와 readAllPagesResult 추가(기존 readAllPages는 이를 호출, 시그니처 유지). SyncRepository는 ascendingOrder=true로 읽고, truncated면 받은 레코드 중 가장 늦은 endTime을 syncedUntil로 저장 + Log.w. SyncResult.truncated 추가, ViewModel 배너에 "OO: 데이터가 많아 일부만 받았습니다. 새로고침하면 이어서 받습니다." 추가.
3. StepsViewModel: SecurityException → NeedPermission 전에 그때까지 성공한 종류로 summaryBuilder.rebuild 실행(실패는 로그만).
4. StepsViewModel: refresh가 화면을 Loading으로 바꾸지 않음. init의 DB 로드는 상태가 Loading일 때 항상 반영. 권한 화면 등 Loaded가 아닐 때 동기화 시작 전에 DB 내용을 먼저 표시(빈 DB면 빈 카드 + "동기화 중…").
- 빌드: assembleDebug assembleDiag 모두 BUILD SUCCESSFUL (실기기 미확인)
