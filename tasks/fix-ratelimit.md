# 구현 요청 (claude-2): Health Connect 요청 한도 초과 방지

## 배경
실기기에서 "Request rejected. Rate limited request quota has been exceeded..." 오류 발생.
원인: MainActivity가 RESUMED마다 vm.refresh()를 호출하고, refresh 1회에 Health Connect 호출이 7회 정도(권한 확인, 걸음 집계, 심박 집계+읽기, 걸음 진단, 수면, 몸무게) 발생.

## 수정 파일
- app/src/main/java/com/gh/healthapp/StepsViewModel.kt
- app/src/main/java/com/gh/healthapp/MainActivity.kt
(다른 파일 수정 금지)

## 설계
1. StepsViewModel
   - `private var lastLoadedAt: Long = 0L` (SystemClock.elapsedRealtime 기준). Loaded 상태로 성공했을 때만 갱신.
   - `fun refreshIfStale(maxAgeMs: Long = 5 * 60_000L)`: 현재 상태가 Loaded이고 마지막 성공 후 maxAgeMs가 안 지났으면 아무것도 안 함, 아니면 refresh().
   - 기존 `refresh()`는 그대로 (강제 새로고침).
   - 오류 메시지: 예외의 message(또는 cause 체인의 message)에 "quota" 또는 "rate limit"(대소문자 무시)이 포함되면 Error 메시지를 "요청이 많아 Health Connect가 잠시 제한했습니다. 몇 분 뒤 다시 시도하세요."로 바꾼다. 그 외 오류는 기존대로.
2. MainActivity
   - RESUMED 때 호출을 `vm.refresh()` → `vm.refreshIfStale()`로 변경.
   - 권한 결과 콜백, "새로고침", "다시 시도" 버튼은 계속 `vm.refresh()`.

## 완료 조건
- assembleDebug 성공 (PowerShell, JAVA_HOME = C:\Program Files\Android\Android Studio\jbr)
- tasks/fix-ratelimit-done.md 에 2~4줄 요약
