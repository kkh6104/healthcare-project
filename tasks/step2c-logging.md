# 구현 요청 (claude-1): 로그 강화

## 규칙
- 수정 허용: HcLog.kt, StepsViewModel.kt, MainActivity.kt. **새 파일 BodyCompositionRepository.kt / BodyCompositionSection.kt는 claude-2가 동시에 만드는 중이니 건드리지 말 것.**
- `HealthConnectClient.readAllPages(name, maxPages = 5, request)` 와 `hcCall(name, count, block)` **시그니처는 바꾸지 말 것** (claude-2가 사용 중). 내부 로그만 강화.

## 로그 (태그 HCDEBUG)
1. refresh 시작: `=== refresh start #N reason=<resume|permission|button|retry> ===` (N은 앱 실행 후 누적 번호). ViewModel의 refresh/refreshIfStale에 reason 파라미터 추가, MainActivity 호출부에서 전달.
2. refresh 종료: `=== refresh end #N calls=<HC 호출 수> fail=<실패 수> <총 ms>ms result=<ok|partial|ratelimited|cancelled> ===`. 호출 수 집계는 hcCall 내부 카운터로.
3. 섹션 시작/끝: `--- section <이름> start ---`, `--- section <이름> ok|fail|skipped (<요약>) ---` (요약 예: steps today=5113, weight latest=82.6 count=10)
4. hcCall: 기존 형식 유지 + 시작 줄 `[name] begin` 추가.
5. readAllPages: 기존 page/token/end 로그 유지 + 첫 요청 시 `[name] range=<시작>~<끝> pageSize=<n> maxPages=<n>` (요청 객체에서 꺼내기 어렵다면 readAllPages에 선택 파라미터 `rangeDesc: String? = null`을 **기본값 있게** 추가해도 됨).
6. 쿨다운 진입/무시: `cooldown start 15m`, `refresh ignored: cooldown <남은초>s`.

## 완료
- assembleDebug 성공 (PowerShell, JAVA_HOME = C:\Program Files\Android\Android Studio\jbr; 동시 빌드 lock 시 30초 후 재시도)
- tasks/step2c-logging-done.md 에 2~4줄
