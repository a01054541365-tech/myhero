# JJK Spec — 06. 확정 결정 기록

> 이 문서는 STEP 2·3에서 확정된 수치와 설계 결정을 기록한다.
> 모든 항목 확정 완료 (2026-05-23).
> §LOCK 수치는 변경 금지.

---

## STEP 2 — 수치 확정

| # | 항목 | 확정값 | 근거 |
|---|---|---|---|
| 2-1 | 마허라가 의식 적응 임계 횟수 | **5회** | config 키 `maharagaThreshold: 5` |
| 2-2 | `sealDurationTicks` | **600틱 (30초)** | config 키 `sealDurationTicks: 600` |
| 2-3 | Zone 종료 기준 | **zoneEndTick 만료, 지속 200틱 (10초)** | config 키 `zoneDurationTicks: 200` |
| 2-4 | CE 재생 수치 | **전투 외 1.0/틱, 전투 중 0.2/틱** | config 키 `ceRegenOutOfCombat: 1.0` / `ceRegenInCombat: 0.2` |
| 2-5 | 속박 선언 방식 | **기존 keyId 처리** | 별도 C2S 패킷 추가 없음 |

---

## STEP 3 — 충돌 항목 확정

| # | 항목 | 확정 | 근거 |
|---|---|---|---|
| 3-1 | 쿨타임 검증 단계 | **2단계** | CombatPipeline §3-6 순서 기준 |
| 3-2 | `domainCooldownUntil` vs Map `"domain"` | **PlayerData 전용 필드 단독 사용** | cooldowns Map `"domain"` 키 사용 금지 |
| 3-3 | `awakeningCooldownUntil` vs Map `"awakening"` | **PlayerData 전용 필드 단독 사용** | cooldowns Map `"awakening"` 키 사용 금지 |
| 3-4 | 개방형 영역 자박 CE 2배 규칙 | **포함** | `isOpen=true` 시 `ceCost × 2` 적용 |
| 3-5 | 잭팟 config 조정 범위 | **`jackpotDurationMinTicks: 60` / `jackpotDurationTicks: 251`** | §LOCK 기본값 유지 |
| 3-6 | `skill_seal` 키 범위 | **히구루마 + 마허라가 동일 `"skill_seal"` 키 공유** | 별도 키 분리 없음 |
| 3-7 | 옷코츠 V 자기 회복 방식 | **직접 HP 회복** | `healingActive` 세팅 없이 서버 로직에서 즉시 처리 |

---

## 파일 배치 확정 (2026-05-23)

| 파일 | 경로 |
|---|---|
| `config.json` | `run/config/jjk/config.json` |
| `techniques.json` | `run/config/jjk/techniques.json` |
| `domains.json` | `run/config/jjk/domains.json` |
| `jjk_spec_06_decisions.md` | `docs/jjk_spec_06_decisions.md` |

---

## config.json 전체 키 목록 (2026-05-23 확정)

| 키 | 값 | 비고 |
|---|---|---|
| `mangaExpEnabled` | `false` | §LOCK |
| `allowDuplicateCharacter` | `false` | §LOCK |
| `gradePvpScaling` | `true` | §LOCK |
| `pvpDamageCapMaxHpRatio` | `0.40` | §LOCK |
| `trialSuccessRate` | `0.60` | §LOCK — 히구루마 재판 성공 기준 |
| `bindingVowBreakBySpecialGradeHit` | `true` | §LOCK |
| `bindingVowTimeoutTicks` | `300` | §LOCK — 15초 |
| `domainBannedChunks` | `["lobby:0,0", "training:0,0"]` | 운영자 추가 가능 |
| `jackpotDurationTicks` | `251` | §LOCK — 기본값 |
| `jackpotDurationMinTicks` | `60` | 운영자 조정 하한 |
| `jackpotDurationMaxTicks` | `251` | §LOCK — 상한 |
| `respawnDelayTicks` | `100` | 5초 |
| `respawnLocation` | `"SPAWN"` | |
| `respawnCePercent` | `0.50` | |
| `respawnHpPercent` | `0.50` | |
| `fingerDropRate` | `0.10` | |
| `fingerMaxCount` | `20` | |
| `blackFlashBaseRate` | `5` | §LOCK |
| `blackFlashZoneBonus` | `10` | §LOCK |
| `xpMultiplierGradeDiff` | `1.5` | |
| `allowCharacterReselect` | `false` | |
| `rikaLifetimeTicks` | `200` | 옷코츠 리카 소환 지속시간 |
| `maharagaThreshold` | `5` | 마허라가 의식 적응 임계 피격 횟수 |
| `sealDurationTicks` | `600` | 술식 봉인 지속 30초 |
| `zoneDurationTicks` | `200` | Zone 지속 10초 |
| `ceRegenOutOfCombat` | `1.0` | 전투 외 틱당 CE 재생 |
| `ceRegenInCombat` | `0.2` | 전투 중 틱당 CE 재생 |

---

## domains.json radius 확정 (2026-05-23)

| domainId | radius | wallHp | sureHitActive |
|---|---|---|---|
| `gojo_unlimited_void` | 30 | 1500 | true |
| `sukuna_malevolent_shrine` | 200 | 0 | true |
| `mahito_self_embodiment` | 15 | 1500 | true |
| `itadori_unnamed` | **20 확정** | 1500 | true |
| `megumi_chimera_shadow` | **20 확정** | 750 | false |
| `jogo_volcano_domain` | **25 확정** | 1500 | true |
| `hakari_jackpot_domain` | **20 확정** | 1500 | true |

---

## techniques.json keyId 체계

각 캐릭터 내 키 순서: F=0, Shift+F=1, R=2, Shift+R=3, V=4.
미사용 키는 `baseDamage=0, ceCost=0, cooldownTicks=0`으로 등록 (`SkillResult.NOT_IMPLEMENTED` 반환).
스쿠나 5개 키 전부 NOT_IMPLEMENTED 스텁. animId는 예약값 21~24 사용 (V=0).
