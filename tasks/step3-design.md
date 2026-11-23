# 3단계 설계: 데이터 로컬 저장

## 원칙
- 저장 형식은 Health Connect 기준. 출처(source)로 health_connect / samsung_csv 구분 (CSV 가져오기는 다음 단계).
- 화면은 DB에서 바로 그림 → Health Connect는 "새로 생긴 부분"만 동기화.

## 저장소: Room (SQLite)
### 1) health_record (원본)
| 컬럼 | 설명 |
|---|---|
| id | 자동 증가 PK |
| type | STEPS, HEART_RATE, SLEEP_SESSION, WEIGHT, BODY_FAT, BONE_MASS, LEAN_BODY_MASS, BODY_WATER_MASS |
| startTime / endTime | epoch ms (순간 측정은 start=end) |
| zoneOffsetSec | 기록 당시 시간대 |
| value / unit | 걸음=count, 심박=bpm, 수면=min(잠든 시간), 무게=kg, 체지방=% |
| source | "health_connect" / "samsung_csv" |
| originPackage | 원래 앱 (com.sec.android.app.shealth, 체중계 앱 등) |
| externalId | HC record metadata.id (심박 샘플은 "recordId#index") |
| lastModified | HC metadata.lastModifiedTime |
| extraJson | 수면 단계 등 부가정보 (nullable) |
- UNIQUE(source, type, externalId) → 같은 기록 다시 받아도 덮어쓰기(upsert)

### 2) daily_summary (AI 챗봇·화면용 하루 요약)
date(PK), steps, hrAvg, hrMin, hrMax, sleepMinutes, weightKg, bodyFatPct, boneMassKg, leanMassKg, bodyWaterKg, updatedAt
- 걸음 합계는 원본 합산이 아니라 HC aggregate 결과 사용 (폰+워치 중복 제거 때문)

### 3) sync_state
type(PK), syncedUntil(epoch ms)

## 동기화 규칙
- 종류별로 하나씩 순차 (기존 섹션 순서 유지, 한도 초과 시 중단+쿨다운 유지)
- 범위: 첫 동기화 = 최근 30일 / 이후 = (syncedUntil - 1일) ~ 지금 (늦게 들어온 기록 보완용 1일 겹침)
- readAllPages(maxPages 5) 사용, 결과 upsert, 성공 시 syncedUntil 갱신
- 동기화한 날짜들의 daily_summary 재계산 (걸음은 해당 날짜 범위 aggregateGroupByPeriod 1회)
- 한계: HC에서 삭제된 기록은 로컬에 남음 (추후 Changes API로 보완)

## 화면
- DB의 daily_summary / 최신 원본을 Flow로 구독 → 앱 켜자마자 표시
- 동기화는 뒤에서 진행 (기존 5분 규칙·새로고침 버튼 유지), 상단에 "마지막 동기화: 오후 11:08" 표시

## 작업 분할
1. claude-2: Room 설정 + 엔티티 3개 + DAO + DB (새 파일 + gradle)
2. 동시에: claude-1 동기화(HC → DB) / claude-2 daily_summary 계산 + 화면을 DB 기반으로
3. claude-1: 연결 · checkcodex-1 최소 검수 · 설치
