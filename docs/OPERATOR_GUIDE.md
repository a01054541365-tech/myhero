# JJK Fabric Mod — 운영자 가이드

> Fabric 1.21.1 / Java 21 / Dedicated Server 전용

---

## 설치

1. Fabric Loader 0.16.5+ 설치
2. 아래 mod jar를 `mods/` 폴더에 복사:
   - `jjk-fabric-1.0.0.jar`
   - `geckolib-fabric-1.21.1-4.8.3.jar`
   - `player-animation-lib-fabric-2.0.4+1.21.1.jar`
   - `fabric-api-0.116.6+1.21.1.jar`
3. 서버 최초 기동 시 `config/jjk/` 폴더와 3개 JSON 파일 자동 생성
4. OptiFabric과 비호환 — 함께 사용 불가

---

## 설정 파일

### 밸런스 데이터 주도 (2026-06-11)

스킬별 **데미지·CE·쿨다운**은 `config/jjk/techniques.json`(영역 CE는 `domains.json`)이 **런타임 권위**다.
모든 스킬셋이 부팅 시 로드된 이 값을 `keyId` 기준으로 읽으므로, JSON만 고치고 `/jj reload`하면
서버 재시작 없이 즉시 튜닝된다. 캐릭터 CE풀·재생·HP도 `techniques.json`의 `characterStats`에서 적용된다.

> 예외(JSON 미등재, 코드 상수 유지): 고죠 커튼·이타도리 shrine·비술사(todo) 근접 피해·DoT/발사체 피해.

### config/jjk/config.json

운영자가 조정 가능한 주요 항목:

| 키 | 기본값 | 설명 |
|---|---|---|
| `allowDuplicateCharacter` | false | 같은 캐릭터 중복 선택 허용 여부 |
| `allowCharacterReselect` | false | 캐릭터 재선택 허용 여부 |
| `gradePvpScaling` | true | 등급 차이 PvP 배율 적용 여부 |
| `jackpotDurationTicks` | 251 | 하카리 잭팟 지속 시간 (60~251 범위) |
| `jackpotDurationMinTicks` | 60 | 잭팟 최소 지속 시간 |
| `jackpotDurationMaxTicks` | 251 | 잭팟 최대 지속 시간 |
| `respawnDelayTicks` | 100 | 사망 후 부활 대기 시간 (틱) |
| `respawnCePercent` | 0.50 | 부활 시 CE 회복 비율 |
| `respawnHpPercent` | 0.50 | 부활 시 HP 회복 비율 |
| `domainBannedChunks` | [] | 영역 전개 금지 청크 목록 (예: `"world:0,0"`) |
| `xpMultiplierGradeDiff` | 1.5 | 등급 차이 2 이상 전투 시 XP 배율 |
| `ceRegenOutOfCombat` | 1.0 | 전투 외 CE 재생 (틱당) |
| `ceRegenInCombat` | 0.2 | 전투 중 CE 재생 (틱당) |
| `mangaExpEnabled` | false | 만화 원작 XP 시스템 활성화 |

**§LOCK 수치 — 절대 변경 금지 (게임 밸런스 설계값):**

| 항목 | 고정값 |
|---|---|
| PvP 피해 상한 (`pvpDamageCapMaxHpRatio`) | `0.40` |
| 흑섬 기본 발동률 (`blackFlashBaseRate`) | `5` |
| 흑섬 Zone 보너스 (`blackFlashZoneBonus`) | `10` |
| 손가락 드롭률 (`fingerDropRate`) | `0.10` |
| 손가락 최대 보유 (`fingerMaxCount`) | `20` |
| 리카 생존 시간 (`rikaLifetimeTicks`) | `200` |
| 재판 성공률 (`trialSuccessRate`) | `0.60` |

설정 변경 후 서버 재시작 없이 즉시 적용: `/jj reload`

---

## /jj 커맨드

