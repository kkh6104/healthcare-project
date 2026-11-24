# walk-ui 완료
- 하단 탭 5개(오늘/활동/걷기/수면/몸): 활동은 Icons.Default.Favorite, 걷기는 HealthIcons.Walk. 탭 인덱스·홈 타일 onOpenTab(걸음/심박→1, 수면→3, 체중→4) 재매핑. 매니페스트에 INTERNET 추가. 앱 시작(MainActivity.onCreate)에서 WalkTracker.restore 1회.
- ui/walk/WalkTab.kt: 쉬는 중엔 "걷기 시작"(권한 요청, 위치 거부 시 안내+설정 열기) + 이번 주 요약 + 최근 기록 목록(거리·시간·페이스·걸음, 상위 3개는 120dp RouteMap 썸네일); 기록 중엔 위쪽 60% RouteMap(followLast) + 거리·시간·페이스·걸음 + 일시정지/재개·종료(확인 다이얼로그). 시간은 서비스가 1초마다 갱신하는 activeMs를 그대로 표시.
- ui/walk/WalkDetail.kt: 전체 경로 지도 + 날짜·시작~끝·거리·활동시간·페이스·걸음 + 기록 삭제(확인 다이얼로그, CASCADE 삭제 후 그날 walkDistanceM 재계산). 상세는 앱바 뒤로가기/시스템 뒤로가기로 닫음. 걷기를 끝내면(알림에서 끝내도 앱이 열려 있으면) 해당 기록 상세로 이동. WalkDao.deleteSession 추가.
- 빌드: assembleDebug assembleDiag testDebugUnitTest 모두 BUILD SUCCESSFUL. 실기기 확인은 못 함(권한 창 흐름, 지도 표시, 종료 후 상세 이동, 삭제 등). 앱이 백그라운드일 때 종료되면 상세 자동 이동은 하지 않음.
