# ui-shell 완료
- MainActivity 재구성: Loaded 상태에서 Scaffold(TopAppBar: 제목+동기화 부제+설정 아이콘 / NavigationBar 4탭 오늘·활동·수면·몸). 설정은 뒤로가기 버튼/시스템 뒤로가기로 복귀, 탭·설정·닫은 배너는 rememberSaveable. 권한/Health Connect 없음/로딩/오류는 탭 밖 전체 화면으로 기존처럼 표시. 배너는 모든 탭 상단 공통, 닫기(X) 포함, "가져오기 완료"는 secondaryContainer·그 외는 errorContainer(문구 기준으로 구분, ViewModel은 미수정).
- 새 파일(com.gh.healthapp.ui): HomeTab(2×2 타일+7일 평균 걸음), SettingsScreen(새로고침/삼성헬스 가져오기/걸음 기록 진단/동기화 상태/개인정보 안내), ActivityTab·SleepTab·BodyTab(기존 카드를 보여 주는 임시 구현). ui/charts는 건드리지 않음.
- 아이콘: material-icons-core만 추가(extended 미사용). 걸음/수면/체중 전용 아이콘은 core에 없어 Home/PlayArrow/Star/Person으로 대체. 종류별 마지막 동기화 시각 목록은 ViewModel에 노출된 값이 없어 마지막 동기화 시각만 표시.
- 빌드: assembleDebug assembleDiag 모두 BUILD SUCCESSFUL (실기기 미확인)

## 탭/화면 시그니처
- @Composable fun HomeTab(state: StepsUiState.Loaded, onOpenTab: (Int) -> Unit)
- @Composable fun ActivityTab(state: StepsUiState.Loaded)
- @Composable fun SleepTab(state: StepsUiState.Loaded)
- @Composable fun BodyTab(state: StepsUiState.Loaded)
- @Composable fun SettingsScreen(lastSync: Long?, syncing: Boolean, importProgress: String?, diagnostics: StepsDiagnostics?, onRefresh: () -> Unit, onImport: () -> Unit, onDiagnose: () -> Unit, modifier: Modifier = Modifier)
- (ActivityTab.kt에 공용) @Composable fun HeartRateCard(hr: HeartRateSummary)
