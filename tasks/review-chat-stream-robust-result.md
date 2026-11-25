PASS
- 로그는 kind/code/finish(대문자 패턴만)/예외 클래스명·attempt model/ms/outcome만(키·내용 없음), 깨진 SSE 줄은 건너뛰고 error 이벤트·candidates 없는 이벤트 처리
- finishReason MAX_TOKENS/기타 → 지금까지 텍스트+안내 문구, 스트림 중단 시 같은 모델 1회 재시도 후 부분 텍스트 있으면 "(답변이 중간에 끊겼어요)" 붙여 저장
- 재시도는 retryBrokenStream 1회로 고정, 모델·thinking·도구 단계 모두 유한 / assembleDebug·testDebugUnitTest BUILD SUCCESSFUL
