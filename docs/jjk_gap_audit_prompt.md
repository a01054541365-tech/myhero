# JJK Mod — Gap Audit Prompt (Claude Code 전용)

> 목적: 프로젝트에 포함된 파일들을 명세 기준 문서와 대조하여,
> 미구현·오구현·누락 항목을 빠짐없이 보고한다.
> 코드 수정 금지. 감사 결과 보고만 수행한다.

---

## 역할

당신은 JJK Fabric Mod의 QA 감사 엔지니어입니다.
아래 **기준 문서**와 **감사 대상 파일**을 대조하여
불일치, 누락, 미구현 항목을 **카테고리별로 구조화된 보고서** 형태로 출력합니다.
코드를 수정하거나 값을 임의로 바꾸지 않습니다.

---

## 기준 문서 (우선순위 순서)

1. `docs/jjk_spec_01_core.txt` — 핵심 규칙, 전투, CE, §LOCK 수치
2. `docs/jjk_spec_02_characters.txt` — 캐릭터별 SkillSet, 5키 명세
3. `docs/jjk_spec_03_systems.txt` — 영역, PlayerData, 게임 플로우, 테스트 목록
4. `docs/jjk_spec_04_network_anim.txt` — 패킷, 애니메이션
5. `docs/jjk_spec_06_decisions.md` — 확정 결정값 (STEP 2·3 확정, 최우선 적용)

---

## 감사 대상 파일

| 파일 | 감사 항목 |
|---|---|
| `run/config/jjk/config.json` | §LOCK 수치 위반, §06 결정값 불일치, 명세 외 추가 키, 누락 키 |
| `run/config/jjk/techniques.json` | 캐릭터 × keyId 완전성, 필수 필드 누락, §LOCK baseDamage·ceCost·cooldownTicks 위반 |
| `run/config/jjk/domains.json` | 7개 도메인 완전성, §06 radius·wallHp 수치 불일치, 필수 필드 누락 |
| `src/main/java/com/jjk/**/*.java` | 클래스 존재 여부, PlayerData 필드, snapshot() 완전성, Migrator schemaVersion |
| `src/main/resources/jjk.mixins.json` | 등록된 Mixin 클래스 완전성 |

---

## 감사 절차

아래 순서대로 각 파일을 읽고 점검한다. 각 단계를 완료한 후 다음 단계로 진행한다.

### STEP A — config.json 감사

1. 파일을 읽는다: `run/config/jjk/config.json`
2. **판단 전 선행 조건**: 아래 "STEP 2 확정값" 목록을 적용하기 전에,
   반드시 `docs/jjk_spec_06_decisions.md` 파일을 직접 열어
   해당 키의 실제 기재값과 대조한다.
   프롬프트 제시값과 spec 파일값이 다르면 **spec 파일값을 우선**하고,
   차이 내용을 [NOTICE] 섹션에 별도 보고한다.
   spec 파일을 읽지 않은 채 프롬프트 제시값만으로 판단하는 것을 금지한다.
3. 기준 문서 `jjk_spec_06_decisions.md` §LOCK 목록과 대조한다.

   **§LOCK 확정값 목록 (변경 금지):**
   ```
   mangaExpEnabled          = false
   allowDuplicateCharacter  = false
   gradePvpScaling          = true
   pvpDamageCapMaxHpRatio   = 0.40
   trialSuccessRate         = 0.60
   bindingVowBreakBySpecialGradeHit = true
   bindingVowTimeoutTicks   = 300
   jackpotDurationTicks     = 251
   jackpotDurationMaxTicks  = 251
   blackFlashBaseRate       = 1
   blackFlashZoneBonus      = 10
   allowCharacterReselect   = false
   ```

4. `jjk_spec_06_decisions.md` STEP 2 확정값과 대조한다.

   **STEP 2 확정값:**
   ```
   maharagaThreshold  = 5
   sealDurationTicks  = 600
   zoneDurationTicks  = 200
   ceRegenOutOfCombat = 1.0
   ceRegenInCombat    = 0.2
   ```

5. 명세에 없는 추가 키가 있으면 목록화한다.
   (단, 명세 외 추가 키는 "명세 미반영 확장" 카테고리로 분류. 삭제 권고가 아닌 확인 요청.)

6. 명세에 있어야 하는데 파일에 없는 키가 있으면 목록화한다.

---

### STEP B — techniques.json 감사

1. 파일을 읽는다: `run/config/jjk/techniques.json`
2. 모든 캐릭터에 대해 keyId 0~4 (F/Shift+F/R/Shift+R/V) 5개 항목이 존재하는지 확인한다.

   **대상 캐릭터 11개:**
   `gojo` `itadori` `megumi` `okkotsu` `mahito` `jogo` `hakari` `inumaki` `nanami` `higuruma` `sukuna`

3. 각 항목에 필수 필드 6개가 모두 있는지 확인한다:
   `character`, `keyId`, `baseDamage`, `ceCost`, `cooldownTicks`, `animId`, `unlockGrade`

