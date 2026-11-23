# ui-icons 완료
- 새 파일 ui/icons/HealthIcons.kt: HealthIcons.Walk / Sleep / Weight (ImageVector). 정확한 Google Material Icons path 데이터를 확신할 수 없어 세 개 모두 **직접 그림**(걷는 사람 실루엣, 초승달, 체중계) — 파일 상단 주석에 명시. material-icons-extended 미사용.
- MainActivity의 NavigationBar 활동/수면/몸 아이콘을 위 3개로 교체(오늘은 Home 유지). ActivityTab/SleepTab/BodyTab은 건드리지 않음.
- 빌드: assembleDebug BUILD SUCCESSFUL. 화면에서 직접 확인하지는 못했으므로 특히 초승달(arcTo 방향)과 체중계 모양은 실기기에서 눈으로 확인 필요.
