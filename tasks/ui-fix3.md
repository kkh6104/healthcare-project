# 구현 요청 (claude-2): 화면 수정 3가지
## 공통
- 데이터 동기화/가져오기 로직은 바꾸지 않는다. 외부 라이브러리 추가 금지. MaterialTheme 색만 사용(하드코딩 색 금지). 한국어, Locale.KOREAN.
- 빌드: PowerShell `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug` (동시 빌드 lock 시 30초 후 재시도)

- 수정 허용: **ui/charts/ 폴더 파일만** (claude-1이 탭/상세 화면을 동시에 수정 중).
1. BarChart: goal 라벨 "목표"가 막대 값 글자와 겹침 → 라벨을 차트 오른쪽 바깥 여백(차트 영역 밖, 막대 영역 폭에서 제외)에 그리거나, 막대 값 글자와 겹치면 목표선 아래쪽으로 옮긴다. 막대 값 글자도 목표선과 겹치면 막대 안쪽 위(onPrimary 색)로.
2. SleepColors.light(): 너무 연해서 안 보임 → primaryContainer 대신 secondary 계열 등 배경(surfaceVariant) 대비가 충분한 색. 4색이 서로 구분되게.
3. LineChart: y 범위가 너무 좁아 변화가 과장됨 → 선택 파라미터 `minYSpan: Float? = null` 추가. 값 범위가 minYSpan보다 작으면 중앙 기준으로 minYSpan만큼 넓힘. 또 상하 10% 여백. 기본값 null이면 여백만 적용. (몸 탭에서 체중 minYSpan=6f, 체지방률 10f로 쓸 예정 — 그 호출부 변경은 claude-1이 함)
## 완료: assembleDebug 성공, tasks/ui-fix3-done.md 에 2~3줄