4. §LOCK baseDamage·ceCost·cooldownTicks 수치를 아래 표와 대조한다.

   **§LOCK 스킬 수치 (변경 금지):**
   | character | keyId | baseDamage | ceCost | cooldownTicks |
   |---|---|---|---|---|
   | gojo | 0 | 42 | 80 | 4 |
   | gojo | 1 | 64 | 160 | 8 |
   | gojo | 2 | 140 | 650 | 60 |
   | gojo | 3 | 0 | 3000 | 360 |
   | itadori | 0 | 34 | 72 | 4 |
   | itadori | 1 | 44 | 90 | 7 |
   | itadori | 2 | 0 | 120 | 20 |
   | itadori | 3 | 0 | 2200 | 300 |
   | megumi | 0 | 38 | 110 | 8 |
   | megumi | 1 | 32 | 90 | 6 |
   | megumi | 2 | 0 | 900 | 180 |
   | megumi | 3 | 0 | 2600 | 360 |
   | megumi | 4 | 0 | 140 | 12 |
   | okkotsu | 0 | 58 | 260 | 30 |
   | okkotsu | 1 | 72 | 180 | 10 |
   | okkotsu | 2 | 0 | 300 | 45 |
   | okkotsu | 3 | 0 | 1100 | 60 |
   | mahito | 0 | 26 | 140 | 8 |
   | mahito | 1 | 48 | 220 | 18 |
   | mahito | 2 | 0 | 120 | 15 |
   | mahito | 3 | 52 | 190 | 12 |
   | mahito | 4 | 0 | 3150 | 360 |
   | jogo | 0 | 46 | 130 | 6 |
   | jogo | 1 | 80 | 280 | 20 |
   | jogo | 2 | 120 | 600 | 75 |
   | jogo | 3 | 54 | 230 | 16 |
   | hakari | 0 | 0 | 600 | 20 |
   | hakari | 1 | 66 | 0 | 8 |
   | hakari | 2 | 0 | 180 | 30 |
   | hakari | 3 | 0 | 450 | 20 |
   | inumaki | 0 | 0 | 120 | 10 |
   | inumaki | 1 | 70 | 280 | 24 |
   | inumaki | 3 | 0 | 240 | 30 |
   | inumaki | 4 | 0 | 150 | 18 |
   | nanami | 0 | 50 | 110 | 6 |
   | nanami | 3 | 86 | 240 | 20 |
   | higuruma | 0 | 0 | 120 | 15 |
   | higuruma | 3 | 0 | 600 | 90 |
   | higuruma | 4 | 999 | 0 | 0 |
   | sukuna | 0 | 72 | 180 | 6 |
   | sukuna | 1 | 55 | 140 | 5 |
   | sukuna | 2 | 95 | 400 | 40 |
   | sukuna | 3 | 110 | 500 | 50 |
   | sukuna | 4 | 0 | 3000 | 360 |

5. `notImplemented=true`인 항목이 명세의 미사용 키와 일치하는지 확인한다.

   **미사용 키 명세 (Phase 3 완료 기준):**
   - `inumaki` keyId 2: 미사용 (NOT_IMPLEMENTED)
   - `nanami` keyId 1, 2, 4: Phase 3에서 구현 완료. notImplemented=false가 정상
   - `higuruma` keyId 1, 2: 미사용 (NOT_IMPLEMENTED)
   - `sukuna` keyId 0~4: Phase 3 구현 완료. notImplemented=false가 정상

---

### STEP C — domains.json 감사

1. 파일을 읽는다: `run/config/jjk/domains.json`
2. 7개 도메인이 전부 존재하는지 확인한다:
   `gojo_unlimited_void`, `sukuna_malevolent_shrine`, `mahito_self_embodiment`,
   `itadori_unnamed`, `megumi_chimera_shadow`, `jogo_volcano_domain`, `hakari_jackpot_domain`

3. 각 도메인에 필수 필드 7개가 모두 있는지 확인한다:
   `domainId`, `ownerCharacter`, `radius`, `ceCost`, `cooldownTicks`, `isOpen`, `wallHp`

4. §06 확정 수치와 대조한다.

   **§06 domains.json 확정 수치:**
   | domainId | radius | wallHp | sureHitActive |
   |---|---|---|---|
   | gojo_unlimited_void | 30 | 1500 | true |
   | sukuna_malevolent_shrine | 200 | 0 | true |
   | mahito_self_embodiment | 15 | 1500 | true |
   | itadori_unnamed | 20 | 1500 | true |
   | megumi_chimera_shadow | 20 | 750 | false |
   | jogo_volcano_domain | 25 | 1500 | true |
   | hakari_jackpot_domain | 20 | 1500 | true |

5. 명세에 없는 추가 도메인이 있으면 목록화한다.

---

### STEP D — Java 소스 파일 존재 감사

`src/main/java/com/jjk/` 하위를 탐색하여 아래 클래스들의 존재 여부를 확인한다.

**존재해야 하는 클래스 목록:**

