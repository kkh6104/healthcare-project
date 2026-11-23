# 구현 요청: 심박수 최근값 샘플 범위 필터

검수 지적: tasks/review-step2a-result.md 3번.
HealthConnectManager.heartRateToday()의 최근값 계산에서, 레코드의 samples 중 **오늘 0시 이상 ~ 지금 이하**(start <= time <= now) 인 샘플만 대상으로 가장 늦은 샘플을 고른다. 범위 안 샘플이 없으면 latestBpm/latestTime은 null.
- 범위 계산에 쓰는 start/now는 aggregate와 같은 값을 재사용.
- 다른 코드는 건드리지 않는다.
완료 조건: assembleDebug 성공(JAVA_HOME은 Android Studio jbr), tasks/fix-step2a-done.md 에 2~3줄 요약.
