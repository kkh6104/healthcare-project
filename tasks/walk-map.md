# 구현 요청 (claude-2): 걷기 1단계 - 지도 컴포저블
설계/계약: tasks/walk-design.md (반드시 읽기). ## 공통
- 빌드: PowerShell `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug` (동시 빌드 lock 시 30초 후 재시도). 라이브러리 버전은 maven 메타데이터로 최신 안정 버전 확인.
- 한국어, MaterialTheme 색만, 기존 동기화/가져오기 로직 변경 금지.

- **새 파일만**: ui/map/RouteMap.kt (+ 필요 시 ui/map/ 안의 보조 파일). claude-1이 동시에 data/, walk/, 매니페스트, gradle(play-services-location)을 수정 중이니 그 파일들은 건드리지 말 것. **osmdroid 의존성 추가만** libs.versions.toml/app/build.gradle.kts에 허용 (충돌 피하려 해당 줄만 추가, 다른 줄 수정 금지).
- GeoPoint는 claude-1이 com.gh.healthapp.walk 패키지에 만든다. 아직 없으면 컴파일을 위해 **ui/map/에 임시로 만들지 말고**, claude-1 완료 후 빌드(최대 10분 대기, 1분 간격 재시도). 그동안 코드 작성은 진행.
## RouteMap
- osmdroid MapView를 AndroidView로 감쌈. Configuration.getInstance().userAgentValue = 앱 패키지명(OSM 타일 정책), 타일 캐시는 앱 캐시 폴더.
- points를 Polyline(primary 색, 두께 6dp)로, 시작점 마커(초록 계열이 아니라 MaterialTheme secondary), 마지막 점 원형 표시.
- followLast=true면 마지막 점으로 카메라 이동(줌 17), false면 전체 경로가 보이게 bounding box로 맞춤(점 1개면 그 점 줌 17, 0개면 서울시청 근처 줌 12 + "위치를 찾는 중" 오버레이).
- interactive=false면 터치 비활성(목록 썸네일용).
- 생명주기: onResume/onPause/onDetach 처리 (LocalLifecycleOwner).
- 지도 하단에 작은 "© OpenStreetMap contributors" 표시(라이선스 필수).
## 완료
- assembleDebug 성공, tasks/walk-map-done.md 에 요약
