# Claude Cowork — JJK 서버 개발 명세서 v5 최종본

> 대상: Minecraft Fabric 1.21.1 서버 모드  
> 목표: Claude Code가 바로 구현 가능한 서버 개발 명세  
> 원칙: 확정 수치는 임의 변경 금지. 명백한 계산 오류와 런타임/빌드 오류만 수정.

---

## §0. CLAUDE.md 작업 규칙

1. 기존 확정 수치와 밸런스 값은 임의 변경하지 않는다.
2. 중복 내용은 최신·상세한 쪽만 남긴다.
3. Java 코드 블록에는 실제 컴파일 가능한 Java 예시만 둔다.
4. `[AI-ADD]` / `[DOCX]` / `[AI-FIX]` / `[AI-FINAL]` 태그는 문서 주석 전용이며 Java 코드 블록 안에 넣지 않는다.
5. Mixin 추가 시 `jjk.mixins.json`도 동시에 수정한다.
6. PlayerData 필드 추가 시 `snapshot()` 복사와 DB 마이그레이션을 함께 작성한다.
7. `[AI-ADD]` / `[DOCX]` / `[AI-FIX]` / `[AI-FINAL]` 태그는 문서 주석 전용. Java 코드 블록 안에 포함 금지(주석 형태도 금지).
8. `bindingVowDeclaredTick`은 `CooldownManager`가 아닌 `PlayerData`에 저장.
9. 각성 발동 전 `awakeningCooldownUntil` 체크 필수. 중복 발동 방지.
10. `DomainInstance`는 class(record 아님). `wallHp`·`currentRadius`는 가변 필드.

### 0-1. 세션 프롬프트

| 번호 | 용도 | 프롬프트 |
|------|------|----------|
| 1 | 서버 공통 | "Fabric 1.21.1 기준. 서버 권위(authoritative)로 구현하고 클라이언트 패킷은 표시 전용." |
| 2 | 전투 | "§3·§4 기준. CombatPipeline 순서와 TickDamageCap을 지키고 확정 수치를 바꾸지 말 것." |
| 3 | 캐릭터 | "§6 기준. ISkillSet 5개 키(F, Shift+F, R, Shift+R, V)를 모두 구현." |
| 4 | 영역 | "§8 기준. DomainInstance는 class이며 wallHp와 currentRadius는 가변." |
| 5 | 저장 | "§13 기준. PlayerData 필드 추가 시 snapshot·schema migration·테스트 동시 반영." |
| 6 | QA | "§18 기준. 변경한 시스템 단위 테스트와 통합 스모크 테스트를 추가." |
| 7 | Mixin 작업 | "§1-6 기준. @Inject at= 반드시 명시. cancellable=true 남용 금지. Mixin 추가 시 jjk.mixins.json 동시 수정." |
| 8 | 게임 플로우 작업 | "§26 기준. 캐릭터 선택·부활·입장 플로우 준수. 손가락 드롭은 atomic 처리 필수." |

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
| `domain/` | `DomainManager` · `DomainInstance` · `DomainPriorityCalculator` · `DomainBoundaryRenderer` |
| `zone/` | `ZoneStateManager` · `ComboTracker` |
| `awakening/` | `AwakeningManager` |
| `burden/` | `BurdenManager` |
| `trial/` | `TrialManager` · `TrialStateMachine` |
| `network/c2s/` | `SkillUseC2SPacket` · `CharacterSelectC2SPacket` |
| `network/s2c/` | `SkillResultS2CPacket` · `ZoneEnterS2CPacket` · `ZoneExitS2CPacket` · `AwakeningS2CPacket` · `CharacterInfoS2CPacket` |
| `data/` | `PlayerData` · `PlayerRepository` · `Migrator` |
| `mixin/` | `ServerPlayerEntityMixin` · `LivingEntityMixin` · `ServerWorldMixin` |

### 1-2. 리소스 파일

`src/main/resources/fabric.mod.json`에는 `jjk.mixins.json`을 등록한다.

`src/main/resources/jjk.mixins.json` 실제 내용:

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

### 1-3. 데이터 파일

| 파일 | 목적 |
|------|------|
| `config/jjk/config.json` | 서버 밸런스와 기능 플래그 |
| `config/jjk/techniques.json` | 스킬 정의 |
| `config/jjk/domains.json` | 영역 전개 정의 |
| `world/jjk/player_data.db` | PlayerData 영속 저장 |
| `world/jjk/audit_log.db` | 운영·전투 감사 로그 |

### 1-4. build.gradle 핵심 설정

`repositories`에는 아래 Maven을 포함한다.

    maven { url 'https://maven.bernie.software/' }
    maven { url 'https://maven.azuredevelopment.xyz/' }
    maven { url 'https://maven.kosmx.dev/' }
    maven { url 'https://maven.fabricmc.net/' }

의존성 버전:

    modImplementation "software.bernie.geckolib:geckolib-fabric-1.21:4.8.3"
    modImplementation "mod.azure.azurelib:azurelib-fabric-1.21.1:3.1.8"
    modImplementation "dev.kosmx.player-anim:player-animation-lib-fabric:2.0.4+1.21.1-fabric"

⚠️ GeckoLib 4.8.4는 Forge 전용. Fabric에서 `ClassNotFoundException` 크래시 발생. 반드시 4.8.3 사용.

### 1-5. 서버 권위 원칙

클라이언트는 키 입력과 표시만 담당한다. 데미지, CE 소모, 쿨타임, 영역 충돌, 손가락 드롭, 캐릭터 선택 중복 검사는 모두 서버에서 확정한다.

### 1-6. Mixin 후킹 명세

`ServerPlayerEntityMixin`

    @Inject method="tick" at=TAIL
      → CEManager.regenTick(player)
      → AwakeningManager.checkAndActivate(player)
      → ZoneStateManager.tick(player)
      → BurdenManager.tick(player)

    @Inject method="onDeath" at=HEAD
      → CharacterCommandService.saveImmediate(data)
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

### 2-1. 목표 환경

Fabric 1.21.1, Java 21, dedicated server 기준. 클라이언트 전용 렌더링 코드는 `client` 소스셋 또는 `EnvType.CLIENT` 분기로 격리한다.

### 2-2. Tick 예산

서버 메인 루프는 평균 50ms/tick 이하를 유지한다. 영역, 장막, 광역 스킬은 엔티티 검색 범위를 캐시하고 5틱 단위 배치 처리 가능.

### 2-3. 네트워크 원칙

스킬 결과 패킷은 연출용이다. 클라이언트가 보낸 데미지·명중·쿨타임 결과는 신뢰하지 않는다.

### 2-4. 저장 원칙

손가락 획득, 캐릭터 선택, 사망, 처형검 획득, 영역 전개 시작/종료는 즉시 저장 대상이다. 일반 CE·HP 변화는 주기 저장 가능.

### 2-5. 성능 모니터링

#### 전체 지표 기준값

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

선언 후 15초 내 조건 미달성 시 자동 해제한다. `bindingVowDeclaredTick`은 `PlayerData`에 저장한다. 속박 파훼 조건은 특급 이상 스킬 직격 시 해제.

### 3-2. 흑섬(Black Flash)

| 항목 | 값 |
|------|----|
| 판정 | 공격 직전·직후 Just Frame |
| 기본 확률 | 숙련도와 Zone 상태 기반 |
| 데미지 | base × 2.5 |
| 후속 효과 | Zone 진입, 콤보 보정 |

⚠️ 원작 vs 게임화 결정:
원작(Akutami Vol.6): 흑섬 데미지 = base^2.5(지수승)  
이 서버: base × 2.5(배율). 이유: base^2.5는 TickDamageCap에 걸려 실질 효과 없음.  
이 결정은 확정값. 코드에서 임의 변경 금지.

### 3-3. Zone

흑섬 성공 또는 특정 콤보 조건 달성 시 진입한다. 종료 후 `zonePenaltyUntilTick`까지 최종 배율 ×0.5 페널티를 적용한다.

### 3-4. TickDamageCap

PvP 최종 피해 상한은 `max_hp × 0.40`이다. PvE 상한은 config로 분리 가능하지만 기본 전투 공식 순서는 동일하다.

### 3-5. Just Frame

Just Frame 윈도우는 8~12틱이다. 30틱 동안 후속 입력이 없으면 콤보를 리셋한다.

### 3-6. CombatPipeline 순서

1. 입력 검증
2. CE·쿨타임 검증
3. 명중·거리·시야 검증
4. baseDamage 산출
5. 흑섬·Zone·각성 판정
6. 방어·저항·무하한·영혼 데미지 처리
7. 최종 배율 clamp
8. TickDamageCap 적용
9. 데미지 반영 및 S2C 전송

### 3-7. 각성(覚醒) 시스템

발동 조건: `currentHp ≤ maxHp × 0.30`  
발동 위치: CombatPipeline 5단계 직후 `AwakeningManager.checkAndActivate()`  
지속시간: 8초(160틱) — `awakeningEndTick = currentTick + 160`  
쿨타임: 120초(2400틱) — `awakeningCooldownUntil = currentTick + 2400`  
효과: `finalMultiplier × 1.25`(클램프 ×4.0 내 포함)  
해제: `awakeningEndTick` 도달 시 자동 해제

구현 클래스: `AwakeningManager`

    checkAndActivate(PlayerData data, float currentHp, float maxHp, long tick):
      if (currentHp > maxHp * 0.30f) return;
      if (tick < data.awakeningCooldownUntil) return;
      if (data.awakeningActive) return;
      data.awakeningActive = true;
      data.awakeningEndTick = tick + 160;
      data.awakeningCooldownUntil = tick + 2400;
      AwakeningS2CPacket(playerUuid, true) 전송

    tickCheck(PlayerData data, long tick):
      if (data.awakeningActive && tick >= data.awakeningEndTick)
        data.awakeningActive = false;
        AwakeningS2CPacket(playerUuid, false) 전송

주의: `awakeningCooldownUntil` 체크 필수. 중복 발동 방지. 각성 체크는 TickDamageCap 체크보다 먼저 수행.

---

## §4. 데미지 공식

### 4-1. 기본 공식

`finalDamage = baseDamage × attackMultiplier × gradeMultiplier × conditionMultiplier × specialMultiplier`

### 4-2. 배율 클램프

최종 배율은 ×0.25~×4.0 범위로 제한한다. 각성(`awakeningActive`) ×1.25와 옷코츠 주력해방(`burstActive`) ×1.30도 이 클램프 안에 포함한다.

### 4-3. 방어 처리

`isSoulDirect=true`면 effectiveDefense를 0으로 강제한다. `NON_SORCERER`는 별도 defense 배율을 적용한다.

### 4-4. [AI-FINAL] 변수 무결성

`awakeningActive`, `burstActive`, `healingActive`, `zonePenaltyUntilTick`, `bindingVowDeclaredTick`은 모두 §13-1 `PlayerData`에 선언되어야 한다.

---

## §5. 주력(CE) 시스템

### 5-1. CE 기본

CE는 `ceCurrent`와 `ceMax`로 관리한다. 재생은 캐릭터, 등급, 전투 상태, 패시브에 따라 달라진다.

### 5-2. CE 소모

스킬 사용 시 서버가 CE를 먼저 예약 차감한다. 검증 실패 시 차감하지 않는다. 영역 전개와 손가락 획득은 즉시 저장 대상이다.

### 5-3. CE 부족

CE 부족 시 스킬은 실패하고 `SkillResultS2CPacket`으로 실패 사유를 전송한다.

---

## §6. 캐릭터별 SkillSet

### 6-1. 공통 키

모든 캐릭터는 `F`, `Shift+F`, `R`, `Shift+R`, `V` 5개 메서드를 구현한다. 미사용 키도 명시적으로 실패 결과를 반환한다.

### 6-2. 고죠 사토루

| 키 | 스킬 | baseDamage | CE | CD | 구현핵심 |
|----|------|------------|----|----|----------|
| F | 창(Blue) | 42 | 80 | 4 | `AttractionFieldSkill`: 전방 12블록 끌어당김 후 데미지. |
| Shift+F | 혁(Red) | 64 | 160 | 8 | `RepulsionBlastSkill`: 폭발 반경 5블록, 넉백 3블록. |
| R | 허식 자(Purple) | 140 | 650 | 60 | `ExistenceEraseProjectile`: 관통, RCT 회복 3초 차단. |
| Shift+R | 무량공처 | 0 | 3000 | 360 | `DomainManager.deployDomain("gojo_unlimited_void")`. |
| V | 무하한 토글 | 0 | 30/s | 1 | `InfinityHandler`: 조건 불충족 공격 취소. |

### 6-3. 이타도리 유지

| 키 | 스킬 | baseDamage | CE | CD | 구현핵심 |
|----|------|------------|----|----|----------|
| F | 경정권 | 34 | 72 | 4 | 1타: `baseDamage=34` 즉시. 2타: 5틱 후 동일 위치 `baseDamage=17`(`EffectDeferQueue` 예약). 검은 섬광 사용 후 60틱 이내: 2타에 흑섬 확률 +20%. |
| Shift+F | 맨지 킥 | 44 | 90 | 7 | 전방 짧은 돌진, hit 시 흐름끊기 카운터 +1. |
| R | 흑섬 집중 | 0 | 120 | 20 | 다음 60틱 Just Frame 보정. |
| Shift+R | 영역 startup | 0 | 2200 | 300 | `DomainManager.deployDomain("itadori_unnamed")`, animId 59. |
| V | RCT | 0 | 40/s | 5 | `healingActive=true` 동안 초당 HP 회복. |

### 6-4. 후시구로 메구미

| 키 | 스킬 | baseDamage | CE | CD | 구현핵심 |
|----|------|------------|----|----|----------|
| F | 누에 | 38 | 110 | 8 | 식신 엔티티 소환. 사망 시 `deadShikigamiIds`에 기록. |
| Shift+F | 옥견 | 32 | 90 | 6 | 추적형 식신 2기. 영구 사망 목록 검사 필수. |
| R | 마허라가 의식 | 0 | 900 | 180 | 적응 카운터 시작, 실패 시 큰 리스크. |
| Shift+R | 감합암예정 | 0 | 2600 | 360 | 미완성 영역, 결계 약함. |
| V | 그림자 이동 | 0 | 140 | 12 | 10블록 내 그림자 위치로 순간이동. |

### 6-5. 옷코츠 유타

| 키 | 스킬 | baseDamage | CE | CD | 구현핵심 |
|----|------|------------|----|----|----------|
| F | 리카 소환 | 58 | 260 | 30 | `RikaEntity` 소환. `burstActive` 중 데미지 ×1.30. |
| Shift+F | 검격 | 72 | 180 | 10 | animId 40. 전방 arc 판정. |
| R | 복사 술식 | 0 | 300 | 45 | 최근 피격 스킬의 약화 복사본 1회 사용. |
| Shift+R | 주력해방 | 0 | 1100 | 60 | 전능치+30%·10초. |
| V | 반전술식 | 0 | 70/s | 8 | 아군 대상 회복 가능. |

