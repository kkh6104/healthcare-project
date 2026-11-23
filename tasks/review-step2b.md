# 검수 요청 (중간, 최소 로직 검사): 수면·몸무게

전체 검사 금지. 코드 수정 금지. 웹 검색 불필요.
명세: tasks/step2b-sleep.md, tasks/step2b-weight.md, tasks/step2b-integrate.md / 요약: 각 *-done.md

1. assembleDebug 성공 여부 (PowerShell, JAVA_HOME = C:\Program Files\Android\Android Studio\jbr)
2. 핵심 로직만: 수면 asleep 계산(깨어있음 단계 차감, 깬 날짜 기준 그룹, lastNight 36시간 조건) / 몸무게 날짜별 마지막 측정·changeKg 7일 기준 / 권한 2개가 매니페스트와 PERMISSIONS 모두에 있는지
3. 새 로직 오류·크래시 가능성 (있을 때만 파일:줄 + 한 줄)

결과: tasks/review-step2b-result.md, 첫 줄 PASS 또는 FAIL, 10줄 이내.
