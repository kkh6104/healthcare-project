# 검수 (중간, 최소 로직 검사, 코드 수정 금지)
명세: tasks/step3b-csv-parser.md, step3b-retention.md, step3b-import.md
핵심만:
1. CSV 시각 UTC 해석 + time_offset, 잘못된 연도 필터, 걸음 하루 최댓값, 헤더 접미사 매칭
2. 심박 정리: 90일 이전 원본만, 정리 후 원본 삭제, 이미 정리된 행 재정리 안 함, 500개씩 나눠 삭제/저장
3. 하루 요약: 출처 우선순위(HC 우선), 걸음 = max(HC, CSV)
4. 가져오기: 하위 samsunghealth_ 폴더 선택, 재가져오기 시 중복 없음, 동기화와 동시 실행 방지, 로그에 개인 수치 없음
5. assembleDebug, assembleDiag, testDebugUnitTest 성공
사소한 지적 금지. 결과: tasks/review-step3b-result.md 첫 줄 PASS/FAIL, 10줄 이내. FAIL 항목은 실제로 생기는 상황 한 줄.