주력해방 구현:

    PlayerData.burstActive = true; burstEndTick = tick + 200;
    DamageCalculator:
      float burst = data.burstActive ? 1.30f : 1.0f;
      attackMultiplier = 1 + min(attackStat * burst, 120) / 100;
    burstActive 중 리카 소환(F키) 데미지도 × 1.30 적용

### 6-6. 마히토

| 키 | 스킬 | baseDamage | CE | CD | 구현핵심 |
|----|------|------------|----|----|----------|
| F | 무위전변 | 26 | 140 | 8 | 접촉 시 `isSoulDirect=true`, 방어 무시. |
| Shift+F | 다중혼 | 48 | 220 | 18 | animId 43. 3체 분열 투사체. |
| R | 영혼 방어 | 0 | 120 | 15 | 80틱 동안 soul damage 저항. |
| Shift+R | 칼날 변형 | 52 | 190 | 12 | animId 44. 팔 변형 근접 공격. |
| V | 자폐원돈과 | 0 | 3150 | 360 | 결계 반경 15블록 내 모든 엔티티 매 틱 영혼 데미지 10. `isSoulDirect=true`. `DamageContext.bypassRCT=true`. ★ 시전자 마히토에게도 적용(양날성): 본인 감쇠 50% → 매 틱 데미지 5. 마히토 HP 50% 이하 또는 200틱 경과 시 자동 해제. `domains.json`: `ownerDamageReduction=0.5`, `autoTargetAll=true`. |

### 6-7. 죠고

| 키 | 스킬 | baseDamage | CE | CD | 구현핵심 |
|----|------|------------|----|----|----------|
| F | 화산탄 | 46 | 130 | 6 | 폭발 반경 4블록, 화염 80틱. |
| Shift+F | 개관 | 80 | 280 | 20 | animId 46. 직선 화염기둥. |
| R | 운석 | 120 | 600 | 75 | 낙하 예고 40틱 후 광역 피해. |
| Shift+R | 불꽃의 고리 | 54 | 230 | 16 | animId 47. 주변 6블록 ring damage. |
| V | 개관철위산 | 0 | 2700 | 360 | 화산 영역. |

### 6-8. 하카리 킨지

| 키 | 스킬 | baseDamage | CE | CD | 구현핵심 |
|----|------|------------|----|----|----------|
| F | 잭팟 발동 | 0 | 600 | 20 | 1/239 확률. 지속시간: `config.jackpotDurationTicks`(기본값 251 = 원작 4분11초). PvP 균형을 위해 운영자가 60~251 범위에서 조정 가능. 잭팟 중: `maxCE` 제한 없음. 매 틱 자동 heal 1HP. `[MANGA-EXP]` |
| Shift+F | 주력방출 | 66 | 0 | 8 | animId 48. 잭팟 중 강화. |
| R | 확률 재굴림 | 0 | 180 | 30 | 잭팟 실패 후 1회 재시도. |
| Shift+R | 불확정 영역 | 0 | 450 | 20 | 무작위 3종 `[SERVER-ORIGINAL]`. 서버 `Random.nextInt(3)`으로 선택: [0] 충격파: 반경 8블록 데미지 45 + 넉백 2블록. [1] CE 흡수: 반경 6블록 엔티티 CE max의 15% 흡수 → 하카리 충전. [2] 속도 장판: 반경 5블록 60틱간 이동속도 -60%(하카리 제외). 선택 결과를 `SkillResultS2CPacket`으로 전송 → 클라이언트 이펙트 분기. |
| V | 영역 전개 | 0 | 2500 | 360 | 사좌철위산. |

### 6-9. 이누마키 토게

채팅 인터셉트 이벤트는 `ServerMessageEvents.ALLOW_CHAT_MESSAGE`와 `ServerMessageEvents.ALLOW_COMMAND_MESSAGE`를 동시에 등록한다.

| 키/명령 | 스킬 | baseDamage | CE | CD | 구현핵심 |
|---------|------|------------|----|----|----------|
| F | !멈춰 | 0 | 120 | 10 | animId 50. 대상 40틱 정지. 부담 증가. |
| Shift+F | !터져 | 70 | 280 | 24 | animId 51. 반경 4블록 폭발. |
| Shift+R | !잠들어 | 0 | 240 | 30 | animId 52. 100틱 수면. |
| V | !달려 | 0 | 150 | 18 | animId 53. 아군 이동속도 +40%, 80틱. |

부담 감소: 전투 외 5/s, 전투 중 2/s. 부담 100 초과 시 술식 봉인.

### 6-10. 나나미 켄토

| 키 | 스킬 | baseDamage | CE | CD | 구현핵심 |
|----|------|------------|----|----|----------|
| F | 십획주법 | 50 | 110 | 6 | 7:3 약점 위치 명중 시 ×1.5. |
| Shift+R | 경계선 | 86 | 240 | 20 | animId 54. 선분 판정, 방어 관통 일부. |

### 6-11. 히구루마 히로미

| 키 | 스킬 | baseDamage | CE | CD | 구현핵심 |
|----|------|------------|----|----|----------|
| F | 증거 제출 | 0 | 120 | 15 | `TrialManager.addEvidence`. |
| Shift+R | 배심원 | 0 | 600 | 90 | animId 55. 기소→선택→판결 StateMachine. |
| V | 처형검 | 999 | 0 | 판결 후 | 유죄 판결 후 1회성. |

히구루마 재판 성공 기준은 60%.

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

### 7-2. 등급별 해금

| 등급 | 데미지 배율 | 패시브 |
|------|------------|--------|
| 4급 | ×1.0 | 없음 |
| 3급 | ×1.15 | 없음 |
| 2급 | ×1.30 | 없음 |
| 1급 | ×1.50 | 없음 |
| 준특급 | ×1.65 | 없음 |
| 특급 | ×1.80 | 캐릭터 고유 패시브 1개 |

PvP/PvE 등급 배율은 분리한다. 기본값: `gradePvpScaling=true`.

---

## §8. 영역 전개

### 8-1. DomainInstance

```java
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DomainInstance {
    public final UUID ownerId;
    public final String domainId;
    public final boolean isOpen;
    public final int maxRadius;
    public int currentRadius;
    public int wallHp;
    public final long deployedAtTick;
    public final Set<UUID> playersInside = new HashSet<>();
    public boolean sureHitActive;

    public DomainInstance(UUID ownerId, String domainId, boolean isOpen, int maxRadius, long deployedAtTick) {
        this.ownerId = ownerId;
        this.domainId = domainId;
        this.isOpen = isOpen;
        this.maxRadius = maxRadius;
        this.currentRadius = maxRadius;
        this.wallHp = isOpen ? 0 : 1500;
        this.deployedAtTick = deployedAtTick;
        this.sureHitActive = false;
    }

    public void applyTeamLimit(int teamDomainCount) {
        if (teamDomainCount >= 2) {
            this.currentRadius = this.maxRadius / 2;
        }
    }
}
```

### 8-2. 영역 충돌

`DomainPriorityCalculator`는 등급, CE 투입량, 시전 숙련도, 결계 외피, 개방형 여부를 계산한다. 팀별 동시 영역은 2개 제한.

### 8-3. 결계형 영역

결계형 기본 `wallHp=1500`. 내부 확정타는 `sureHitActive=true`일 때만 적용한다.

### 8-4. 개방형(OPEN) 영역 특수 처리

`DomainManager.deployDomain()` 내 분기 처리:

1. `wallHp = isOpen ? 0 : 1500`(개방형은 결계 없음)
2. 개방형 시전자는 다른 결계형 영역의 `wallHp`에 데미지 가능
3. 반경 최대 200블록(`domains.json` `openMaxRadius: 200`), 자박 시 radius = openMaxRadius, CE 소모 2배
4. 영역 충돌 priority 계산: `isOpen=true`이면 결계외피 가중치(0.15) = 0
5. `DomainBoundaryRenderer`: `isOpen=true`이면 particle dome 미생성

`domains.json` 추가 필드:

    "isOpen": false
    "openMaxRadius": 200

---

## §9. 반전술식과 상태 이상

### 9-1. 반전술식(RCT)

`healingActive=true`인 동안 CE를 초당 소모하고 HP를 회복한다. `DamageContext.bypassRCT=true`인 영혼 데미지는 RCT로 즉시 복구할 수 없다.

### 9-2. 영혼 데미지

`isSoulDirect=true`면 방어를 0으로 처리하고 별도 soul damage 로그를 남긴다.

### 9-3. 상태 이상

정지, 수면, 둔화, 술식 봉인은 서버 tick에서 만료 시간을 검사한다.

---

## §10. 커맨드와 운영

### 10-1. 운영자 명령

| 명령 | 목적 |
|------|------|
| `/jj reload` | config와 technique reload |
| `/jj selectchar <player> <characterId>` | 운영자 캐릭터 지정 |
| `/jj data save <player>` | 즉시 저장 |
| `/jj domain clear` | 영역 강제 종료 |

### 10-2. 감사 로그

사망, 손가락 획득, 처형검, 캐릭터 변경, 운영자 명령은 `audit_log`에 기록한다.

---

## §11. 클라이언트 패킷과 애니메이션

### 11-1. 패킷 등록 원칙

`CustomPayload` ID는 `jjk:<snake_case>` 형식. S2C는 표시 전용이며 서버 상태를 변경하지 않는다.

### 11-2. AnimationRegistry

| animId | 애니메이션 | animId | 애니메이션 |
|--------|-----------|--------|-----------|
| 0 | 공통 idle | 20 | 고죠 V |
| 1 | 공통 cast | 21 | 스쿠나 F |
| 2 | 공통 hit | 22 | 스쿠나 Shift+F |
| 3 | 공통 dash | 23 | 스쿠나 R |
| 4 | 공통 guard | 24 | 스쿠나 Shift+R |
| 5 | 공통 death | 25 | 메구미 F |
| 6 | 공통 respawn | 26 | 메구미 Shift+F |
| 7 | 공통 domain_start | 27 | 메구미 R |
| 8 | 공통 domain_loop | 28 | 메구미 Shift+R |
| 9 | 공통 black_flash | 29 | 메구미 V |
| 10 | 이타도리 F | 30 | 죠고 F |
| 11 | 이타도리 Shift+F | 31 | 죠고 R |
| 12 | 이타도리 R | 32 | 죠고 V |
| 13 | 이타도리 Shift+R | 33 | 마히토 F |
| 14 | 이타도리 V | 34 | 마히토 R |
| 15 | 고죠 F | 35 | 마히토 domain |
| 16 | 고죠 Shift+F | 36 | 하카리 F |
| 17 | 고죠 R | 37 | 하카리 V |
| 18 | 고죠 Shift+R | 38 | 히구루마 F |
| 19 | 고죠 domain | 39 | 히구루마 V |
| 40 | 옷코츠 Shift+F | 50 | 이누마키 F (!멈춰) |
| 41 | 옷코츠 Shift+R (주력해방) | 51 | 이누마키 Shift+F (!터져) |
| 42 | 옷코츠 V | 52 | 이누마키 Shift+R (!잠들어) |
| 43 | 마히토 Shift+F (다중혼) | 53 | 이누마키 V (!달려) |
| 44 | 마히토 Shift+R (칼날) | 54 | 나나미 Shift+R (경계선) |
| 45 | 마히토 V | 55 | 히구루마 Shift+R (배심원) |
| 46 | 죠고 Shift+F (개관) | 56 | 흑섬 Great |
| 47 | 죠고 Shift+R (불꽃의 고리) | 57 | 각성 진입 |
| 48 | 하카리 Shift+F (주력방출) | 58 | 존 진입 |
| 49 | 하카리 Shift+R (불확정) | 59 | 이타도리 영역 startup |

```java
import java.util.HashMap;
import java.util.Map;

public final class AnimationRegistry {
    private static final Map<Integer, String> ANIMATIONS = new HashMap<>();

    static {
        register(0, "common_idle");
        register(1, "common_cast");
        register(2, "common_hit");
        register(3, "common_dash");
        register(4, "common_guard");
        register(5, "common_death");
        register(6, "common_respawn");
        register(7, "common_domain_start");
        register(8, "common_domain_loop");
        register(9, "common_black_flash");
        register(10, "itadori_f");
        register(11, "itadori_shift_f");
        register(12, "itadori_r");
        register(13, "itadori_shift_r");
        register(14, "itadori_v");
        register(15, "gojo_f");
        register(16, "gojo_shift_f");
        register(17, "gojo_r");
        register(18, "gojo_shift_r");
        register(19, "gojo_domain");
        register(20, "gojo_v");
        register(21, "sukuna_f");
        register(22, "sukuna_shift_f");
        register(23, "sukuna_r");
        register(24, "sukuna_shift_r");
        register(25, "megumi_f");
        register(26, "megumi_shift_f");
        register(27, "megumi_r");
        register(28, "megumi_shift_r");
        register(29, "megumi_v");
        register(30, "jogo_f");
        register(31, "jogo_r");
        register(32, "jogo_v");
        register(33, "mahito_f");
        register(34, "mahito_r");
        register(35, "mahito_domain");
        register(36, "hakari_f");
        register(37, "hakari_v");
        register(38, "higuruma_f");
        register(39, "higuruma_v");
        register(40, "okkotsu_shift_f");
        register(41, "okkotsu_shift_r_burst");
        register(42, "okkotsu_v");
        register(43, "mahito_shift_f");
        register(44, "mahito_shift_r");
        register(45, "mahito_v");
        register(46, "jogo_shift_f");
        register(47, "jogo_shift_r");
        register(48, "hakari_shift_f");
        register(49, "hakari_shift_r");
        register(50, "inumaki_f_stop");
        register(51, "inumaki_shift_f_burst");
        register(52, "inumaki_shift_r_sleep");
        register(53, "inumaki_v_run");
        register(54, "nanami_shift_r");
        register(55, "higuruma_shift_r_jury");
        register(56, "black_flash_great");
        register(57, "awakening_enter");
        register(58, "zone_enter");
        register(59, "itadori_domain_startup");
    }

    private AnimationRegistry() {
    }

    public static void register(int id, String animationName) {
        ANIMATIONS.put(id, animationName);
    }

    public static String get(int id) {
        return ANIMATIONS.get(id);
    }
}
```

### 11-3. PlayerAnimator 2.0.4 API

```java
public final class PlayerAnimationDispatcher {
    private PlayerAnimationDispatcher() {
    }

    public static void play(ServerPlayerEntity player, String animName, AnimationPayload payload) {
        var layerContainer = PlayerAnimationAccess.getPlayerAssociatedData(player);
        if (layerContainer == null) {
            return;
        }
        layerContainer.activate(animName, payload.phase());
    }
}
```

