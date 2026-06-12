# FABLE_NOTES — 자율 개선 작업 기록 (2026-06-11)

## [미결 항목]

- `src/client/java/com/jjk/client/hud/BlackFlashOverlay.java` — 흑섬 Perfect 시 "화면 흑백 0.3초" 효과는 진짜 그레이스케일이 아니라 검은 오버레이 플래시(5틱+5틱 페이드)로 근사 구현되어 있음. 실제 흑백 반전은 GameRenderer post-process 셰이더(`minecraft:shaders/post/desaturate.json`) 적용이 필요하고, 이를 호출하려면 GameRendererAccessor 믹스인 추가가 필요해 영역 B 허용 범위(hud/ 내부)를 벗어남 — 권장 조치: 별도 작업으로 client 믹스인 + `jjk.client.mixins.json` 등록 후 구현.
- `src/client/java/com/jjk/client/hud/SkillCooldownHUD.java` — 봉인된 스킬의 슬롯 단위 표시 불가. 클라이언트는 `sealedSkillId`(스킬 ID 문자열)만 받고 스킬 ID→keyId 매핑 데이터가 클라이언트에 없음. 화면 상단 중앙의 기존 봉인 인디케이터(JjkHudRenderer.renderSealIndicator)가 기능을 대신함 — 권장 조치: SealedSkillSyncS2CPacket에 keyId 필드 추가 시 슬롯 자물쇠 오버레이 구현 가능.
- `docs/jjk_spec_06_decisions.md` — CE 비율별 오라/HUD 색상 기준이 decisions 문서에 없음(영역 C 프롬프트는 있다고 전제). 사실상 표준인 서버 보스바 `CeBossBarManager.colorFor()`(70% BLUE / 40% GREEN / 20% YELLOW / 미만 RED)에 CE바·CE오라를 맞춤 — 권장 조치: 운영자가 색상 기준을 decisions 문서에 등재.
- ~~`TechniqueAnimMap.java` sukuna:4 폴백 불일치~~ → 2026-06-11 decisions §P3-6 기준(V=7)으로 수정 완료.
- `run/config/jjk/techniques.json` — choso가 animId 60~63, todo가 animId 64를 사용하는데 이는 레지스트리상 nanami/higuruma 애니메이션의 재사용임. 의도인지 placeholder인지 판단 불가 — 권장 조치: choso/todo 전용 animId 신설 여부 운영자 확인.
- `src/client/java/com/jjk/client/effect/WorldAmbientEffects.java` — "던전/구조물 구역 ambient"는 클라이언트에서 구조물 정보를 알 수 없어 광량 기반(하늘빛 0 + 블록광 ≤5)으로 근사 구현함. 구조물 정확 감지가 필요하면 서버에서 S2C 동기화 필요.
- `ParticleThrottle` — 서버 TPS는 클라이언트에 동기화되지 않아 "TPS 연동"은 불가능. FPS를 부하 지표로 사용하는 동적 상한(60/40/24/12)으로 구현함. 서버 TPS 연동이 필요하면 TPS S2C 패킷 신설 필요.

## [밸런스 패스 2026-06-11 — 운영자 지시로 §LOCK 해제 후 전면 조정]

설계 기준 (데미지 공식: `final = max(0, base × BF × clamp(atkMult×grade×cond×combo, 0.25~4) − def)` → 틱캡 40%):

