# JJK 서버 개발 명세서 v5 — 1순위 권위 문서

> 대상: Minecraft Fabric 1.21.1 서버 모드 (Java 21)
> 목표: Claude Code가 매 세션 시작 시 가장 먼저 읽는 지침서.
> 원칙: 확정 수치는 임의 변경 금지. 명백한 계산 오류와 런타임/빌드 오류만 수정.
> 틱 기준: 1초 = 20틱 (1틱 = 50ms). 모든 tick 환산은 이 기준.
> CD 단위: 틱(ticks). 스킬 표의 CD=4는 4틱=0.2초.
> 최종 확정일: 2026-05-23

---

## §0. CLAUDE.md 작업 규칙

1. 기존 확정 수치와 밸런스 값은 임의 변경하지 않는다.
2. 중복 내용은 최신·상세한 쪽만 남긴다.
3. Java 코드 블록에는 실제 컴파일 가능한 Java 예시만 둔다.
4. `[AI-ADD]` / `[DOCX]` / `[AI-FIX]` / `[AI-FINAL]` 태그는 문서 주석 전용이며 Java 코드 블록 안에 포함 금지(주석 형태도 금지).
5. Mixin 추가 시 `jjk.mixins.json`도 동시에 수정한다.
6. PlayerData 필드 추가 시 `snapshot()` 복사와 DB 마이그레이션을 함께 작성한다.
7. `bindingVowDeclaredTick`은 `CooldownManager`가 아닌 `PlayerData`에 저장. 미선언 상태 sentinel = `-1L`.
8. 각성 발동 전 `awakeningCooldownUntil` 체크 필수. 중복 발동 방지.
9. `DomainInstance`는 class(record 아님). `wallHp`·`currentRadius`는 가변 필드.
10. 스킬 표의 CD와 §13-3 cooldown_persist는 같은 키를 공유한다. 스킬 keyId와 쿨타임 cooldown_type을 혼동하지 말 것.

### 0-1. 세션 프롬프트

| 번호 | 용도 | 프롬프트 |
|------|------|----------|
| 1 | 서버 공통 | "Fabric 1.21.1 기준. 서버 권위(authoritative)로 구현하고 클라이언트 패킷은 표시 전용." |
| 2 | 전투 | "jjk_spec_v5.md §3·§4 기준. CombatPipeline 순서와 TickDamageCap을 지키고 확정 수치를 바꾸지 말 것." |
| 3 | 캐릭터 | "jjk_spec_v5.md §6 기준. ISkillSet 5개 키(F, Shift+F, R, Shift+R, V)를 모두 구현." |
| 4 | 영역 | "jjk_spec_v5.md §8 기준. DomainInstance는 class이며 wallHp와 currentRadius는 가변." |
| 5 | 저장 | "jjk_spec_v5.md §13 기준. PlayerData 필드 추가 시 snapshot·schema migration·테스트 동시 반영." |
| 6 | QA | "jjk_spec_v5.md §18 기준. 변경한 시스템 단위 테스트와 통합 스모크 테스트를 추가." |
| 7 | Mixin 작업 | "jjk_spec_v5.md §1-7 기준. @Inject at= 반드시 명시. cancellable=true 남용 금지. Mixin 추가 시 jjk.mixins.json 동시 수정." |
| 8 | 게임 플로우 | "jjk_spec_v5.md §26 기준. 캐릭터 선택·부활·입장 플로우 준수. 손가락 드롭은 atomic 처리 필수." |

---

## §1. 프로젝트 구조와 빌드

### 1-1. 패키지 구조

root package: `com.jjk`

| 패키지 | 주요 클래스 |
|--------|-------------|
| `combat/` | `CombatPipeline` · `DamageCalculator` · `TickDamageCap` · `DamageContext` |
| `ce/` | `CEManager` · `CEPool` · `CERegenRule` |
| `character/` | `CharacterRegistry` · `ISkillSet` · `SkillRegistry` · `CharacterCommandService` |
| `character/impl/` | `GojoSkillSet` · `SukunaSkillSet` · `ItadoriSkillSet` · 기타 캐릭터 SkillSet |
| `domain/` | `DomainManager` · `DomainInstance` · `DomainPriorityCalculator` |
| `zone/` | `ZoneStateManager` · `ComboTracker` |
| `awakening/` | `AwakeningManager` |
| `burden/` | `BurdenManager` |
| `trial/` | `TrialManager` · `TrialStateMachine` |
| `finger/` | `FingerSystem` |
| `team/` | `TeamManager` |
| `respawn/` | `RespawnManager` |
| `effect/` | `EffectDeferQueue` · `InfinityHandler` |
| `network/c2s/` | `SkillUseC2SPacket` · `CharacterSelectC2SPacket` |
| `network/s2c/` | `SkillResultS2CPacket` · `ZoneEnterS2CPacket` · `ZoneExitS2CPacket` · `AwakeningS2CPacket` · `CharacterInfoS2CPacket` · `CharacterSelectS2CPacket` · `CharacterConfirmS2CPacket` · `CharacterSelectFailS2CPacket` · `RespawnS2CPacket` |
| `data/` | `PlayerData` · `PlayerRepository` · `Migrator` |
| `mixin/` | `ServerPlayerEntityMixin` · `LivingEntityMixin` · `ServerWorldMixin` |
| `entity/` | `RikaEntity` *(client 렌더는 client 소스셋 격리)* |

> `DomainBoundaryRenderer`는 client 소스셋 또는 `EnvType.CLIENT` 분기로 격리한다. 서버 패키지에 두면 dedicated server에서 classloading 오류 발생.

### 1-2. 소스셋 분리 원칙

- `src/main/` — 서버+공용 로직. 전투, CE, 데이터 저장, 네트워크 패킷 등 모두 여기.
- `src/client/` — 클라이언트 전용 렌더링 코드. `DomainBoundaryRenderer`, GeckoLib 식신 렌더, PlayerAnimator HUD.
- 서버에서 classloading되면 크래시나는 Minecraft 클라이언트 클래스(GL, RenderSystem 등)는 반드시 `src/client/`로 격리.

### 1-3. jjk.mixins.json 실제 내용

`src/main/resources/fabric.mod.json`에 `jjk.mixins.json` 등록 필수.

```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "com.jjk.mixin",
  "compatibilityLevel": "JAVA_21",
  "mixins": [
    "ServerPlayerEntityMixin",
    "LivingEntityMixin",
    "ServerWorldMixin"
  ],
  "client": [],
  "injectors": { "defaultRequire": 1 }
}
```

### 1-4. build.gradle 의존성 버전

`repositories`에 아래 Maven을 포함한다.

    maven { url 'https://maven.bernie.software/' }
    maven { url 'https://maven.azuredevelopment.xyz/' }
    maven { url 'https://maven.kosmx.dev/' }
    maven { url 'https://maven.fabricmc.net/' }

의존성 (버전 고정):

    modImplementation "software.bernie.geckolib:geckolib-fabric-1.21:4.8.3"
    modImplementation "mod.azure.azurelib:azurelib-fabric-1.21.1:3.1.8"
    modImplementation "dev.kosmx.player-anim:player-animation-lib-fabric:2.0.4+1.21.1-fabric"

GeckoLib 4.8.4는 Forge 전용. Fabric에서 `ClassNotFoundException` 크래시 발생. 반드시 **4.8.3** 사용.

### 1-5. 데이터 파일 경로

| 파일 | 경로 |
|------|------|
| `config.json` | `run/config/jjk/config.json` |
| `techniques.json` | `run/config/jjk/techniques.json` |
| `domains.json` | `run/config/jjk/domains.json` |
| `player_data.db` | `world/jjk/player_data.db` |
| `audit_log.db` | `world/jjk/audit_log.db` |

### 1-6. 서버 권위 원칙

클라이언트는 키 입력과 표시만 담당한다. 데미지, CE 소모, 쿨타임, 영역 충돌, 손가락 드롭, 캐릭터 선택 중복 검사는 모두 서버에서 확정한다.

### 1-7. Mixin 후킹 명세

`ServerPlayerEntityMixin`

    @Inject method="tick" at=TAIL
      → CEManager.regenTick(player)
      → AwakeningManager.tickCheck(data, currentTick)   ← tick 기반 만료 체크
      → ZoneStateManager.tick(player)
      → BurdenManager.tick(player)

    @Inject method="onDeath" at=HEAD
      → PlayerRepository.saveImmediate(data)            ← 저장 주체는 PlayerRepository
      → AwakeningManager.reset(player)
      → ZoneStateManager.reset(player)

