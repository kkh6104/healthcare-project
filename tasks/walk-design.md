# 걷기 기능 설계
- 하단 탭 5개: 오늘 / 활동 / **걷기** / 수면 / 몸
- 위치: Fused Location (play-services-location), 2초 간격 HIGH_ACCURACY, 정확도 > 30m 또는 속도 > 4 m/s(14.4km/h) 점은 버림
- 걸음: TYPE_STEP_COUNTER 센서, 시작 시점 값과의 차이
- 포그라운드 서비스(foregroundServiceType=location) + 알림 "걷는 중 · 1.2km · 15:03" (일시정지/종료 액션)
- 권한: ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, FOREGROUND_SERVICE, FOREGROUND_SERVICE_LOCATION, POST_NOTIFICATIONS(13+), ACTIVITY_RECOGNITION(걸음 센서, 10+). 백그라운드 위치 권한은 요청하지 않음.
- DB v2: walk_session, walk_point 추가 + daily_summary.walkDistanceM 컬럼. **Migration(1,2) 필수, destructive 금지.**
- 지도: 1차 OpenStreetMap(osmdroid), 이후 카카오맵으로 교체 가능하게 `RouteMap` 컴포저블 하나로 감쌈.
- 경로는 기기 안에만 저장.

## 공유 계약 (두 에이전트가 이 이름 그대로 사용)
```kotlin
// com.gh.healthapp.walk
data class GeoPoint(val lat: Double, val lon: Double)
enum class WalkStatus { IDLE, RUNNING, PAUSED }
data class WalkLiveState(val status: WalkStatus = WalkStatus.IDLE, val sessionId: Long? = null, val points: List<GeoPoint> = emptyList(), val distanceM: Double = 0.0, val activeMs: Long = 0, val steps: Int = 0, val startedAt: Long? = null)
object WalkTracker { val state: StateFlow<WalkLiveState>; fun start(context: Context); fun pause(context: Context); fun resume(context: Context); fun stop(context: Context) }
// com.gh.healthapp.ui.map
@Composable fun RouteMap(points: List<GeoPoint>, modifier: Modifier = Modifier, followLast: Boolean = false, interactive: Boolean = true)
```
