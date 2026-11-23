# step2c-logging 완료
- HcLog.kt: HcStats(호출/실패 카운터) 추가, hcCall에 "[name] begin" 줄과 카운트 추가, readAllPages 첫 요청 시 "[name] range=시작~끝 pageSize maxPages" 로그(요청 객체에서 추출, 시그니처 불변).
- StepsViewModel.kt: refresh/refreshIfStale에 reason(resume|permission|button|retry) 추가, "=== refresh start #N reason ===" / "=== refresh end #N calls fail ms result ===" (ok|partial|ratelimited|cancelled), 섹션 start/ok/fail/skipped 로그(요약 포함), "cooldown start 15m", "refresh ignored: cooldown Ns" 로그.
- MainActivity.kt: 호출부에서 reason 전달.
- 빌드: assembleDebug BUILD SUCCESSFUL. hcCall/readAllPages 시그니처는 변경 없음. 로그 확인: adb logcat -s HCDEBUG
