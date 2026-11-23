# 1단계 리뷰 결과: FAIL

## 빌드

- 지정된 Android Studio JBR을 `JAVA_HOME`으로 설정하고 `./gradlew.bat assembleDebug` 실행: **BUILD SUCCESSFUL**. 36개 작업이 모두 UP-TO-DATE였으며 출력된 경고는 없었다. 기존 산출물을 재사용한 빌드다.
- Android 10 실기기에서 설치, 권한 화면, 삼성 헬스 동기화 및 집계 결과는 실행 확인하지 못했다.

## 발견 사항

1. **[중요] `app/src/main/java/com/gh/healthapp/HealthConnectManager.kt:34`, `app/src/main/java/com/gh/healthapp/MainActivity.kt:81` — 미설치를 업데이트 필요로 표시.** `SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED`는 제공자가 미설치이거나 업데이트가 필요한 경우 모두 반환될 수 있지만 항상 `UPDATE_REQUIRED`로 변환한다. Android 10에서 Health Connect가 설치되지 않아도 업데이트가 필요하다고 안내한다. 설치 또는 업데이트가 필요하다는 공통 문구를 사용하거나 별도로 설치 여부를 확인하라. [공식 SDK 상태 설명](https://developer.android.com/reference/kotlin/androidx/health/connect/client/HealthConnectClient)
2. **[중요] `app/src/main/java/com/gh/healthapp/StepsViewModel.kt:28`, `app/src/main/java/com/gh/healthapp/MainActivity.kt:61` 및 `:68` — 동시 새로고침 결과가 서로 덮어씀.** 권한 결과 콜백과 RESUMED 진입은 각각 독립적인 코루틴을 시작하고 이전 요청을 취소하거나 순서를 검사하지 않는다. 예를 들어 권한 창에서 복귀할 때 두 조회가 겹치면 먼저 시작한 요청이 늦게 완료되어 더 최신의 로드 결과 또는 권한 상태를 덮을 수 있다. 새로고침 작업을 직렬화하거나 이전 작업 취소와 최신 요청만 결과 반영을 적용하라. 무한 권한 요청 루프는 코드에서 확인되지 않는다. 완료 순서 역전은 코드에 근거한 시나리오이며 실기기 재현은 **추정**이다.
3. **[중요] `app/src/main/java/com/gh/healthapp/MainActivity.kt:84` — Play 스토어가 없거나 실행 불가능하면 앱 종료 가능.** `startActivity`의 대상 패키지를 Play 스토어로 고정하고 예외를 처리하지 않는다. 스토어가 비활성화되거나 인텐트를 처리할 수 없는 기기에서는 설치/업데이트 버튼을 누를 때 `ActivityNotFoundException`이 발생한다. 실행 가능 여부를 확인하거나 예외를 처리하고 대체 안내를 제공하라.
4. **[경미(nit)] `app/src/main/java/com/gh/healthapp/HealthConnectManager.kt:35` — 지원 불가 상태를 미설치로 취급.** `SDK_UNAVAILABLE`도 `NOT_INSTALLED`로 변환되어 설치 안내가 표시된다. 지원되지 않는 환경에선 설치를 유도해도 해결되지 않을 수 있다. 지원 불가 상태를 별도로 표현하라. [공식 SDK 상태 설명](https://developer.android.com/reference/kotlin/androidx/health/connect/client/HealthConnectClient)
5. **[경미(nit)] `app/src/main/java/com/gh/healthapp/HealthConnectManager.kt:47` 및 `:60` — 두 집계의 시간 기준이 다름.** 오늘 합계는 현재 기기 시간대의 자정부터 현재 시각까지의 Instant 범위이고, 일별 집계는 각 기록의 로컬 시간 기준으로 오늘 자정부터 내일 자정까지다. 시간대가 바뀌었거나 미래 시각의 기록이 있으면 오늘 카드와 일별 오늘 값이 달라질 수 있다. 제품에서 원하는 시간대와 종료 시각을 정하고 두 집계에 일관되게 적용하거나 차이를 안내하라. 실제 데이터에 따른 차이는 **추정**이며 실기기 확인이 필요하다.

## 확인 사항 및 한계

- READ_STEPS, 제공자 패키지 visibility, rationale 액티비티 및 Android 14 권한 사용 액티비티 별칭이 선언되어 있다. 권한 요청 계약과 요청 전 권한 확인이 사용된다.
- 일별 범위는 오늘 포함 7일이며 날짜별 결과를 매칭하고 빈 날짜를 0으로 채운다. Health Connect 집계 API를 사용하며 앱에서 원시 레코드를 단순 합산하지 않는다. 실제 중복 제거 및 시간대별 값은 기기와 데이터 소스에서 검증하지 않았다.
- `availability()` 호출은 예외 처리 밖에 있다. 해당 호출이 실제 예외를 발생시키는 상황은 확인하지 못했으므로 확정 결함으로 분류하지 않았다.