GeckoLib 4.8.3에서는 `thenPlay(animName)` 대신 `then(animName, Animation.LoopType.PLAY_ONCE)`를 사용한다.

### 11-4. S2C 패킷 표

| 패킷 | 필드 | 용도 |
|------|------|------|
| `SkillResultS2CPacket` | playerUuid, skillId, result | 스킬 연출 |
| `ZoneEnterS2CPacket` | playerUuid, durationTicks | Zone 진입 |
| `ZoneExitS2CPacket` | playerUuid | Zone 종료 |
| `AwakeningS2CPacket` | playerUuid, active | 각성 표시 |

### 11-5. Zone/Awakening 패킷 정의

```java
import java.util.UUID;

public record ZoneEnterS2CPacket(UUID playerUuid, int durationTicks) implements CustomPayload {
    public static final Id<ZoneEnterS2CPacket> ID = new Id<>(Identifier.of("jjk", "zone_enter"));
    public static final PacketCodec<RegistryByteBuf, ZoneEnterS2CPacket> CODEC = PacketCodec.of(
        (buf, pkt) -> {
            PacketCodecs.STRING.encode(buf, pkt.playerUuid().toString());
            buf.writeInt(pkt.durationTicks());
        },
        buf -> new ZoneEnterS2CPacket(UUID.fromString(PacketCodecs.STRING.decode(buf)), buf.readInt())
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
```

`ZoneExitS2CPacket`: `playerUuid` 단일 필드(`PacketCodec.tuple` 사용).  
`AwakeningS2CPacket`: `playerUuid + boolean active`(`PacketCodec.tuple` 사용).

---

## §12. 설정 파일

### 12-1. config.json 핵심값

    "mangaExpEnabled": false
    "allowDuplicateCharacter": false
    "gradePvpScaling": true
    "jackpotDurationTicks": 251
    "respawnDelayTicks": 100
    "respawnCePercent": 0.50
    "respawnHpPercent": 0.50
    "domainBannedChunks": ["lobby:0,0", "training:0,0"]

### 12-2. techniques.json

`character`, `keyId`, `baseDamage`, `ceCost`, `cooldownTicks`, `animId`는 필수.

### 12-3. domains.json

`domainId`, `ownerCharacter`, `radius`, `ceCost`, `cooldownTicks`, `isOpen`, `openMaxRadius`를 명시한다.

---

## §13. 데이터 저장

### 13-1. PlayerData — 34개 필드

```java
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerData {
    public UUID uuid;
    public String characterId;
    public String grade;
    public long xp;
    public int mastery;
    public float ceCurrent;
    public float ceMax;
    public float hpCurrent;
    public float hpMax;
    public int attackStat;
    public int defenseStat;
    public int speedStat;
    public int fingerCount;
    public List<String> unlockedSkills = new ArrayList<>();
    public Map<String, Long> cooldowns = new HashMap<>();
    public long bindingVowDeclaredTick;
    public long domainCooldownUntil;
    public long jackpotCooldownUntil;
    public long curtainCooldownUntil;
    public String trialState;
    public int burden;
    public boolean zoneActive;
    public long zoneEndTick;
    public long lastCombatTick;
    public String lastKnownIp;

    public boolean awakeningActive;
    public long awakeningEndTick;
    public long awakeningCooldownUntil;
    public boolean burstActive;
    public long burstEndTick;
    public List<String> deadShikigamiIds = new ArrayList<>();
    public boolean healingActive;
    public long zonePenaltyUntilTick;

    public int schemaVersion;

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
        copy.trialState = this.trialState;
        copy.burden = this.burden;
        copy.zoneActive = this.zoneActive;
        copy.zoneEndTick = this.zoneEndTick;
        copy.lastCombatTick = this.lastCombatTick;
        copy.lastKnownIp = this.lastKnownIp;
        copy.awakeningActive = this.awakeningActive;
        copy.awakeningEndTick = this.awakeningEndTick;
        copy.awakeningCooldownUntil = this.awakeningCooldownUntil;
        copy.burstActive = this.burstActive;
        copy.burstEndTick = this.burstEndTick;
        copy.deadShikigamiIds = new ArrayList<>(this.deadShikigamiIds);
        copy.healingActive = this.healingActive;
        copy.zonePenaltyUntilTick = this.zonePenaltyUntilTick;
        copy.schemaVersion = this.schemaVersion;
        return copy;
    }
}
```

### 13-2. 저장 타이밍

로그아웃, 사망, 캐릭터 선택, 손가락 획득, 쿨타임 영속 대상 변경 시 즉시 저장한다.

### 13-3. cooldown_persist

`cooldown_type` 허용값:

    'domain'       영역 전개 쿨타임
    'binding_vow'  속박 쿨타임
    'jackpot'      하카리 잭팟 쿨타임
    'awakening'    각성 쿨타임 (2400틱)
    'burst'        옷코츠 주력해방 쿨타임
    'curtain'      장막 쿨타임
    'zone_penalty' 존 종료 페널티 만료 틱
    'skill_seal'   히구루마 술식 봉인 만료

---

## §14. 보안과 검증

### 14-1. 입력 검증

거리, 시야, 쿨타임, CE, 캐릭터 소유 여부는 서버에서 검증한다.

### 14-2. 레이스 컨디션

손가락 드롭, 캐릭터 중복, 처형검 지급, 영역 소유권 변경은 lock 또는 단일 서버 tick 큐로 원자 처리한다.

---

## §15. 식신과 소환수

### 15-1. 식신 생명주기

소환, 명령, 사망, 영구 사망 기록을 서버에서 관리한다. `deadShikigamiIds`에 포함된 식신은 재소환 불가.

### 15-2. 렌더링

GeckoLib 식신 렌더는 client 전용으로 격리한다.

---

## §16. 장막과 금지 구역

### 16-1. 장막

장막은 시야, 이동, 스킬 사용 제한을 적용하는 월드 영역이다.

### 16-2. 영역 금지 청크

`config.json domainBannedChunks`에 로비·훈련장 좌표를 추가한다. 금지 청크에서는 영역 전개 실패 처리.

---

## §17. QA 시나리오

### 17-1. 통합 스모크

캐릭터 선택→스킬 사용→Zone→각성→사망→부활→저장/재접속 플로우를 1회 이상 검증한다.

### 17-2. 성능 스모크

20명 동시 접속, 영역 4개, 식신 20기, 광역 스킬 반복 조건에서 TPS 18 이상 유지.

---

## §18. 테스트 계획

| 테스트 클래스 | 검증 항목 |
|-------------|----------|
| `DamageCalculatorTest` | 배율 clamp, PvP cap max_hp × 0.40, soul direct 방어 무시 |
| `DomainManagerTest` | 결계형 wallHp 1500, 개방형 wallHp 0, 팀당 2개 제한 |
| `PlayerDataSnapshotTest` | 34개 필드 snapshot 복사 |
| `PacketCodecTest` | ZoneEnter/ZoneExit/Awakening codec |
| `AwakeningManagerTest` | HP 30% 임계 발동, 160틱 자동해제, CD 2400틱, 중복 발동 방지 |
| `ComboTrackerTest` | Just Frame 8~12틱 윈도우, 30틱 리셋, 흐름끊기 카운터 |
| `BurdenManagerTest` | 스킬별 부담 증가, 전투외 5/s·전투중 2/s 분기, 100 초과 봉인 |
| `TrialManagerTest` | 기소→선택→판결→처형검 StateMachine 전체 플로우 |

---

## §19. 구현 Phase

### Phase 1 (서버 핵심)

1. Fabric 초기화, config 로더, PlayerData 저장
2. CombatPipeline, CEManager, CooldownManager
3. CharacterRegistry + 기본 4개 SkillSet
4. DomainManager, TickDamageCap, audit_log

### Phase 2 (애니메이션·렌더)

1. AnimationRegistry (animId 0~59) + SkillAnimController (PlayerAnimator 2.0.4 API)
2. GeckoLib 4.8.3 식신 엔티티 렌더 (client 전용)
3. AzureLib 영역 비주얼 오버레이 기초
4. ZoneEnterS2CPacket / AwakeningS2CPacket 클라이언트 처리

### Phase 3 (캐릭터 확장)

1. 나머지 7개 캐릭터 SkillSet
2. TrialManager, BurdenManager, JackpotManager
3. 신규 캐릭터 추가 절차 §27 반영

---

## §20. 릴리스 게이트

### 20-1. Phase 1 게이트

서버 dedicated 기동, 1시간 soak, 저장/재접속, PvP cap, 캐릭터 선택 중복, 영역 충돌 통과.

### 20-2. Phase 2 게이트

클라이언트 없는 서버에서 classloading 오류 없음. 클라이언트에서는 animId 0~59 누락 없음.

---

## §21. 로깅

### 21-1. 서버 로그

전투 로그는 debug flag에서만 상세 출력한다. 운영 이벤트는 항상 audit_log에 저장한다.

### 21-2. 에러 처리

config 누락은 기본값 적용 후 경고. schema mismatch는 migrator 실패 시 서버 기동 중단.

---

## §22. 캐릭터별 운영 주의

### 22-1. 고죠

무하한은 영역 전개, 영역 전연, 마호라가 적응, 특정 영혼 직접 공격에 의해 무력화될 수 있다.

### 22-2. 하카리

잭팟 지속시간은 `config.jackpotDurationTicks`로 조정한다. 기본값 251틱.

### 22-3. 이누마키 부담

부담은 스킬별로 증가한다. 전투 외 5/s, 전투 중 2/s 감소. 100 초과 시 술식 봉인.

---

## §23. 확정 운영값

| 항목 | 확정값 |
|------|--------|
| 히구루마 재판 성공 기준 | 60% |
| PvP 최종 피해 상한 | max_hp × 0.40 |
| MANGA-EXP 기본값 | off (`mangaExpEnabled: false`) |
| 속박 파훼 조건 | 특급 이상 스킬 직격 시 해제 |
| 동일 캐릭터 중복 | 팀당 1명 (`allowDuplicateCharacter: false`) |
| 영역 금지 청크 | `config.json domainBannedChunks`에 로비·훈련장 좌표 추가 |
| 등급 배율 PvP/PvE 분리 | 분리 (`gradePvpScaling: true`) |

config.json 스키마 상단 확정값:

    "mangaExpEnabled": false
    "allowDuplicateCharacter": false
    "gradePvpScaling": true
    "pvpDamageCapMaxHpRatio": 0.40
    "trialSuccessRate": 0.60
    "bindingVowBreakBySpecialGradeHit": true
    "domainBannedChunks": ["lobby:0,0", "training:0,0"]

---

## §24. 참조 무결성

### 24-1. 필수 참조

`§1-6`, `§3-7`, `§8-4`, `§11-5`, `§13-1`, `§26`, `§27`은 구현 중 반드시 실제 섹션으로 유지한다.

### 24-2. [AI-FINAL] 미확정값 처리

코드 작성에 영향을 주는 미확정 항목은 §23 확정 운영값으로 대체했다. 새 미확정값을 추가할 경우 기본값과 config key를 함께 명시한다.

---

## §25. 원작 구현 체크리스트

| 항목 | 상태 |
|------|------|
| 흑섬 base × 2.5 게임화 결정 명시 | 완료 |
| 고죠 무하한 예외 조건 | 완료 |
| 스쿠나 개방형 영역 | 완료 |
| 이타도리 경정권 2타 | 완료 |
| 메구미 식신 영구 사망 | 완료 |
| 옷코츠 주력해방 burstActive | 완료 |
| 마히토 자폐원돈과 양날성 | 완료 |
| 하카리 잭팟 1/239 및 config 지속시간 | 완료 |
| 이누마키 채팅 이벤트명 | 완료 |
| 히구루마 재판 성공 기준 60% | 완료 |

---

## §26. 플레이어 입장 플로우 & 게임 루프

### 26-1. 최초 접속 플로우

이벤트: `ServerPlayConnectionEvents.JOIN`

처리 순서:
1. `repository.load(uuid)` 시도
2. null이면 최초 접속:
   - `PlayerData.createDefault(uuid)` 생성
   - `characterId = null`(미선택 상태)
   - `repository.saveImmediate(data)`
   - `CharacterSelectS2CPacket` 전송 → 클라이언트 캐릭터 선택 화면
3. null이 아니면 재접속:
   - 기존 데이터 로드
   - `CharacterInfoS2CPacket` 전송

### 26-2. 캐릭터 선택 시스템

C2S: `CharacterSelectC2SPacket(String characterId)`

서버 수신 처리:
1. `CharacterRegistry`에 등록된 유효한 `characterId`인지 검증
2. `config.allowDuplicateCharacter=false`이면 동일 캐릭터 중복 체크
3. `data.characterId = selectedCharacterId`
4. 기존 플레이어: 등급·숙련도 유지. 신규: `grade="4급"`
5. `unlockedSkills` 초기화(등급 기반 §7-1)
6. `saveImmediate(data)`
7. `CharacterConfirmS2CPacket` → 클라이언트 인게임 전환

캐릭터 재선택: `/jj selectchar` [운영자 결정 — config 옵션으로 허용 여부 제어]

### 26-3. 부활 시스템

config.json 추가 필드:

    "respawnDelayTicks": 100
    "respawnLocation": "SPAWN"
    "respawnCePercent": 0.50
    "respawnHpPercent": 0.50

사망 시 처리(`ServerPlayerEntityMixin.onDeath`):
1. `saveImmediate(data)`
2. `audit_log` 기록
3. 스쿠나 플레이어 → 손가락 드롭 판정(§26-4)
4. `RespawnManager.scheduleRespawn(player, config.respawnDelayTicks)`

부활 처리:
- 부활 지점 텔레포트
- CE = `ceMax × respawnCePercent`
- 각성·존·속박 상태 초기화
- `RespawnS2CPacket` → 클라이언트 부활 연출

### 26-4. 손가락 드롭 경쟁 처리 (원자적)

특급 주령 처치 시 레이스컨디션 방지:

```java
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class FingerSystem {
    private final Object fingerLock = new Object();
    private final Set<UUID> droppedMobs = Collections.synchronizedSet(new HashSet<>());
    private final PlayerRepository repository;
    private final JjkConfig config;

    public FingerSystem(PlayerRepository repository, JjkConfig config) {
        this.repository = repository;
        this.config = config;
    }

    public void tryDrop(UUID mobId, UUID killerUuid) {
        synchronized (fingerLock) {
            if (droppedMobs.contains(mobId)) {
                return;
            }
            if (Math.random() > config.fingerDropRate()) {
                return;
            }
            droppedMobs.add(mobId);
            PlayerData data = repository.load(killerUuid);
            data.fingerCount = Math.min(data.fingerCount + 1, 20);
            repository.saveImmediate(data);
            if (data.fingerCount == 20) {
                triggerFullRevival(killerUuid);
            }
        }
    }

    private void triggerFullRevival(UUID playerUuid) {
        FullRevivalEvents.trigger(playerUuid);
    }
}
```

