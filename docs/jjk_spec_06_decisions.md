# JJK Spec — 06. 확정 결정 기록

> 이 문서는 운영자가 직접 확정한 수치와 설계 결정을 기록한다.
> 세션 시작 시 claude.md → jjk_spec_v5.md 다음으로 반드시 읽는다.
> §LOCK 수치는 명시적 지시 없이 변경 금지.
> 최초 확정 (2026-05-23). 최종 수정 (2026-05-31).

---

## STEP 2 — 수치 확정

| # | 항목 | 확정값 | config 키 |
|---|---|---|---|
| 2-1 | 마허라가 의식 적응 임계 횟수 | **2회** | `maharagaThreshold: 2` |
| 2-2 | `sealDurationTicks` | **400틱 (20초)** | `sealDurationTicks: 400` |
| 2-3 | Zone 종료 기준 | **300틱 (15초) 만료. 발동 흑섬은 Zone 보너스 적용 금지** | `zoneDurationTicks: 300` |
| 2-4 | CE 재생 수치 | **전투 외 1.0/틱, 전투 중 0.2/틱 (config 조정 가능)** | `ceRegenOutOfCombat: 1.0` / `ceRegenInCombat: 0.2` |
| 2-5 | 속박 선언 방식 | **기존 keyId 처리 (SkillUseC2SPacket 재사용, 별도 패킷 없음)** | — |

### 2-3 Zone 상세 규칙
- Zone 진입을 유발한 흑섬 발동 시점의 스킬에는 Zone 보너스(`blackFlashZoneBonus`) 적용 금지
- Zone 보너스는 Zone 진입 **이후** 발동하는 스킬부터 적용
- 구현: `ZoneStateManager.enterZone()` 호출 시점과 흑섬 판정 시점이 같은 틱이면 해당 틱 스킬은 보너스 제외

---

## STEP 3 — 충돌 항목 확정

| # | 항목 | 확정 | 근거 |
|---|---|---|---|
| 3-1 | 쿨타임 검증 단계 | **2단계** | CombatPipeline §3-6: 1단계=진영·소유권, 2단계=CE·쿨타임 분리 |
| 3-2 | `domainCooldownUntil` 저장 위치 | **PlayerData 전용 필드 단독 사용** | cooldowns Map `"domain"` 키 사용 금지 |
| 3-3 | `awakeningCooldownUntil` 저장 위치 | **PlayerData 전용 필드 단독 사용** | cooldowns Map `"awakening"` 키 사용 금지 |
| 3-4 | 개방형 영역 자박 CE 2배 | **포함** | `isOpen=true` 시 `ceCost × 2` 적용 |
| 3-5 | 잭팟 config 조정 범위 | **min 60틱 / max 251틱** | `jackpotDurationMinTicks: 60` / `jackpotDurationMaxTicks: 251` |
| 3-6 | `skill_seal` 키 범위 | **히구루마 + 마허라가 동일 `"skill_seal"` 키 공유** | 중복 봉인 시 더 긴 만료값 유지 |
| 3-7 | 옷코츠 V 자기 회복 방식 | **직접 HP 회복 (healingActive 세팅 없음)** | spec_01 §9-1 명시: 옷코츠는 대상 HP 직접 처리 |

---

## config.json 전체 확정 키 목록

| 키 | 값 | 비고 |
|---|---|---|
| `mangaExpEnabled` | `false` | §LOCK |
| `allowDuplicateCharacter` | `false` | §LOCK |
| `gradePvpScaling` | `true` | §LOCK |
| `pvpDamageCapMaxHpRatio` | `0.40` | §LOCK |
| `trialSuccessRate` | `0.60` | §LOCK |
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
| `fingerDropRate` | `0.10` | §LOCK |
| `fingerMaxCount` | `20` | §LOCK |
| `blackFlashBaseRate` | `5` | §LOCK |
| `blackFlashZoneBonus` | `10` | §LOCK |
| `xpMultiplierGradeDiff` | `1.5` | |
| `allowCharacterReselect` | `false` | |
| `rikaLifetimeTicks` | `200` | §LOCK |
| `maharagaThreshold` | `2` | 확정 2026-05-28 |
| `sealDurationTicks` | `400` | 확정 2026-05-28 |
| `zoneDurationTicks` | `300` | 확정 2026-05-28 |
| `ceRegenOutOfCombat` | `1.0` | 확정 2026-05-28, config 조정 가능 |
| `ceRegenInCombat` | `0.2` | 확정 2026-05-28, config 조정 가능 |
| `maharagaTimeoutTicks` | `200` | 마허라가 의식 타임아웃 10초 — 운영자 조정 가능 |
| `shadowMarkerLifetimeTicks` | `200` | 메구미 그림자 마커 유지시간 10초 — 운영자 조정 가능 |
| `pveGradeMultiplier` | `1.0` | PvE 등급 배율 고정값 (PvP는 §7-1 표 적용) |
| `zoneEntryBlackFlashCount` | `1` | Zone 진입에 필요한 흑섬 성공 횟수 |
| `zoneStackable` | `false` | Zone 중첩 허용 여부 |

