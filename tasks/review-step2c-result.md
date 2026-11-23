FAIL
1. OK — 체성분 4종 모두 기본 maxPages=5인 readAllPages를 사용하며 직접 pageToken 루프는 없다.
2. OK — diag만 플래그 true이며 4개 권한은 diag 매니페스트와 플래그 조건부 PERMISSIONS에 있다.
3. OK — 몸무게 뒤 체성분 4종을 순차 조회하고 한도 초과 시 나머지를 skipped 처리하며 쿨다운한다.
4. NG — StepsViewModel.kt:110,193 및 HcLog.kt:26: 전역 호출 카운터 차이를 사용하므로 진단 조회가 refresh와 겹치면 refresh의 calls에 진단 호출도 포함된다. 요청된 시작/종료·섹션·begin·range 로그는 있다.
5. OK — 지정된 JDK로 assembleDebug 및 assembleDiag 성공 (76개 작업 UP-TO-DATE).
