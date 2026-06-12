# JJK Spec — 06. 확정 결정 기록

> 이 문서는 운영자가 직접 확정한 수치와 설계 결정을 기록한다.
> 세션 시작 시 claude.md → jjk_spec_v5.md 다음으로 반드시 읽는다.
> §LOCK 수치는 명시적 지시 없이 변경 금지.
> 최초 확정 (2026-05-23). 최종 수정 (2026-06-03).

---

## STEP 2 — 수치 확정

| # | 항목 | 확정값 | config 키 |
|---|---|---|---|
| 2-1 | 마허라가 의식 적응 임계 횟수 | **3회** (밸런스 패스 2026-06-11) | `maharagaThreshold: 3` |
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
| `blackFlashBaseRate` | `1` | §LOCK — Normal 발동률 0.1% (퍼센트 정수 단위) |
| `blackFlashZoneBonus` | `10` | §LOCK |
| `xpMultiplierGradeDiff` | `1.5` | |
| `allowCharacterReselect` | `false` | |
| `rikaLifetimeTicks` | `200` | §LOCK |
| `maharagaThreshold` | `3` | 밸런스 패스 2026-06-11 (이전 2 → 3) |
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

## config.json 확장 키 목록 (구현 과정 추가분, 2026-06-03 등재)

> 아래 키들은 초기 확정 목록에 없었으나 구현 과정에서 추가됐다.
> 삭제 금지. 수치 변경 시 운영자 확인 필요.

### 전투 시스템 확장

| 키 | 실제값 | 설명 |
|---|---|---|
| `zoneEntryBlackFlashCount` | 1 | Zone 진입 필요 흑섬 횟수 |
| `zoneStackable` | false | Zone 중첩 허용 여부 |
| `maharagaTimeoutTicks` | 200 | 마허라가 의식 적응 타임아웃 틱 |
| `shadowMarkerLifetimeTicks` | 200 | 메구미 그림자 마커 지속 틱 |
| `pveGradeMultiplier` | 1.0 | PvE 등급 데미지 배율 |
| `blackFlashLowHpBonus` | 7 | 저체력(30% 이하) 흑섬 추가 발동률(%) |
| `blackFlashZoneAtkBonus` | 15 | Zone 중 공격력 보너스(%) |
| `blackFlashZoneSkillBonus` | 10 | Zone 중 스킬 데미지 보너스(%) |
| `fingerStatBonusPercent` | 5 | 스쿠나 손가락 1개당 스탯 보너스(%) |

### 각성 시스템

| 키 | 실제값 | 설명 |
|---|---|---|
| `awakeningHpThreshold` | 0.30 | 각성 발동 HP 임계값 (30%) — config.json 권위, 2026-06-11 확정 |
| `awakeningMultiplier` | 1.5 | 각성 중 데미지 배율 |

### 방어 시스템

| 키 | 실제값 | 설명 |
|---|---|---|
| `shieldCeDrainRatio` | 0.005 | 수동 방어 CE 소모율 (max_ce × 0.5%/s) |
| `shieldDamageReduction` | 0.02 | 수동 방어 데미지 감소율 (2%) |
| `simpleBarrierCostActivate` | 0.03 | 간이 영역 발동 CE 비용 (max_ce × 3%) |
| `simpleBarrierCostPerSecond` | 0.002 | 간이 영역 유지 CE 소모 (max_ce × 0.2%/s) |
| `simpleBarrierSureHitNegate` | 0.7 | 간이 영역 필중 무효화율 (70%) |
| `simpleBarrierAllyBonus` | 0.08 | 간이 영역 아군 방어 보너스 (8%) |
| `fallingBlossomSureHitBlock` | 0.8 | 낙화의 정 필중 차단율 (80%) |
| `fallingBlossomCeDrain` | 0.02 | 낙화의 정 CE 소모율 (max_ce × 2%) |

### 반전술식

| 키 | 실제값 | 설명 |
|---|---|---|
| `reverseHealSelfPerTick` | 0.3 | 자기 반전 틱당 회복량 |
| `reverseCeDrainSelfRatio` | 0.008 | 자기 반전 CE 소모율 |
| `reverseHealOtherPerTick` | 0.4 | 타인 반전 틱당 회복량 |
| `reverseCeDrainOtherRatio` | 0.012 | 타인 반전 CE 소모율 |

