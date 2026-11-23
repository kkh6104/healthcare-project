# 구현 요청 (claude-2): 요청 한도 초과 2차 대응

## 배경
5분 캐시 적용 후에도 실기기 로그에 `HC_QuotaBucketRateLimit: rate limit exceeded`가 반복되고 앱에 한도 초과 오류가 다시 표시됨.
현재 refresh 1회 = Health Connect 호출 약 8회(권한 확인, 걸음 그룹 집계, 심박 집계, 심박 readRecords, 걸음 진단 readRecords(pageSize 1000), 수면 readRecords, 몸무게 readRecords). 오류가 나면 화면 전체가 오류 문구로 바뀌어 기존 데이터도 사라짐.

## 수정 파일
- StepsViewModel.kt, MainActivity.kt, HealthConnectManager.kt (Sleep/Weight 모듈 파일은 수정 금지)

## 설계
1. **호출 줄이기**
   - 걸음 진단(stepsDiagnostics)은 refresh에서 빼고, 화면의 진단 줄 자리에 작은 텍스트 버튼 "걸음 기록 진단"을 두어 눌렀을 때만 1회 조회해 결과를 그 자리에 표시.
   - 권한 확인(getGrantedPermissions)은 한 번 모두 허용된 것이 확인되면 ViewModel에 기억해 두고, 이후 refresh에서는 생략. 단, 데이터 조회에서 SecurityException이 나면 기억을 지우고 NeedPermission 상태로 전환.
2. **데이터 유지**
   - 마지막으로 성공한 Loaded 데이터를 ViewModel에 보관.
   - refresh 실패 시 보관된 데이터가 있으면 Loaded 화면을 그대로 두고, 화면 맨 위에 오류 배너(errorContainer 배경 Card, 메시지 + 닫기 없이 표시)만 추가. 보관 데이터가 없을 때만 기존 Error 화면.
   - 이를 위해 Loaded에 `banner: String? = null` 필드 추가.
3. **한도 초과 시 쿨다운**
   - 한도 초과(quota/rate limit) 오류가 나면 그 시각부터 **15분** 동안 refresh/refreshIfStale 호출을 무시하고, 배너(또는 Error 화면) 메시지를 "요청이 많아 Health Connect가 잠시 제한했습니다. N분 뒤 다시 시도하세요."로 표시 (N = 남은 분, 올림). 새로고침/다시 시도 버튼을 눌러도 쿨다운 중이면 조회하지 않고 메시지의 남은 분만 갱신.
   - 시간은 SystemClock.elapsedRealtime 기준.
4. **로딩 표시**: 보관 데이터가 있을 때 refresh 중에는 화면을 Loading으로 바꾸지 말고 기존 화면 유지 (깜빡임 방지).

## 완료 조건
- assembleDebug 성공 (PowerShell, JAVA_HOME = C:\Program Files\Android\Android Studio\jbr)
- tasks/fix-ratelimit2-done.md 에 3~5줄 요약
