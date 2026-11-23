# 검수 (중간, 최소, 코드 수정 금지)
명세: tasks/ui-shell.md, ui-charts.md, ui-tabs.md, ui-icons.md
1. 데이터 로직(StepsViewModel 동기화/가져오기/DB)이 바뀌지 않았는지 (화면만 변경)
2. 그래프가 빈 데이터/값 1개/모두 0일 때 크래시 없이 "데이터 없음" 처리하는지 (0으로 나누기 등)
3. 탭 전환·설정 진입/뒤로가기, 설정의 새로고침/가져오기/진단 버튼이 기존 동작에 연결됐는지
4. material-icons-extended 미사용
5. assembleDebug, assembleDiag 성공
사소한 지적(디자인 취향) 금지. 결과: tasks/review-ui-result.md 첫 줄 PASS/FAIL, 7줄 이내. FAIL 항목은 실제로 생기는 상황 한 줄.