### 26-5. 진영 분류 (캐릭터 선택 시 자동 배정)

    JUJUTSU_SORCERER: 고죠·이타도리·메구미·옷코츠·나나미·히구루마·하카리·이누마키
    CURSED_SPIRIT:    스쿠나·마히토·죠고 (운영자 설정으로 선택 가능)
    NON_SORCERER:     비술사 타입

`TeamManager`:
- 같은 진영 공격 불가
- 영역 진영별 2개 제한 체크
- 등급 차이 2 이상 전투 시 XP × `config.xpMultiplierGradeDiff` 적용

---

## §27. 신규 캐릭터 추가 절차 (확장성 가이드)

12번째 이후 캐릭터 추가 시 아래 순서로 실행:

1. `techniques.json`에 스킬 항목 추가(`character`·`keyId`·`baseDamage`·`ceCost` 필수)
2. `domains.json`에 영역 파라미터 추가(영역 있는 캐릭터만)
3. `AnimationRegistry.java`에 animId 등록(60번부터 순서대로)
4. `impl/` 패키지에 `{CharName}SkillSet.java` 생성 — `ISkillSet` 5개 메서드 전부 구현
5. `SkillRegistry.register("{charId}", new {CharName}SkillSet())` 등록
6. `PlayerData`에 캐릭터 전용 필드 필요 시 추가 + `snapshot()` 반영
7. `schema_version` 증가 + `MigratorV{N}toV{N+1}` 작성
8. `ISkillSetTest`에 신규 캐릭터 케이스 추가
9. §25 원작 구현 체크리스트에 신규 항목 추가
10. Phase 1 릴리스 게이트 재통과 확인

keyId 범위: byte 0~127(현재 0~10 사용, 여유 충분)  
animId 범위: byte 0~127(현재 0~59 사용, 60~127 여유)



Claude Cowork — JJK 서버 최종 확장 제안서: 이펙트, 기술, 개발/시스템
본 문서는 jjk_spec_v5.md 명세서 및 이전 분석 가이드에서 다루지 않은, JJK 서버의 완성도를 한 단계 더 높이고 플레이어 경험을 극대화하기 위한 이펙트(VFX/SFX/Motion/Presentation), 기술/스킬(Gameplay Mechanics), 개발/시스템(Infrastructure/Tools) 측면의 추가 제안 사항들을 종합적으로 정리한 것입니다. 이는 단순한 모드를 넘어 독자적인 게임으로서의 잠재력을 발휘하기 위한 확장 로드맵의 일환입니다.


이펙트 (VFX/SFX/Motion/Presentation) 확장
기존 연출 가이드에서 다룬 내용을 보완하고, 더욱 몰입감 있는 경험을 제공하기 위한 추가적인 이펙트 및 연출 요소들입니다.
1.1. 환경 상호작용 이펙트
스킬 사용 시 주변 환경과의 동적인 상호작용을 통해 현실감과 파괴감을 증대시킵니다.
•
블록 파괴/변형: 강력한 스킬(예: 허식 자, 영역 전개) 사용 시 주변 블록이 일시적으로 파괴되거나, 금이 가거나, 색상이 변하는 셰이더 효과를 적용합니다. 실제 블록 파괴는 서버 부하를 유발하므로 클라이언트 측에서만 렌더링되는 가상 파괴 효과를 구현합니다.
•
유체(Fluid) 반응: 물, 용암 등의 유체 위에서 스킬 사용 시 물보라, 파동, 증기 등의 파티클 이펙트를 발생시킵니다. 특히 물 속에서 주술을 사용할 경우, 물이 주력에 반응하여 끓어오르거나 얼어붙는 연출을 추가합니다.
•
식물/지형 반응: 풀, 나뭇잎, 꽃 등이 스킬의 바람이나 충격파에 흔들리거나 흩날리는 동적인 반응을 추가합니다. 지면에 스킬이 명중할 경우, 작은 흙먼지나 돌멩이가 튀어 오르는 파티클을 생성합니다.
1.2. 피격 이펙트 다양화
피격 종류와 강도에 따라 차별화된 시각 및 청각 피드백을 제공하여 전투의 전략성을 높입니다.
•
일반 피격: 붉은색 혈흔 파티클과 함께 둔탁한 타격음.
•
치명타(Critical Hit): 일반 피격보다 크고 밝은 붉은색/노란색 스파크 파티클, 화면이 순간적으로 강하게 흔들리며 날카로운 파열음.
•
영혼 데미지: 대상의 모델링이 순간적으로 일그러지거나 투명해지는 셰이더 효과, 보라색/검은색 영혼 파티클이 흩어지며 기괴한 비명 소리.
•
가드/회피 성공: 공격이 튕겨나가는 듯한 시각 이펙트(스파크, 방어막 파티클)와 함께 금속성 튕겨나가는 소리 또는 바람 가르는 소리.
1.3. UI/HUD 연출 강화
플레이어의 상태와 스킬 정보를 더욱 직관적이고 동적으로 전달합니다.
•
CE 게이지: CE 소모 및 회복 시 게이지 바가 유동적으로 움직이며, 특정 임계치(예: CE 부족, CE 최대치) 도달 시 게이지 주변에 빛나는 테두리나 깜빡이는 효과를 추가합니다.
•
쿨타임: 스킬 아이콘 위에 쿨타임 진행 상황을 시각적으로 표시(원형 채우기, 숫자 카운트다운)하고, 쿨타임 완료 시 빛나는 효과와 함께 짧은 알림음.
•
상태 이상: 플레이어 화면 가장자리에 상태 이상 아이콘과 함께 해당 상태 이상을 나타내는 미니멀한 셰이더 효과(예: 둔화 시 화면 가장자리 흐림, 술식 봉인 시 화면에 주술 문양 오버레이).
•
데미지 폰트: 피격 시 데미지 숫자가 팝업될 때, 일반 데미지, 치명타, 영혼 데미지, 회복량 등에 따라 폰트 스타일, 색상, 크기, 애니메이션(흔들림, 솟아오름)을 차별화합니다.
1.4. 날씨/시간 연동 이펙트
주술적인 분위기를 강조하고 게임 세계에 생동감을 부여합니다.
•
밤/낮 변화: 밤에는 주령의 활동이 활발해지며, 특정 지역에 어두운 안개나 음산한 파티클이 발생하도록 연출합니다. 낮에는 주술사들의 활동이 강조되는 밝고 활기찬 분위기를 유지합니다.
•
특정 날씨: 비, 눈, 안개 등의 날씨 효과를 주술적인 요소와 결합합니다. 예를 들어, 비 오는 날에는 주력이 더욱 강해지는 듯한 시각 효과(빗방울에 주력 파티클 혼합)를 추가할 수 있습니다.
1.5. 잔상 및 가속 이펙트
빠른 움직임과 스킬 사용 시 속도감을 극대화합니다.
•
대시/순간이동: 캐릭터가 빠르게 이동하거나 순간이동할 때, 이동 경로에 잔상(Ghosting) 효과를 남기거나, 주변 공간이 순간적으로 왜곡되는 셰이더 효과를 적용합니다.
•
공격 속도 증가: 공격 속도가 증가하는 버프를 받았을 때, 공격 모션에 미세한 모션 블러(Motion Blur)를 추가하여 속도감을 강조합니다.


기술/스킬 (Gameplay Mechanics) 확장
기존 캐릭터 스킬 외에 주술회전 세계관의 깊이를 더하고 플레이어에게 더 다양한 전략적 선택지를 제공하는 시스템들입니다.
2.1. 주구 (Cursed Tools) 시스템
주술회전 세계관의 핵심 요소인 주구를 아이템화하고, 스킬과 연동하여 전투에 활용할 수 있도록 합니다.
•
획득 및 강화: 필드 드롭, 퀘스트 보상, 제작 등을 통해 주구를 획득하고, 강화 재료를 사용하여 성능을 향상시킵니다.
•
고유 능력: 각 주구는 고유한 능력(예: 주력 소모 감소, 특정 스킬 데미지 증가, 상태 이상 부여)을 가지며, 일부 주구는 특정 캐릭터만 사용할 수 있도록 제한합니다.
•
장착 슬롯: 플레이어 인벤토리에 주구 장착 슬롯을 추가하고, 장착된 주구의 효과가 자동으로 적용되도록 합니다.
2.2. 주언 (Cursed Speech) 시스템
이누마키 토게의 주언처럼 음성 입력 또는 텍스트 기반의 특수 스킬 시스템을 도입합니다.
•
텍스트 입력: 채팅창에 특정 명령어를 입력하거나, 전용 UI를 통해 주언을 선택하여 발동합니다. 주언의 강도에 따라 플레이어에게 부담(Burden)이 가중됩니다.
•
효과:
대상의 움직임 봉쇄, 데미지 부여, 강제 이동 등 다양한 효과를 가집니다. 강력한 주언일수록 부담이 커지고, 실패 시 역효과(자신에게 데미지, 침묵 상태)가 발생할 수 있습니다.
2.3. 주력 조작 (Cursed Energy Manipulation)
주력을 직접 조작하여 방어, 이동, 보조적인 공격 등에 활용하는 범용적인 시스템입니다.
•
주력 강화 (CE Reinforcement): 일정량의 CE를 소모하여 일시적으로 방어력이나 공격력을 증가시킵니다. (예: 주력을 몸에 둘러 방어력을 높이거나, 주먹에 집중하여 타격 데미지 증가).
•
주력 방출 (CE Blast): 소량의 CE를 빠르게 방출하여 주변의 적을 밀쳐내거나, 투사체를 상쇄하는 등의 보조적인 효과를 줍니다.
•
주력 감지 (CE Sensing): 주변의 주력(적 엔티티, 주구 등)을 감지하여 미니맵에 표시하거나, 시각적으로 강조하는 능력.
2.4. 협동/경쟁 콘텐츠
플레이어 간의 상호작용을 유도하고 게임의 수명을 연장하기 위한 콘텐츠입니다.
•
주령 토벌전 (Cursed Spirit Extermination): 특정 시간/장소에 강력한 특급 주령이 출현하여 다수의 플레이어가 협동하여 토벌하는 레이드 콘텐츠. 토벌 성공 시 희귀 주구나 재료 드롭.
•
주술 고전 대항전 (Jujutsu High Tournament): 플레이어 간의 PvP 대결을 위한 랭킹 시스템 및 토너먼트 콘텐츠. 승리 시 명예 점수, 특별 칭호, 보상 지급.
•
영역 배틀 (Domain Battle): 다수의 플레이어가 동시에 영역을 전개하여 서로의 영역을 파괴하거나 점령하는 대규모 PvP 콘텐츠. 영역의 상성, 전략적 위치 선점 등이 중요.


개발/시스템 (Infrastructure/Tools) 확장
서버의 안정성, 확장성, 운영 편의성을 높이기 위한 기술적 및 시스템적 제안입니다.
3.1. 모듈형 스킬 시스템 (Modular Skill System)
신규 캐릭터 및 스킬 추가 시 코드 수정 없이 데이터만으로 스킬을 정의하고 확장할 수 있는 시스템입니다.
•
데이터 기반 스킬 정의: techniques.json 및 domains.json을 확장하여 스킬의 모든 속성(데미지 타입, 이펙트 ID, 타겟팅 방식, 판정 범위, 추가 효과 등)을 데이터로 정의합니다. 이를 통해 개발자는 JSON 파일만 수정하여 새로운 스킬을 추가할 수 있습니다.
•
컴포넌트 기반 스킬 효과: 스킬 효과를 작은 컴포넌트(예: DamageComponent, ApplyStatusEffectComponent, SpawnProjectileComponent)로 분리하고, 이 컴포넌트들을 조합하여 복잡한 스킬을 구성합니다. (예: GojoBlueSkill = AttractionComponent + DamageComponent + VFXComponent).
3.2. 실시간 모니터링 및 디버깅 툴
서버 운영 및 개발 효율성을 극대화하기 위한 인게임 툴입니다.
•
인게임 디버그 UI: 운영자 전용 커맨드(예: /jj debug)를 통해 플레이어의 PlayerData 실시간 조회 및 수정, 스킬 쿨타임 강제 초기화, CE/HP 조작 등의 기능을 제공하는 GUI 툴.
•
성능 프로파일러: 특정 스킬이나 영역이 서버 TPS에 미치는 영향을 실시간으로 측정하고 시각화하는 툴. 병목 현상(Bottleneck)을 빠르게 식별하고 최적화할 수 있도록 돕습니다.
•
이펙트 뷰어: 모든 VFX/SFX/Animation을 인게임에서 미리 보고 테스트할 수 있는 개발자 전용 툴. 이펙트의 타이밍, 지속 시간, 파티클 속성 등을 즉시 조정하며 개발할 수 있습니다.
3.4. 보안 강화 및 안티-치트 시스템
공정한 게임 환경을 유지하기 위한 지속적인 노력입니다.
•
패킷 암호화/난독화: 클라이언트-서버 간 통신 패킷을 암호화하거나 난독화하여 스니핑(Sniffing) 및 변조 시도를 어렵게 합니다.
•
행동 패턴 분석: 비정상적인 플레이어 행동(예: 비정상적인 이동 속도, 스킬 쿨타임 무시)을 감지하고 자동으로 제재하는 시스템을 도입합니다.
•
클라이언트 무결성 검사: 클라이언트 파일 변조 여부를 주기적으로 검사하여 불법적인 모드 사용을 방지합니다.


결론
이 제안서에 담긴 이펙트, 기술, 개발/시스템 확장 요소들은 JJK 서버를 단순한 마인크래프트 모드를 넘어, 주술회전 세계관을 충실히 구현하고 플레이어에게 깊이 있는 경험을 제공하는 독자적인 콘텐츠로 발전시킬 잠재력을 가지고 있습니다.
이러한 확장들은 개발 초기 단계부터 고려되어야 하며, 각 요소의 구현 난이도와 서버 리소스 소모량을 면밀히 검토하여 점진적으로 적용하는 것이 중요합니다. 특히, 모듈형 스킬 시스템과 실시간 디버깅 툴은 장기적인 개발 효율성과 유지보수성에 큰 기여를 할 것입니다.




Claude Cowork — JJK 서버 연출(Presentation) 및 이펙트 개선 가이드

본 문서는 jjk_spec_v5.md 명세서를 바탕으로, 마인크래프트 환경에서 '주술회전' 특유의 압도적인 타격감과 분위기를 극대화하기 위한 이펙트(VFX), 사운드(SFX), 모션(Animation), 카메라 연출(Camera Work) 관점의 개선 및 추가 사항을 정리한 가이드입니다.




1. 공통 전투 시스템 연출 (Core Combat Presentation)

1.1. 흑섬 (Black Flash) 연출 극대화

