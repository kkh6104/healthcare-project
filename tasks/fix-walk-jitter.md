# 구현 요청 (claude-1): 걷기 GPS 흔들림 보정
## 실기기 측정 (session 1, 77초)
- 점 65개, **실제 간격 약 1초**(설정 2초 무시됨), 정확도 5.5~12m, 점 간 이동 대부분 0.0~1.0m
- 정지 구간에도 0.1~0.4m씩 거리 누적, 한 번 1초에 3.7m 점프
- GPS 거리 41.8m vs 걸음 34보(약 25m) → 흔들림으로 거리 과대

## 수정 (WalkService / WalkMath)
1. LocationRequest: interval 2000, `setMinUpdateIntervalMillis(2000)`, `setMinUpdateDistanceMeters(2f)`.
2. 거리 누적 앵커 방식: 마지막으로 "인정한 점"(anchor)에서의 거리 d가 `max(3m, (anchor.accuracy + new.accuracy) / 2 * 0.5)` 이상일 때만 거리에 d를 더하고 anchor 갱신. 그 미만이면 거리 미누적(점은 저장해도 됨, 표시용).
3. 속도 필터: anchor 대비 속도 > 2.5 m/s(시속 9km)면 버림. 단 연속 3번 버려지면(실제로 멀리 이동한 경우) 마지막 점을 새 anchor로 재설정(거리는 더하지 않음, 로그).
4. 정확도 필터: > 25m 버림.
5. 표시용 경로: WalkLiveState.points와 상세 화면 경로는 **이동 평균(최근 5점, 정확도 가중)으로 다듬은 점** 사용. DB에는 원본 저장. 다듬기 함수는 WalkMath에 internal 순수 함수로 두고 상세 화면(저장된 원본 점 로드 시)에도 동일 적용.
6. 종료 시 로그: `walk end dist=Nm steps=N stepDist≈N(=steps*0.7) accepted=N rejected=N`
7. WalkMathTest에 케이스 추가: 정지 상태 흔들림(반경 3m 안에서 30점) → 거리 0, 직선 보행 → 실제 거리 ±10%, 단발 점프 무시.
수정 허용: walk/WalkService.kt, walk/WalkMath.kt, 테스트, ui/walk/WalkDetail.kt(다듬기 적용만). assembleDebug testDebugUnitTest 성공 후 tasks/fix-walk-jitter-done.md 2~4줄.
