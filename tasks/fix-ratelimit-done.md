# fix-ratelimit 완료
- StepsViewModel: lastLoadedAt(Loaded 성공 시 갱신)와 refreshIfStale(maxAgeMs = 5분) 추가. refresh()는 그대로(강제).
- 오류 메시지(cause 체인 포함)에 "quota"/"rate limit"(대소문자 무시)이 있으면 한국어 제한 안내 문구로 대체.
- MainActivity: RESUMED 시 vm.refreshIfStale() 사용. 권한 콜백과 버튼들은 vm.refresh() 유지.
- assembleDebug 성공.
