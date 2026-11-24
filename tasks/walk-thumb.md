# 구현 요청 (claude-2): 걷기 목록 썸네일을 지도 대신 선 그림으로
지도는 OpenStreetMap 유지로 결정. 목록 썸네일 3개가 매번 지도(타일)를 불러오는 건 낭비라 Canvas 선 그림으로 바꾼다.
- 새 파일 ui/walk/RouteThumbnail.kt: `@Composable fun RouteThumbnail(points: List<GeoPoint>, modifier: Modifier = Modifier)` — 위경도를 bounding box로 정규화(경도는 cos(위도) 보정, 가로세로 비율 유지, 8dp 여백), primary 선 3dp + 시작점 secondary 점 + 끝점 primary 점, 배경 surfaceVariant 둥근 모서리. 점 0~1개면 "경로 없음".
- ui/walk/WalkTab.kt 목록의 RouteMap(interactive=false) 썸네일을 RouteThumbnail로 교체. **썸네일 개수 제한(상위 3개)은 없애고 모든 항목에 표시** (가벼우므로). 점 로딩은 항목별 LaunchedEffect로 하되 DB 읽기만.
- 다른 파일 수정 금지. assembleDebug (JAVA_HOME = Android Studio jbr) 성공 후 tasks/walk-thumb-done.md 1~2줄.
