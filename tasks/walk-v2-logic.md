# 구현 요청 (codex-1): 걷기 기록 개선 — 자동 일시정지·음성 안내·탈것 감지·거리 보정
## 공통
- 빌드: PowerShell `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug testDebugUnitTest` (다른 에이전트와 동시 빌드 lock 시 30초 후 재시도). 외부 라이브러리 추가 금지. 한국어, MaterialTheme 색만.
- 기존 설계: tasks/walk-design.md, 이전 보정: tasks/fix-walk-jitter.md, tasks/fix-walk-jitter-done.md.

- **수정 허용: walk/ 패키지(WalkService, WalkMath, WalkTracker 등), app/src/test/.../walk/, 새 파일 walk/WalkVoice.kt, walk/WalkSettings.kt.** ui/ 폴더는 codex-2가 동시에 수정 중이니 건드리지 말 것.

## 실측 근거 (같은 산책, 삼성헬스 vs 우리 앱)
- 걸음 1,949 vs 1,948 (같음), 거리 1.50km vs 1.40km (-7%), 시간 16:41 vs 18:32 (삼성은 정지 시간 자동 제외)
- 버스 테스트: 걸음 0인데 GPS 거리 80~130m 누적 (막혀서 시속 9km 미만으로 이동)

## 1. 자동 일시정지/재개
- WalkStatus에 `AUTO_PAUSED` 추가 (기존 PAUSED는 사용자 일시정지).
- RUNNING 중 **최근 10초 동안 걸음 증가 0** 이고 **앵커 대비 이동 < 5m** 이면 AUTO_PAUSED: activeMs 증가 중지, 거리 미누적(점은 저장해도 됨).
- AUTO_PAUSED 중 **걸음 증가가 3초 안에 3걸음 이상**이거나 앵커 대비 이동 ≥ 8m이면 RUNNING 복귀.
- 걸음 센서가 없거나 권한이 없으면 걸음 조건 대신 GPS 이동만으로 판단(10초간 이동 < 5m).
- 걸음 즉시성: STEP_COUNTER를 `registerListener(..., SENSOR_DELAY_NORMAL, 0)`(maxReportLatency 0)으로 등록하고, TYPE_STEP_DETECTOR가 있으면 함께 등록해 최근 걸음 시각 판단에 사용.
- 설정 `autoPause` 꺼져 있으면 동작 안 함.

## 2. 음성 안내 (walk/WalkVoice.kt)
- Android TextToSpeech(Locale.KOREAN). 서비스 시작 시 초기화, 종료 시 shutdown.
- 문구: 자동 일시정지 "운동을 일시정지했습니다", 재개 "운동을 다시 시작합니다". (수동 일시정지/재개/종료 때는 말하지 않음)
- **이어폰 연결 시에만**: AudioManager.getDevices(GET_DEVICES_OUTPUTS)에 TYPE_WIRED_HEADSET, TYPE_WIRED_HEADPHONES, TYPE_BLUETOOTH_A2DP, TYPE_BLUETOOTH_SCO, TYPE_USB_HEADSET 중 하나가 있을 때.
- AudioFocus AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK 요청 후 말하고, 끝나면(UtteranceProgressListener) 반납. AudioAttributes USAGE_ASSISTANCE_NAVIGATION_GUIDANCE.
- 같은 안내가 15초 안에 반복되면 생략(상태가 왔다 갔다 할 때 시끄럽지 않게).
- TTS 초기화 실패/한국어 미지원이면 조용히 무시 + 로그.
- 설정 `voice` 꺼져 있으면 말하지 않음.

## 3. 탈것 감지
- RUNNING 중 최근 20초 걸음 증가 < 5 인데 GPS로는 앵커 대비 이동이 계속되면(20초간 ≥ 15m) **"탈것 구간"**으로 보고 거리 미누적, activeMs도 미증가. 상태는 AUTO_PAUSED와 구분해 `WalkLiveState.inVehicle = true` (음성 안내 없음).
- 걸음이 다시 늘면(3초 3걸음) 해제. 해제 시 앵커를 현재 점으로 재설정(탈것 이동 거리가 더해지지 않게).
- 걸음 센서가 없으면 이 기능은 끔.

## 4. 거리 보정 완화
- 앵커 최소 거리: 두 점 정확도 평균 ≤ 8m 이면 2m, 그 외 기존 max(3m, 평균정확도*0.5).

## 5. 설정 (walk/WalkSettings.kt)
- SharedPreferences: `autoPause`(기본 true), `voice`(기본 true). get/set 함수 + Flow 또는 StateFlow로 노출 (codex-2가 UI 스위치에서 사용).

## 6. 상태/로그
- WalkLiveState에 `autoPaused: Boolean`(또는 status AUTO_PAUSED), `inVehicle: Boolean` 추가. 알림 문구: AUTO_PAUSED면 "자동 일시정지 · 1.23km", 탈것이면 "이동 중(걸음 없음) · 1.23km".
- 로그(HCDEBUG): `walk autoPause on/off`, `walk vehicle on/off`, `walk voice said=<문구> | skipped(reason)`. 종료 로그에 autoPausedMs, vehicleMs, vehicleDistM(제외된 거리) 추가.
- WalkSessionEntity 스키마 변경 금지(DB 버전 유지). 필요한 값은 로그로만.

## 7. 테스트 (WalkMathTest 등, 순수 함수로 분리해서)
- 10초 걸음 0 + 정지 → 자동 일시정지, 3초 3걸음 → 재개
- 걸음 거의 없이 GPS 직선 이동 30초 → 거리 0 (탈것), 이후 걸음 재개 시 탈것 거리 미포함
- 정확도 좋은 직선 보행에서 2m 기준으로 거리 오차 ±5%

## 완료
- assembleDebug testDebugUnitTest 성공, tasks/walk-v2-logic-done.md 에 요약 + 바뀐 공개 API(WalkLiveState 필드, WalkStatus, WalkSettings)