`LivingEntityMixin`

    @Inject method="damage" at=HEAD cancellable=true
      → InfinityHandler.checkAndCancel(self, source, cir)
      → TickDamageCap 초과 시 cir.setReturnValue(false)

    @ModifyVariable method="damage" ordinal=0 at=HEAD
      → isSoulDirect=true 시 effectiveDefense=0 강제
      → NON_SORCERER defense 배율 적용

`ServerWorldMixin`

    @Inject method="tick" at=TAIL
      → DomainManager.tickDomains(world)

주의: `@Inject cancellable=true` 남용 금지. Mixin 추가 시 `jjk.mixins.json` 동시 등록 필수.

---

## §2. 서버 성능과 운영 기준

### 2-1. Tick 예산

서버 메인 루프는 평균 **50ms/tick(= 20TPS)** 이하를 유지한다. 영역, 장막, 광역 스킬은 엔티티 검색 범위를 캐시하고 5틱 단위 배치 처리 가능.

### 2-2. 성능 모니터링

| 지표 | 경고 | 위험 | 조치 |
|------|------|------|------|
| TPS | 18 미만 | 15 미만 | 광역 tick 주기 증가, particle 감축 |
| 메인스레드 | 45ms/tick 초과 | 60ms/tick 초과 | 영역 엔티티 스캔 분산 |
| 힙 | 75% 초과 | 90% 초과 | 캐시 TTL 축소 |
| 패킷 | 120/s/player 초과 | 200/s/player 초과 | S2C 연출 패킷 병합 |
| 엔티티 | 800 초과 | 1200 초과 | 식신·투사체 lifetime 점검 |

---

## §3. 전투 핵심 시스템

### 3-1. 속박(Binding Vow)

- `bindingVowDeclaredTick` 저장 위치: **`PlayerData`** (CooldownManager 사용 금지)
- 미선언 상태 sentinel: **`-1L`**
- 타임아웃: **300틱(15초)** — 선언 후 미달성 시 자동 해제
- 선언 방식: 기존 keyId 처리 (별도 C2S 패킷 추가 없음) — decisions §2-5
- 파훼 조건: 특급 이상 스킬 직격 시 해제 (`bindingVowBreakBySpecialGradeHit: true`)

### 3-2. 흑섬(Black Flash) — §LOCK

| 항목 | 값 |
|------|----|
| 판정 | 공격 전후 Just Frame 윈도우(8~12틱) 내 |
| 기본 확률 | Zone 없을 때 `config.blackFlashBaseRate`%, Zone 중 +`config.blackFlashZoneBonus`% |
| 데미지 | **base × 2.5** |
| 후속 효과 | Zone 진입, 콤보 보정 |

원작(Akutami Vol.6): 흑섬 데미지 = base^2.5(지수승).
**이 서버: base × 2.5(배율).** 이유: base^2.5는 TickDamageCap에 걸려 실질 효과 없음.
**이 결정은 §LOCK 확정값. 코드에서 임의 변경 금지.**

### 3-3. Zone

- 흑섬 성공 또는 특정 콤보 조건 달성 시 진입
- 지속시간: `zoneEndTick` 만료 기준. **`zoneDurationTicks: 200`(10초)** — decisions §2-3
- 종료 후 `zonePenaltyUntilTick`까지 최종 배율 **×0.5** 페널티 적용
- Zone 관리: `ZoneStateManager`. `ComboTracker`와 Just Frame 윈도우(8~12틱) 공유

### 3-4. TickDamageCap

PvP 최종 피해 상한: **`max_hp × 0.40`**. PvE 상한은 config로 분리 가능.

### 3-5. Just Frame

Just Frame 윈도우: **8~12틱**. 30틱 동안 후속 입력 없으면 콤보 리셋. 흑섬 판정과 콤보 시스템이 공유.

### 3-6. CombatPipeline 9단계

`CombatPipeline.process(PlayerData attacker, PlayerData target, SkillUseC2SPacket packet, long currentTick)` 순서:

1. **입력 검증** — TeamManager: 같은 진영 여부, characterId 소유 여부
2. **CE·쿨타임 검증** — CEManager: ceCurrent 충분 여부, cooldowns Map 만료 여부 (2단계 — decisions §3-1)
3. **명중·거리·시야 검증** — 서버 측 거리/시야 계산
4. **baseDamage 산출** — techniques.json 기반
5. **흑섬·Zone·각성 판정** — 이 단계에서 `AwakeningManager.checkAndActivate(data, hpCurrent, hpMax, tick)` 호출
6. **방어·저항·무하한·영혼 데미지 처리** — InfinityHandler, isSoulDirect, NON_SORCERER 배율
7. **최종 배율 clamp** — ×0.25~×4.0
8. **TickDamageCap 적용** — `min(finalDamage, target.hpMax * 0.40f)`
9. **데미지 반영 및 S2C 전송** — `SkillResultS2CPacket`

### 3-7. 각성(覚醒) 시스템

- 발동 조건: `currentHp <= maxHp × 0.30`
- 발동 위치: CombatPipeline 5단계. `AwakeningManager.checkAndActivate()` 호출
- 지속시간: **160틱(8초)** — `awakeningEndTick = currentTick + 160`
- 쿨타임: **2400틱(120초)** — `awakeningCooldownUntil = currentTick + 2400`
- 효과: `finalMultiplier × 1.25` (클램프 ×4.0 내 포함)
- 해제: `awakeningEndTick` 도달 시 `tickCheck`에서 자동 해제

`AwakeningManager` 로직 (checkAndActivate):

    if (currentHp > maxHp * 0.30f) return;
    if (tick < data.awakeningCooldownUntil) return;
    if (data.awakeningActive) return;
    data.awakeningActive = true;
    data.awakeningEndTick = tick + 160;
    data.awakeningCooldownUntil = tick + 2400;
    → AwakeningS2CPacket(playerUuid, true) 전송

`AwakeningManager` 로직 (tickCheck):

    if (data.awakeningActive && tick >= data.awakeningEndTick)
      data.awakeningActive = false;
      → AwakeningS2CPacket(playerUuid, false) 전송

필드명 전체: `awakeningActive`, `awakeningEndTick`, `awakeningCooldownUntil` (모두 PlayerData에 선언).
`domainCooldownUntil`과 `awakeningCooldownUntil`은 **PlayerData 전용 필드** — cooldowns Map의 `"domain"` / `"awakening"` 키 사용 금지 (decisions §3-2, §3-3).

---

## §4. 데미지 공식

### 4-1. finalDamage 공식 전체

    finalDamage = baseDamage × attackMultiplier × gradeMultiplier × conditionMultiplier × specialMultiplier

- `attackMultiplier`: attackStat 기반 (옷코츠 burstActive 시 ×1.30 포함)
- `gradeMultiplier`: §7-1 등급 배율 표
- `conditionMultiplier`: 각성(×1.25), Zone 페널티(×0.5), 흑섬(×2.5) 등
- `specialMultiplier`: 캐릭터 특수 조건(무위전변 방어 무시 등)

### 4-2. 배율 클램프 — §LOCK

최종 배율은 **×0.25~×4.0** 범위로 제한. 각성(×1.25), 옷코츠 burstActive(×1.30) 모두 이 클램프 안에 포함.

### 4-3. 방어 처리

- `isSoulDirect=true`: effectiveDefense를 **0으로 강제**, 별도 soul damage 로그
- `NON_SORCERER`: 별도 defense 배율 적용

### 4-4. DamageContext 필수 필드

```java
public class DamageContext {
    public boolean isSoulDirect;   // true = 방어 무시, soul damage 로그
    public boolean bypassRCT;      // true = RCT로 즉시 회복 불가
    public UUID attackerUuid;
    public UUID targetUuid;
    public String skillId;
    public float rawDamage;
    public float finalDamage;
}
```

---

## §5. CE 시스템

### 5-1. CEManager.regenTick 동작

`CEManager.regenTick(player)`: 매 tick Mixin TAIL에서 호출. `CERegenRule`을 참조해 틱당 재생량 계산. `CEPool`은 캐릭터별 재생 규칙을 캐싱. `lastCombatTick` 기준으로 전투 중/외 분기.

### 5-2. CE 재생 수치 — §LOCK (decisions §2-4)

- **전투 외**: `ceRegenOutOfCombat: 1.0` /틱
- **전투 중**: `ceRegenInCombat: 0.2` /틱

### 5-3. CE 소모·부족 처리

