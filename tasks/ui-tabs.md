# 구현 요청 (claude-2): UI-2 활동·수면·몸 탭

## 공통
- 데이터 로직(ViewModel의 동기화/가져오기/DB 읽기)은 바꾸지 않는다. 화면만.
- 외부 차트/내비게이션 라이브러리 추가 금지 (Compose Canvas, 상태 기반 탭).
- 한국어, Locale.KOREAN, MaterialTheme 색/타이포 사용 (다크 모드에서도 보이게 하드코딩 색 금지).
- 빌드: PowerShell `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug` (동시 빌드 lock 시 30초 후 재시도)

- 수정 허용: **ui/ActivityTab.kt, ui/SleepTab.kt, ui/BodyTab.kt 세 파일만** (claude-1이 MainActivity와 아이콘 파일을 동시에 수정 중). ui/charts/ 부품 사용(API: tasks/ui-charts-done.md).
- 시그니처 유지: `ActivityTab(state: StepsUiState.Loaded)`, `SleepTab(...)`, `BodyTab(...)`. ActivityTab.kt의 공용 `HeartRateCard(hr)`는 유지(다른 곳에서 사용 가능).
- 데이터는 Loaded 안의 기존 값만 사용 (StepsViewModel.kt를 읽어 필드 확인). ViewModel 수정 금지.

## 활동 탭
- 카드 "걸음": 오늘 값 크게 + BarChart(최근 7일, 라벨 "월/화…" 요일, 오늘 highlight, goal = 6000f) + 아래 "7일 평균 N걸음 · 최고 N걸음(날짜)".
- HeartRateCard(오늘 심박).
## 수면 탭
- 카드 "어젯밤": 시간 크게, "오후 11:10 ~ 오전 2:37", 단계가 있으면 StackedBar(깊은/얕은/렘/깸, SleepColors), 없으면 "단계 정보 없음".
- 카드 "최근 7일": BarChart(수면 시간을 시간 단위 Float, 라벨 요일, valueFormatter "6.5h" 형식, goal = 7f) + "평균 N시간 M분".
- 기록이 전혀 없으면 "수면 기록이 없습니다. 설정에서 삼성헬스 데이터를 가져올 수 있어요." 안내.
## 몸 탭
- 카드 "체중": 최근 값 크게 + 측정일 + 1주 변화 + LineChart(최근 기록, x=epochDay, label "11/19" 형식).
- 카드 "체성분": 지표별 한 줄(값, 1주 변화) + 체지방률은 LineChart 작게(height 120.dp). 기록 없는 지표는 "기록 없음".
- 각 탭은 LazyColumn/verticalScroll로 스크롤.

## 완료
- assembleDebug 성공, tasks/ui-tabs-done.md 에 2~4줄
