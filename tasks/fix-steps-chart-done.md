# fix-steps-chart 완료
- 원인: BarChart가 막대마다 고정 좌우 여백 6dp(합 12dp)를 썼는데, 30/90일은 칸 폭이 그보다 좁아 막대 폭이 0 이하가 되어 안 보였음(값 라벨 글자도 좁은 칸에서 깨짐).
- 수정: 막대 수에 따라 간격 축소(8개 이하 6dp, 14개 이하 3dp, 그 초과 0.5dp), 14개 초과 시 막대 위 값 라벨 생략, x 라벨은 칸 폭을 넘어도 잘리지 않게 중앙 배치(softWrap 끔). StepsDetail의 30일 라벨 간격을 7일로 변경(90일은 15일 유지).
- 수정 파일: ui/charts/BarChart.kt, ui/detail/StepsDetail.kt. SleepDetail은 호출만 확인(수정 없음; 같은 BarChart를 쓰므로 함께 개선됨). assembleDebug 성공, 실기기 확인은 안 함.