- **방어력 50~85 → 8~17**: 원천 차감 방식이라 기존엔 저위력기가 탱커에게 0데미지였음. 이제 모든 스킬이 유효타.
- **스킬 데미지 ~1/3 재조정**: 연타기(쿨 8~14틱) 최종 ~15% HP, 중기술 ~30%, 궁극기는 틱캡(40%) 보장 한방. PvP TTK 목표 10~20초.
- **연타기 쿨타임 4~6틱 → 8~14틱**: 초당 5회 난사 방지, CE보다 쿨이 페이스 결정.
- **영역 CE 전면 인하 (풀의 ~70%)**: 기존엔 8캐릭터 중 7명이 영역 전개 물리적 불가(예: 스쿠나 개방형 ×2=6000 > 풀 4000, 하카리 2500 > 풀 1000). 영역 키의 techniques.json ceCost는 0으로 — domains.json이 단일 차감 기준 (CombatPipeline+DomainManager 이중 차감 제거).
- **영역 쿨타임 360틱(18초) → 1200틱(60초)**: 궁극기 위상 확립, 영역 스팸 메타 방지. 옷코츠 1500틱.
- **스쿠나 영역 radius 200 → 30 시작 / openMaxRadius 100**: 즉시 200블록 필중은 서버에서 압살.
- **각성 HP 임계 0.05 → 0.30**: 빈사에서만 발동하던 사문 기믹을 실제 역전 기믹으로. 배율 1.5/160틱/2400틱 유지.
- **maharagaThreshold 5 → 3, zoneDuration 200 → 300틱(15초), blackFlashBaseRate 런타임 1 → 5**.
- **커튼 800/2000 → 500/1500**: 저CE풀 캐릭터(이누마키 900)도 기본 커튼 사용 가능.
- **rikaMeleeDamage 20 → 8**: 소환수 평타가 본체 스킬보다 강하던 문제.
- 유지: PvP 틱캡 0.40, 흑섬 ×2.5/Just Frame, 클램프 ×0.25~4.0, CE 재생, 캐릭터 HP/공격/CE풀, 처형검 999(재판 기믹), 토도 CE 0 기믹.

## [수치 감사 결과 (2026-06-11) — 같은 날 운영자 §LOCK 해제 지시로 해소됨]

감사에서 발견했던 충돌(각성 임계 0.30/0.05/0.10 3중 불일치, 각성 배율 1.25 vs 1.5, CLAUDE.md sealDuration 600 구버전, 런타임 config §LOCK 이탈 3건)은 운영자가 §LOCK을 해제하고 밸런스 전권을 위임하면서 위 "밸런스 패스" 값으로 일괄 확정됨. 각성 임계 0.30 / 배율 1.5 / seal 400 채택.

## [완료 항목]

- **영역A**: 흑섬 이펙트에 검은 연기+붉은 잔광(DustParticleEffect) 지그재그 방사 번개 추가(`CommonEffects.spawnBlackRedLightning`, ItadoriEffects·SkillFxDispatcher 공용 사용). 각성 폴백 이펙트를 TOTEM burst → 발밑 기류 링 + 3블록 내 부양 기류(spec_05 §1-3)로 교체. WorldAmbientEffects에 영역 활성 ambient(8틱 주기 PORTAL/WITCH 하강 입자) + 어두운 지하 ambient(30틱 주기 ASH) 추가. ParticleThrottle을 FPS 연동 동적 상한으로 개선. 고죠 창(수렴)/혁(방사 충격파)/자(보라 빔), 마히토 비틀림 나선은 기존 구현이 spec_05를 이미 충족해 유지.
- **영역B**: HUD 세로 겹침 해소 — 쿨다운 슬롯(−62~−44)이 HP바(−52)·CE바(−48)를 덮고 CE바가 바닐라 갑옷/공기 줄(−49)과 겹치던 것을 HP −55 / CE −62 / 슬롯 −84로 재배치. HP바 색상 단계 간 lerp 전환 + 전투 중(inCombat) 붉은 테두리 추가. CE바 색상을 보스바 기준(70/40/20)으로 통일, 30% 이하 점멸 경고 유지. SkillCooldownHUD에 쿨타임 완료 순간 금색 테두리 플래시(12틱) 추가. DomainIndicator에 영역 내부 화면 가장자리 보라 비네팅 추가.
- **영역C**: CEAuraRenderer 색상을 보스바 기준 4단계로 통일(NON_SORCERER 억제는 서버 HudPacketSender:87-90에서 기적용 확인). AwakeningAuraRenderer를 TOTEM 단일 → TOTEM+END_ROD 상승 기류 혼합 + 발밑 CLOUD로 개선. DomainBoundaryRenderer 개방형 영역 경계 링 밀도 24→48, 상승 속도 0.05→0.12, 2단 높이 링으로 가시성 강화.
- **영역D**: sounds.json 존재 확인(코드에 SoundEvent 참조 없음 → 누락 ID 없음). AnimationRegistry 61~65(nanami_overtime, nanami_ten_puncture, higuruma_argument, higuruma_evidence, inumaki_r_scatter)의 애니메이션 JSON 누락 발견 → 기존 형식 그대로 빈 skeleton 5개 생성.
- **영역E**: `AnimationCache.loadAll` 루프 상한 `id <= 60` 하드코딩으로 animId 61~65(techniques.json에서 실사용)가 로드 누락되던 버그 수정(→ 65). `TrialStateMachine.broadcastVerdict/broadcastSealedSkillSync`의 server null 가드 누락 수정 — 무죄 판결(40%) 경로에서 테스트가 확률적으로 NPE 실패하던 flaky 원인(HigurumaTrialTest, pre-existing 버그). 미사용 import 0건(checkstyle), main 소스셋의 client-only import 0건 확인. checkstyle MagicNumber 등 스타일 경고는 지시대로 미수정.