### 캐릭터 특수 시스템

| 키 | 실제값 | 설명 |
|---|---|---|
| `tenShadowsBodyBonus` | 0.2 | 메구미 십종영법 본체 보너스 (20%) |
| `burdenDecayOutOfCombat` | 0.25 | 이누마키 부담 전투 외 감소량/틱 |
| `burdenDecayInCombat` | 0.1 | 이누마키 부담 전투 중 감소량/틱 |
| `burdenSealThreshold` | 100 | 이누마키 부담 봉인 임계값 |

### 주술도구

| 키 | 실제값 | 설명 |
|---|---|---|
| `cursedToolAttackBonus_dagger` | 0.08 | 단검 공격력 보너스 (8%) |
| `cursedToolAttackBonus_spear` | 0.14 | 창 공격력 보너스 (14%) |
| `cursedToolAttackBonus_cloud` | 0.2 | 구름 공격력 보너스 (20%) |
| `cursedToolAttackBonus_inverted` | 0.28 | 역천 공격력 보너스 (28%) |
| `cursedToolAttackBonus_soul` | 0.18 | 혼 공격력 보너스 (18%) |
| `cursedToolCeReduction_spear` | 0.08 | 창 CE 소모 감소율 (8%) |
| `cursedToolRangeBonus_cloud` | 0.12 | 구름 사거리 보너스 (12%) |
| `cursedToolDefPenetration_inverted` | 0.15 | 역천 방어 관통율 (15%) |
| `cursedToolBlackFlashBonus_soul` | 5 | 혼 흑섬 보너스율 (5%) |
| `cursedToolSealCooldown_inverted` | 120 | 역천 봉인 쿨타임 (틱) |

### 주술고등학교 건물

| 키 | 실제값 | 설명 |
|---|---|---|
| `jjtBuilding_enabled` | true | JJT 건물 생성 활성화 |
| `jjtBuilding_centerX` | 0 | 건물 중심 X 좌표 |
| `jjtBuilding_centerY` | 64 | 건물 중심 Y 좌표 |
| `jjtBuilding_centerZ` | 0 | 건물 중심 Z 좌표 |
| `jjtBuilding_generated` | true | 건물 생성 완료 플래그 |
| `trainingRoomPos` | [0.0, 64.0, 0.0] | 훈련실 위치 |
| `infirmaryPos` | [10.0, 64.0, 0.0] | 의무실 위치 |
| `storagePos` | [-10.0, 64.0, 0.0] | 창고 위치 |
| `entrancePos` | [0.0, 64.0, 20.0] | 입구 위치 |

### XP 시스템

| 키 | 실제값 | 설명 |
|---|---|---|
| `xpGrade4to3` | 500 | 4급 → 3급 필요 XP |
| `xpGrade3to2` | 1200 | 3급 → 2급 필요 XP |
| `xpGrade2to1` | 2500 | 2급 → 1급 필요 XP |
| `xpGrade1toSemi` | 5000 | 1급 → 준1급 필요 XP |
| `xpGradeSemiToSpecial` | 12000 | 준1급 → 특급 필요 XP |
| `xpOnKill` | 50 | 처치 시 획득 XP |
| `xpOnDamagePerHit` | 1 | 타격 시 획득 XP |
| `xpOnDamageCapPerCombat` | 20 | 전투당 데미지 XP 상한 |
| `xpOnBlackFlash` | 15 | 흑섬 발동 시 획득 XP |
| `xpOnPerfect` | 30 | Perfect 흑섬 시 획득 XP |
| `xpOnDailyLogin` | 30 | 일일 로그인 획득 XP |

### 창(唱) 시스템

| 키 | 실제값 | 설명 |
|---|---|---|
| `chantMaxTicks` | 60 | 창 최대 지속 틱 (3초) |
| `chantMaxMultiplier` | 2.0 | 창 최대 배율 |
| `chantCeDrainRatio` | 0.2 | 창 CE 소모율 |

### 커튼 시스템

