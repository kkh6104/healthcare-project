# 구현 요청 (claude-1): 진단용 복제 앱 + 페이지 토큰 로그

## 목적
원래 앱(com.gh.healthapp)은 Health Connect 요청 한도를 다 써서 30분 대기 중. 패키지 이름이 다른 복제 앱을 만들어 한도를 따로 받아 지금 바로 테스트한다. 또한 무한 반복 원인(마지막 페이지의 pageToken 값)을 실제로 확인한다.

## 1. 복제 앱 (app/build.gradle.kts)
- buildTypes에 `create("diag") { initWith(getByName("debug")); applicationIdSuffix = ".diag"; versionNameSuffix = "-diag"; matchingFallbacks += listOf("debug") }`
- 앱 이름: diag 빌드에서만 "HealthApp 진단"으로 보이게 (resValue("string", "app_name", "HealthApp 진단") 사용 시 기존 res/values/strings.xml의 app_name과 충돌하지 않도록 방법 선택: 예) app/src/diag/res/values/strings.xml 에 app_name 재정의).
- debug/release 동작은 변경 없음.

## 2. 페이지 토큰 로그 (HcLog.kt의 readAllPages)
- 각 페이지 응답 후 `Log.i("HCDEBUG", "[name] page=N token=" + 값표시)` 기록. 값표시: null이면 "null", 빈 문자열이면 "EMPTY", 그 외는 앞 20자 + "(len=길이)". 
- 종료 이유도 기록: "end: null", "end: empty", "end: same-token", "end: maxPages".
- 종료 조건 로직 자체는 바꾸지 않는다.

## 완료 조건
- PowerShell: `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug assembleDiag` 둘 다 성공
- tasks/diag-clone-done.md 에 2~4줄 (설치 태스크 이름 포함)
