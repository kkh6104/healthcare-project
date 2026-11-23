# ui-charts 완료
- 새 파일 3개만 추가 (패키지 com.gh.healthapp.ui.charts), 각 파일에 @Preview 1개. 외부 라이브러리 없이 Compose Canvas/레이아웃만 사용, MaterialTheme 색만 사용. assembleDebug 성공(Preview는 렌더링 확인 안 함).

## 공개 API
- BarChart.kt: data class BarItem(label: String, value: Float, highlight: Boolean = false); @Composable fun BarChart(items: List<BarItem>, modifier: Modifier = Modifier, height: Dp = 160.dp, valueFormatter: (Float) -> String = { it.toInt().toString() }, goal: Float? = null)
- LineChart.kt: data class LinePoint(x: Float, y: Float, label: String? = null); @Composable fun LineChart(points: List<LinePoint>, modifier: Modifier = Modifier, height: Dp = 180.dp, yFormatter: (Float) -> String = { "%.1f" })
- StackedBar.kt: data class Segment(label: String, value: Float, color: Color); @Composable fun StackedBar(segments: List<Segment>, modifier: Modifier = Modifier, height: Dp = 20.dp); object SleepColors { @Composable fun deep(), light(), rem(), awake(): Color }
