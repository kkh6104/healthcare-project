# 구현 요청 (claude-1): 걷기 2단계 - 탭·화면 연결
설계/계약: tasks/walk-design.md, API: tasks/walk-core-done.md, tasks/walk-map-done.md. ## 공통
- 빌드: PowerShell `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug` (동시 빌드 lock 시 30초 후 재시도). 라이브러리 버전은 maven 메타데이터로 최신 안정 버전 확인.
- 한국어, MaterialTheme 색만, 기존 동기화/가져오기 로직 변경 금지.

## 0. 매니페스트: INTERNET 권한 추가 (지도 타일).
## 1. 하단 탭 5개: 오늘 / 활동 / 걷기 / 수면 / 몸. 아이콘은 기존 HealthIcons.Walk를 걷기 탭에, 활동 탭은 Icons.Default 중 적당한 것(예: 막대 느낌이 없으면 Favorite 등). 기존 탭 인덱스/상세 화면/HomeTab onOpenTab 인덱스 매핑을 새 순서에 맞게 수정.
## 2. ui/walk/WalkTab.kt
- IDLE: 큰 "걷기 시작" 버튼(권한 확인: WalkPermissions.missing → RequestMultiplePermissions, 위치 거부 시 안내 + 설정 열기 버튼) + "이번 주 N회 · N.N km" 요약 + 최근 기록 목록(날짜·시작시각, 거리 km 소수2, 시간, 평균 페이스 "분'초"/km, 걸음; 작은 RouteMap(interactive=false, height 120dp) 썸네일은 상위 3개만).
- RUNNING/PAUSED: RouteMap(followLast=true) 화면 위쪽 60% + 아래 통계(거리 크게, 시간 mm:ss/ h:mm:ss, 페이스, 걸음) + 버튼(일시정지↔재개, 종료(확인 다이얼로그)). 1초마다 시간 표시 갱신(startedAt/activeMs 기준, 상태가 RUNNING일 때만 증가).
- 종료 후 해당 기록 상세로 이동.
## 3. ui/walk/WalkDetail.kt: RouteMap(전체 경로) + 날짜, 시작~끝, 거리, 활동시간, 페이스, 걸음 + "기록 삭제"(확인 다이얼로그, CASCADE). 뒤로가기(아이콘+시스템).
## 4. 앱 시작 시 WalkTracker.restore(context) 1회 (MainActivity onCreate 또는 ViewModel init).
## 5. 홈 타일에 걷기 정보는 이번엔 넣지 않음.
## 완료: assembleDebug assembleDiag testDebugUnitTest 성공, tasks/walk-ui-done.md 3~5줄