| 패키지 | 클래스 |
|---|---|
| `combat/` | CombatPipeline, DamageCalculator, TickDamageCap, DamageContext |
| `ce/` | CEManager, CEPool, CERegenRule |
| `character/` | CharacterRegistry, SkillRegistry, CharacterCommandService |
| `character/api/` | ISkillSet, SkillResult |
| `character/impl/` | GojoSkillSet, ItadoriSkillSet, MegumiSkillSet, OkkotsuSkillSet, MahitoSkillSet, JogoSkillSet, HakariSkillSet, InumakiSkillSet, NanamiSkillSet, HigurumaSkillSet, SukunaSkillSet |
| `domain/` | DomainManager, DomainInstance, DomainPriorityCalculator |
| `zone/` | ZoneStateManager, ComboTracker |
| `awakening/` | AwakeningManager |
| `burden/` | BurdenManager |
| `trial/` | TrialManager, TrialStateMachine |
| `finger/` | FingerSystem |
| `team/` | TeamManager |
| `respawn/` | RespawnManager |
| `effect/` | EffectDeferQueue, InfinityHandler |
| `entity/` | RikaEntity |
| `network/c2s/` | SkillUseC2SPacket, CharacterSelectC2SPacket |
| `network/s2c/` | SkillResultS2CPacket, ZoneEnterS2CPacket, ZoneExitS2CPacket, AwakeningS2CPacket, CharacterInfoS2CPacket, CharacterSelectS2CPacket, CharacterConfirmS2CPacket, CharacterSelectFailS2CPacket, RespawnS2CPacket |
| `data/` | PlayerData, PlayerRepository, Migrator |
| `audit/` | AuditLogger |
| `mixin/` | ServerPlayerEntityMixin, LivingEntityMixin, ServerWorldMixin |
| `tick/` | TickScheduler |

---

### STEP E — PlayerData 필드 감사

`src/main/java/com/jjk/data/PlayerData.java`를 읽고 아래 필드들이 선언되어 있는지 확인한다.

**명세 필수 필드 목록:**
```
uuid, characterId, grade, xp, mastery
ceCurrent, ceMax, hpCurrent, hpMax
attackStat, defenseStat, speedStat
fingerCount, unlockedSkills, cooldowns
bindingVowDeclaredTick, domainCooldownUntil, jackpotCooldownUntil, curtainCooldownUntil
trialState, burden
zoneActive, zoneEndTick, lastCombatTick, lastKnownIp
awakeningActive, awakeningEndTick, awakeningCooldownUntil
burstActive, burstEndTick
deadShikigamiIds
healingActive
zonePenaltyUntilTick
hasExecutionSword
jackpotActive, jackpotEndTick
schemaVersion
```

`snapshot()` 메서드가 존재하는지, 위 필드들을 모두 복사하는지 확인한다.

---

### STEP F — jjk.mixins.json 감사

`src/main/resources/jjk.mixins.json`을 읽고 아래 3개 Mixin이 등록되어 있는지 확인한다:
`ServerPlayerEntityMixin`, `LivingEntityMixin`, `ServerWorldMixin`

---

## 출력 형식

```
═══════════════════════════════════════════════
JJK Mod Gap Audit Report
감사 기준: jjk_spec_01~03, jjk_spec_06_decisions.md
═══════════════════════════════════════════════

[CRITICAL] §LOCK 수치 위반
| 파일 | 항목 | 명세값 | 실제값 |
... (없으면 "없음")

[CRITICAL] §06 결정값 불일치
| 파일 | 항목 | 명세값 | 실제값 |
... (없으면 "없음")

[WARNING] 명세 외 추가 키/항목
| 파일 | 항목 | 실제값 | 비고 |
... (없으면 "없음")

[WARNING] 미구현 클래스 (파일 없음)
| 패키지 | 클래스 | 분류 |
... (없으면 "없음")

[WARNING] 스텁 미완성 클래스
| 클래스 | 미완성 메서드 | 비고 |
... (없으면 "없음")

[INFO] PlayerData 누락 필드
| 필드명 | 비고 |
... (없으면 "없음")

[INFO] techniques.json 불일치
| character | keyId | 항목 | 명세값 | 실제값 |
... (없으면 "없음")

[INFO] domains.json 불일치
| domainId | 항목 | 명세값 | 실제값 |
... (없으면 "없음")

[INFO] jjk.mixins.json 불일치
| Mixin 클래스 | 상태 |
... (없으면 "없음")

[NOTICE] 프롬프트 ↔ spec 문서 충돌 (있을 경우에만)
| 항목 | 프롬프트 제시값 | spec 파일 실제값 |
...

요약
CRITICAL 건수: N건
WARNING 건수: N건
INFO 건수: N건
```

---

## 절대 금지

- 코드 수정 또는 파일 내용 변경
- §LOCK 수치를 "자동 수정"하는 행위
- spec 파일을 직접 읽지 않고 프롬프트 제시값만으로 판단하는 행위
- 파일이 없는 클래스를 "존재하는 것으로 간주"
- 불확실한 내용을 추측해서 "이상 없음"으로 처리

파일 접근 오류가 발생하면 해당 항목을 "[접근 오류 — 수동 확인 필요]"로 표시하고 계속 진행한다.
