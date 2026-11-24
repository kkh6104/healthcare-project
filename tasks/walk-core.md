# 구현 요청 (claude-1): 걷기 1단계 - 기록 서비스·DB·권한
설계/계약: tasks/walk-design.md (반드시 읽기). ## 공통
- 빌드: PowerShell `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug` (동시 빌드 lock 시 30초 후 재시도). 라이브러리 버전은 maven 메타데이터로 최신 안정 버전 확인.
- 한국어, MaterialTheme 색만, 기존 동기화/가져오기 로직 변경 금지.

- **ui/map/ 폴더는 claude-2가 동시에 만드는 중이니 건드리지 말 것.** MainActivity/탭 UI도 이번엔 수정하지 않는다(다음 단계).

## 1. 의존성
- com.google.android.gms:play-services-location 최신 안정 버전.
## 2. DB v2 (data/)
- WalkSessionEntity(walk_session): id PK auto, startTime Long, endTime Long?, distanceM Double, activeMs Long, steps Int, status String("RUNNING"/"PAUSED"/"DONE")
- WalkPointEntity(walk_point): id PK auto, sessionId Long (FK→walk_session ON DELETE CASCADE, index), time Long, lat Double, lon Double, accuracy Float, altitude Double?
- DailySummaryEntity에 `walkDistanceM: Double? = null` 추가
- HealthDatabase version=2, `MIGRATION_1_2` (CREATE TABLE 2개 + 인덱스 + ALTER TABLE daily_summary ADD COLUMN walkDistanceM REAL). Room이 생성할 스키마와 정확히 일치하게 작성(컬럼 타입/NOT NULL/기본값). fallbackToDestructiveMigration 사용 금지.
- WalkDao: insertSession, updateSession, insertPoints, sessionsFlow()(최신순, DONE만), session(id), points(sessionId), runningSession()
## 3. walk 패키지
- WalkTracker(계약대로, StateFlow 보유). start→WalkService 시작(startForegroundService), 나머지는 서비스에 Intent 액션.
- WalkService: foregroundServiceType="location". FusedLocationProviderClient 2초 HIGH_ACCURACY. 점 필터(정확도>30m, 직전 점 대비 속도>4m/s 버림). 거리 = 연속 점 간 Haversine 합(PAUSED 중엔 기록 안 함, 재개 시 첫 점은 거리 계산에서 이전 점과 연결하지 않음). 걸음: STEP_COUNTER 시작값 차이(센서 없거나 권한 없으면 0).
  - 점은 10개 또는 10초마다 DB에 일괄 저장, 세션 거리/시간/걸음도 갱신.
  - 알림: 채널 "walk", "걷는 중 · 1.23km · 15:03"(1초마다 갱신은 과하니 5초), 액션 일시정지/재개/종료, 탭하면 앱 열기.
  - 종료: 세션 DONE, endTime, 남은 점 저장, 그날 daily_summary.walkDistanceM = 그날 DONE 세션 거리 합으로 갱신(행이 없으면 생성 시 다른 필드 null), 서비스 stopSelf.
  - 앱이 죽었다 다시 켜졌을 때 RUNNING/PAUSED 세션이 남아 있으면 WalkTracker 상태를 복원할 수 있게 `fun restore(context)` 제공 (서비스가 살아 있지 않으면 해당 세션을 DONE으로 마감).
- 순수 함수(거리 계산, 점 필터)는 internal로 분리 + JVM 단위 테스트 (app/src/test/.../walk/WalkMathTest.kt).
## 4. 매니페스트
- 설계의 권한들, <service android:name=".walk.WalkService" android:foregroundServiceType="location" android:exported="false"/>
## 5. 권한 요청 헬퍼
- `walk/WalkPermissions.kt`: 필요한 권한 배열(SDK 버전별) + 모두 허용됐는지 확인 함수. UI에서 쓸 수 있게.
## 완료
- assembleDebug testDebugUnitTest 성공, tasks/walk-core-done.md 에 요약 + 공개 API
