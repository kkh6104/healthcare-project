# ui-detail 완료
- 카드 터치 → 상세 화면: MainActivity에 detail: DetailKind?(rememberSaveable) 추가. 상세가 열리면 탭 대신 표시, 하단 탭바 숨김, TopAppBar 뒤로가기 아이콘·시스템 뒤로가기로 복귀. 활동/수면/몸 탭 카드에 clickable + "자세히 ›"(DetailHeader), 탭 시그니처에 onOpenDetail 추가. 몸 탭 체중 LineChart에 minYSpan=6f, 체지방률에 10f 반영(차트 쪽 파라미터가 이미 있어 대기 없이 빌드됨).
- 데이터: data/DetailReader.kt(DB만 읽음, HC 호출 없음) — stepsDaily, heartRateHourly, hrDailyAvg, sleepNights, metricHistory(출처 우선순위 적용). HealthDao에 summariesBetween 쿼리 추가, HealthConnectManager.detail 추가. LocalHealthReader/동기화·가져오기 로직은 수정하지 않음.
- 상세 화면(ui/detail/): StepsDetail, HeartRateDetail, SleepLastNightDetail, SleepHistoryDetail, WeightDetail, BodyCompositionDetail + DetailKind/DetailScreen/DetailCommon(기간 칩 7·30·90일, 기본 30일). 기록 없음/읽는 중/읽기 실패를 구분해서 표시.
- 빌드: assembleDebug assembleDiag 모두 BUILD SUCCESSFUL. 화면에서 직접 확인하지는 못했음(특히 90일 막대 그래프의 값 라벨 겹침, 체성분 지표별 minYSpan(골질량 1f·나머지 6f 임의 선택)은 실기기 확인 필요).

## 공개 API
- enum class DetailKind { STEPS, HEART_RATE, SLEEP_LAST_NIGHT, SLEEP_HISTORY, WEIGHT, BODY_COMPOSITION } (ui.detail, title 속성 포함)
- fun ActivityTab/SleepTab/BodyTab(state: StepsUiState.Loaded, onOpenDetail: (DetailKind) -> Unit)
- fun DetailScreen(kind: DetailKind, state: StepsUiState.Loaded, detail: DetailReader)
- class DetailReader(dao): stepsDaily(days) 최신순 / heartRateHourly(date) / hrDailyAvg(days) 최신순 / sleepNights(days) 최신순 / metricHistory(type, days) 시간 오름차순
