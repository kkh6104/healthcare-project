# fix-step1 완료

## HealthConnectManager.kt
- Availability를 AVAILABLE / INSTALL_OR_UPDATE_REQUIRED / UNSUPPORTED 로 변경하고 매핑 수정.
- todaySteps() 삭제(및 미사용 import 정리).
- openPlayStore(context) 추가: market:// → https 브라우저 → Toast 순서로 시도.

## StepsViewModel.kt
- refreshJob을 두고 refresh() 시작 시 이전 작업 cancel.
- availability() 호출을 try 안으로 이동. CancellationException은 다시 던짐.
- 오늘 값은 dailySteps(7)의 첫 항목 사용.

## MainActivity.kt
- INSTALL_OR_UPDATE_REQUIRED: 안내 문구 + 플레이스토어 버튼(openPlayStore 호출).
- UNSUPPORTED: 안내 문구만 표시(버튼 없음).

## 빌드
assembleDebug: BUILD SUCCESSFUL
