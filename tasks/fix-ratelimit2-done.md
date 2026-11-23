# fix-ratelimit2 완료
- 호출 감소: 걸음 진단은 refresh에서 제외(버튼 "걸음 기록 진단"을 누를 때만 1회 조회, vm.diagnostics). 권한 확인은 한 번 확인되면 기억하고, SecurityException 시 기억 삭제 후 NeedPermission.
- 데이터 유지: 마지막 Loaded 보관, 실패 시 Loaded 유지 + 맨 위 errorContainer 배너(Loaded.banner). 보관 데이터가 없을 때만 Error 화면. 보관 데이터가 있으면 refresh 중 Loading 전환 없음.
- 쿨다운: quota/rate limit 오류 시 15분(elapsedRealtime) 동안 refresh/refreshIfStale/진단 조회 무시, 메시지에 남은 분(올림) 표시.
- 수정 파일: StepsViewModel.kt, MainActivity.kt (HealthConnectManager.kt는 변경 불필요해 수정 안 함). assembleDebug 성공.