- 스킬 사용 시 서버가 CE를 먼저 예약 차감
- CE 부족 시 스킬 실패 → `SkillResultS2CPacket(result=FAIL, reason=CE_INSUFFICIENT)` 전송
- 영역 전개와 손가락 획득은 즉시 저장 대상

---

## §6. 캐릭터별 스킬 수치 — §LOCK (임의 변경 금지)

### 6-1. ISkillSet 인터페이스

```java
package com.jjk.character;

public interface ISkillSet {
    SkillResult onF(PlayerData data, ServerPlayerEntity player, long tick);
    SkillResult onShiftF(PlayerData data, ServerPlayerEntity player, long tick);
    SkillResult onR(PlayerData data, ServerPlayerEntity player, long tick);
    SkillResult onShiftR(PlayerData data, ServerPlayerEntity player, long tick);
    SkillResult onV(PlayerData data, ServerPlayerEntity player, long tick);
}
```

모든 캐릭터는 5개 메서드를 전부 구현한다. 미사용 키도 명시적으로 `SkillResult.NOT_IMPLEMENTED`를 반환한다.

### 6-2. 고죠 사토루 (`characterId = "gojo"`)

| 키 | 스킬 | baseDamage | CE | CD(틱) | 구현핵심 |
|----|------|------------|----|--------|----------|
| F | 창(Blue) | 42 | 80 | 4 | `AttractionFieldSkill`: 전방 12블록 끌어당김 후 데미지. |
| Shift+F | 혁(Red) | 64 | 160 | 8 | `RepulsionBlastSkill`: 폭발 반경 5블록, 넉백 3블록. |
| R | 허식 자(Purple) | 140 | 650 | 60 | `ExistenceEraseProjectile`: 관통, RCT 회복 3초(60틱) 차단. |
| Shift+R | 무량공처 | 0 | 3000 | 360 | `DomainManager.deployDomain("gojo_unlimited_void")`. |
| V | 무하한 토글 | 0 | 30/s | 1 | `InfinityHandler`: 조건 불충족 공격 취소. |

**무하한 무력화 조건** (InfinityHandler에서 체크, 4가지):
1. 상대방의 영역 전개 중
2. 영역 전연(Curtain) 내부
3. 메구미 마허라가 의식 적응 카운터 달성 (`maharagaThreshold: 5` 피격)
4. `isSoulDirect=true` 공격

### 6-3. 이타도리 유지 (`characterId = "itadori"`)

| 키 | 스킬 | baseDamage | CE | CD(틱) | 구현핵심 |
|----|------|------------|----|--------|----------|
| F | 경정권 | 34 | 72 | 4 | 1타: `baseDamage=34` 즉시. 2타: 5틱 후 동일 위치 `baseDamage=17` (`EffectDeferQueue` 예약). 흑섬 사용 후 60틱 이내: 2타에 흑섬 확률 +20%. |
| Shift+F | 맨지 킥 | 44 | 90 | 7 | 전방 짧은 돌진, hit 시 `ComboTracker` 흐름끊기 카운터 +1. |
| R | 흑섬 집중 | 0 | 120 | 20 | 다음 60틱 Just Frame 보정(흑섬 확률 보너스 적용). |
| Shift+R | 영역 startup | 0 | 2200 | 300 | `DomainManager.deployDomain("itadori_unnamed")`, animId 59. |
| V | RCT | 0 | 40/s | 5 | `data.healingActive=true` 동안 초당 HP 회복. CE 소모 지속. |

`EffectDeferQueue`: tick 기반 큐. `schedule(targetPos, baseDamage, delayTicks, attackerUuid)`. 대상 사망 시 자동 취소.

### 6-4. 후시구로 메구미 (`characterId = "megumi"`)

| 키 | 스킬 | baseDamage | CE | CD(틱) | 구현핵심 |
|----|------|------------|----|--------|----------|
| F | 누에 | 38 | 110 | 8 | 식신 엔티티 소환. 사망 시 `data.deadShikigamiIds`에 기록 → 영구 재소환 불가. |
| Shift+F | 옥견 | 32 | 90 | 6 | 추적형 식신 2기. 소환 전 `deadShikigamiIds` 검사 필수. |
| R | 마허라가 의식 | 0 | 900 | 180 | 적응 카운터 시작. 카운터는 피격 횟수 기반. `maharagaThreshold: 5` 달성 시 무하한 무력화. 실패(타임아웃 또는 CE 고갈) 시 CE 전량 소진 + `sealDurationTicks: 600`(30초) 스킬 봉인. |
| Shift+R | 감합암예정 | 0 | 2600 | 360 | 미완성 영역. `wallHp=750`(일반 결계 절반). `sureHitActive=false`. |
| V | 그림자 이동 | 0 | 140 | 12 | 바닥 그림자 마커(별도 서버 객체) 위치로 순간이동. 10블록 이내 마커 없으면 실패. |

### 6-5. 옷코츠 유타 (`characterId = "okkotsu"`)

| 키 | 스킬 | baseDamage | CE | CD(틱) | 구현핵심 |
|----|------|------------|----|--------|----------|
| F | 리카 소환 | 58 | 260 | 30 | `RikaEntity` 소환(GeckoLib 엔티티). `burstActive` 중 데미지 ×1.30. 소환 후 `rikaLifetimeTicks: 200`틱 자동 소멸. |
| Shift+F | 검격 | 72 | 180 | 10 | animId 40. 전방 arc 판정. |
| R | 복사 술식 | 0 | 300 | 45 | 최근 피격 스킬의 약화 복사본 1회 사용 (baseDamage × 0.7). |
| Shift+R | 주력해방 | 0 | 1100 | 60 | `data.burstActive=true`, `data.burstEndTick=tick+200`. 전능치 +30%·10초(200틱). |
| V | 반전술식 | 0 | 70/s | 8 | 조준 대상(아군) HP **직접 회복**. `healingActive` 세팅 없이 서버 로직에서 즉시 처리. (decisions §3-7) |

주력해방 구현:

```java
data.burstActive = true;
data.burstEndTick = tick + 200;

// DamageCalculator에서:
float burst = data.burstActive ? 1.30f : 1.0f;
attackMultiplier = 1 + Math.min(attackStat * burst, 120) / 100;

// burstEndTick 만료 처리 (tick Mixin에서):
if (data.burstActive && tick >= data.burstEndTick) {
    data.burstActive = false;
}
// burstActive 중 리카 소환(F) 데미지도 × 1.30 적용
```

### 6-6. 마히토 (`characterId = "mahito"`)

| 키 | 스킬 | baseDamage | CE | CD(틱) | 구현핵심 |
|----|------|------------|----|--------|----------|
| F | 무위전변 | 26 | 140 | 8 | 접촉 시 `isSoulDirect=true`, 방어 무시. |
| Shift+F | 다중혼 | 48 | 220 | 18 | animId 43. 3체 분열 투사체. |
| R | 영혼 방어 | 0 | 120 | 15 | 80틱 동안 soul damage 저항. `data.cooldowns.put("soul_resist", tick+80)`. |
| Shift+R | 칼날 변형 | 52 | 190 | 12 | animId 44. 팔 변형 근접 공격. |
| V | 자폐원돈과 | 0 | 3150 | 360 | `DomainManager.deployDomain("mahito_self_embodiment")`. 결계 반경 15블록 내 모든 엔티티 매 틱 영혼 데미지 10. `isSoulDirect=true`, `bypassRCT=true`. ★양날성: 마히토 본인 감쇠 50% → 매 틱 데미지 5. 마히토 HP 50% 이하 또는 200틱 경과 시 자동 해제. |

### 6-7. 죠고 (`characterId = "jogo"`)

| 키 | 스킬 | baseDamage | CE | CD(틱) | 구현핵심 |
|----|------|------------|----|--------|----------|
| F | 화산탄 | 46 | 130 | 6 | 폭발 반경 4블록, 화염 80틱. |
| Shift+F | 개관 | 80 | 280 | 20 | animId 46. 직선 화염기둥. |
| R | 운석 | 120 | 600 | 75 | 낙하 예고 40틱 후 광역 피해 (`EffectDeferQueue` 활용). |
| Shift+R | 불꽃의 고리 | 54 | 230 | 16 | animId 47. 주변 6블록 ring damage. |
| V | 개관철위산 | 0 | 2700 | 360 | `DomainManager.deployDomain("jogo_volcano_domain")`. |

### 6-8. 하카리 킨지 (`characterId = "hakari"`)

