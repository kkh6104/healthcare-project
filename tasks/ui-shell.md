# 구현 요청 (claude-1): UI-1 탭 구조 + 오늘·설정 화면

## 공통
- 데이터 로직(ViewModel의 동기화/가져오기/DB 읽기)은 바꾸지 않는다. 화면만.
- 외부 차트/내비게이션 라이브러리 추가 금지 (Compose Canvas, 상태 기반 탭).
- 한국어, Locale.KOREAN, MaterialTheme 색/타이포 사용 (다크 모드에서도 보이게 하드코딩 색 금지).
- 빌드: PowerShell `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug` (동시 빌드 lock 시 30초 후 재시도)

- **ui/charts/ 폴더는 claude-2가 동시에 만드는 중이니 건드리지 말 것.**

## 구조
- MainActivity의 화면을 다음으로 재구성:
  - Scaffold: TopAppBar(제목 "내 건강 데이터", 부제 한 줄 "마지막 동기화: 오후 11:37" / "동기화 중…" / "아직 동기화하지 않았습니다", 오른쪽 설정 아이콘 버튼)
  - bottomBar: NavigationBar 4개 탭 — 오늘 / 활동 / 수면 / 몸 (아이콘: Home, DirectionsWalk, Bedtime, MonitorWeight 등. material-icons가 없으면 BOM으로 `androidx.compose.material:material-icons-extended` 대신 `material-icons-core`만 추가하고 core에 없는 아이콘은 Icons.Default 중 가까운 것 사용. extended는 용량이 커서 금지)
  - 설정 아이콘 → 설정 화면(뒤로가기 버튼/시스템 뒤로가기로 복귀). 탭/설정 상태는 rememberSaveable.
- 권한 필요 / Health Connect 없음 / 첫 로딩 상태는 탭 구조 밖에서 기존처럼 전체 화면으로.
- 배너: ViewModel의 배너를 **종류 구분**(성공=secondaryContainer, 오류/경고=errorContainer). "가져오기 완료"는 성공. 배너는 모든 탭 상단에 공통 표시 + 닫기(X) 버튼.

## 새 파일 (패키지 com.gh.healthapp.ui)
1. ui/HomeTab.kt — `@Composable fun HomeTab(state: StepsUiState.Loaded, onOpenTab: (Int) -> Unit)`
   - 2×2 타일(Card, 클릭 시 해당 탭 이동): 걸음(오늘 N걸음, 작은 글씨 "어제 N"), 심박(오늘 평균 N bpm, "최근 N"), 수면(어젯밤 N시간 M분 / 기록 없음), 체중(최근 N kg, 측정 날짜)
   - 타일 아래 "최근 7일 평균 걸음 N" 한 줄.
2. ui/SettingsScreen.kt — 새로고침, 삼성헬스 데이터 가져오기(+안내 문구, 진행 문구), 걸음 기록 진단(+결과), 동기화 상태(종류별 마지막 동기화 시각 목록이 있으면 표시, 없으면 마지막 동기화 시각만), "개인정보 처리 안내"(PrivacyPolicyActivity 열기). 기존 버튼 동작 그대로 연결.
3. ui/ActivityTab.kt, ui/SleepTab.kt, ui/BodyTab.kt — **임시 구현**: 각각 `@Composable fun ActivityTab(state: StepsUiState.Loaded)` 등 시그니처로 만들고, 기존 카드(걸음 7일 목록+심박 카드 / SleepCard / WeightCard+BodyCompositionCard)를 그대로 보여 준다. (다음 단계에서 claude-2가 이 3개 파일만 그래프로 교체)
- 화면별 스크롤은 각 탭 내부에서.

## 완료
- assembleDebug assembleDiag 성공, tasks/ui-shell-done.md 에 3~5줄 + 탭 파일 시그니처
