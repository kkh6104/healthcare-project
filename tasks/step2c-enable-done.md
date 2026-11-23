# step2c-enable 완료
- BODY_COMPOSITION 플래그와 모든 분기 제거(PERMISSIONS, 순차 로딩, 화면, 안내 문구는 항상 체성분 포함). build.gradle.kts의 buildConfigField/buildConfig 제거(다른 BuildConfig 사용처 없음). 체성분 권한 4개를 main 매니페스트로 이동하고 src/diag/AndroidManifest.xml 삭제. diag 빌드타입(.diag, "HealthApp 진단")은 유지. PrivacyPolicyActivity 문구에 "체성분" 추가.
- StepsViewModel.refreshIfStale: refreshJob이 진행 중이면 "refresh ignored: in progress" 로그만 남기고 반환. 버튼/권한/재시도 refresh()는 기존처럼 이전 작업 취소 후 재시작.
- 빌드: assembleDebug assembleDiag 모두 BUILD SUCCESSFUL (첫 시도는 BUILD FAILED였으나 원인 로그를 확인하지 못했고, 코드 변경 없이 재실행하자 성공함. 일시적 문제로 추정)