| 키 | 스킬 | baseDamage | CE | CD(틱) | 구현핵심 |
|----|------|------------|----|--------|----------|
| F | 잭팟 발동 | 0 | 600 | 20 | 1/239 확률. 지속시간: `config.jackpotDurationTicks`(기본값 **251틱**). 운영자 60~251 범위 조정 가능. 잭팟 중: CE CAP 체크 우회(`jackpotActive=true`면 ceMax 검사 스킵). 매 틱 자동 heal 1HP. |
| Shift+F | 주력방출 | 66 | 0 | 8 | animId 48. 잭팟 중 강화(baseDamage ×1.5). |
| R | 확률 재굴림 | 0 | 180 | 30 | 잭팟 실패 후 1회 재시도. |
| Shift+R | 불확정 영역 | 0 | 450 | 20 | 무작위 3종. `Random.nextInt(3)`으로 선택: [0] 충격파: 반경 8블록 데미지 45 + 넉백 2블록. [1] CE 흡수: 반경 6블록 엔티티 CE max의 15% 흡수 → 하카리 충전. [2] 속도 장판: 반경 5블록 60틱간 이동속도 -60%(하카리 제외). 결과를 `SkillResultS2CPacket`으로 전송. |
| V | 사좌철위산 | 0 | 2500 | 360 | `DomainManager.deployDomain("hakari_jackpot_domain")`. |

`PlayerData` 잭팟 전용 필드:

    boolean jackpotActive;
    long jackpotEndTick;

잭팟 종료 시 `jackpotActive=false`, `jackpotEndTick=0` 복원. `ceMax` 필드 자체는 변경하지 않는다.
`jackpotDurationMinTicks: 60` / `jackpotDurationTicks: 251` §LOCK (decisions §3-5).

### 6-9. 이누마키 토게 (`characterId = "inumaki"`)

채팅 인터셉트 이벤트: `ServerMessageEvents.ALLOW_CHAT_MESSAGE`와 `ServerMessageEvents.ALLOW_COMMAND_MESSAGE`를 **동시에** 등록한다.

| 키/명령 | 스킬 | baseDamage | CE | CD(틱) | 구현핵심 |
|---------|------|------------|----|--------|----------|
| F | !멈춰 | 0 | 120 | 10 | animId 50. 대상 40틱 정지. 부담 +15. |
| Shift+F | !터져 | 70 | 280 | 24 | animId 51. 반경 4블록 폭발. 부담 +30. |
| R | (미사용) | — | — | — | `SkillResult.NOT_IMPLEMENTED` 반환. 향후 주언 추가 예정. |
| Shift+R | !잠들어 | 0 | 240 | 30 | animId 52. 100틱 수면. 부담 +25. |
| V | !달려 | 0 | 150 | 18 | animId 53. 아군 이동속도 +40%, 80틱. 부담 +10. |

부담 감소 공식: 전투 외 5/s(틱당 0.25), 전투 중 2/s(틱당 0.1). `lastCombatTick` 기준으로 분기. 부담 100 초과 시 `data.cooldowns.put("skill_seal", tick + sealDurationTicks)`. `sealDurationTicks: 600`(30초) — decisions §2-2, §3-6.

### 6-10. 나나미 켄토 (`characterId = "nanami"`)

| 키 | 스킬 | baseDamage | CE | CD(틱) | 구현핵심 |
|----|------|------------|----|--------|----------|
| F | 십획주법 | 50 | 110 | 6 | 7:3 약점 위치 명중 시 ×1.5. 약점은 히트박스를 7:3으로 분할한 후방 30% 영역. |
| Shift+F | (미사용) | — | — | — | `SkillResult.NOT_IMPLEMENTED` 반환. |
| R | (미사용) | — | — | — | `SkillResult.NOT_IMPLEMENTED` 반환. |
| Shift+R | 경계선 | 86 | 240 | 20 | animId 54. 선분 판정, 방어 관통 30%. |
| V | (미사용) | — | — | — | `SkillResult.NOT_IMPLEMENTED` 반환. |

### 6-11. 히구루마 히로미 (`characterId = "higuruma"`)

| 키 | 스킬 | baseDamage | CE | CD(틱) | 구현핵심 |
|----|------|------------|----|--------|----------|
| F | 증거 제출 | 0 | 120 | 15 | `TrialManager.addEvidence(attacker, target)`. |
| Shift+F | (미사용) | — | — | — | `SkillResult.NOT_IMPLEMENTED` 반환. |
| R | (미사용) | — | — | — | `SkillResult.NOT_IMPLEMENTED` 반환. |
| Shift+R | 배심원 | 0 | 600 | 90 | animId 55. `TrialManager.startTrial(attacker, target)`. 기소→선택→판결 StateMachine. |
| V | 처형검 | 999 | 0 | 판결 후 | 유죄 판결 후 1회성. `data.hasExecutionSword=true`일 때만 사용 가능. 사용 후 `false` 복원. |

`PlayerData` 처형검 전용 필드:

    boolean hasExecutionSword;

히구루마 재판 성공 기준: **60%** (`trialSuccessRate: 0.60`).
재판 StateMachine: `IDLE → ACCUSED(대상 지정) → DELIBERATION(60틱 타임아웃) → VERDICT(유죄/무죄) → END`.
`skill_seal` 키: 히구루마 술식 봉인과 마허라가 의식 실패 봉인이 동일한 `"skill_seal"` 키를 공유한다 — decisions §3-6.

### 6-12. 스쿠나 료멘 (`characterId = "sukuna"`) — Phase 3 실구현 완료 (2026-05-28)

스쿠나 SkillSet은 Phase 3에서 실구현 완료됨 (decisions.md 2026-05-28).
AnimationRegistry animId 21~24 사용.
정확한 수치는 techniques.json sukuna 항목 기준. §LOCK 수치 변경 금지.

| 키 | 스킬 | 구현핵심 |
|-|-|-|
| F | 해체 | 참격 판정 |
| Shift+F | 필살참 | 강화 참격 |
| R | 개·화염 | 화염 광역 |
| Shift+R | 세계절단참 | 광역 참격 |
| V | 복마어주자 | `DomainManager.deployDomain("sukuna_malevolent_shrine")` |

> 수치(baseDamage·ceCost·cooldownTicks)는 techniques.json을 단일 소스로 사용.
> 이 문서에 수치를 중복 기재하지 않는다.

---

## §7. 등급과 성장

### 7-1. 등급별 데미지 배율

| 등급 | 데미지 배율 | 패시브 |
|------|------------|--------|
| 4급 | ×1.0 | 없음 |
| 3급 | ×1.15 | 없음 |
| 2급 | ×1.30 | 없음 |
| 1급 | ×1.50 | 없음 |
| 준특급 | ×1.65 | 없음 |
| 특급 | ×1.80 | 캐릭터 고유 패시브 1개 |

### 7-2. 등급별 스킬 해금

PvP/PvE 등급 배율은 분리한다. 기본값: `gradePvpScaling=true`. `unlockedSkills`는 캐릭터 선택 시 현재 등급에 맞게 초기화한다. 구체적인 스킬 해금 목록은 techniques.json에 `unlockGrade` 필드로 명시한다.

---

## §8. 영역 전개

### 8-1. DomainInstance — class(record 아님)

```java
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DomainInstance {
    public final UUID ownerId;
    public final String domainId;
    public final boolean isOpen;
    public final int maxRadius;
    public int currentRadius;     // 가변 필드
    public int wallHp;            // 가변 필드 — 개방형=0, 결계형=domains.json 값
    public final long deployedAtTick;
    public final Set<UUID> playersInside = new HashSet<>();
    public boolean sureHitActive; // 결계형 영역에서 내부 확정타 활성화 여부

    public DomainInstance(UUID ownerId, String domainId, boolean isOpen,
                          int maxRadius, int wallHpFromConfig, long deployedAtTick) {
        this.ownerId = ownerId;
        this.domainId = domainId;
        this.isOpen = isOpen;
        this.maxRadius = maxRadius;
        this.currentRadius = maxRadius;
        this.wallHp = isOpen ? 0 : wallHpFromConfig; // 하드코딩 금지 — domains.json에서 읽어온 값 사용
        this.deployedAtTick = deployedAtTick;
        this.sureHitActive = false;
    }

    public void applyTeamLimit(int teamDomainCount) {
        if (teamDomainCount >= 2) {
            this.currentRadius = this.maxRadius / 2;
        }
    }

    public void restoreRadius() {
        this.currentRadius = this.maxRadius;
    }
}
```

### 8-2. 영역 충돌 우선순위 공식

    priority = (grade × 0.30) + (ceInvested × 0.25) + (mastery × 0.30) + (wallHpRatio × 0.15)
    isOpen=true 이면 wallHpRatio 가중치 = 0

