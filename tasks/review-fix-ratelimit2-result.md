FAIL
1. OK — refresh에서 진단 조회를 제거하고 진단 버튼으로 조회한다.
2. OK — 권한 확인 성공을 기억하며 SecurityException 시 기억을 지우고 NeedPermission으로 전환한다.
3. OK — 일반 조회 실패 시 보관 데이터와 배너를 표시하고 데이터가 없으면 Error를 표시한다.
4. NG — StepsViewModel.kt:70,110: 진단과 refresh가 겹쳐 진단에서 쿨다운이 시작되면 이미 진행 중인 refresh가 취소되지 않아 쿨다운 중에도 후속 조회 및 성공 시 배너 덮어쓰기가 가능하다.
5. OK — 지정된 JDK로 assembleDebug 성공 (36개 작업 UP-TO-DATE).
