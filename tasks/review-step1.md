# 검수 요청: 1단계 (Health Connect 걸음 수 읽기)

## 역할
너는 검수 담당이다. **코드를 수정하지 말고**, 발견한 문제를 결과 파일에 기록만 한다.

## 배경
- Android 앱 (Kotlin, Jetpack Compose). 삼성헬스 → Health Connect → 이 앱으로 걸음 수를 읽어 화면에 표시.
- 테스트 기기: **Android 10 (API 29)**. Health Connect는 시스템 내장이 아니라 플레이스토어 앱(com.google.android.apps.healthdata)으로 설치됨.
- minSdk 28, targetSdk/compileSdk 37, Health Connect SDK androidx.health.connect:connect-client 1.1.0.

## 검수 대상
- app/src/main/AndroidManifest.xml
- app/src/main/java/com/gh/healthapp/HealthConnectManager.kt
- app/src/main/java/com/gh/healthapp/StepsViewModel.kt
- app/src/main/java/com/gh/healthapp/MainActivity.kt
- app/src/main/java/com/gh/healthapp/PrivacyPolicyActivity.kt
- gradle/libs.versions.toml, app/build.gradle.kts

## 확인할 것
1. **빌드**: PowerShell에서
   `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug`
   (시스템 Java 26은 쓰지 말 것). 성공/실패와 경고를 기록.
2. **Health Connect 연동 정확성**
   - 매니페스트: READ_STEPS 권한, <queries>, 권한 rationale 인텐트(Android 13 이하), VIEW_PERMISSION_USAGE activity-alias(Android 14+)가 올바른지
   - getSdkStatus 처리, 권한 요청 계약(createRequestPermissionResultContract) 사용, 권한 확인
   - 걸음 수 집계: 오늘 합계(aggregate)와 최근 7일 날짜별(aggregateGroupByPeriod)의 시간 범위·시간대·날짜 매칭이 맞는지, 중복 제거가 되는지
   - Android 10 + 별도 설치 Health Connect 환경에서 문제될 부분
3. **앱 동작 버그**: 상태 전환(로딩/미설치/권한필요/로드됨/오류), onResume마다 refresh 하는 구조로 인한 문제(예: 권한 창에서 돌아올 때 중복 호출, 무한 루프), 예외 처리 누락, 크래시 가능성
4. **명백한 개선점**은 "nit"로 구분해서 짧게만.

## 결과
`tasks/review-step1-result.md`에 작성:
- 맨 위에 한 줄 결론: **PASS** / **PASS (경미한 지적)** / **FAIL**
- 빌드 결과
- 발견 사항 목록: 각각 [심각도: 치명/중요/경미(nit)] 파일:줄 — 문제 — 실패 시나리오 — 수정 제안
- 확신이 없는 항목은 "추정"이라고 표시