현재 명세서에는 animId 9 (common_black_flash)와 animId 56 (black_flash_great)만 정의되어 있습니다. 흑섬은 주술회전의 꽃이므로 타격감을 극대화해야 합니다.

•
VFX (시각 효과):

•
타격 순간 화면 전체에 짧은 흑백 반전(Invert) 셰이더 효과 적용 (0.1초).

•
타격 지점에서 검은색과 붉은색이 섞인 번개 형태의 파티클(Particle)이 방사형으로 퍼져나가도록 구현.



•
SFX (음향 효과):

•
일반 타격음이 아닌, 공간이 왜곡되는 듯한 묵직한 저음의 폭발음(Bass Drop) 추가.



•
Camera (카메라 연출):

•
타격 순간 강한 화면 흔들림(Camera Shake) 적용.

•
타격 대상에게 미세한 넉백(Knockback)과 함께 0.1초간 피격 모션 정지(Hit Stop)를 주어 타격의 무게감을 강조.



1.2. Zone 및 각성 (Awakening) 진입 연출

Zone과 각성은 플레이어의 상태가 극적으로 변하는 시점이므로, 주변 플레이어들도 이를 명확히 인지할 수 있어야 합니다.

•
Zone 진입 (animId 58):

•
VFX: 플레이어의 눈 위치에서 푸른색 안광 파티클이 궤적을 그리며 남도록 연출 (Trail Renderer 활용).

•
SFX: 심장 박동 소리(Heartbeat)가 점차 빨라지며 페이드인.



•
각성 진입 (animId 57):

•
VFX: 플레이어 발밑에서 강한 기류(Aura)가 위로 솟구치는 파티클 이펙트. 주변 3블록 내의 흙이나 돌 파티클이 공중으로 떠오르는 연출(Anti-gravity effect).

•
SFX: 날카로운 이명 소리와 함께 폭발적인 기합 소리.



1.3. Just Frame (저스트 프레임) 피드백

콤보 시스템에서 Just Frame(8~12틱) 성공 여부를 플레이어가 직관적으로 알 수 있어야 합니다.

•
UI/VFX: Just Frame 타이밍에 맞춰 크로스헤어(Crosshair) 주변에 순간적으로 빛나는 UI 이펙트 또는 플레이어 손끝에 작은 스파크 파티클 발생.

•
SFX: 성공 시 경쾌하고 날카로운 타격음(예: 유리 깨지는 소리 톤업) 출력.




2. 캐릭터별 스킬 연출 상세 가이드

2.1. 고죠 사토루 (Gojo Satoru)

압도적인 강함과 공간을 다루는 능력을 시각적으로 표현해야 합니다.

•
창 (Blue) - F:

•
VFX: 목표 지점에 공간이 일그러지는 구체(블랙홀 형태) 렌더링. 주변 엔티티가 끌려갈 때 잔상(Motion Blur) 효과 적용.

•
SFX: 진공 청소기처럼 주변 공기를 빨아들이는 고주파 음향.



•
혁 (Red) - Shift+F:

•
VFX: 손끝에서 응축된 붉은 구체가 생성된 후, 폭발 시 붉은색 충격파(Shockwave) 링이 퍼져나감.

•
SFX: 응축될 때의 고음 핑(Ping) 소리 후, 묵직한 폭발음.



•
허식 자 (Purple) - R:

•
VFX: 창(푸른색)과 혁(붉은색) 파티클이 융합되어 거대한 보라색 구체 생성. 발사 궤적을 따라 지면 블록이 파괴되는 듯한 파티클(실제 파괴는 아님) 연출.

•
Camera: 발사 순간 시야각(FOV)이 순간적으로 넓어졌다가 좁아지는 왜곡 효과.



•
무하한 토글 - V:

•
VFX: 공격을 막아낼 때마다 플레이어 주변에 육각형 패턴의 반투명한 방어막(Shield)이 순간적으로 나타났다 사라짐.



2.2. 이타도리 유지 (Itadori Yuji)

육탄전의 묵직함과 타격감을 살리는 데 집중합니다.

•
경정권 - F:

•
VFX: 1타 타격 시 일반 타격 파티클, 5틱 후 2타(지연 타격) 발생 시 타격 부위에서 푸른색 주력 파티클이 폭발하듯 터짐.

•
SFX: 1타(퍽) -> 2타(쾅!) 하는 이중 타격음의 리듬감 강조.



•
맨지 킥 - Shift+F:

•
Animation: 돌진 시 몸을 낮추고 회전하는 역동적인 모션.

•
VFX: 발끝을 따라 반원 형태의 궤적(Trail) 파티클 생성.



2.3. 후시구로 메구미 (Fushiguro Megumi)

그림자를 활용한 연출과 식신의 생동감이 중요합니다.

•
식신 소환 (누에, 옥견) - F, Shift+F:

•
VFX: 소환 시 바닥에 검은 그림자가 넓어지며, 그 안에서 식신이 솟아오르는 연출 (GeckoLib 애니메이션과 연계).

•
SFX: 액체가 끓어오르는 듯한 그림자 소환음.



•
그림자 이동 - V:

•
VFX: 플레이어가 바닥의 그림자 속으로 가라앉듯 사라지고(Scale Y 감소 애니메이션), 목표 지점의 그림자에서 솟아오름.



2.4. 옷코츠 유타 (Okkotsu Yuta)

리카의 압도적인 존재감과 주력의 방대함을 표현합니다.

•
리카 소환 - F:

•
VFX: 등 뒤에서 거대한 검은색/보라색 오라가 뿜어져 나오며 리카 엔티티 등장.

•
SFX: 기괴하고 압도적인 리카의 포효 소리.



•
주력해방 - Shift+R:

•
VFX: 플레이어 전신을 감싸는 핑크빛 주력 오라(Aura)가 일렁임. 이동 시 잔상 효과.



2.5. 마히토 (Mahito)

기괴하고 불쾌한 영혼의 변형을 시각화합니다.

•
무위전변 - F:

•
VFX: 타격 시 대상의 모델링이 순간적으로 일그러지거나 부풀어 오르는 듯한 셰이더 효과 (또는 기괴한 형태의 파티클 부착).

•
SFX: 뼈와 살이 비틀리는 듯한 불쾌한 효과음.



•
다중혼 - Shift+F:

•
VFX: 발사되는 투사체가 울부짖는 얼굴 형태를 띠도록 렌더링.






3. 영역 전개 (Domain Expansion) 연출

영역 전개는 게임 내 가장 화려하고 압도적인 궁극기이므로, 클라이언트 렌더링(AzureLib 등 활용)을 극한으로 끌어올려야 합니다.

3.1. 영역 전개 공통 시퀀스 (animId 7, 8)

1.
수인(Hand Sign) 결성: 캐릭터가 고유의 수인을 맺는 애니메이션 재생.

2.
암전 (Blackout): 수인 결성 직후, 영역 반경 내의 화면이 0.5초간 완전히 어두워짐.

3.
환경 전환 (Environment Shift): 어둠이 걷히면서 영역 고유의 셰이더/스카이박스(Skybox)가 적용된 환경으로 전환.

4.
SFX: "영역 전개(Domain Expansion)" 보이스 출력(가능할 경우) 및 공간이 닫히는 거대한 굉음.

3.2. 캐릭터별 영역 연출

•
고죠 (무량공처):

•
VFX: 우주 공간이나 심연을 연상케 하는 스카이박스 렌더링. 영역 내 엔티티들의 머리 위에 정보가 쏟아지는 듯한 매트릭스 형태의 파티클 효과.



•
스쿠나 (복마어주자):

•
VFX: 개방형 영역이므로 벽이 없음. 대신 중심에 거대한 기괴한 신전(Shrine) 모델링이 솟아오름. 영역 반경 내 바닥이 피로 물드는 데칼(Decal) 효과. 참격이 날아다니는 궤적 파티클.



•
마히토 (자폐원돈과):

•
VFX: 거대한 손들이 얽혀 있는 형태의 돔(Dome) 렌더링. 바닥은 늪처럼 일렁이는 텍스처 적용.






4. 기타 시스템 연출

4.1. 장막 (Curtain)

•
VFX: 하늘에서 검은색 반투명한 장막이 돔 형태로 천천히 내려오는 연출. 장막 내부에서는 하늘이 검게 변하고 조명(Lighting)이 어두워짐.

•
SFX: "어둠보다 검게, 그보다 더 짙게..." 하는 읊조림과 함께 무거운 결계음.

4.2. 히구루마 재판 (Trial)

•
VFX: 재판 시작 시 대상과 히구루마 주변에 거대한 법정 형태의 환영(Illusion) 렌더링. 판결(유죄/무죄) 시 머리 위에 거대한 텍스트 파티클 또는 UI 이펙트 출력.

•
처형검: 유죄 판결 시 하늘에서 빛나는 처형검이 떨어져 히구루마의 손에 쥐어지는 연출.

4.3. 사망 및 부활 (animId 5, 6)

•
사망: 단순한 쓰러짐이 아니라, 주력이 흩어지며 재가 되어 사라지는 파티클 연출 (특히 주령 진영).

•
부활: 스폰 지점에서 주력이 모여들며 형체를 갖추는 역재생 형태의 파티클 연출.




5. 구현 시 주의사항 (클라이언트 사이드)

•
성능 최적화: 화려한 파티클과 셰이더는 클라이언트 프레임(FPS) 저하의 주원인입니다. 파티클 생성량은 클라이언트 설정(Config)에서 조절할 수 있도록 옵션을 제공해야 합니다.

•
네트워크 동기화: S2C 패킷(SkillResultS2CPacket 등)을 수신한 즉시 클라이언트에서 이펙트를 재생하여 딜레이를 최소화해야 타격감이 살아납니다.

•
AzureLib 및 PlayerAnimator 활용: 복잡한 모델링(영역, 식신)은 AzureLib을, 캐릭터의 역동적인 모션(돌진, 수인)은 PlayerAnimator를 적극 활용하여 바닐라 마인크래프트의 한계를 극복해야 합니다.



파일이 잘렸다. 중간 부분을 읽는다.2개의 파일 조회함2개의 파일 조회함전체 파일을 읽었다. 분석을 시작한다.

JJK 서버 개발 명세서 v5 — 전문가 분석 보고서

1. 파일 구조 파악
파일은 총 1,580줄. §0~§27의 핵심 명세(1~1,147줄)와 그 뒤에 붙은 3개의 별도 문서가 하나의 파일에 병합되어 있다:

본체: §0~§27 (Fabric Mod 개발 명세서)
부록 A: "최종 확장 제안서 — 이펙트, 기술, 개발/시스템" (1,148~1,259줄)
부록 B: "연출 및 이펙트 개선 가이드" (1,263~1,580줄)

이 3개의 문서는 서로 다른 성격(구현 명세 vs. 기획 제안서 vs. 아트 가이드)임에도 구분 없이 한 파일에 합쳐져 있다.

2. 분야별 점수
구조·아키텍처 설계: 74
패키지 구조, Mixin 후킹, 서버 권위 원칙, 패킷 분리는 명확하다. 단, CooldownManager가 패키지 표에 있으나 §0-1 세션 프롬프트에서 언급되는 것 외에 별도 명세가 없고, RespawnManager·FingerSystem·TeamManager 등 일부 클래스는 패키지 표에 미등재.
수치·공식 정확성: 79
흑섬(base×2.5), TickDamageCap(max_hp×0.40), 각성(160틱/2400틱), 부활 CE 50% 등 주요 수치는 근거와 함께 명시되어 있다. 단, 이누마키 Shift+R(!잠들어) 스킬이 §6-9 표에 keyId R이 없어 5개 키를 채우지 못한 것이 발견됨.
데이터 모델 완전성: 72
PlayerData 34개 필드 선언과 snapshot() 복사가 완전히 일치한다. 단, bindingVowDeclaredTick 값이 0일 때 "속박 미선언" vs. "속박 0틱 선언"을 구분하는 sentinel 로직이 없음. schemaVersion 초기값 미명시.
구현 가능성: 71
핵심 클래스(DomainInstance, AnimationRegistry, FingerSystem, ZoneEnterS2CPacket)는 실제 컴파일 가능한 Java 코드로 제공됨. 단, SkillUseC2SPacket·CharacterSelectC2SPacket·PlayerRepository·CombatPipeline 등 20개 이상 핵심 클래스의 실제 시그니처/메서드 명세가 없어 개발자가 직접 설계해야 한다.
내부 일관성: 61
§7-1과 §7-2가 동일한 표를 그대로 중복 기재. §27에서 "12번째 이후 캐릭터"라 했으나 현재 캐릭터는 11개(고죠·이타도리·메구미·옷코츠·마히토·죠고·하카리·이누마키·나나미·히구루마·스쿠나). 충돌 검출 §5에서 상세 기술.
보안 설계: 55
서버 권위, 원자처리(FingerSystem), 입력 검증 체크리스트는 명시되어 있다. 단, 실제 검증 코드 예시가 없고, lastKnownIp 저장 목적·사용처·개인정보 처리 방침이 전혀 없다. Rate limit, 패킷 검증 로직이 "있다"고만 적히고 구현 방법이 없다.
확장성: 77
§27 신규 캐릭터 추가 절차가 10단계로 명확히 문서화됨. keyId 0~127(68개 여유), animId 0~127(68개 여유). 단, PlayerData에 캐릭터 전용 필드가 flat 구조로 쌓이는 설계는 캐릭터 수 증가 시 DB 스키마 변경 부담이 기하급수적으로 커진다.
테스트 커버리지: 58
§18에 8개 테스트 클래스가 명시됨. FingerSystemTest·TeamManagerTest·DomainPriorityCalculatorTest·CombatPipelineIntegrationTest 등 중요 클래스 테스트가 누락. 실제 테스트 코드가 명세에 없고 "검증 항목"만 나열되어 있음.
문서 가독성: 63
§0~§27은 섹션 구조가 명확하고 AI·사람 모두 읽기 편하다. 단, 1,148줄 이후 부록 2개가 마크다운 구조 없이 이어붙어 있고(bullet이 •\n 형식으로 깨짐), 파일 단일 책임 원칙 위반으로 독자 혼란 유발.
미확정 항목 투명성: 80
[MANGA-EXP], [SERVER-ORIGINAL], [운영자 결정] 태그로 미확정·선택 항목이 명시적으로 표시됨. §24-2에 미확정값 처리 원칙도 있다. 단, 캐릭터 재선택 허용 여부가 "운영자 결정"으로만 남아 있고 config key가 없음.
종합 점수: 69.0

3. 위험도 분류
🔴 치명
[C-1] 이누마키 R 키 미구현

문제: §6-9 표에 R 행이 없다. ISkillSet은 5개 키 전부 구현이 필수(§6-1).
위치: §6-9
실제 결과: ISkillSet.onR() 미구현 → 컴파일러 오류 또는 런타임 AbstractMethodError. 세션 프롬프트 3번 "5개 키를 모두 구현"과 직접 충돌.

