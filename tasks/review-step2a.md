# 검수 요청 (중간, 최소 로직 검사): 심박수 추가 + 걸음 진단

전체 검사 금지. 아래만 확인하고 코드는 수정하지 않는다. 웹 검색 불필요.
명세: tasks/step2a-heartrate.md / 구현 요약: tasks/step2a-heartrate-done.md

1. 빌드: PowerShell `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug` 성공 여부
2. 명세 항목 구현 여부 (READ_HEART_RATE 매니페스트+PERMISSIONS / heartRateToday 집계·최근값 / stepsDiagnostics / Loaded 필드 / 화면 순서·문구, LazyColumn 하나로 스크롤)
3. 새로 생긴 로직 오류·크래시 가능성 (있을 때만 파일:줄 + 한 줄)

결과: tasks/review-step2a-result.md, 첫 줄 PASS 또는 FAIL, 10줄 이내.
