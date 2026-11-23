# diag-clone 완료
- app/build.gradle.kts: diag 빌드타입 추가(debug 복제, applicationIdSuffix ".diag" → com.gh.healthapp.diag, versionNameSuffix "-diag"). 앱 이름은 app/src/diag/res/values/strings.xml에서 "HealthApp 진단"으로 재정의. debug/release 동작 변경 없음.
- HcLog.kt readAllPages: 페이지마다 "[name] page=N token=값"(null/EMPTY/앞 20자+len) 로그, 종료 이유(end: null/empty/same-token/maxPages) 로그. 종료 조건 로직은 그대로.
- 빌드: assembleDebug assembleDiag 모두 BUILD SUCCESSFUL
- 설치 태스크: installDiag (.\gradlew.bat installDiag), 로그: adb logcat -s HCDEBUG