[C-2] PlayerAnimationDispatcher.play() API 불완전

문제: layerContainer.activate(animName, payload.phase()) 호출에서 AnimationPayload 타입 및 phase() 메서드가 정의되지 않음. PlayerAnimator 2.0.4 실제 API와 다를 가능성 있음.
위치: §11-3
실제 결과: 컴파일 실패 또는 런타임 크래시.

[C-3] ZoneEnterS2CPacket CODEC 인코더/디코더 순서 불일치

문제: 인코더에서 playerUuid → durationTicks 순으로 쓰는데, 디코더도 동일 순서이므로 얼핏 맞아 보이나, PacketCodec.of() 시그니처는 (encoder, decoder) 순이며 PacketCodecs.STRING.encode(buf, ...) 사용이 Fabric 1.21.1 PacketCodec API와 일치하는지 확인 필요. PacketCodecs.STRING은 static 팩토리가 아닌 인스턴스일 수 있음.
위치: §11-5
실제 결과: 컴파일 오류 또는 패킷 역직렬화 실패 → Zone 입장/퇴장 이벤트 전체 불동작.


🟠 높음
[H-1] bindingVowDeclaredTick = 0 구분 불가

문제: long 타입 기본값 0은 "0틱에 선언"과 "미선언"을 구분할 수 없다.
위치: §3-1, §13-1
실제 결과: 서버 재시작 후 모든 플레이어가 0틱에 속박을 선언한 것으로 오해될 수 있음. 속박 15초 타이머 로직 오작동.

[H-2] FingerSystem.tryDrop() 내부 repository.load() 동기 블로킹

문제: synchronized(fingerLock) 블록 안에서 DB I/O(repository.load(), repository.saveImmediate())를 호출한다. DB가 느릴 경우 서버 메인스레드 차단 가능.
위치: §26-4
실제 결과: 서버 TPS 급락 또는 데드락 위험.

[H-3] lastKnownIp 개인정보 문제

문제: PlayerData에 IP가 저장되나 목적, 암호화, 삭제 정책이 전혀 없다. GDPR/개인정보보호법상 민감 데이터.
위치: §13-1
실제 결과: 법적 리스크 및 DB 유출 시 플레이어 IP 노출.

[H-4] awakeningCooldownUntil 설정 순서 위험

문제: checkAndActivate에서 awakeningActive = true 설정 후 awakeningCooldownUntil을 설정하는데, 두 줄 사이에서 tickCheck가 선행 실행되면 cooldown 없이 두 번 발동 가능한 race window가 존재(싱글 틱이지만 동일 틱 내 다중 호출 시).
위치: §3-7
실제 결과: 각성 중복 발동 → 데미지 배율 스택.

[H-5] 나나미 SkillSet 2개 키 미정의

문제: §6-10에서 나나미는 F와 Shift+R만 정의. Shift+F, R, V 3개 키가 없다.
위치: §6-10
실제 결과: ISkillSet 미구현 메서드 → 컴파일 오류 또는 사일런트 실패.


🟡 중간
[M-1] §7-1과 §7-2 완전 중복 표
§7-1(등급별 데미지 배율)과 §7-2(등급별 해금) 표의 내용이 완전히 동일하다. §7-2에 "등급별 해금" 기능(스킬 해금, 장비 해금 등)이 빠져 있다.
[M-2] PlayerData flat 구조의 캐릭터 필드
deadShikigamiIds, burstActive, burstEndTick 등 특정 캐릭터 전용 필드가 PlayerData에 직접 선언됨. 캐릭터 추가 시마다 PlayerData + snapshot() + schema_migration 전체 수정 필요.
[M-3] schemaVersion 초기값 및 마이그레이션 0번 미정의
§13-1에 schemaVersion 필드는 있으나 초기값, 현재 버전 번호, MigratorV0toV1의 존재 여부가 명시되지 않음.
[M-4] domains.json의 sureHitActive 초기화 트리거 미정의
DomainInstance.sureHitActive가 false로 초기화되는데, 언제 true로 전환되는지, 어느 config 필드로 제어하는지 명세 없음.
[M-5] TrialStateMachine 전이 조건 미명세
§6-11과 §18에 TrialManager 존재는 명시되나 기소→선택→판결→처형검의 각 전이 조건(타임아웃, 플레이어 응답 방식)이 없다.
[M-6] CooldownManager 명세 없음
§0-2 세션 프롬프트에서 언급되나 패키지 표에 CooldownManager가 있고 실제 역할·인터페이스가 전혀 정의되지 않음. cooldowns Map<String, Long> 필드(PlayerData)와의 역할 경계 불분명.
[M-7] RespawnManager 클래스 패키지 미지정
§26-3에서 사용되나 §1-1 패키지 표에 없음.
[M-8] 등급 배율 PvP/PvE 분리 방법 미정의
gradePvpScaling=true가 확정값이나 PvE 배율 값이나 분기 로직이 명시되지 않음.

🔵 낮음

FullRevivalEvents.trigger() 구현체 위치 미정의
장막(§16-1) 시야·이동 제한 구체적 수치 없음
캐릭터 재선택 config key 미정의(§26-2에 "운영자 결정"으로만 남음)
§22-1 고죠 무하한 무력화 조건이 InfinityHandler 코드 연동 없이 산문으로만 기술
하카리 잭팟 중 maxCE 제한 없음 처리 코드 누락
audit_log.db 스키마 미정의
config/jjk/config.json 전체 스키마 미제공(핵심값만 나열)


4. 누락 분석
[있어야 하는데 없음]

PlayerRepository 인터페이스/구현체 명세 — DB 연동의 핵심 클래스이나 시그니처·SQLite 스키마·트랜잭션 처리가 전혀 없다. saveImmediate()가 동기인지 비동기인지조차 불명확.
CombatPipeline 실제 메서드 시그니처 — §3-6에 9단계가 나열되어 있으나 실제 Java 메서드 명세가 없어 구현 시 설계를 처음부터 해야 한다.
ISkillSet 인터페이스 코드 — "5개 메서드를 구현한다"고 반복 언급하나 인터페이스 선언 자체가 없다.
config.json 전체 스키마 — 핵심값만 조각조각 나열되어 있어 실제 JSON 파일을 만들 수 없다.
나나미 Shift+F, R, V 스킬 정의 — 캐릭터가 등록되어 있으나 3개 키 정의 없음.
스쿠나 SkillSet 전체 — §6에 스쿠나가 없다. CharacterRegistry와 AnimationRegistry에 등록(animId 21~24)되어 있으나 스킬 정의가 없음.
Migrator 구현 명세 — 존재는 언급되나 실제 마이그레이션 절차·버전 번호·SQL이 없다.
DamageContext 필드 정의 — isSoulDirect, bypassRCT 등이 언급되나 클래스 전체 필드 목록이 없다.

[있는데 내용이 불충분함]

§14 보안과 검증 — "서버에서 검증한다"는 원칙만 있고 실제 검증 코드·실패 시 페널티·반복 위반 시 제재 로직이 없다. 구현 가능 수준이 아님.
§16 장막 — 시야 제한 반경, 이동 제한 조건, CE 소모 여부가 없다.
§8-2 영역 충돌 — DomainPriorityCalculator가 "등급, CE 투입량, 시전 숙련도, 결계 외피, 개방형 여부를 계산한다"고만 되어 있고 실제 공식이 없다.
§9-3 상태 이상 — 종류(정지, 수면, 둔화, 술식 봉인)만 나열되고 각 상태 이상의 tick 관리 필드, 상호작용(수면 중 피격 시 해제 여부 등)이 없다.

[있어서는 안 되거나 제거해야 할 것]

§7-1과 §7-2 완전 중복 표 — 즉시 제거, §7-2에 실제 "해금 내용(스킬 언락 조건 등)"을 채워야 한다.
1,148줄 이후 부록 2개 — 명세서 파일에 기획 제안서와 아트 가이드가 병합되어 있다. Claude Code가 이 부분을 명세로 오인하여 구현 시도할 위험이 있다. 별도 파일로 분리 필수.
[AI-ADD]/[DOCX]/[AI-FIX]/[AI-FINAL] 태그 — 내부 작업용 태그가 확정 명세에 잔류(§4-4의 [AI-FINAL]). 구분을 명확히 하거나 제거해야 한다.


5. 내부 충돌 검출
충돌 [1]: 등급 배율 표 중복
  A 위치: §7-1 → "등급별 데미지 배율" 표
  B 위치: §7-2 → "등급별 해금" 표
  판정: A가 맞음 (§7-2는 내용이 다르게 채워져야 함)
  근거: 두 표가 완전히 동일하며 §7-2는 해금 조건을 명시해야 하는 섹션이다.

충돌 [2]: 이누마키 채팅 이벤트 등록 방식
  A 위치: §6-9 서두 → "ServerMessageEvents.ALLOW_CHAT_MESSAGE와 ALLOW_COMMAND_MESSAGE 동시 등록"
  B 위치: §0-1 세션 프롬프트 (이누마키 관련 별도 세션 없음, §3 캐릭터 프롬프트에 미언급)
  판정: A가 맞음
  근거: 이벤트명이 §6-9에만 명시되어 있어 세션 프롬프트 3번을 사용하는 개발자는 이 정보를 누락할 수 있다.

충돌 [3]: 스쿠나 SkillSet 존재 여부
  A 위치: §1-1 패키지 표 → "character/impl/ 기타 캐릭터 SkillSet"으로 포함 암시
  B 위치: §6 (§6-2~§6-11) → 스쿠나 SkillSet 항목 없음
  판정: 운영자 결정 필요
  근거: AnimationRegistry에 animId 21~24(스쿠나)가 있고 §26-5에 진영 분류에 CURSED_SPIRIT에 스쿠나가 있으나 실제 스킬이 정의되지 않았다. 의도적 생략인지 누락인지 불분명.

충돌 [4]: 각성 발동 위치
  A 위치: §3-7 → "CombatPipeline 5단계 직후 checkAndActivate()"
  B 위치: §1-6 Mixin 명세 → "ServerPlayerEntityMixin tick at=TAIL에서 AwakeningManager.checkAndActivate(player)"
  판정: 운영자 결정 필요
  근거: 피격 시 즉각 각성(CombatPipeline 5단계)과 매 틱 조건 체크(tick at=TAIL) 두 경로가 모두 존재한다. 두 경로가 동시에 있으면 중복 발동 방지 로직이 양쪽 모두에 있어야 한다.

충돌 [5]: 캐릭터 수 "12번째 이후"
  A 위치: §27 → "12번째 이후 캐릭터 추가 시"
  B 위치: §6 실제 캐릭터 목록 → 고죠, 이타도리, 메구미, 옷코츠, 마히토, 죠고, 하카리, 이누마키, 나나미, 히구루마 = 10개. 스쿠나 정의 여부 미확정.
  판정: 운영자 결정 필요
  근거: 현재 정의된 캐릭터가 10개(스쿠나 포함 시 11개)인데 "12번째 이후"는 현재 미정의된 1~2개 캐릭터 추가가 전제된 것으로 보임. 명시적으로 12개 목록을 확정해야 한다.

충돌 [6]: 마히토 영역 wallHp
  A 위치: §8-3 → "결계형 기본 wallHp=1500"
  B 위치: §6-6 → 마히토 V "자폐원돈과": 결계형 영역이나 wallHp 값 미명시
  판정: A가 맞음 (적용됨이 전제)
  근거: domains.json에 값이 들어가겠지만 §6-6 표에서 명시하지 않아 개발자가 놓칠 수 있다.

6. 확장성 평가
신규 캐릭터 추가 변경 범위: §27에 10단계가 정의되어 있다. 문제는 PlayerData에 캐릭터 전용 필드(예: deadShikigamiIds, burstActive)를 매번 추가하는 구조다. 20개 캐릭터가 되면 PlayerData는 60개 이상의 필드를 가지며 매 추가마다 snapshot(), DB 마이그레이션, 테스트가 연동 수정된다.
하드코딩 위험값:

DomainInstance 생성자의 wallHp = isOpen ? 0 : 1500 하드코딩. domains.json에서 읽어야 함.
FingerSystem의 data.fingerCount = Math.min(data.fingerCount + 1, 20) 상한 20 하드코딩.
animId가 byte 0~127 범위라 명시되어 있어 128개 제한이 명확하다. 현재 60개 사용, 향후 10개 캐릭터 추가(각 5~6개) 시 한계 도달 가능.

현재 구조가 허용하는 최대 규모: animId 기준 68개 여유이므로 평균 5개 스킬 기준 13~14개 캐릭터 추가 가능.
확장 시 가장 먼저 깨질 부분: PlayerData flat 구조. 캐릭터 수가 15개를 넘어가면 DB 마이그레이션 비용이 선형이 아니라 누적 부담으로 증가하며, snapshot() 메서드 누락이 발생하기 시작한다.

7. 비판적 종합 평가
지금 당장 이 파일을 기준으로 개발을 시작할 수 있는가?
→ 조건부. ISkillSet 인터페이스, CombatPipeline, PlayerRepository, DamageContext 클래스 설계를 개발자가 스스로 채워야 하며, 스쿠나 SkillSet과 나나미 3개 키를 먼저 확정해야 한다. 이 조건들이 충족되면 Phase 1 구현 착수 가능.
이 파일의 가장 큰 약점 하나는 무엇인가?
→ 구현 명세의 핵심 인터페이스(ISkillSet, CombatPipeline, PlayerRepository, DamageContext)가 이름만 있고 실제 코드 수준 명세가 없어, Claude Code가 이 파일만으로 일관된 구현을 생성하기 어렵다.
만약 네가 이 파일을 처음 받은 개발자라면, 첫 번째로 무엇을 할 것인가?
→ 1,148줄 이후 부록 2개를 별도 파일로 즉시 분리하고, ISkillSet.java 인터페이스 선언 코드를 작성하여 §6 전체 캐릭터가 컴파일 가능한지 확인한다.
이 파일이 목적을 100% 달성하려면 앞으로 얼마나 더 작업이 필요한가?
→ 누락된 클래스 명세 작성 20~30시간 + 스쿠나·나나미 스킬 정의 2~3시간 + 부록 분리 및 §7-2 재작성 1~2시간. 총 25~35시간 문서 작업 추가 필요.

8. 우선순위별 수정 로드맵
[1] 부록 2개 파일 분리
  - 수정 내용: 1,148줄~끝을 jjk_extension_proposal.md와 jjk_presentation_guide.md로 분리
  - 예상 소요: 30분
  - 의존성: 없음
  - 효과: Claude Code가 기획 제안을 구현 명세로 오인하는 위험 제거

[2] ISkillSet 인터페이스 코드 추가
  - 수정 내용: §6-1에 5개 메서드 시그니처가 포함된 실제 Java 인터페이스 코드 작성
  - 예상 소요: 1시간
  - 의존성: 없음
  - 효과: [C-1], [H-5] 치명/높음 위험 기반 해결, 모든 SkillSet 구현의 기준점 확보

