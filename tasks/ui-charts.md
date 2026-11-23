# 구현 요청 (claude-2): UI-1 그래프 부품

## 공통
- 데이터 로직(ViewModel의 동기화/가져오기/DB 읽기)은 바꾸지 않는다. 화면만.
- 외부 차트/내비게이션 라이브러리 추가 금지 (Compose Canvas, 상태 기반 탭).
- 한국어, Locale.KOREAN, MaterialTheme 색/타이포 사용 (다크 모드에서도 보이게 하드코딩 색 금지).
- 빌드: PowerShell `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug` (동시 빌드 lock 시 30초 후 재시도)

- **새 파일만 만든다** (claude-1이 MainActivity 등을 동시에 수정 중).

## 새 파일 (패키지 com.gh.healthapp.ui.charts)
1. app/src/main/java/com/gh/healthapp/ui/charts/BarChart.kt
   - `data class BarItem(val label: String, val value: Float, val highlight: Boolean = false)`
   - `@Composable fun BarChart(items: List<BarItem>, modifier: Modifier = Modifier, height: Dp = 160.dp, valueFormatter: (Float) -> String = { it.toInt().toString() }, goal: Float? = null)`
   - 막대 위에 값(작은 글씨), 아래에 라벨. highlight 막대는 primary, 나머지는 primary 40% 투명. goal이 있으면 점선 가로선 + "목표" 라벨. 값이 모두 0이면 "데이터 없음".
2. ui/charts/LineChart.kt
   - `data class LinePoint(val x: Float, val y: Float, val label: String? = null)`
   - `@Composable fun LineChart(points: List<LinePoint>, modifier: Modifier = Modifier, height: Dp = 180.dp, yFormatter: (Float) -> String = { "%.1f".format(it) })`
   - x 오름차순 정렬 후 선 + 점. y축 최소/최대 라벨(좌측), 첫·마지막 점의 label을 하단에. 점 1개면 점만, 0개면 "데이터 없음". y 범위가 0이면 ±1 여유.
3. ui/charts/StackedBar.kt
   - `data class Segment(val label: String, val value: Float, val color: Color)`
   - `@Composable fun StackedBar(segments: List<Segment>, modifier: Modifier = Modifier, height: Dp = 20.dp)` — 가로 100% 막대를 비율대로 나눔 + 아래 범례(색 점, 라벨, 퍼센트). 합이 0이면 "데이터 없음".
   - `object SleepColors { @Composable fun deep(): Color; light(); rem(); awake() }` — MaterialTheme colorScheme 기반 (예: deep=primary, light=primaryContainer, rem=tertiary, awake=error 계열).
- 각 파일에 @Preview 1개씩.

## 완료
- assembleDebug 성공, tasks/ui-charts-done.md 에 공개 API