## [데이터 주도 완결 + 밸런스 패스 실제 적용 (2026-06-13)]

밸런스 패스(위 2026-06-11)가 **런타임에 전혀 적용되지 않던 근본 원인 2건**을 발견·수정:

1. **`TechniqueLoader.load()` 호출부 부재 (치명적)** — 코드 전체에서 호출되는 곳이 0곳이었음. 즉 techniques.json은 한 번도 로드되지 않았고, 모든 게터가 기본값(ce 10 / cd 20 / bd 20)을 반환. `characterStats`(CE풀·재생·HP)도 미등록 상태였음(`CharacterRegistry.STATS` 항상 비어 있음, `CEPool`은 `DEFAULT_RULE`로 폴백). domains.json만 `DomainManager` 생성자에서 로드되어 영역 CE만 적용되던 비대칭이 여기서 비롯됨. → `JJKMod.onInitialize()`에 `TechniqueLoader.load(config/jjk/techniques.json)` 추가, `/jj reload`에도 techniques 재로드 + `DomainManager.reloadDefs()` 추가(기존 reload는 config만 갱신하면서 "techniques·domains 리로드 완료" 오안내).
2. **스킬셋 하드코딩 상수** — 13개 중 11개가 CE/CD/데미지를 자체 상수로 처리(예: Mahito CE_V=3150, Hakari BD_SF=66). 전부 `TechniqueLoader`(keyId 기준) 게터로 전환. Jogo/Sukuna는 이미 전환되어 있었음. NonSorcerer(todo)는 CE 무관 + todo JSON이 placeholder(animId 전부 64)로 보여 상수 유지(운영자 확인 필요).

**도메인 버그 2건 수정 (영원히 전개 불가였음)**: `MahitoSkillSet.useSelfEmbodiment`이 미등록 ID `"mahito_domain"`(정답 `mahito_self_embodiment`) 호출 + CE_V(3150) 사전검사 > 풀 1800; `HakariSkillSet.useDomainDeploy`이 `"hakari_domain"`(정답 `hakari_jackpot_domain`). 두 스킬셋의 `use()`를 `dispatch()`로 라우팅하고 버그 있던 중복 private 메서드 삭제 → onV가 단일 경로. Okkotsu `useTrueMutualLove`의 CE_3(3000) > 풀 1600 사전검사도 제거(domains.json 1100이 단일 차감). 영역 키 techniques.json ceCost=0이라 onV의 `ce(4)` 사전검사는 통과, 실제 CE는 DomainManager가 domains.json 기준 단일 차감.

**테스트**: 312개 전부 통과(클린런 2회 결정적). 스테일 테스트 정리 — `SukunaSkillSetTest`의 onV CE 기대값(데이터 주도 후 DomainManager 위임으로 player==null이면 `FAIL_CONDITION`), `HigurumaTrialTest`는 techniques.json 로드 추가 + key1 CE 160→140(JSON 기준) + 전역 `JJKMod.INSTANCE` 오염 정리 `@AfterAll`(CombatPipelineIntegrationTest 순서 의존 NPE 제거).

**docs**: jjk_spec_v5.md/decisions의 구버전 수치(seal 600→400, maharaga 5/2→3, zone 200→300, 각성 ×1.25→1.5, 임계 0.05/0.10→0.30) 정정, §6에 데이터 주도 배너 추가. 스테일 아티팩트 `docs/files.zip`·`docs/step8_AwakeningManager_patch_final.java`(1인자 AwakeningS2CPacket과 불일치) 삭제. OPERATOR_GUIDE에 데이터 주도/`/jj reload`·`/jj build city` 반영.