[3] 스쿠나 SkillSet 정의 추가
  - 수정 내용: §6에 §6-x 스쿠나 항목 추가. animId 21~24 대응 스킬 5개 정의
  - 예상 소요: 2~3시간
  - 의존성: [2] ISkillSet 인터페이스 확정 이후
  - 효과: 충돌 [3] 해소, CharacterRegistry 완전성 확보

[4] 나나미 Shift+F, R, V 스킬 정의
  - 수정 내용: §6-10 표에 3개 키 추가. 미구현 키는 명시적 NoOp 반환 명세라도 작성
  - 예상 소요: 1시간
  - 의존성: [2] 이후
  - 효과: [H-5] 해소

[5] CombatPipeline 메서드 시그니처 명세 추가
  - 수정 내용: §3-6에 CombatPipeline.java의 주요 메서드 시그니처(입력 파라미터, 반환값) 코드 블록 추가
  - 예상 소요: 3~4시간
  - 의존성: [2] 이후
  - 효과: 구현 가능성 점수 대폭 향상, 개발자 설계 부담 제거

[6] PlayerRepository 인터페이스 명세 추가
  - 수정 내용: §13에 PlayerRepository.java 인터페이스, SQLite 테이블 스키마, 동기/비동기 처리 방침 추가
  - 예상 소요: 3~4시간
  - 의존성: 없음
  - 효과: [H-2] FingerSystem 블로킹 위험 설계 수준에서 해소 가능

[7] DamageContext 클래스 필드 목록 추가
  - 수정 내용: §4 또는 §3-6에 DamageContext.java 전체 필드 선언 코드 블록 추가
  - 예상 소요: 1시간
  - 의존성: [5] 이후
  - 효과: isSoulDirect, bypassRCT 등 참조 필드의 일관성 보장

[8] bindingVowDeclaredTick sentinel 처리 명세
  - 수정 내용: §3-1에 "미선언 상태 = -1L" 또는 별도 boolean 필드 추가 명시
  - 예상 소요: 30분
  - 의존성: [6] DB 스키마 작업 이후
  - 효과: [H-1] 해소

[9] §7-2 등급별 해금 표 재작성
  - 수정 내용: 중복 배율 표 제거, 등급별 스킬 해금 조건 실제 내용으로 대체
  - 예상 소요: 1~2시간
  - 의존성: 없음
  - 효과: 충돌 [1] 해소, 문서 일관성 향상

[10] 각성 발동 경로 단일화
  - 수정 내용: §3-7과 §1-6의 checkAndActivate 호출 경로를 CombatPipeline 경로 하나로 통일하거나, 두 경로 모두 유지 시 중복 발동 방지 책임 소재를 명시
  - 예상 소요: 1시간
  - 의존성: [5] CombatPipeline 명세 이후
  - 효과: 충돌 [4] 해소, [H-4] 완화

[11] config.json 전체 스키마 통합 작성
  - 수정 내용: §12-1에 조각난 config 값들을 단일 JSON 블록으로 통합 명시
  - 예상 소요: 1시간
  - 의존성: [3][4] 스킬 정의 완료 이후
  - 효과: 배포 시 config 누락 방지

[12] PacketCodecs API 검증 및 코드 수정
  - 수정 내용: §11-5 ZoneEnterS2CPacket CODEC을 Fabric 1.21.1 실제 API로 검증·수정
  - 예상 소요: 1~2시간
  - 의존성: 없음 (즉시 가능)
  - 효과: [C-3] 치명 위험 해소입력 내용: 이것이외의 추가할거나 수정할것들있으면 전부 다알려줘이것이외의 추가할거나 수정할것들있으면 전부 다알려줘오후 10:29Claude 응답: 파일을 다시 정밀 검토한다.파일을 다시 정밀 검토한다. 1회차 분석에서 놓친 세부 항목을 찾기 위해 섹션별로 재독한다.Re-read sections not fully coveredRe-read sections not fully covered구체적인 미비점들을 더 확인한다.Check CE regen, zone, burden, trial detail sectionsCheck CE regen, zone, burden, trial detail sections이제 1회차에서 놓친 모든 세부 항목을 정리한다. 추가 발견 사항만 빠짐없이 보고한다.

1회차 분석 이후 추가 발견 사항 — 전체 목록

A. 추가 내부 충돌 / 논리 오류
[충돌 A-1] checkAndActivate 시그니처 불일치
§3-7 pseudo-code:
checkAndActivate(PlayerData data, float currentHp, float maxHp, long tick)
§1-6 Mixin 명세:
AwakeningManager.checkAndActivate(player)
player 단일 파라미터와 (data, currentHp, maxHp, tick) 4개 파라미터가 충돌한다. Mixin에서 호출할 때 어떤 시그니처를 쓰는지 확정하지 않으면 구현마다 다르게 작성된다.

[충돌 A-2] 옷코츠 burstEndTick 시간 불일치
§6-5 표: "주력해방 — 10초"
§6-5 구현 코드: burstEndTick = tick + 200
10초 = 200틱 (20틱/초 기준)은 수식 자체는 맞으나, §3-7 각성의 "8초(160틱)"과 비교하면 서버가 20TPS를 전제한다. 이 전제가 §2-2 "50ms/tick" 기준과 충돌한다. 50ms/tick = 20TPS이므로 실제로는 맞지만 어디에도 "1초 = 20틱" 기준이 명시되지 않아 개발자가 다른 전제를 쓸 수 있다. 전체 명세에 1초 = 20틱 기준 명문화가 없다.

[충돌 A-3] onDeath 저장 담당 클래스 불일치
§1-6 Mixin: CharacterCommandService.saveImmediate(data)
§2-4 저장 원칙: "사망은 즉시 저장"
§13-2 저장 타이밍: "로그아웃, 사망, 캐릭터 선택, 손가락 획득 시 즉시 저장"
§26-3 부활 처리: RespawnManager가 내부적으로 저장 처리 암시
onDeath 에서 저장 주체가 CharacterCommandService인지 PlayerRepository를 직접 호출하는지, RespawnManager와 중복 저장되는지 불분명. 현재 3개 섹션이 같은 이벤트에 대해 서로 다른 클래스를 호출한다고 암시한다.

[충돌 A-4] 흑섬 확률 공식 미정의 + "Zone 상태 기반" 순환 참조
§3-2: "기본 확률 = 숙련도와 Zone 상태 기반"
§3-3: "흑섬 성공 시 Zone 진입"
흑섬 확률이 Zone 상태에 따라 달라지는데, Zone은 흑섬 성공으로 진입한다. 즉, Zone이 없으면 흑섬 확률이 낮고, 흑섬이 없으면 Zone에 진입 못 하는 부트스트랩 문제가 있다. 이 순환을 어떻게 초기화하는지 (예: Zone 없을 때 기본 확률 X%, Zone 중 Y% 보정) 수치가 전혀 없다.

[충돌 A-5] DomainInstance.wallHp 생성자 하드코딩 vs. domains.json 충돌
§8-1 코드: this.wallHp = isOpen ? 0 : 1500; — 하드코딩
§8-3: "결계형 기본 wallHp=1500" — 기본값
§8-4 domains.json 필드에 wallHp가 없다. 캐릭터별로 영역 내구도를 다르게 설정하려면 domains.json에 wallHp 필드가 있어야 하는데, 생성자가 하드코딩이라 config 제어가 불가능하다.

[충돌 A-6] §3-2 Just Frame 판정 시점 vs. §3-5 정의 충돌
§3-2(흑섬): "공격 직전·직후 Just Frame"
§3-5(Just Frame): "Just Frame 윈도우는 8~12틱"
"공격 직전·직후"가 각각 몇 틱인지 불명. 흑섬 Just Frame 윈도우가 §3-5의 콤보 Just Frame과 동일 8~12틱인지, 별도 윈도우인지 명세가 없다. ComboTracker와 흑섬 판정이 같은 윈도우를 공유하는지도 불명.

[충돌 A-7] 이타도리 V(RCT) vs. 옷코츠 V(반전술식) 중복
§6-3 이타도리 V: "RCT — healingActive=true 동안 초당 HP 회복"
§6-5 옷코츠 V: "반전술식 — 아군 대상 회복 가능"
두 스킬이 모두 healingActive 필드를 사용하는가? PlayerData에 healingActive는 1개다. 이타도리가 자기 자신을 회복하는 것과 옷코츠가 아군을 회복하는 것이 같은 healingActive 플래그를 공유하면 충돌한다. 두 캐릭터가 동시에 RCT를 사용할 때 어느 플레이어의 healingActive가 세팅되는지 혼동 가능.

[충돌 A-8] 죠고 animId 31이 jogo_r로 등록됐으나 스킬 표에서 R이 "운석"
§11-2 AnimationRegistry 표: animId 31 = "죠고 R"
§11-2 코드: register(31, "jogo_r")
§6-7 표: 죠고 R = "운석"
그런데 §6-7 표에서 죠고 Shift+F(개관) = animId 46, Shift+R(불꽃의 고리) = animId 47로 명시됨.
죠고 F(화산탄) = animId 30, 죠고 R(운석) = animId 31, 죠고 V(개관철위산 영역) = animId 32인데 죠고 Shift+F·Shift+R 애니가 40번대에 등록되어 있다. animId 30~32는 죠고 F/R/V만 커버하고, Shift+F(46), Shift+R(47)은 나중에 추가된 것이다. 이 불연속적인 번호 배치가 의도적인지 문서에 설명이 없다.

B. 추가 누락 항목
[B-1] 1초 = 20틱 기준 미명시 (전체 명세의 근본 전제)
각성 160틱=8초, 쿨타임 4틱=0.2초 등 모든 시간 환산이 20TPS를 전제한다. 그러나 어느 섹션에도 "이 명세는 20TPS(1틱=50ms) 기준으로 작성되었다"는 명문이 없다. §2-2에 "50ms/tick"은 있지만 이것이 틱 기준 수치 환산의 근거라고 명시되지 않아 §0 CLAUDE.md에 한 줄 추가가 필요하다.

[B-2] CharacterConfirmS2CPacket 패킷 패키지 표·S2C 패킷 표 미등록
§26-2에서 CharacterConfirmS2CPacket이 사용되나 §1-1 패키지 표와 §11-4 S2C 패킷 표 어디에도 없다.

[B-3] RespawnS2CPacket S2C 패킷 표 미등록
§26-3에서 RespawnS2CPacket이 언급되나 §11-4에 없다.

[B-4] CharacterSelectS2CPacket S2C 패킷 표 미등록 + 방향 혼동
§26-1에서 CharacterSelectS2CPacket 전송이 언급된다. §1-1 패키지 표에서는 CharacterSelectC2SPacket만 있고, CharacterSelectS2CPacket(서버→클 선택 화면 열기)은 패키지 표와 §11-4 어디에도 없다.

[B-5] EffectDeferQueue 클래스 패키지·명세 없음
§6-3 이타도리 경정권: "EffectDeferQueue 예약"으로 사용되나 §1-1 패키지 표에 없고, 구현 방법(LinkedList? ScheduledExecutor? Tick 기반 queue?)이 전혀 없다. 5틱 후 지연 데미지가 어떻게 스케줄되고 취소(예: 대상 사망 시)되는지 미정의.

[B-6] InfinityHandler 클래스 패키지 미등록
§1-6 Mixin에서 InfinityHandler.checkAndCancel()이 호출되나 §1-1 패키지 표에 없다. combat/에 속하는지 character/impl/에 속하는지 불명.

[B-7] CEPool, CERegenRule 클래스 명세 없음
§1-1에 ce/ 패키지에 CEPool·CERegenRule이 있으나 §5(CE 시스템)에는 이 두 클래스의 역할·메서드가 전혀 없다. CEManager.regenTick(player)가 어떻게 CERegenRule을 참조하는지, CEPool이 어떤 캐싱 역할을 하는지 불명.

[B-8] ZoneStateManager 명세 없음
§1-6 Mixin에서 ZoneStateManager.tick(player)와 ZoneStateManager.reset(player)가 호출되나, Zone 섹션(§3-3)에는 ZoneStateManager의 역할·필드·메서드가 없다. Zone 지속 시간(zoneEndTick)을 누가 관리하는지, ComboTracker와의 관계가 불명.

[B-9] ComboTracker 와 흑섬 연동 명세 없음
§1-1에 zone/ComboTracker가 있고 §18 테스트에 ComboTrackerTest가 있는데, §3 전투 섹션 어디에도 ComboTracker의 역할(콤보 카운트 관리, 흐름끊기 카운터 관리)이 명세되어 있지 않다. 이타도리 맨지킥의 "흐름끊기 카운터 +1"이 ComboTracker와 연동되는지 불명.

[B-10] 메구미 마허라가 의식(R) 상세 로직 없음
§6-4: "적응 카운터 시작, 실패 시 큰 리스크" — 적응 카운터가 무엇인지, 카운터 증가 조건, 실패 조건, "큰 리스크"의 실제 효과(HP 손실? CE 봉인?)가 전혀 없다. 구현 불가 수준.

[B-11] 메구미 그림자 이동(V) 텔레포트 조건 불명
§6-4: "10블록 내 그림자 위치로 순간이동" — "그림자 위치"가 무엇인지 정의 없다. 월드의 그림자 블록인지, 플레이어가 별도로 배치하는 식신 기반 포인트인지, 단순히 10블록 내 임의 위치인지 불명.

[B-12] 히구루마 처형검(V) PlayerData 저장 필드 없음
§6-11: "유죄 판결 후 1회성" 처형검. PlayerData에 처형검 보유 여부를 저장하는 필드가 없다. §13-3 cooldown_persist에 skill_seal은 있으나 처형검 보유 boolean이 없다. 서버 재시작 시 처형검 유지 여부 불명.

[B-13] 하카리 잭팟 중 maxCE 제한 없음 처리 코드 없음
§6-8: "잭팟 중: maxCE 제한 없음" — 어떻게 구현하는가? PlayerData.ceMax를 임시로 올리는가, 별도 플래그로 CAP 체크를 우회하는가. 잭팟 종료 시 ceMax 원상복구 로직이 없으면 데이터 오염.

[B-14] 하카리 jackpotCooldownUntil과 잭팟 활성 필드 분리 문제
PlayerData에 jackpotCooldownUntil은 있으나 잭팟 활성 여부(jackpotActive), 잭팟 종료 틱(jackpotEndTick)이 없다. 잭팟 지속 중 상태를 어떻게 추적하는지 불명.

[B-15] CEManager.regenTick(player) 재생 수치 없음
§5-1: "재생은 캐릭터, 등급, 전투 상태, 패시브에 따라 달라진다" — 각 캐릭터·등급별 CE 재생량 수치가 전혀 없다. CERegenRule이 이를 제공할 것으로 보이지만 명세가 없다.

