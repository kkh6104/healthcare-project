FAIL
1. OK — UTC 해석과 offset 저장, 연도 필터, 날짜별 걸음 최댓값 및 헤더 접미사 매칭을 확인했다.
2. NG — HeartRateRetention.kt:29-49: 이미 정리된 시간대에 새로운 원본이 들어오면 기존 요약을 합치지 않고 REPLACE해 이전 심박 집계가 사라진다. 90일 조건·원본 삭제·재정리 제외·500개 분할은 구현돼 있다.
3. NG — DailySummaryBuilder.kt:15-17,76: 기존 요약에 저장된 CSV 최댓값을 HC 값처럼 재사용하므로 CSV를 더 낮은 값으로 재가져와도 이전 값이 남아 현재 HC와 CSV의 최댓값이 아닐 수 있다.
4. NG — StepsViewModel.kt:165-172,305-316: 가져오기 시작 시 기존 refresh만 취소하므로 가져오는 중 새로고침 버튼이나 RESUMED가 새 동기화를 시작할 수 있다. 하위 폴더 선택과 동일 원본 ID의 중복 방지는 구현돼 있다.
5. OK — 지정된 JDK로 assembleDebug, assembleDiag, testDebugUnitTest 성공 (82개 작업 UP-TO-DATE).
