# walk-core 완료
- 의존성: play-services-location 21.4.0(maven 메타데이터의 최신 release). 매니페스트에 위치/포그라운드 서비스(location)/알림/걸음 센서 권한과 WalkService(foregroundServiceType=location) 등록. MainActivity/탭 UI/ui/map은 수정하지 않음.
- DB v2: WalkSessionEntity/WalkPointEntity(FK CASCADE, sessionId 인덱스) + WalkDao + DailySummaryEntity.walkDistanceM(기본 null) 추가, HealthDatabase version=2 + MIGRATION_1_2(테이블 2개·인덱스·ALTER, destructive 미사용). 추가로 DailySummaryBuilder.rebuild가 기존 walkDistanceM을 지우지 않고 유지하도록 수정, HealthDao.updateWalkDistance 추가.
- walk 패키지: WalkTracker(계약대로 StateFlow + start/pause/resume/stop + restore), WalkService(2초 HIGH_ACCURACY, 정확도 30m·속도 4m/s 필터, Haversine 거리, 재개 후 첫 점은 거리 연결 안 함, 걸음 센서 시작값 차이, 점 10개/10초 저장, 알림 "걷는 중 · 1.23km · 15:03"(시간은 활동 시간 mm:ss) 5초 갱신 + 일시정지/재개/종료 액션, 종료 시 DONE 마감과 그날 walkDistanceM 갱신), WalkPermissions(required/missing/hasAll/hasLocation), WalkMath(internal 순수 함수) + WalkMathTest 7건.
- 빌드: assembleDebug testDebugUnitTest BUILD SUCCESSFUL. 실기기에서는 확인하지 못함(특히 v1→v2 마이그레이션 실행, 위치 수신, 알림 액션, 앱 종료 후 restore 마감). 일시정지 구간은 지도 선이 이어져 보일 수 있음(계약상 점 목록이 하나).

## 공개 API
- data class GeoPoint(lat, lon) / enum WalkStatus { IDLE, RUNNING, PAUSED } / data class WalkLiveState(status, sessionId, points, distanceM, activeMs, steps, startedAt)
- object WalkTracker { val state: StateFlow<WalkLiveState>; start(context); pause(context); resume(context); stop(context); restore(context) }
- object WalkPermissions { required(): Array<String>; missing(context): List<String>; hasAll(context): Boolean; hasLocation(context): Boolean }
- WalkDao: insertSession, updateSession, insertPoints, sessionsFlow() (DONE만, 최신순), session(id), points(sessionId), runningSession(), doneBetween(from, to)
- HealthDatabase.walkDao(), HealthDatabase.MIGRATION_1_2
