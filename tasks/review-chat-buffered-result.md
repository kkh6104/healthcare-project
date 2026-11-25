PASS
1. 화면 표시 없이 버퍼링, 중단 시 StreamInterruptedException은 부분 텍스트 없이 버리고 다음 시도는 처음부터 → 화면·DB에 부분 텍스트 없음
2. QuestionAttempts가 질문당 5회 상한, 1/2/4/4초±20% 대기, 모델별 5분 쿨다운, 404 모델 제외(전부 제외 시 Busy) → 유한
3. 401/403/400 키 오류·429·UnknownHost/연결 실패는 ChatException으로 재시도 없이 즉시 실패
4. UI는 progress+CircularProgressIndicator 사용, streaming 참조 없음(저장소에 남은 streaming은 항상 null), 다시 보내기·복사 유지
5. 로그에 키·내용 없음(attempts/model/ms/outcome 등), assembleDebug·testDebugUnitTest BUILD SUCCESSFUL
