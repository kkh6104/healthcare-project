# 최종 검수 때 다시 볼 항목 (중간 검수에서 넘긴 것)
- [ratelimit] 오류 원인 체인 10단계 제한 (의도된 무한루프 방지)
- [diag-ratelimit] getSdkStatus/getOrCreate 로그 미적용, 진단 Job 쿨다운 시 미취소
- [step2c] refresh calls 카운터에 진단 호출 섞임
- [step3] 1일 겹침 범위 내 5,000건 초과 시 진행 불가 / 권한 없을 때 저장 카드 대신 권한 화면(설계대로)
- [step3b] 이미 정리된 시간대에 같은 출처의 새 원본이 오면 요약 REPLACE (현재 경로상 발생 거의 없음)
- [step3b] 걸음 CSV를 더 낮은 값으로 재가져오기 시 이전 값 유지
- [step3b] 가져오기 중 RESUMED로 동기화가 동시에 시작될 수 있음 (버튼은 비활성)
