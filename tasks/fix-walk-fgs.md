# 구현 요청 (claude-1): WalkService 권한 방어
검수 지적: Android 14+에서 위치 권한 없이 서비스가 시작되면 startForeground(location)에서 SecurityException.
- WalkService 시작 처리에서 WalkPermissions.hasLocation 확인 → 없으면 세션 만들지 않고 stopSelf + WalkTracker 상태 IDLE.
- startForeground 호출을 try/catch(SecurityException, ForegroundServiceStartNotAllowedException(API31+ 클래스는 SDK 체크))로 감싸 실패 시 안전 종료 + Log.w("HCDEBUG","walk fgs failed: ...").
- WalkTracker.start도 권한 없으면 서비스 시작하지 않고 false 반환하도록 (시그니처 Boolean 반환으로 변경 가능, 호출부 수정).
WalkService.kt / WalkTracker.kt / 호출부만. assembleDebug 성공 후 tasks/fix-walk-fgs-done.md 1~2줄.