팀별 동시 영역 2개 제한. `DomainPriorityCalculator`가 계산.

### 8-3. 결계형 처리

- 기본 `wallHp`는 `domains.json`의 `wallHp` 필드에서 읽는다 (기본 1500). 하드코딩 금지.
- 내부 확정타(`sureHitActive`): 영역 완전 전개(startup 애니 완료) 후 `DomainManager`가 `true`로 설정.
- `megumi_chimera_shadow`는 예외: `sureHitActive=false` (미완성 영역).

### 8-4. 개방형(OPEN) 특수 처리 — decisions §3-4

`DomainManager.deployDomain()` 내 분기:

1. `wallHp = isOpen ? 0 : wallHpFromConfig`
2. 개방형 시전자는 다른 결계형 영역의 `wallHp`에 데미지 가능
3. 자박 시 radius = `openMaxRadius: 200`, **CE 소모 2배** (`isOpen=true` 시 `ceCost × 2`)
4. 영역 충돌 priority: `isOpen=true`이면 결계외피 가중치(0.15) = 0
5. `DomainBoundaryRenderer`(client 전용): `isOpen=true`이면 particle dome 미생성

### 8-5. domains.json 7개 도메인 확정값 (decisions.md 2026-05-23)

```json
{
  "domains": [
    {
      "domainId": "gojo_unlimited_void",
      "ownerCharacter": "gojo",
      "radius": 30,
      "ceCost": 3000,
      "cooldownTicks": 360,
      "isOpen": false,
      "openMaxRadius": 200,
      "wallHp": 1500,
      "sureHitActive": true,
      "ownerDamageReduction": 0.0,
      "autoTargetAll": false
    },
    {
      "domainId": "sukuna_malevolent_shrine",
      "ownerCharacter": "sukuna",
      "radius": 200,
      "ceCost": 3000,
      "cooldownTicks": 360,
      "isOpen": true,
      "openMaxRadius": 200,
      "wallHp": 0,
      "sureHitActive": true,
      "ownerDamageReduction": 0.0,
      "autoTargetAll": false
    },
    {
      "domainId": "mahito_self_embodiment",
      "ownerCharacter": "mahito",
      "radius": 15,
      "ceCost": 3150,
      "cooldownTicks": 360,
      "isOpen": false,
      "openMaxRadius": 200,
      "wallHp": 1500,
      "sureHitActive": true,
      "ownerDamageReduction": 0.5,
      "autoTargetAll": true
    },
    {
      "domainId": "itadori_unnamed",
      "ownerCharacter": "itadori",
      "radius": 20,
      "ceCost": 2200,
      "cooldownTicks": 300,
      "isOpen": false,
      "openMaxRadius": 200,
      "wallHp": 1500,
      "sureHitActive": true,
      "ownerDamageReduction": 0.0,
      "autoTargetAll": false
    },
    {
      "domainId": "megumi_chimera_shadow",
      "ownerCharacter": "megumi",
      "radius": 20,
      "ceCost": 2600,
      "cooldownTicks": 360,
      "isOpen": false,
      "openMaxRadius": 200,
      "wallHp": 750,
      "sureHitActive": false,
      "ownerDamageReduction": 0.0,
      "autoTargetAll": false
    },
    {
      "domainId": "jogo_volcano_domain",
      "ownerCharacter": "jogo",
      "radius": 25,
      "ceCost": 2700,
      "cooldownTicks": 360,
      "isOpen": false,
      "openMaxRadius": 200,
      "wallHp": 1500,
      "sureHitActive": true,
      "ownerDamageReduction": 0.0,
      "autoTargetAll": false
    },
    {
      "domainId": "hakari_jackpot_domain",
      "ownerCharacter": "hakari",
      "radius": 20,
      "ceCost": 2500,
      "cooldownTicks": 360,
      "isOpen": false,
      "openMaxRadius": 200,
      "wallHp": 1500,
      "sureHitActive": true,
      "ownerDamageReduction": 0.0,
      "autoTargetAll": false
    }
  ]
}
```

---

## §9. 반전술식과 상태 이상

### 9-1. RCT (healingActive, bypassRCT)

- `healingActive=true`인 동안 CE를 초당 소모하고 HP를 회복
- `DamageContext.bypassRCT=true`인 영혼 데미지는 RCT로 즉시 복구 불가
- 이타도리 V 사용 시 자신의 `healingActive` 세팅
- 옷코츠 V는 `healingActive` 세팅 없이 서버 로직에서 대상 HP 직접 회복 (decisions §3-7)

### 9-2. 영혼 데미지 (`isSoulDirect`)

`isSoulDirect=true`면 effectiveDefense=0 강제. 별도 soul damage 로그 기록.

### 9-3. 상태 이상 만료 처리

정지, 수면, 둔화, 술식 봉인은 서버 tick에서 만료 시간을 검사한다. 각 상태 이상은 `PlayerData.cooldowns` Map에 `"status_<type>"` 키로 만료 틱을 저장한다.

---

## §12. 설정 파일

### 12-1. config.json 전체 키 목록 (27개 — §LOCK 명시 항목 변경 금지)

| 키 | 값 | 비고 |
|----|----|----|
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
| `ceRegenOutOfCombat` | `1.0` | §LOCK — 전투 외 틱당 CE 재생 |
| `ceRegenInCombat` | `0.2` | §LOCK — 전투 중 틱당 CE 재생 |

### 12-2. techniques.json 필드 구조

필수 필드: `character`, `keyId`, `baseDamage`, `ceCost`, `cooldownTicks`, `animId`.

```json
{
  "techniques": [
    {
      "character": "gojo",
      "keyId": 0,
      "skillName": "blue",
      "baseDamage": 42,
      "ceCost": 80,
      "cooldownTicks": 4,
      "animId": 15,
      "unlockGrade": "4급"
    }
  ]
}
```

keyId 체계: F=0, Shift+F=1, R=2, Shift+R=3, V=4. 미사용 키는 `baseDamage=0, ceCost=0, cooldownTicks=0`으로 등록.

### 12-3. domains.json 필드 구조

필수 필드: `domainId`, `ownerCharacter`, `radius`, `ceCost`, `cooldownTicks`, `isOpen`, `openMaxRadius`, `wallHp`, `sureHitActive`, `ownerDamageReduction`, `autoTargetAll`. 전체 내용은 §8-5 참조.

---

## §13. PlayerData

### 13-1. PlayerData — 전체 필드

```java
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerData {
    // === 기본 정보 ===
    public UUID uuid;
    public String characterId;
    public String grade;
    public long xp;
    public int mastery;

    // === 전투 스탯 ===
    public float ceCurrent;
    public float ceMax;
    public float hpCurrent;
    public float hpMax;
    public int attackStat;
    public int defenseStat;
    public int speedStat;

    // === 진행 데이터 ===
    public int fingerCount;
    public List<String> unlockedSkills = new ArrayList<>();
    public Map<String, Long> cooldowns = new HashMap<>();

    // === 쿨타임 영속 전용 필드 (cooldowns Map과 별개) ===
    public long bindingVowDeclaredTick; // 미선언 = -1L
    public long domainCooldownUntil;    // cooldowns Map "domain" 키 사용 금지 (decisions §3-2)
    public long jackpotCooldownUntil;
    public long curtainCooldownUntil;
    public long awakeningCooldownUntil; // cooldowns Map "awakening" 키 사용 금지 (decisions §3-3)

    // === 상태 ===
    public String trialState;   // "IDLE"|"ACCUSED"|"DELIBERATION"|"VERDICT"|"END"
    public int burden;
    public boolean zoneActive;
    public long zoneEndTick;
    public long lastCombatTick;
    public String lastKnownIp;

    // === 각성 ===
    public boolean awakeningActive;
    public long awakeningEndTick;

    // === 옷코츠 주력해방 ===
    public boolean burstActive;
    public long burstEndTick;

    // === 메구미 식신 ===
    public List<String> deadShikigamiIds = new ArrayList<>();

    // === 회복 ===
    public boolean healingActive;

    // === Zone 페널티 ===
    public long zonePenaltyUntilTick;

    // === 히구루마 처형검 ===
    public boolean hasExecutionSword;

    // === 하카리 잭팟 ===
    public boolean jackpotActive;
    public long jackpotEndTick;

    // === 옷코츠 복사 술식 ===
    public String  lastReceivedSkillId;
    public float   lastReceivedBaseDamage;
    public float   lastReceivedCeCost;
    public int     lastReceivedCooldownTicks;
    public boolean lastReceivedIsDomain;

    // === 메구미 마허라가 의식 ===
    public int maharagaCounter; // 무하한 피격 횟수, maharagaThreshold 도달 시 Infinity 무력화

    // === 이타도리 흑섬 집중 ===
    public long blackFlashFocusEndTick; // 0 = 비활성, 양수 = 버프 만료 틱

    // === 스키마 버전 ===
    public int schemaVersion; // 현재 버전: 4
}
```

