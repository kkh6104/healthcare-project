# fix-ratelimit2b 완료
- 쿨다운 시작 시(refresh/진단 어디서든) refreshJob과 diagJob을 모두 cancel하고, 결과 반영 직전에 쿨다운 여부를 확인해 쿨다운 중이면 상태를 덮어쓰지 않음. StepsViewModel.kt만 수정, assembleDebug 성공.