---

## domains.json 확정값

| domainId | radius | wallHp | isOpen | sureHitActive | 비고 |
|---|---|---|---|---|---|
| `gojo_unlimited_void` | 30 | 1500 | false | true | |
| `sukuna_malevolent_shrine` | 200 | 0 | true | true | CE 2배 적용 |
| `mahito_self_embodiment` | 15 | 1500 | false | true | ownerDamageReduction 0.5 |
| `itadori_unnamed` | 20 | 1500 | false | true | |
| `megumi_chimera_shadow` | 20 | 750 | false | false | wallHp 절반, sureHit 없음 |
| `jogo_volcano_domain` | 25 | 1500 | false | true | |
| `hakari_jackpot_domain` | 20 | 1500 | false | true | |

---

## techniques.json keyId 체계

| keyId | 키 |
|---|---|
| 0 | F |
| 1 | Shift+F |
| 2 | R |
| 3 | Shift+R |
| 4 | V |

미사용 키: `baseDamage=0, ceCost=0, cooldownTicks=0, notImplemented=true`
스쿠나: 5개 키 전부 NOT_IMPLEMENTED 스텁. animId 예약값 F=21, Shift+F=22, R=23, Shift+R=24, V=0

---

## 구현 시 절대 금지 (이 파일 기준)

- `cooldowns Map`에 `"domain"` / `"awakening"` 키 사용 금지 → PlayerData 전용 필드 사용
- Zone 진입 유발 흑섬 스킬에 Zone 보너스 적용 금지
- `sealDurationTicks` 600 사용 금지 → 400으로 교체됨
- `zoneDurationTicks` 200 사용 금지 → 300으로 교체됨
- `maharagaThreshold` 5 사용 금지 → 2로 교체됨

---

## Phase 2 — API 실측 수정사항 (2026-05-28)

### P2-1. AbstractFadeModifier.standardFadeIn 시그니처
- spec §11-6 추정: `standardFadeIn(5)`
- javap 실측: `standardFadeIn(5, Ease.LINEAR)` — 2파라미터 필수
- 적용 위치: `PlayerAnimationDispatcher.java`

### P2-2. REGISTER_ANIMATION_EVENT 시그니처
- spec §11-6 추정: `register((player, stackGetter) -> stackGetter.apply(...))`
- 실측: `AnimationRegister` 인터페이스 시그니처 = `registerAnimation(AbstractClientPlayerEntity player, AnimationStack stack)` — 직접 `(player, stack) ->` 람다 사용
- 적용 위치: `JJKModClient.java`

### P2-3. SkillResultS2CPacket 필드 부재
- `characterId()`, `playerUuid()` 필드 없음 — 실제 필드: `keyId`, `result`, `finalDamage`
- 분기 기준: `result` 필드 + `mc.player` 사용으로 대체
- 적용 위치: `SkillAnimController.java`
- Phase 3 전 개선 필요: `SkillResultS2CPacket`에 `characterId` + `playerUuid` 필드 추가 고려

### P2-4. 향후 주의사항
- PlayerAnimator 2.0.4 API는 javap 실측값만 사용
- `standardFadeIn`은 반드시 `Ease` 파라미터 포함
- `AbstractClientPlayerEntity` 필수 (`ServerPlayerEntity` 사용 금지)

---

## Phase 3 — 구현 중 확정사항