### 13-2. snapshot() 전체 코드

```java
public PlayerData snapshot() {
    PlayerData copy = new PlayerData();
    copy.uuid = this.uuid;
    copy.characterId = this.characterId;
    copy.grade = this.grade;
    copy.xp = this.xp;
    copy.mastery = this.mastery;
    copy.ceCurrent = this.ceCurrent;
    copy.ceMax = this.ceMax;
    copy.hpCurrent = this.hpCurrent;
    copy.hpMax = this.hpMax;
    copy.attackStat = this.attackStat;
    copy.defenseStat = this.defenseStat;
    copy.speedStat = this.speedStat;
    copy.fingerCount = this.fingerCount;
    copy.unlockedSkills = new ArrayList<>(this.unlockedSkills);
    copy.cooldowns = new HashMap<>(this.cooldowns);
    copy.bindingVowDeclaredTick = this.bindingVowDeclaredTick;
    copy.domainCooldownUntil = this.domainCooldownUntil;
    copy.jackpotCooldownUntil = this.jackpotCooldownUntil;
    copy.curtainCooldownUntil = this.curtainCooldownUntil;
    copy.awakeningCooldownUntil = this.awakeningCooldownUntil;
    copy.trialState = this.trialState;
    copy.burden = this.burden;
    copy.zoneActive = this.zoneActive;
    copy.zoneEndTick = this.zoneEndTick;
    copy.lastCombatTick = this.lastCombatTick;
    copy.lastKnownIp = this.lastKnownIp;
    copy.awakeningActive = this.awakeningActive;
    copy.awakeningEndTick = this.awakeningEndTick;
    copy.burstActive = this.burstActive;
    copy.burstEndTick = this.burstEndTick;
    copy.deadShikigamiIds = new ArrayList<>(this.deadShikigamiIds);
    copy.healingActive = this.healingActive;
    copy.zonePenaltyUntilTick = this.zonePenaltyUntilTick;
    copy.hasExecutionSword = this.hasExecutionSword;
    copy.jackpotActive = this.jackpotActive;
    copy.jackpotEndTick = this.jackpotEndTick;
    copy.lastReceivedSkillId       = this.lastReceivedSkillId;
    copy.lastReceivedBaseDamage    = this.lastReceivedBaseDamage;
    copy.lastReceivedCeCost        = this.lastReceivedCeCost;
    copy.lastReceivedCooldownTicks = this.lastReceivedCooldownTicks;
    copy.lastReceivedIsDomain      = this.lastReceivedIsDomain;
    copy.maharagaCounter           = this.maharagaCounter;
    copy.blackFlashFocusEndTick    = this.blackFlashFocusEndTick;
    copy.schemaVersion = this.schemaVersion;
    return copy;
}
```

`snapshot()` 테스트는 필드 수를 하드코딩하지 말고 Java Reflection으로 자동 카운트한다.

### 13-3. player_data.db SQL 스키마

```sql
CREATE TABLE IF NOT EXISTS player_data (
    uuid                         TEXT    PRIMARY KEY,
    character_id                 TEXT,
    grade                        TEXT    NOT NULL DEFAULT '4급',
    xp                           INTEGER NOT NULL DEFAULT 0,
    mastery                      INTEGER NOT NULL DEFAULT 0,
    ce_current                   REAL    NOT NULL DEFAULT 100.0,
    ce_max                       REAL    NOT NULL DEFAULT 100.0,
    hp_current                   REAL    NOT NULL DEFAULT 20.0,
    hp_max                       REAL    NOT NULL DEFAULT 20.0,
    attack_stat                  INTEGER NOT NULL DEFAULT 10,
    defense_stat                 INTEGER NOT NULL DEFAULT 10,
    speed_stat                   INTEGER NOT NULL DEFAULT 10,
    finger_count                 INTEGER NOT NULL DEFAULT 0,
    unlocked_skills              TEXT    NOT NULL DEFAULT '[]',
    cooldowns                    TEXT    NOT NULL DEFAULT '{}',
    binding_vow_declared_tick    INTEGER NOT NULL DEFAULT -1,
    domain_cooldown_until        INTEGER NOT NULL DEFAULT 0,
    jackpot_cooldown_until       INTEGER NOT NULL DEFAULT 0,
    curtain_cooldown_until       INTEGER NOT NULL DEFAULT 0,
    awakening_cooldown_until     INTEGER NOT NULL DEFAULT 0,
    trial_state                  TEXT    NOT NULL DEFAULT 'IDLE',
    burden                       INTEGER NOT NULL DEFAULT 0,
    zone_active                  INTEGER NOT NULL DEFAULT 0,
    zone_end_tick                INTEGER NOT NULL DEFAULT 0,
    last_combat_tick             INTEGER NOT NULL DEFAULT 0,
    last_known_ip                TEXT,
    awakening_active             INTEGER NOT NULL DEFAULT 0,
    awakening_end_tick           INTEGER NOT NULL DEFAULT 0,
    burst_active                 INTEGER NOT NULL DEFAULT 0,
    burst_end_tick               INTEGER NOT NULL DEFAULT 0,
    dead_shikigami_ids           TEXT    NOT NULL DEFAULT '[]',
    healing_active               INTEGER NOT NULL DEFAULT 0,
    zone_penalty_until_tick      INTEGER NOT NULL DEFAULT 0,
    has_execution_sword          INTEGER NOT NULL DEFAULT 0,
    jackpot_active               INTEGER NOT NULL DEFAULT 0,
    jackpot_end_tick             INTEGER NOT NULL DEFAULT 0,
    schema_version               INTEGER NOT NULL DEFAULT 1
);
```

### 13-4. Migrator schemaVersion 관리

현재 schemaVersion = **1**. `createDefault(uuid)`: `schemaVersion=1`, `bindingVowDeclaredTick=-1L`.

향후 필드 추가 시:

    MigratorV1toV2: ALTER TABLE player_data ADD COLUMN ...
    schemaVersion 2로 업데이트

mismatch (`schemaVersion < currentVersion`) 시 Migrator 실행. Migrator 실패 시 서버 기동 중단.

### 13-5. cooldown_persist 허용 키 목록

    # domainCooldownUntil    → PlayerData 전용 필드 사용 (decisions.md §3-2)
    # awakeningCooldownUntil → PlayerData 전용 필드 사용 (decisions.md §3-3)
    'binding_vow'  속박 쿨타임
    'jackpot'      하카리 잭팟 쿨타임
    'burst'        옷코츠 주력해방 쿨타임
    'curtain'      장막 쿨타임
    'zone_penalty' 존 종료 페널티 만료 틱
    'skill_seal'   히구루마 + 마허라가 봉인 공유 키 (decisions §3-6)
    'soul_resist'  마히토 영혼 방어 만료

실제 사용 키 네임스페이스:
  "cd_{characterId}_{keyId}"  일반 스킬 쿨타임 (예: "cd_gojo_0")
  "skill_seal"                히구루마 + 마허라가 봉인 공유 키
  "status_soul_resist"        마히토 영혼 방어 만료
  "status_{type}"             기타 상태이상 만료

사용 금지 키:
  "domain"    → PlayerData.domainCooldownUntil 사용
  "awakening" → PlayerData.awakeningCooldownUntil 사용

---

## §14. 보안과 검증

### 14-1. 입력 검증 6개 항목

CombatPipeline 1·2단계에서 서버 측 검증:

1. **거리**: 공격자-대상 간 서버 측 거리 계산
2. **시야**: 서버 측 ray-cast
3. **쿨타임**: `data.cooldowns.get(keyId) > currentTick` 여부 (2단계 — decisions §3-1)
4. **CE**: `data.ceCurrent >= skill.ceCost`
5. **캐릭터 소유**: `data.characterId.equals(expectedCharId)`
6. **진영**: `TeamManager.canAttack(attacker, target)`

검증 실패 시 `SkillResultS2CPacket(result=FAIL, reason=...)` 전송. 서버는 아무 상태도 변경하지 않는다.

### 14-2. 레이스 컨디션 처리 원칙

