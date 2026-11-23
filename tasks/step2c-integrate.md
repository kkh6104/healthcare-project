# 구현 요청 (claude-1): 체성분 연결 (복제 앱 diag 빌드에서만)

사용할 모듈: BodyCompositionRepository.kt / BodyCompositionSection.kt (tasks/step2c-body-done.md). 이 두 파일은 수정하지 않는다.

## 1. 기능 플래그
- app/build.gradle.kts: `buildFeatures { buildConfig = true }` 추가. defaultConfig에 `buildConfigField("boolean", "BODY_COMPOSITION", "false")`, diag 빌드타입에서 `"true"`로 덮어쓰기. debug/release는 false.

## 2. 권한
- 새 파일 app/src/diag/AndroidManifest.xml 에 READ_BODY_FAT, READ_BONE_MASS, READ_LEAN_BODY_MASS, READ_BODY_WATER_MASS uses-permission 선언 (diag 빌드에만 병합되도록).
- HealthConnectManager.PERMISSIONS: BuildConfig.BODY_COMPOSITION일 때만 BodyMetric 4종의 BodyCompositionRepository.readPermission(...) 추가.
- HealthConnectManager에 `val body by lazy { BodyCompositionRepository(client) }`.

## 3. 순차 로딩 (StepsViewModel)
- 기존 순서(걸음 → 심박 → 수면 → 몸무게) 뒤에, 플래그가 true면 **체지방률 → 골질량 → 제지방량 → 체내 수분량**을 각각 독립 섹션으로 하나씩 순차 조회.
- 섹션 이름(로그용): body:BODY_FAT 등. 기존 섹션 로그(start/ok/fail/skipped + 요약 latest=값 count=n) 형식 그대로.
- 일반 실패는 다음 섹션 계속, 한도 초과는 즉시 중단 + 남은 섹션 skipped + 쿨다운 (기존 규칙과 동일).
- Loaded에 `body: List<Section<MetricSummary>>` (플래그 false면 빈 리스트).

## 4. 화면 (MainActivity)
- WeightCard 다음에 체성분 영역: 플래그 true일 때만.
  - 성공한 지표들은 BodyCompositionCard(성공 summaries)로 한 카드에.
  - 실패/건너뜀 지표는 카드 아래 작은 빨간 글씨 "골질량 불러오기 실패: 메시지" / "골질량 건너뜀(한도 초과)".
- NeedPermission 안내 문구는 플래그 true면 "... 몸무게, 체성분을 읽으려면 ..."으로.

## 완료
- PowerShell: `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug assembleDiag` 둘 다 성공
- tasks/step2c-integrate-done.md 에 3~5줄
