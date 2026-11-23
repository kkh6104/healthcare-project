# 검수 요청 (중간, 최소 로직 검사): 1단계 수정분

전체 검사 금지. 아래 항목만 확인하고 코드는 수정하지 않는다. 웹 검색 불필요.

수정 명세: tasks/fix-step1.md / 구현 요약: tasks/fix-step1-done.md

## 확인할 것 (각각 OK/NG 한 줄)
1. 빌드: PowerShell `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug` 성공 여부
2. 명세 4개 항목이 그대로 구현됐는지 (Availability 3상태 매핑과 화면 문구 / refreshJob 취소 + CancellationException 재던짐 / openPlayStore 3단계 폴백 / 오늘 값 = dailySteps 첫 항목)
3. 수정으로 새로 생긴 로직 오류나 크래시 가능성이 있는지 (있을 때만 파일:줄 + 한 줄 설명)

## 결과
tasks/review-fix-step1-result.md 에 첫 줄 PASS 또는 FAIL, 그 아래 위 3항목 결과. 10줄 이내.
