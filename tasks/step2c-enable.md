# 구현 요청 (claude-1): 체성분을 원래 앱에도 적용 + 권한 직후 중복 새로고침 수정

## 1. 체성분 전체 적용 (플래그 제거)
- 복제 앱(diag)에서 실기기 검증 완료. 이제 모든 빌드에서 체성분을 켠다.
- BuildConfig.BODY_COMPOSITION 플래그와 그 조건 분기를 **제거**하고 항상 체성분 포함 (PERMISSIONS, 순차 로딩, 화면, 권한 안내 문구).
- app/build.gradle.kts의 buildConfigField 제거. 다른 BuildConfig 사용처가 없으면 `buildConfig = true`도 제거.
- app/src/diag/AndroidManifest.xml의 체성분 권한 4개를 app/src/main/AndroidManifest.xml로 옮기고 diag 매니페스트 파일은 삭제.
- **diag 빌드타입 자체(applicationIdSuffix .diag, 앱 이름 "HealthApp 진단")는 유지** (한도 걸렸을 때 테스트용).
- PrivacyPolicyActivity 문구에 "체성분" 추가.

## 2. 권한 직후 중복 새로고침
- 실기기 로그: 권한 허용 직후 `refresh #2 reason=permission` 시작 → 바로 `refresh #3 reason=resume`이 #2를 취소하고 다시 시작 (불필요한 요청 1회).
- 수정: `refreshIfStale`(resume 경로)은 **진행 중인 refreshJob이 있으면(isActive) 아무것도 하지 않고** `refresh ignored: in progress` 로그만 남긴다. 버튼/권한/재시도 경로의 refresh()는 기존처럼 이전 작업을 취소하고 새로 시작.

## 완료
- PowerShell: `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug assembleDiag` 둘 다 성공
- tasks/step2c-enable-done.md 에 3~4줄
