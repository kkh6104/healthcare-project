# 구현 요청 (claude-2): 걸음 상세 30일/90일 막대그래프가 안 보임

실기기: 걸음 "자세히" 화면에서 7일은 보이는데 30일·90일을 고르면 그래프가 보이지 않음.
- 원인을 코드로 찾는다 (ui/charts/BarChart.kt, ui/detail/StepsDetail.kt). 의심: 막대 수가 많을 때 막대 폭/간격 계산이 0 이하, 값 라벨/라벨 텍스트 측정이 폭을 다 차지, weight/Row 레이아웃 문제 등.
- 수정 방향: 막대가 많으면(예: 14개 초과) 막대 위 값 라벨 생략, x 라벨은 일정 간격만(예: 30일은 7일마다, 90일은 15일마다), 막대 최소 폭 1dp·간격 비율 축소. 필요하면 Canvas로 막대를 직접 그리기.
- 수정 허용: ui/charts/BarChart.kt, ui/detail/StepsDetail.kt (그리고 같은 BarChart를 쓰는 SleepHistoryDetail은 호출부 확인만, 수면 상세 파일은 claude-1이 수정 중이니 수정 금지).
- 원인을 tasks/fix-steps-chart-done.md 에 한 줄로 적을 것.
- 빌드: PowerShell `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug` (lock 시 30초 후 재시도)
