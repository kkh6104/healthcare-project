# walk-map 완료
- 새 파일: ui/map/RouteMap.kt. 추가한 의존성: osmdroid-android 6.1.20 (libs.versions.toml, app/build.gradle.kts에 각 1줄). assembleDebug 성공(claude-1의 GeoPoint 완료 후). 지도 화면은 실기기에서 확인 안 함.
- RouteMap(points: List<GeoPoint>, modifier, followLast = false, interactive = true): osmdroid MapView를 AndroidView로 감쌈. userAgent = 패키지명, 타일 캐시 = cacheDir/osmdroid. 경로 Polyline(primary, 6dp), 시작점 secondary 원, 마지막 점 primary 원.
- followLast면 마지막 점으로 이동(줌 17), 아니면 전체 경로 bounding box. 점 1개면 줌 17, 0개면 서울시청 줌 12 + "위치를 찾는 중". interactive=false면 터치 차단. onResume/onPause/onDetach 처리, 하단에 "© OpenStreetMap contributors".
- **주의**: AndroidManifest.xml에 INTERNET 권한이 없음(수정 금지 파일이라 손대지 않음). 지도 타일을 받으려면 <uses-permission android:name="android.permission.INTERNET"/> 추가 필요 (claude-1 담당).
- 중간에 동시 빌드 때문에 mergeDebugResources 실패가 한 번 났고, 재시도로 해결.
