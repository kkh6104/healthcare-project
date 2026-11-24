# ui-fix3 완료
- BarChart: "목표" 라벨을 차트 오른쪽 바깥 여백(32dp, 막대 영역에서 제외)으로 이동. 막대 값 글자가 목표선과 겹치면(막대가 충분히 높을 때) 막대 안쪽 위로 옮기고 색은 onPrimary(강조 막대)/onSurface(흐린 막대).
- SleepColors.light(): primaryContainer → secondary (deep=primary, light=secondary, rem=tertiary, awake=error).
- LineChart: minYSpan: Float? = null 추가(범위가 좁으면 중앙 기준으로 확장), 상하 10% 여백 적용. 호출부는 변경 안 함. ui/charts/ 파일만 수정, assembleDebug 성공(화면은 실기기에서 확인 안 함).
