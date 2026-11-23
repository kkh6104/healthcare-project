# diag-ratelimit 완료
- HcLog.kt 신설: hcCall(name, count, block)이 HCDEBUG 태그로 ok/FAIL(예외 클래스·메시지·cause 2개)·소요시간·count를 기록하고 예외는 그대로 재던짐. Section<T>(Ok/Failed)도 여기에 정의.
- 모든 Health Connect 호출을 hcCall로 감쌈(Sleep/Weight 로직 변경 없음). refresh 시작 시 "=== refresh start ===" 로그.
- StepsViewModel: Loaded를 steps/heartRate/sleep/weight 섹션 구조로 변경. 순서대로 하나씩 로딩하고 한 섹션이 실패해도 다음 계속. quota/rate limit이면 남은 섹션은 "건너뜀(한도 초과)"로 표시 + 15분 쿨다운 + 배너. 권한 기억/쿨다운/진단 버튼/5분 refreshIfStale 유지.
- MainActivity: 실패 섹션은 "OO 불러오기 실패: 메시지"를 작은 빨간 글씨로 표시, 성공 섹션은 기존 카드.
- 빌드: assembleDebug BUILD SUCCESSFUL (실기기 미확인)

## 감싼 호출 이름
getGrantedPermissions, aggregateGroupByPeriod(steps), aggregate(heartRate), readRecords(heartRate), readRecords(steps) [진단], readRecords(sleep), readRecords(weight)

로그 확인: adb logcat -s HCDEBUG
