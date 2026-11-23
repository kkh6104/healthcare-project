# step2b-integrate 완료
- AndroidManifest.xml: READ_SLEEP, READ_WEIGHT 권한 추가.
- HealthConnectManager.kt: PERMISSIONS에 수면/몸무게 읽기 권한 추가, sleep/weight lazy Repository 추가(client는 private 유지).
- StepsViewModel.kt: Loaded에 sleep, weight 추가, refresh()에서 함께 조회(기존 취소 구조 유지).
- MainActivity.kt: 제목 "내 건강 데이터", 심박수 카드 뒤에 SleepCard, WeightCard 배치, 권한 안내 문구 수정.
- PrivacyPolicyActivity.kt: 안내 문구를 "걸음 수, 심박수, 수면, 몸무게 데이터"로 수정.
- Sleep/Weight 모듈 4개 파일은 수정하지 않음.
- 빌드: assembleDebug BUILD SUCCESSFUL (실기기 미확인)
