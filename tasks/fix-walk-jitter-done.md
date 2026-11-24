# fix-walk-jitter 완료
- WalkMath: shouldAccept를 대체하는 DistanceTracker(앵커 방식) 추가 — 정확도 >25m 버림, 앵커 대비 속도 >2.5m/s 버림(연속 3번이면 앵커 이동·거리 미가산), 앵커 거리가 max(3m, 평균정확도*0.5) 이상일 때만 거리 가산. 표시용 다듬기 smoothWindow/smoothRoute(최근 5점 정확도 가중 이동 평균) 추가. 기존 shouldAccept와 그 테스트는 제거하고 같은 의미의 트래커 테스트로 교체.
- WalkService: LocationRequest interval 2000 + minUpdateInterval 2000 + minUpdateDistance 2m, 트래커로 거리 누적, 지도(WalkLiveState.points)에는 다듬은 점·DB에는 원본 저장, 일시정지/재개 시 앵커·창 초기화, 종료 시 "walk end dist=… steps=… stepDist≈…(=steps*0.7) accepted=… rejected=…" 로그, 앵커 재설정 로그. WalkDetail은 저장된 원본 점을 smoothRoute로 다듬어 표시.
- WalkMathTest에 정지 흔들림(반경 3m 30점→거리 0), 직선 보행(±10%), 단발 점프 무시, 연속 3회 앵커 재설정, 다듬기 케이스 추가 (총 18건). 중간에 내가 쓴 테스트 값의 속도가 필터에 걸려 1건 실패했으나 테스트 수정 후 통과.
- 빌드: assembleDebug testDebugUnitTest BUILD SUCCESSFUL. 실제 걷기로 거리 개선 여부는 실기기 확인 필요(임계값 3m/0.5배/2.5m/s는 지시 그대로).
