PASS
- 5xx(500/502/503/504) 같은 모델 1.5초 후 1회 재시도 → 다음 모델, ModelFallback이 질문(도구 루프) 단위로 유지, 마지막 모델 실패 시 Busy
- 400+API_KEY_INVALID → Other("API 키가 올바르지 않습니다"), ChatTab/Repository when에 Busy 분기 있음, 로그는 model/tools/ms/result만(키 없음)
- assembleDebug·testDebugUnitTest BUILD SUCCESSFUL