손가락 드롭, 캐릭터 중복, 처형검 지급, 영역 소유권 변경은 lock 또는 단일 서버 tick 큐로 원자 처리한다. §26-4 `FingerSystem.tryDrop` 참조.

---

## §18. 테스트 계획

### 18-1. 12개 테스트 클래스

| 테스트 클래스 | 검증 항목 |
|-------------|-----------|
| `DamageCalculatorTest` | 배율 clamp, PvP cap max_hp × 0.40, soul direct 방어 무시 |
| `DomainManagerTest` | 결계형 wallHp domains.json 값, 개방형 wallHp 0, 팀당 2개 제한, applyTeamLimit/restoreRadius |
| `PlayerDataSnapshotTest` | 전체 필드 snapshot 복사 (Reflection 기반 자동 카운트) |
| `PacketCodecTest` | ZoneEnter/ZoneExit/Awakening codec 직렬화·역직렬화 |
| `AwakeningManagerTest` | HP 30% 임계 발동, 160틱 자동해제, CD 2400틱, 중복 발동 방지 |
| `ComboTrackerTest` | Just Frame 8~12틱 윈도우, 30틱 리셋, 흐름끊기 카운터 |
| `BurdenManagerTest` | 스킬별 부담 증가, 전투외 5/s·전투중 2/s 분기, 100 초과 봉인 |
| `TrialManagerTest` | 기소→선택→판결→처형검 StateMachine 전체 플로우 |
| `FingerSystemTest` | 드롭 원자성, droppedMobs 중복 방지, fingerCount 상한 20 |
| `TeamManagerTest` | 같은 진영 공격 불가, 진영별 영역 2개 제한 |
| `CombatPipelineIntegrationTest` | 9단계 순서 전체, TickDamageCap 적용, 각성 발동 연동 |
| `MigratorTest` | V1 스키마 정상 로딩, 미래 버전 mismatch 감지 |

### 18-2. Phase 1 릴리스 게이트 조건

- 서버 dedicated 기동 성공
- 1시간 soak (TPS 18 이상 유지)
- 저장/재접속 데이터 무결성
- PvP cap (`max_hp × 0.40`) 동작 확인
- 캐릭터 선택 중복 방지 동작 확인
- 영역 충돌 우선순위 계산 정상 동작

---

## §23. 확정 운영값 — §LOCK 표

| 항목 | 확정값 |
|------|--------|
| 히구루마 재판 성공 기준 | 60% (`trialSuccessRate: 0.60`) |
| PvP 최종 피해 상한 | `max_hp × 0.40` |
| MANGA-EXP 기본값 | off (`mangaExpEnabled: false`) |
| 속박 파훼 조건 | 특급 이상 스킬 직격 시 해제 |
| 속박 타임아웃 | 300틱(15초) |
| 속박 sentinel | `-1L` |
| 동일 캐릭터 중복 | 팀당 1명 (`allowDuplicateCharacter: false`) |
| 영역 금지 청크 | `domainBannedChunks: ["lobby:0,0", "training:0,0"]` |
| 등급 배율 PvP/PvE 분리 | 분리 (`gradePvpScaling: true`) |
| 흑섬 데미지 배율 | `base × 2.5` (지수승 아님, 게임화 확정) |
| 흑섬 Just Frame 윈도우 | 8~12틱 |
| Zone 지속시간 | 200틱(10초) (`zoneDurationTicks: 200`) |
| Zone 종료 페널티 | ×0.5 (`zonePenaltyUntilTick` 만료까지) |
| 각성 발동 HP 임계 | `maxHp × 0.30` |
| 각성 지속시간 | 160틱(8초) |
| 각성 쿨타임 | 2400틱(120초) |
| CE 재생 (전투 외) | 1.0/틱 |
| CE 재생 (전투 중) | 0.2/틱 |
| 마허라가 적응 임계 | 5회 피격 (`maharagaThreshold: 5`) |
| 술식 봉인 지속 | 600틱(30초) (`sealDurationTicks: 600`) |
| skill_seal 키 | 히구루마 + 마허라가 공유 |
| 잭팟 기본값 | 251틱 (`jackpotDurationTicks: 251`) |
| 잭팟 최솟값 | 60틱 (`jackpotDurationMinTicks: 60`) |
| 개방형 영역 자박 CE | `ceCost × 2` |
| 옷코츠 V 회복 방식 | 직접 HP 회복 (`healingActive` 세팅 없음) |
| 리카 수명 | 200틱 (`rikaLifetimeTicks: 200`) |
| domainCooldownUntil | PlayerData 전용 필드 (cooldowns Map 키 사용 금지) |
| awakeningCooldownUntil | PlayerData 전용 필드 (cooldowns Map 키 사용 금지) |
| domains.json gojo radius | 30, wallHp 1500 |
| domains.json sukuna radius | 200, wallHp 0 (개방형) |
| domains.json mahito radius | 15, wallHp 1500 |
| domains.json itadori radius | 20, wallHp 1500 |
| domains.json megumi radius | 20, wallHp 750, sureHitActive false |
| domains.json jogo radius | 25, wallHp 1500 |
| domains.json hakari radius | 20, wallHp 1500 |

---

## §24. 참조 무결성

### 24-1. 작업별 참조 섹션 표

| 작업 | 참조 섹션 |
|------|-----------|
| 빌드·Mixin·의존성 | §1 |
| 성능 모니터링 | §2 |
| 전투·CombatPipeline·각성 | §3 |
| 데미지 공식·DamageContext | §4 |
| CE 재생·소모 | §5 |
| 캐릭터 SkillSet 구현 | §6 |
| 등급 배율 | §7 |
| 영역·domains.json | §8 |
| RCT·영혼 데미지·상태이상 | §9 |
| config.json·techniques.json | §12 |
| PlayerData·저장·마이그레이션 | §13 |
| 보안·검증 | §14 |
| 테스트 계획 | §18 |
| 확정값 §LOCK 표 | §23 |
| 게임 플로우·입장·부활·FingerSystem | §26 |
| 신규 캐릭터 추가 | §27 |

### 24-2. 미확정값 처리

코드 작성에 영향을 주는 미확정 항목은 §23 확정 운영값으로 대체했다. 새 미확정값을 추가할 경우 기본값과 config key를 함께 명시한다.

---

## §26. 게임 플로우

### 26-1. 최초 접속 플로우

이벤트: `ServerPlayConnectionEvents.JOIN`

처리 순서:

1. `repository.load(uuid)` 시도
2. null이면 최초 접속:
   - `PlayerData.createDefault(uuid)` 생성 (`schemaVersion=1`, `bindingVowDeclaredTick=-1L`, `characterId=null`)
   - `repository.saveImmediate(data)`
   - `CharacterSelectS2CPacket` 전송 → 클라이언트 캐릭터 선택 화면
3. null이 아니면 재접속:
   - 기존 데이터 로드
   - `CharacterInfoS2CPacket(playerUuid, characterId, grade)` 전송

### 26-2. 캐릭터 선택 시스템

C2S: `CharacterSelectC2SPacket(String characterId)`

서버 수신 처리 7단계:

1. `CharacterRegistry`에 등록된 유효한 `characterId`인지 검증
2. `config.allowDuplicateCharacter=false`이면 동일 캐릭터 중복 체크
3. `data.characterId = selectedCharacterId`
4. 기존 플레이어: 등급·숙련도 유지. 신규: `grade="4급"`
5. `unlockedSkills` 초기화 (등급 기반)
6. `saveImmediate(data)`
7. `CharacterConfirmS2CPacket(playerUuid, characterId)` → 클라이언트 인게임 전환

검증 실패 시: `CharacterSelectFailS2CPacket(reason=...)` 전송. 캐릭터 재선택: `config.allowCharacterReselect=true`일 때만 허용 (기본 false).

### 26-3. 부활 시스템

사망 처리 4단계 (`ServerPlayerEntityMixin.onDeath`):

1. `PlayerRepository.saveImmediate(data)`
2. `audit_log` 기록
3. 스쿠나 플레이어 → 손가락 드롭 판정 (§26-4)
4. `RespawnManager.scheduleRespawn(player, config.respawnDelayTicks)`

부활 후 상태 초기화:
- 부활 지점 텔레포트 (`config.respawnLocation`)
- CE = `ceMax × config.respawnCePercent`
- HP = `hpMax × config.respawnHpPercent`
- `awakeningActive=false`, `zoneActive=false`, `bindingVowDeclaredTick=-1L`
- `RespawnS2CPacket(playerUuid, spawnPos)` → 클라이언트 부활 연출

