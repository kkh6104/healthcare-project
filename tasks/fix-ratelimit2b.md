# 구현 요청 (claude-2): 쿨다운 시작 시 진행 중 조회 취소
검수 지적(tasks/review-fix-ratelimit2-result.md 4번): 쿨다운이 시작되는 지점(refresh든 진단이든)에서 진행 중인 refreshJob과 진단 Job을 모두 cancel()하고, 쿨다운 중에는 성공 결과가 늦게 도착해도 상태를 덮어쓰지 않게 한다(결과 반영 직전에 쿨다운 여부 확인). StepsViewModel.kt만 수정. assembleDebug 성공 후 tasks/fix-ratelimit2b-done.md 에 1~2줄.