| 커맨드 | 권한 | 설명 |
|---|---|---|
| `/jj select <characterId>` | 플레이어 | 캐릭터 선택 |
| `/jj info` | 플레이어 | 현재 상태 조회 (캐릭터·CE·HP·각성·Zone·부담·봉인) |
| `/jj reload` | OP 2 | config·techniques·domains 핫리로드 (스킬/영역 수치 즉시 재적용) |
| `/jj build city` | OP 2 | 현재 위치에 시부야 도심 구조물 생성 (기존 월드용) |
| `/jj selectchar <player> <characterId>` | OP 2 | 다른 플레이어 캐릭터 강제 지정 |
| `/jj data save <player>` | OP 2 | 플레이어 데이터 즉시 저장 |
| `/jj domain clear` | OP 2 | 모든 활성 영역 강제 종료 |
| `/jj debug tps` | OP 2 | TPS·MSPT·힙 사용량 출력 |
| `/jj debug domain <domainId>` | OP 2 | 시전자 위치에 영역 강제 전개 (테스트용) |
| `/jj debug give finger` | OP 2 | 손가락 +1 지급 |

### 캐릭터 ID 목록 (11개)

| 진영 | ID |
|---|---|
| 주술사 (JUJUTSU_SORCERER) | `gojo` `itadori` `megumi` `okkotsu` `nanami` `inumaki` `hakari` `higuruma` |
| 저주 영령 (CURSED_SPIRIT) | `mahito` `jogo` `sukuna` |

### 영역 domainId 목록

`gojo_unlimited_void` · `sukuna_malevolent_shrine` · `mahito_self_embodiment` ·
`itadori_unnamed` · `megumi_chimera_shadow` · `jogo_volcano_domain` · `hakari_jackpot_domain` ·
`okkotsu_true_mutual_love` · `cursed_spirit_domain`(NPC 전용)

---

## 캐릭터별 주요 특이사항

| 캐릭터 | 특이사항 |
|---|---|
| 이누마키 | 채팅으로 스킬 발동: `!멈춰` `!터져` `!잠들어` `!달려` |
| 하카리 | 잭팟(1/239 확률) 중 CE 상한 체크 우회, 매 틱 HP 1 자동 회복 |
| 히구루마 | 증거 제출 후 재판 성공 시 처형검 획득 (1회성, 사망 시 소멸) |
| 마히토 | 자폐원돈과 전개 시 소유자도 매 틱 5 데미지 (양날성) |
| 스쿠나 | CURSED_SPIRIT 진영, 개방형 영역 (경계선 없음) |

---

## 데이터 저장

- **DB 경로:** `world/jjk/player_data.db` (SQLite)
- **감사 로그:** `world/jjk/audit_log.db`
- **P0 즉시 저장 이벤트:** 손가락 획득·캐릭터 선택·사망·처형검 획득·영역 전개
- **P1 주기 저장:** 일반 CE·HP 변화

정기 백업 권장 (최소 1일 1회).

---

## 성능 기준

- 권장 TPS: 18+ (idle 기준 20 TPS)
- 동시 활성 영역: 전체 4개 / 팀당 2개 (DomainManager 자동 제한)
- TickScheduler 등록 타스크: 7개 (period 1~10틱)

---

## 문제 해결

**서버 기동 실패 (DB Migration 오류):**
`world/jjk/player_data.db` 삭제 후 재기동 (플레이어 데이터 초기화됨)

**캐릭터 선택 불가 (이미 선택됨):**
`config.json`의 `allowCharacterReselect: true` 임시 설정 → `/jj reload` → 재선택 → 원복

**TPS 저하:**
`/jj debug tps`로 현재 MSPT 확인.
`ceRegenOutOfCombat` / `ceRegenInCombat` 값을 줄이면 CE 재생 틱 부하 감소.

**영역 응답 없음:**
`/jj domain clear`로 모든 영역 강제 종료 후 재전개.

---

## 스키마 버전 이력

| 버전 | 변경 내용 |
|---|---|
| 1 | 초기 스키마 (Phase 1) |
| 2 | zone_entry_tick, last_attack_tick 컬럼 추가 (Phase 2) |
