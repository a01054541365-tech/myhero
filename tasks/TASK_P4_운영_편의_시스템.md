# TASK P4: 운영 편의 시스템

> 우선순위: 🟠 중간 (1단계 — HOTFIX 완료 후 시작, P1·P2와 병렬 가능)  
> 소스셋: main  
> 완료 기준: `./gradlew compileJava` 성공

---

## 의존 관계

- **선행 필수:** HOTFIX 완료
- **P1·P2와 병렬 가능**
- **후행 차단:** P3 (questProgress/seasonXp 버전 확정 필요)

---

## 수정 항목

### 1. 서버 종료 Flush
- 서버 종료 시 미저장 PlayerData 즉시 flush
- P0 이벤트 저장 보장

### 2. 7일 백업
- PlayerData DB 7일 자동 백업 시스템
- 백업 파일 보관 경로 설정

### 3. 퀘스트 시스템 (15종)
- `PlayerData`에 필드 추가:
  - `questProgress` (Map)
  - `completedDailyQuests` (Set)
  - `completedWeeklyQuests` (Set)
  - `lastQuestResetDay` (long)
- `snapshot()` 복사 라인 추가 필수
- `DbMigrator.java` CURRENT_VERSION +1 + ALTER TABLE
- 일일/주간 퀘스트 15종 구현

### 4. Discord 연동
- 서버 이벤트 → Discord 웹훅 알림
- 주요 이벤트: 사망, 영역 전개, 처형검 획득 등

### 5. 채팅 채널 분리
- 진영별/전체 채팅 채널 분리
- `TeamManager` 연동

### 6. 시즌 시스템
- `PlayerData`에 `seasonXp` 필드 추가
- 시즌 XP 획득 및 보상 체계

### 7. DB 마이그레이션 CLI
- 관리자가 직접 마이그레이션을 실행할 수 있는 CLI 명령 추가

---

## DB 마이그레이션

```
P1의 CURRENT_VERSION 이후에 수행
현재 CURRENT_VERSION 읽기 → +1
ALTER TABLE player_data ADD COLUMN quest_progress TEXT DEFAULT '{}';
ALTER TABLE player_data ADD COLUMN completed_daily_quests TEXT DEFAULT '[]';
ALTER TABLE player_data ADD COLUMN completed_weekly_quests TEXT DEFAULT '[]';
ALTER TABLE player_data ADD COLUMN last_quest_reset_day INTEGER DEFAULT 0;
ALTER TABLE player_data ADD COLUMN season_xp INTEGER DEFAULT 0;
```

## 검증
```
./gradlew compileJava
서버 종료 → flush 확인
퀘스트 완료 → 보상 확인
Discord 웹훅 이벤트 수신 확인
```