[B-16] BurdenManager 명세 없음
§1-1과 §1-6 Mixin에서 BurdenManager.tick(player)가 호출되나, 이누마키 섹션(§6-9, §22-3) 외에 BurdenManager 클래스 자체의 역할·필드·메서드 명세가 없다. burden 필드 초기화, 최댓값, 봉인 해제 조건이 §6-9에서만 산문으로 나온다.

[B-17] TeamManager 패키지 미지정 + 팀 체크 시점 불명
§26-5에서 TeamManager의 역할이 정의되지만 §1-1 패키지 표에 없다. "같은 진영 공격 불가" 체크가 CombatPipeline 1단계(입력 검증)에서 이루어지는지, Mixin damage at=HEAD에서 이루어지는지 명시되지 않아 이중 구현 가능성이 있다.

[B-18] TrialManager 기소 대상 선정 로직 없음
§6-11: "기소→선택→판결" StateMachine이 있으나, 누가 피고가 되는지, 히구루마가 공격 시 자동 기소인지, 별도 스킬로 선택하는지, 기소 가능 최대 거리가 얼마인지 불명.

[B-19] audit_log.db 테이블 스키마 없음
§1-3, §10-2, §21-1에서 audit_log.db가 언급되나, 어떤 테이블 구조(timestamp, eventType, playerUuid, detail 등)인지 전혀 없다. 구현자가 임의로 스키마를 만들 수밖에 없다.

[B-20] player_data.db SQL 스키마 없음
PlayerData 34개 필드는 정의되어 있으나, 실제 SQLite 테이블 DDL(CREATE TABLE)이 없다. cooldowns Map<String, Long>과 unlockedSkills List<String>, deadShikigamiIds List<String> 같은 컬렉션 타입을 어떻게 직렬화하는지(JSON 문자열? 별도 테이블? BLOB?) 미정의.

[B-21] Migrator 버전 0 초기 스키마 없음
§27에 MigratorV{N}toV{N+1} 패턴이 언급되나 현재 schemaVersion의 초기값(0? 1?)과 V0→V1 마이그레이터가 없다. 최초 배포 시 schemaVersion이 어떻게 세팅되는지 불명.

[B-22] domains.json 전체 예시 없음
§12-3에 필수 필드 목록만 있고 실제 JSON 예시가 없다. gojo_unlimited_void, sukuna_malevolent_shrine, itadori_unnamed 등이 코드에서 문자열로 참조되나 어떤 domains.json 항목으로 대응하는지 연결이 없다.

[B-23] techniques.json 전체 예시 없음
§12-2도 마찬가지. 필수 필드만 열거되어 있고 실제 JSON 구조 예시가 없다. 개발자가 스키마를 직접 설계해야 한다.

[B-24] SkillUseC2SPacket 필드 정의 없음
§1-1에 있으나 §11-4 패킷 표에 없다. 클라이언트가 어떤 정보를(characterId? keyId? targetUuid? playerPos?) 서버로 보내는지 전혀 없다.

[B-25] CharacterSelectC2SPacket 필드 정의 있으나 검증 실패 응답 패킷 없음
§26-2에 C2S 패킷은 정의됐으나 "캐릭터 선택 실패(중복, 미등록)" 시 클라이언트에 어떤 S2C 패킷을 보내는지 없다. 클라이언트는 성공/실패 여부를 어떻게 아는가.

[B-26] config.json reload 시 런타임 반영 범위 불명
§10-1 /jj reload가 있으나, reload 시 어떤 값이 즉시 반영되고 어떤 값은 서버 재시작이 필요한지 명시 없다. 예: 이미 전개 중인 영역의 wallHp가 reload로 바뀌는지 여부.

[B-27] RikaEntity 클래스 패키지·생명주기 없음
§6-5에서 RikaEntity 소환이 언급되나 §1-1에 없다. GeckoLib 엔티티인지, 바닐라 몹 기반인지, 리카의 AI(추적, 공격 대상 선정), lifetime(소환 지속시간), 재소환 가능 여부가 없다.

[B-28] 장막(Curtain) 발동 주체·스킬 미정의
§16-1에 장막 기능이 있으나 어떤 캐릭터가 어떤 키로 장막을 치는지, CE 비용이 얼마인지 전혀 없다. curtainCooldownUntil이 PlayerData에 있고 §13-3에도 있으나 장막을 치는 스킬 자체가 §6 캐릭터 표 어디에도 없다.

[B-29] DomainBoundaryRenderer가 서버 코드에 있음
§1-1 패키지 표에 domain/DomainBoundaryRenderer가 서버 패키지로 등록되어 있다. 그러나 렌더러는 클라이언트 전용이어야 한다(§2-1 원칙). 서버 패키지에 렌더러 클래스가 있으면 dedicated server에서 클래스로딩 오류 발생 가능. §15-2에서 식신 렌더는 client 전용으로 격리한다고 하면서 도메인 렌더러는 서버 패키지에 있는 것은 모순이다.

[B-30] Phase별 캐릭터 구현 대상 불명
§19 Phase 1에 "기본 4개 SkillSet", Phase 3에 "나머지 7개 캐릭터"라고 했는데 "기본 4개"가 누구인지 명시되지 않았다. 고죠·이타도리·메구미·옷코츠인지, 고죠·스쿠나·이타도리·마히토인지 개발자가 임의로 선택해야 한다.

C. 코드 수준 버그 / 수정 필요
[코드-1] DomainInstance.applyTeamLimit() 영구 축소 문제
javapublic void applyTeamLimit(int teamDomainCount) {
    if (teamDomainCount >= 2) {
        this.currentRadius = this.maxRadius / 2;
    }
}
팀 영역이 1개로 줄었을 때 currentRadius를 원복하는 로직이 없다. 한번 축소되면 영구 축소 상태가 된다.

[코드-2] FingerSystem.tryDrop() — Math.random() 사용 위험
Math.random()은 thread-safe하지 않고, synchronized 블록 안에서 fingerDropRate() 기준이 무엇인지(0~1 float인지 0~100 int인지) 정의되지 않아 비교 방향이 맞는지 확인 불가. > config.fingerDropRate()가 드롭 확률인지 실패 확률인지 혼동된다. 예: fingerDropRate=0.3이면 Math.random() > 0.3은 70% 확률로 드롭 → 의도와 반대.

[코드-3] AnimationRegistry.get(id) 미등록 ID 반환값
javapublic static String get(int id) {
    return ANIMATIONS.get(id);
}
존재하지 않는 animId 호출 시 null을 반환한다. 호출처에서 null 체크 없으면 NPE. 예외 처리 또는 기본값 반환 로직이 없다.

[코드-4] AnimationRegistry의 register() 메서드가 public
외부에서 임의로 animId를 덮어쓸 수 있다. static 등록 블록 완료 후에는 외부 접근을 막아야 한다. private static으로 변경하거나 불변 Map으로 전환 필요.

[코드-5] PlayerData.snapshot()에서 컬렉션 얕은 복사 문제
javacopy.cooldowns = new HashMap<>(this.cooldowns);
cooldowns의 value가 Long(불변 타입)이므로 이 경우는 안전하지만, deadShikigamiIds의 요소(String)도 불변이므로 현재는 괜찮다. 그러나 나중에 컬렉션 value가 mutable 객체로 바뀌면 얕은 복사가 버그가 된다. 이에 대한 주의 경고가 명세에 없다.

[코드-6] ZoneEnterS2CPacket — PacketCodec.of() 람다 방향 오류 가능성
javapublic static final PacketCodec<RegistryByteBuf, ZoneEnterS2CPacket> CODEC = PacketCodec.of(
    (buf, pkt) -> { ... },  // encoder: (buf, value) 순
    buf -> new ZoneEnterS2CPacket(...)  // decoder
);
Fabric 1.21.1의 PacketCodec.of(PacketEncoder, PacketDecoder) 시그니처에서 PacketEncoder<B, V>는 (B buf, V value)이므로 (buf, pkt) 순서는 맞다. 그러나 PacketCodecs.STRING이 PacketCodec<PacketByteBuf, String> 타입인 반면 버퍼가 RegistryByteBuf이기 때문에 타입 불일치 컴파일 오류가 발생할 수 있다. buf.writeString(pkt.playerUuid().toString())과 buf.readString()으로 직접 쓰는 것이 안전하다.

[코드-7] PlayerAnimationDispatcher.play() — AnimationPayload 미정의
javapublic static void play(ServerPlayerEntity player, String animName, AnimationPayload payload) {
    ...
    layerContainer.activate(animName, payload.phase());
}
AnimationPayload 타입이 어느 패키지인지, phase()가 PlayerAnimator 2.0.4 API의 실제 메서드인지 확인 필요. PlayerAnimator 2.0.4의 실제 API는 AnimationEndState 또는 IAnimation 기반으로, phase() 메서드가 존재하지 않을 수 있다.

D. 수정 필요 (기존 내용 교정)
[수정-1] §0 규칙 4번과 7번 완전 중복
규칙 4번과 7번이 동일한 내용([AI-ADD] 등 태그 금지)을 두 번 적어두었다. 하나를 제거하고, 7번에 "주석 형태도 금지"가 추가된 만큼 4번을 삭제하고 7번만 남기면 된다.

[수정-2] §1-4 build.gradle — fabric.mod.json 핵심 설정 누락
fabric.mod.json에 schemaVersion, id, version, environment, entrypoints(서버 전용 main), depends(fabricloader, minecraft 버전)이 없다. Mixin 등록만 있고 mod 기본 선언이 없어 실제로 파일을 작성하려면 완전히 처음부터 만들어야 한다.

[수정-3] §6-8 하카리 표 — 잭팟 발동 keyId가 F인데 CD 수치 단위 불명
"CD | 20" — 쿨타임 20이 틱인지 초인지 불명. §6-2 고죠 표에서 CD=4는 틱(4틱=0.2초, 너무 짧아서 초일 가능성), 고죠 R CD=60, 영역 CD=360인 것을 보면 초 단위로 보이나 확인 필요. 전체 SkillSet 표에서 CD 단위가 어디에도 명시되지 않았다. 각성 쿨타임은 §3-7에서 "2400틱"으로 틱 단위가 명시됐는데, 스킬 표의 CD는 다른 단위를 쓰는가?

[수정-4] §6-9 이누마키 표 — R 키 완전 누락 (1회차 [C-1]과 별개 추가 검증)
재검토 결과, §6-9 표에는 F, Shift+F, Shift+R, V 4개만 있다. R 행이 없는 것이 확인됨. 이 칸에 들어갈 스킬이 있어야 하는데, 이누마키는 원작에서 R 키에 해당하는 추가 주언(예: !부숴 등)이 있을 수 있으나 운영자가 명시적으로 결정하지 않은 상태다.

[수정-5] §11-2 AnimationRegistry 표 — 빈 animId 구간 설명 없음
animId 21~24는 스쿠나, 25~29는 메구미, 30~32는 죠고(F/R/V)이고 33~35는 마히토(F/R/domain)이다. 죠고 Shift+F(46)·Shift+R(47), 마히토 Shift+F(43)·Shift+R(44)·V(45)가 나중에 추가되면서 중간에 비어있는 것처럼 보이는 번호 배치가 됐다. animId 33~39가 마히토·하카리·히구루마 일부, 40~59가 나머지 캐릭터로 이중 구조가 되어있는데 이에 대한 설명이 없다. 신규 개발자는 번호 배치 의도를 알 수 없다.

[수정-6] §13-1 PlayerData 필드 수 불일치
제목은 "34개 필드"라고 하는데, 실제 선언 필드를 직접 카운트하면:
uuid, characterId, grade, xp, mastery, ceCurrent, ceMax, hpCurrent, hpMax, attackStat, defenseStat, speedStat, fingerCount(13) + unlockedSkills, cooldowns(2) + bindingVowDeclaredTick, domainCooldownUntil, jackpotCooldownUntil, curtainCooldownUntil, trialState, burden, zoneActive, zoneEndTick, lastCombatTick, lastKnownIp(10) + awakeningActive, awakeningEndTick, awakeningCooldownUntil, burstActive, burstEndTick, deadShikigamiIds, healingActive, zonePenaltyUntilTick(8) + schemaVersion(1) = 34개.
카운트는 맞다. 단, §18 PlayerDataSnapshotTest의 "34개 필드 snapshot 복사" 테스트가 이 정확한 숫자를 하드코딩하고 있는데, 필드가 추가될 때마다 테스트 숫자도 수동으로 갱신해야 하는 fragile 구조다. 리플렉션 기반으로 자동 카운트하도록 명세 개선 필요.

E. 구조적 제안 (기존 내용 보완)
[구조-1] §0 CLAUDE.md에 추가해야 할 규칙
현재 10개 규칙 중 다음 3가지가 없다:

"1초 = 20틱 기준. 모든 tick 환산은 이 기준을 따른다."
"CD 단위는 틱(ticks). 표에서 CD=4는 4틱=0.2초."
"스킬 표의 CD와 §13-3 cooldown_persist는 같은 키를 공유한다. 스킬 키(keyId)와 쿨타임 키(cooldown_type)를 혼동하지 말 것."


[구조-2] §6 캐릭터 섹션에 스쿠나가 없는 이유를 명시해야 함
지금은 스쿠나가 빠진 이유를 독자가 유추해야 한다. "스쿠나는 Phase 3에서 추가(또는 별도 설계 중)" 같은 한 줄이라도 없으면 누락으로 오인된다.

[구조-3] §19 Phase 구분과 §27 확장 절차의 중복 및 충돌
§19 Phase 3: "나머지 7개 캐릭터 SkillSet" — 현재 11개 중 Phase 1에서 4개를 구현하면 나머지는 7개.
§27: "12번째 이후 캐릭터 추가 절차" — Phase 3에서 추가할 캐릭터와 §27에서 말하는 신규 캐릭터가 같은지 다른지 불명. Phase 3 구현 대상 7개에도 §27 절차를 따라야 하는지도 불명.

[구조-4] §24 참조 무결성의 참조 대상 섹션 변경 감지 체계 없음
§24-1: "§1-6, §3-7, §8-4, §11-5, §13-1, §26, §27은 구현 중 반드시 실제 섹션으로 유지"라고 했으나, 이 섹션들이 실제로 존재하는지 검사하는 자동화 수단이 없다. 향후 버전업(v6, v7)에서 섹션 번호가 바뀌면 이 참조들이 조용히 깨진다.

이상이 1회차 분석에서 누락된 추가 항목 전체다. 요약하면:
카테고리추가 발견 건수내부 충돌 / 논리 오류8건누락 항목30건코드 수준 버그7건수정 필요 (교정)6건구조적 개선4건합계55건
1회차 보고(12개 로드맵 항목)와 합산하면 총 67건의 수정·추가·교정 사항이 이 명세서에 존재한다.