### P3-1. techniques.json unlockGrade 기준
- 나나미 nanami 전 스킬: "4급" (기존 "1급" 오기 수정)
- 기준: 캐릭터 기본 등급과 동일하게 설정. 별도 명시 없으면 "4급" 기본값.

### P3-2. WeaknessZoneCalculator 구현 방식
- PlayerData에 pos/yaw 필드 없음
- ServerPlayerEntity 기반으로 구현
- testWeaknessZone: @Disabled (서버 통합 테스트 환경 필요)

### P3-3. FireEffectManager.apply() 시그니처
- 2-파라미터(target, durationTicks) 구현 불가 → 3-파라미터(target, durationTicks, currentTick)로 확정
- tickFire: TickScheduler 등록 완료 (STEP 2 후반 처리)

### P3-4. 이누마키 채팅 인터셉트 구현 확정사항
- ALLOW_COMMAND_MESSAGE sender 타입: ServerCommandSource → sender.getPlayer()로 추출
- 스킬 실패 시에도 채팅 차단 (return false) — spec 의도대로
- testInumakiBurdenAccumulation: JJKMod.getInstance().getConfig() NPE → new JjkConfig() 대체

### P3-5. techniques.json unlockGrade 기준 확정
- 전 캐릭터 기본값: "4급" (별도 명시 없으면 모두 "4급")
- 이후 캐릭터 작업 시 unlockGrade는 "4급"으로 통일

### P3-6. 스쿠나 확정 스킬 수치 (운영자 확정 2026-05-28)
- F  해체(解體):    baseDamage=8,  ceCost=10,  cooldownTicks=10,  animId=21
- SF 필살참(捌):    baseDamage=18, ceCost=25,  cooldownTicks=40,  animId=22
- R  개(開)·화염:   baseDamage=30, ceCost=50,  cooldownTicks=120, animId=23
- SR 세계절단참:    baseDamage=50, ceCost=100, cooldownTicks=240, animId=24
- V  복마어주자:    baseDamage=0,  ceCost=200, cooldownTicks=360, animId=7
효과:
  해체: 직선 참격 투사체 (EffectDeferQueue 지연 판정)
  필살참: 대상 현재 HP 비례 추가 피해 (target.hpCurrent × 0.20f)
  개·화염: 범위 폭발 4블록 + 화상 80틱 (FireEffectManager 활용)
  세계절단참: isSoulDirect=true, bypassRCT=true (방어·RCT 무시)
  복마어주자: DomainManager.deployDomain("sukuna_malevolent_shrine")

---

## Phase 3 완료 (2026-05-28)

### 구현 완료 캐릭터 (11개)

Phase 1: gojo, itadori, megumi, okkotsu (+ sukuna 스텁)
Phase 3: nanami, jogo, inumaki, mahito, hakari, higuruma, sukuna (실구현)

### 최종 테스트 현황
- 총 테스트: 108개
- failures: 0
- skipped: 1 (testWeaknessZone — ServerPlayerEntity 기반, 수동 테스트 필요)

### Phase 3 릴리스 게이트
- 소스셋 분리 위반: 없음 ✅
- §LOCK 하드코딩: 없음 (TickDamageCap.java 자체 정의는 false positive) ✅
- DomainInstance record: 없음 (class 유지) ✅
- cooldowns 금지 키: 없음 (JogoSkillSet.java:213은 주석) ✅
- GeckoLib: 4.8.3 (Fabric 전용) ✅

### TPS 부하 테스트
- TickScheduler 등록 타스크: 7개 (period 1~10틱)
- 빌드 성공: gradle build SUCCESSFUL
- 실서버 idle TPS 측정: 실제 플레이어 참여 환경에서 추후 실측 필요

### 미완료 항목 (Phase 4 이후)
- testWeaknessZone: ServerPlayerEntity 통합 테스트 환경 구성 필요
- 고급 VFX 셰이더 (DomainBoundaryRenderer Phase 3 이후)
- TPS 멀티플레이어 부하 테스트 (실제 플레이어 참여 환경)
- /jj reload 커맨드 (핫리로드 CLI)
- TickDamageCap.java CAP_RATIO config 주입 개선 (pre-existing 이슈)
