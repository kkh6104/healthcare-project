# step2c-integrate 완료
- build.gradle.kts: buildConfig 활성화, BODY_COMPOSITION 플래그(기본 false, diag 빌드타입만 true). app/src/diag/AndroidManifest.xml에 체성분 4종 READ 권한 선언(diag에만 병합).
- HealthConnectManager: PERMISSIONS를 플래그가 true일 때만 BodyMetric 4종 권한 포함하도록 구성, body 저장소(lazy) 추가.
- StepsViewModel: 기존 4개 섹션 뒤에 body:BODY_FAT → BONE_MASS → LEAN_BODY_MASS → BODY_WATER_MASS를 독립 섹션으로 순차 조회(기존 section 로그/한도 초과 규칙 동일). Loaded.body: List<Section<MetricSummary>>(플래그 false면 빈 리스트).
- MainActivity: 플래그 true일 때 WeightCard 다음에 BodyCompositionCard(성공 지표) + 실패/건너뜀 지표는 빨간 작은 글씨, 권한 안내 문구에 "체성분" 추가.
- 빌드: assembleDebug assembleDiag 모두 BUILD SUCCESSFUL (실기기 미확인)
