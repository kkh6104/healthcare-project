# step3-integrate 완료
- HealthConnectManager: dao(private lazy), syncRepo, summaryBuilder, local(LocalHealthReader) 추가. 기존 직접 조회 함수/Repository는 삭제하지 않음(ViewModel에서만 호출 중단), stepsDiagnostics 유지.
- StepsViewModel: 화면 데이터는 항상 LocalHealthReader(DB)로 생성. init에서 DB 내용으로 즉시 Loaded 표시. refresh는 권한 확인 → 8종 순차 sync(섹션 로그 sync:<TYPE>, fetched/dates) → 성공분 summaryBuilder.rebuild 1회 → DB 재조회 → 실패/건너뜀은 "동기화 실패: 수면(메시지), 골질량(건너뜀)" 배너. 한도 초과 시 즉시 중단 + 남은 종류 skipped + 15분 쿨다운. refreshIfStale은 DB 마지막 동기화 시각(lastSyncFlow) 5분 기준, 진행 중이면 건너뜀.
- MainActivity: 제목 아래 "마지막 동기화: 오후 h:mm" / "아직 동기화하지 않았습니다" / "동기화 중…" 표시. 카드 구성·순서는 그대로.
- 참고: Loaded의 Section 타입과 화면의 섹션별 실패 분기 코드는 남겨 두었으나 DB 읽기는 항상 Ok라 실제로는 배너만 표시됨. 권한 부족/Unavailable 화면은 DB 데이터가 있어도 기존처럼 표시.
- 빌드: assembleDebug assembleDiag 모두 BUILD SUCCESSFUL (실기기 미확인)