| 키 | 실제값 | 설명 |
|---|---|---|
| `curtainBasicCeCost` | 800 | 기본 커튼 발동 CE |
| `curtainBasicCePerTick` | 0.8 | 기본 커튼 유지 CE/틱 |
| `curtainBasicDurationTicks` | 4000 | 기본 커튼 지속 틱 |
| `curtainBasicRadius` | 25 | 기본 커튼 반경 |
| `curtainBasicCooldownTicks` | 600 | 기본 커튼 쿨타임 틱 |
| `curtainSpecialCeCost` | 2000 | 특수 커튼 발동 CE |
| `curtainSpecialCePerTick` | 1.5 | 특수 커튼 유지 CE/틱 |
| `curtainSpecialDurationTicks` | 8000 | 특수 커튼 지속 틱 |
| `curtainSpecialRadius` | 45 | 특수 커튼 반경 |
| `curtainSpecialCooldownTicks` | 1200 | 특수 커튼 쿨타임 틱 |

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
| `cursed_spirit_domain` | 15 | 800 | true |

> **cursed_spirit_domain 비고**: NPC 전용 영역. ownerCharacter=cursed_spirit.
> ceCost=0 (NPC 자동 전개), autoTargetAll=true, cooldownTicks=200.
> 플레이어 캐릭터 영역 7개와 별도 관리. 수치 근거: domains.json 실측값 (2026-06-03 등재).

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
스쿠나: 5개 키 전부 실구현 완료 (2026-06-01). 수치는 §P3-6 및 techniques.json 참조. animId F=21, SF=22, R=23, SR=24, V=7

---

## 구현 시 절대 금지 (이 파일 기준)

- `cooldowns Map`에 `"domain"` / `"awakening"` 키 사용 금지 → PlayerData 전용 필드 사용
- Zone 진입 유발 흑섬 스킬에 Zone 보너스 적용 금지
- `sealDurationTicks` 600 사용 금지 → 400으로 교체됨
- `zoneDurationTicks` 200 사용 금지 → 300으로 교체됨
- `maharagaThreshold` 5 사용 금지 → 3으로 교체됨 (config.json 권위, 2026-06-11)

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

### P3-6. 스쿠나 확정 스킬 수치 (2026-06-01 커밋 d4fa590으로 업그레이드 확정)

| keyId | 스킬 | baseDamage | ceCost | cooldownTicks | animId |
|---|---|---|---|---|---|
| 0 (F 해체) | dismantle | 72 | 180 | 6 | 21 |
| 1 (SF 필살참) | arrow | 55 | 140 | 5 | 22 |
| 2 (R 개·화염) | reverse_eight_handled | 95 | 400 | 40 | 23 |
| 3 (SR 세계절단참) | cleave | 110 | 500 | 50 | 24 |
| 4 (V 복마어주자) | malevolent_shrine | 0 | 3000 | 360 | 7 |

효과:
- 해체: 직선 참격 (HitValidator 기반)
- 필살참: 대상 현재 HP 비례 추가 피해 (target.hpCurrent × 0.20f). 손가락 10개+ 시 범위 ×1.3
- 개·화염: 범위 폭발 + 화상 80틱 (FireEffectManager 활용)
- 세계절단참: isSoulDirect=true, bypassRCT=true (방어·RCT 무시). 손가락 20개 시 CD ×0.80
- 복마어주자: DomainManager.deployDomain("sukuna_malevolent_shrine")

> 수치 근거: 2026-06-01 커밋(d4fa590) 의도적 업그레이드.
> techniques.json이 단일 소스. §LOCK 적용.
> (이전값: F baseDamage=8/ceCost=10/CD=10 — 2026-05-30 72cb376 커밋)

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

---

## STEP 4 — 2026-06-06 추가 확정

| # | 항목 | 확정값 | 비고 |
|---|---|---|---|
| 4-1 | `awakeningHpThreshold` | **0.30** | 각성 발동 HP 임계 (HP 30% 이하 시 트리거). config.json 권위 — 구버전 0.05/0.10 표기는 폐기(2026-06-11) |
| 4-2 | `blackFlashBaseRate` 재확인 | **1 유지** | §LOCK — spec §3-2 Normal 0.1%와 일치. 변경 불필요 확인됨 (2026-06-06) |
| 4-3 | `OkkotsuSkillSet.onShiftR` burstActive 세팅 | **player=null 경로에서도 세팅 필수** | burstActive=true, burstEndTick=tick+200은 CE 검증 통과 후 player 참조 없이 즉시 세팅 |
| 4-4 | 테스트 Migrator 기대값 | **assertEquals(17, CURRENT_VERSION)** | ReleaseGateTest×2 + ISkillSetTest 수정 완료 (2026-06-06) |
