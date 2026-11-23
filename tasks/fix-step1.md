# 구현 요청: 1단계 검수 지적 수정

검수 결과 원문: tasks/review-step1-result.md (참고용). 아래 설계대로만 수정한다. 범위 밖 리팩토링 금지.

## 대상 파일
- app/src/main/java/com/gh/healthapp/HealthConnectManager.kt
- app/src/main/java/com/gh/healthapp/StepsViewModel.kt
- app/src/main/java/com/gh/healthapp/MainActivity.kt

## 수정 1. Health Connect 상태 구분 (검수 1, 4번)
- `Availability` enum을 `AVAILABLE`, `INSTALL_OR_UPDATE_REQUIRED`, `UNSUPPORTED` 3개로 바꾼다.
  - SDK_AVAILABLE → AVAILABLE
  - SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED → INSTALL_OR_UPDATE_REQUIRED
  - 그 외(SDK_UNAVAILABLE) → UNSUPPORTED
- 화면 문구:
  - INSTALL_OR_UPDATE_REQUIRED: "Health Connect 설치 또는 업데이트가 필요합니다." + "플레이스토어에서 열기" 버튼
  - UNSUPPORTED: "이 기기에서는 Health Connect를 사용할 수 없습니다." (버튼 없음)

## 수정 2. 새로고침 겹침 방지 (검수 2번)
- StepsViewModel에 `private var refreshJob: Job?`를 두고, refresh() 시작 시 이전 작업을 cancel()한 뒤 새로 launch 한다. 최신 요청 결과만 화면에 반영되게.
- availability() 호출도 try 블록 안으로 옮긴다.
- 주의: CancellationException은 Error 상태로 바꾸지 말고 다시 던질 것 (catch (e: Exception)에서 걸러내기).

## 수정 3. 플레이스토어 열기 실패 시 크래시 방지 (검수 3번)
- HealthConnectManager에 `fun openPlayStore(context: Context)` 같은 함수를 만들어 다음 순서로 시도:
  1. 기존 market:// 인텐트 (com.android.vending)
  2. ActivityNotFoundException이면 https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata 를 ACTION_VIEW로 (브라우저)
  3. 그것도 실패하면 Toast "플레이스토어를 열 수 없습니다."
- MainActivity 버튼은 이 함수를 호출.

## 수정 4. 오늘 걸음 수 기준 통일 (검수 5번)
- 오늘 카드 값은 별도 aggregate 호출 대신 dailySteps(7) 결과의 오늘 값(목록 첫 항목)을 쓴다. 쓰지 않게 된 todaySteps()는 삭제.

## 완료 조건
- PowerShell: `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug` 성공
- tasks/fix-step1-done.md 에 수정 요약(파일별 3~5줄)과 빌드 결과를 적는다.
