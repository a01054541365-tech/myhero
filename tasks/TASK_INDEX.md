# JJK Mod 2차 업그레이드 — 태스크 인덱스

> 생성일: 2026-06-05  
> 기준: 크래시 로그 분석 + 26개 추가 항목 (C-02, O-01 제외)

---

## 실행 순서 (의존 관계)

```
[0단계 — 즉시 실행, 나머지 모두 차단]
  HOTFIX: CursedSpiritEntity NPE 크래시
  → 이것 없이는 서버가 월드 진입 직후 종료됨

[1단계 — HOTFIX 완료 후, 병렬 가능]
  P1: 원작 캐릭터·스킬 완성 (쵸소, 세계절단, 완전부활, 옷코츠)
  P2: 밸런스·보안 수정 (속박파훼, RTT클램프, 하카리, 이누마키, 제재)
  P4: 운영 편의 시스템 (서버종료flush, 백업, 퀘스트, Discord, 채팅, 시즌, DB마이그)
  P5: 리소스 누락 수정 (아이템 모델 12개, 애니메이션, 쉐이더 조사)

[2단계 — P1 완료 후 (DbMigrator 버전 확정 이후)]
  P3: 경제 시스템 + CE Control 성장 경로 + 주구 기초
  → P1에서 bloodResource 마이그레이션,
    P4에서 questProgress/seasonXp 마이그레이션이 완료된 후
    P3에서 추가 마이그레이션 수행 (버전 충돌 방지)
```

---

## 파일별 요약

| 파일명 | 내용 | 소스셋 | 우선순위 |
|--------|------|--------|---------|
| `HOTFIX_CursedSpirit_NPE_크래시.md` | NPE 크래시 + config 누락 키 수정 | main | 🔴 즉시 |
| `TASK_P1_원작_캐릭터_스킬_완성.md` | 쵸소 추가, 세계절단, 완전부활, 옷코츠 영역 재분류 | main | 🔴 높음 |
| `TASK_P2_밸런스_보안_수정.md` | 속박파훼, RTT클램프, 하카리 상한, 이누마키 채팅, 제재 | main | 🔴 높음 |
| `TASK_P3_경제_CEControl_주구.md` | CE결정체 화폐, ceControl 성장, 주구 드롭 | main | 🟠 중간 |
| `TASK_P4_운영_편의_시스템.md` | 서버종료flush, 7일백업, 퀘스트15종, Discord, 채팅채널, 시즌, DB마이그CLI | main | 🟠 중간 |
| `TASK_P5_리소스_누락_수정.md` | 아이템 모델 JSON 12개, 애니메이션 더미, 쉐이더 조사 | resources | 🟡 낮음 |

---

## DB 마이그레이션 버전 관리 (충돌 방지)

| 태스크 | 추가 필드 | 버전 |
|--------|----------|------|
| 현재 서버 (기준) | — | 13 또는 14 |
| P1 | `bloodResource` (쵸소) | 현재+1 |
| P4 | `questProgress`, `completedDailyQuests`, `completedWeeklyQuests`, `lastQuestResetDay`, `seasonXp` | P1+1 |
| P3 | CE결정체 관련 필드 (있으면) | P4+1 |

**각 태스크 시작 전 `DbMigrator.java`의 `CURRENT_VERSION`을 읽고 +1 증가.**  
이전 태스크가 이미 증가시켰으면 그 값에서 다시 +1.

---

## 공통 제약 (모든 태스크 공통)

1. 모든 파일 수정 전 **읽기 먼저**
2. `src/main`에 client-only import 절대 금지
3. `GeoEntity` 서버 엔티티 implements 금지
4. `CombatPipeline` 틱 순서 1~9 수정 금지
5. `DamageCalculator` 공식 구조 수정 금지
6. `§LOCK` 수치(`blackFlashBaseRate`, `awakeningHpThreshold` 등) 수정 금지
7. 각 태스크 완료 기준은 `./gradlew compileJava` 성공

---

> **삭제 기준:** P5 완료 후 이 폴더의 모든 .md 파일 삭제.