### 26-4. 손가락 드롭 원자적 처리 — FingerSystem.tryDrop 전체 코드

```java
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.Random;

public final class FingerSystem {
    private final Object fingerLock = new Object();
    private final Set<UUID> droppedMobs = Collections.synchronizedSet(new HashSet<>());
    private final PlayerRepository repository;
    private final JjkConfig config;
    private final Random random = new Random();

    public FingerSystem(PlayerRepository repository, JjkConfig config) {
        this.repository = repository;
        this.config = config;
    }

    public void tryDrop(UUID mobId, UUID killerUuid) {
        synchronized (fingerLock) {
            if (droppedMobs.contains(mobId)) return;
            if (random.nextFloat() >= config.fingerDropRate()) return;
            droppedMobs.add(mobId);
            PlayerData data = repository.load(killerUuid);
            data.fingerCount = Math.min(data.fingerCount + 1, config.fingerMaxCount());
            repository.saveImmediate(data);
            if (data.fingerCount >= config.fingerMaxCount()) {
                FullRevivalEvents.trigger(killerUuid);
            }
        }
    }
}
```

### 26-5. 진영 분류 표

| 진영 | 소속 캐릭터 |
|------|------------|
| `JUJUTSU_SORCERER` | 고죠·이타도리·메구미·옷코츠·나나미·히구루마·하카리·이누마키 |
| `CURSED_SPIRIT` | 스쿠나·마히토·죠고 (운영자 설정으로 선택 가능) |
| `NON_SORCERER` | 비술사 타입 |

`TeamManager` (패키지: `com.jjk.team`):
- 같은 진영 공격 불가 → CombatPipeline 1단계에서 체크
- 영역 진영별 2개 제한 체크
- 등급 차이 2 이상 전투 시 XP × `config.xpMultiplierGradeDiff` 적용

### 26-6. AnimationRegistry (animId 0~59)

| animId | 애니메이션 | animId | 애니메이션 |
|--------|-----------|--------|-----------|
| 0 | common_idle | 30 | jogo_f |
| 1 | common_cast | 31 | jogo_r |
| 2 | common_hit | 32 | jogo_v |
| 3 | common_dash | 33 | mahito_f |
| 4 | common_guard | 34 | mahito_r |
| 5 | common_death | 35 | mahito_domain |
| 6 | common_respawn | 36 | hakari_f |
| 7 | common_domain_start | 37 | hakari_v |
| 8 | common_domain_loop | 38 | higuruma_f |
| 9 | common_black_flash | 39 | higuruma_v |
| 10 | itadori_f | 40 | okkotsu_shift_f |
| 11 | itadori_shift_f | 41 | okkotsu_shift_r_burst |
| 12 | itadori_r | 42 | okkotsu_v |
| 13 | itadori_shift_r | 43 | mahito_shift_f |
| 14 | itadori_v | 44 | mahito_shift_r |
| 15 | gojo_f | 45 | mahito_v |
| 16 | gojo_shift_f | 46 | jogo_shift_f |
| 17 | gojo_r | 47 | jogo_shift_r |
| 18 | gojo_shift_r | 48 | hakari_shift_f |
| 19 | gojo_domain | 49 | hakari_shift_r |
| 20 | gojo_v | 50 | inumaki_f_stop |
| 21 | sukuna_f (예약) | 51 | inumaki_shift_f_burst |
| 22 | sukuna_shift_f (예약) | 52 | inumaki_shift_r_sleep |
| 23 | sukuna_r (예약) | 53 | inumaki_v_run |
| 24 | sukuna_shift_r (예약) | 54 | nanami_shift_r |
| 25 | megumi_f | 55 | higuruma_shift_r_jury |
| 26 | megumi_shift_f | 56 | black_flash_great |
| 27 | megumi_r | 57 | awakening_enter |
| 28 | megumi_shift_r | 58 | zone_enter |
| 29 | megumi_v | 59 | itadori_domain_startup |

animId 60~127: 신규 캐릭터 예약. keyId 범위: byte 0~127 (현재 0~10 사용).

### 26-7. S2C 패킷 전체 목록

| 패킷 | 필드 | 용도 |
|------|------|------|
| `SkillResultS2CPacket` | playerUuid, skillId, result, reason(nullable) | 스킬 연출 및 실패 사유 |
| `ZoneEnterS2CPacket` | playerUuid, durationTicks | Zone 진입 |
| `ZoneExitS2CPacket` | playerUuid | Zone 종료 |
| `AwakeningS2CPacket` | playerUuid, active | 각성 표시 |
| `CharacterInfoS2CPacket` | playerUuid, characterId, grade | 재접속 시 캐릭터 정보 |
| `CharacterSelectS2CPacket` | (없음) | 최초 접속 캐릭터 선택 화면 |
| `CharacterConfirmS2CPacket` | playerUuid, characterId | 선택 완료 후 인게임 전환 |
| `CharacterSelectFailS2CPacket` | reason(String) | 선택 실패 사유 |
| `RespawnS2CPacket` | playerUuid, spawnPos | 부활 연출 |

ZoneEnterS2CPacket 정의:

```java
import java.util.UUID;

public record ZoneEnterS2CPacket(UUID playerUuid, int durationTicks) implements CustomPayload {
    public static final Id<ZoneEnterS2CPacket> ID = new Id<>(Identifier.of("jjk", "zone_enter"));
    public static final PacketCodec<RegistryByteBuf, ZoneEnterS2CPacket> CODEC = PacketCodec.of(
        (buf, pkt) -> {
            buf.writeString(pkt.playerUuid().toString());
            buf.writeInt(pkt.durationTicks());
        },
        buf -> new ZoneEnterS2CPacket(UUID.fromString(buf.readString()), buf.readInt())
    );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
```

---

## §27. 신규 캐릭터 추가 절차

12번째 이후 캐릭터 추가 시 아래 순서로 실행:

1. `techniques.json`에 스킬 항목 추가 (`character`·`keyId`·`baseDamage`·`ceCost`·`cooldownTicks`·`animId`·`unlockGrade` 필수)
2. `domains.json`에 영역 파라미터 추가 (영역 있는 캐릭터만)
3. `AnimationRegistry`에 animId 등록 (60번부터 순서대로)
4. `impl/` 패키지에 `{CharName}SkillSet.java` 생성 — `ISkillSet` 5개 메서드 전부 구현. 미사용 키는 `SkillResult.NOT_IMPLEMENTED` 반환.
5. `SkillRegistry.register("{charId}", new {CharName}SkillSet())` 등록
6. `PlayerData`에 캐릭터 전용 필드 필요 시 추가 + `snapshot()` 반영
7. `schema_version` 증가 + `MigratorV{N}toV{N+1}` 작성
8. `ISkillSetTest`에 신규 캐릭터 케이스 추가
9. §26-5 진영 분류 표에 신규 캐릭터 추가
10. Phase 1 릴리스 게이트 재통과 확인

keyId 범위: byte 0~127 (현재 0~10 사용, 여유 충분)
animId 범위: byte 0~127 (현재 0~59 사용, 60~127 여유)

Phase 3에서 추가할 7개 캐릭터(마히토·죠고·하카리·이누마키·나나미·히구루마·스쿠나)도 이 절차를 따른다. §19 Phase 3와 §27 절차는 동일 대상이다.

---

## §25. 원작 구현 체크리스트

| 기능 | 상태 |
|---|---|
| 고죠 무한·오색자·영역 | 완료 |
| 이타도리 이격형 발산권·천원지방각 | 완료 |
| 메구미 누에·신성 사냥개·마허라가 | 완료 |
| 옷코츠 리카 주력해방 | 완료 |
| 스쿠나 해체·필살참·개·세계절단참·복마어주자 | 완료 |
| 나나미 십획주법 7:3 약점 판정 | 완료 |
| 죠고 화염 도트 데미지 TickScheduler 등록 | 완료 |
| 이누마키 채팅 인터셉트 + 부담 시스템 | 완료 |
| 마히토 자폐원돈과 양날성 DomainManager 처리 | 완료 |
| 하카리 잭팟 1/239 + config 지속시간 | 완료 |
| 히구루마 처형검 1회성 + saveImmediate | 완료 |
| 흑섬 Just Frame 8~12틱 판정 | 완료 |
| 영역 충돌·우선순위 계산 | 완료 |
| TickDamageCap PvP 상한 | 완료 |
| 각성 HP 30% 트리거 + 쿨타임 | 완료 |
| 손가락 드롭 atomic 처리 | 완료 |
| 채팅 스킬 인터셉트 (이누마키) | 완료 |
