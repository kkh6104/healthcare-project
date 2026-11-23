# 구현 요청 (claude-1): UI-2 탭 아이콘

## 공통
- 데이터 로직(ViewModel의 동기화/가져오기/DB 읽기)은 바꾸지 않는다. 화면만.
- 외부 차트/내비게이션 라이브러리 추가 금지 (Compose Canvas, 상태 기반 탭).
- 한국어, Locale.KOREAN, MaterialTheme 색/타이포 사용 (다크 모드에서도 보이게 하드코딩 색 금지).
- 빌드: PowerShell `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug` (동시 빌드 lock 시 30초 후 재시도)

- 수정 허용: MainActivity.kt, 새 파일 ui/icons/HealthIcons.kt. **ui/ActivityTab.kt, SleepTab.kt, BodyTab.kt는 claude-2가 동시에 수정 중이니 건드리지 말 것.**
- material-icons-extended 추가 금지.
- ui/icons/HealthIcons.kt 에 ImageVector 3개를 직접 정의: `HealthIcons.Walk`(Material Icons "DirectionsWalk"), `HealthIcons.Sleep`("Bedtime"), `HealthIcons.Weight`("MonitorWeight"). Google Material Icons(Apache License 2.0)의 24dp path 데이터를 사용하고 파일 상단에 출처/라이선스 주석. path 데이터를 정확히 기억하지 못하면 단순한 대체 도형(걷는 사람 실루엣/초승달/체중계)을 직접 그려도 됨 — 그 경우 주석에 "직접 그림"이라고 명시.
- NavigationBar의 활동/수면/몸 아이콘을 위 3개로 교체 (오늘은 Home 유지).

## 완료
- assembleDebug 성공, tasks/ui-icons-done.md 에 1~2줄 (path 출처 명시)
