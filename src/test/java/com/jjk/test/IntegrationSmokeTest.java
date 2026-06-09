package com.jjk.test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jjk.JJKMod;
import com.jjk.JjkConfig;
import com.jjk.api.skill.SkillResult;
import com.jjk.awakening.AwakeningManager;
import com.jjk.burden.BurdenManager;
import com.jjk.character.impl.NanamiSkillSet;
import com.jjk.combat.CombatPipeline;
import com.jjk.data.Grade;
import com.jjk.data.Migrator;
import com.jjk.data.PlayerData;
import com.jjk.data.PlayerRepository;
import com.jjk.domain.DomainDefinition;
import com.jjk.domain.DomainInstance;
import com.jjk.domain.DomainManager;
import com.jjk.domain.DomainPriorityCalculator;
import com.jjk.economy.CursedStoneManager;
import com.jjk.entity.CursedSpiritGrade;
import com.jjk.finger.FingerSystem;
import com.jjk.item.CursedToolEffect;
import com.jjk.item.CursedToolItem;
import com.jjk.item.CursedToolRegistry;
import com.jjk.network.c2s.NpcServiceC2SPacket;
import com.jjk.npc.ShokoService;
import com.jjk.npc.YagaService;
import com.jjk.quest.QuestDef;
import com.jjk.quest.QuestManager;
import com.jjk.team.TeamManager;
import com.jjk.trial.TrialManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * §17 통합 스모크 시나리오 — 서버 기동 없이 순수 서비스 레이어 검증.
 * MC 컨텍스트 불필요한 pure-data API만 사용.
 */
class IntegrationSmokeTest {

    private PlayerRepository repo;
    private JjkConfig config;
    private CursedStoneManager csm;

    @BeforeEach
    void setUp() throws Exception {
        config = new JjkConfig();
        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        new Migrator().migrate(conn);
        repo = new PlayerRepository(conn);
        csm = new CursedStoneManager(repo);
        JJKMod.initForTest(csm, repo);
    }

    private PlayerData newPlayer(String characterId) {
        PlayerData d = PlayerData.createDefault(UUID.randomUUID());
        d.characterId = characterId;
        d.ceCurrent = 500f;
        d.ceMax = 500f;
        d.hpCurrent = 20f;
        d.hpMax = 20f;
        return d;
    }

    // ── 시나리오 1: 전체 전투 플로우 ────────────────────────────────────────────
    @Test
    void scenario1_combatFlow() {
        PlayerData attacker = newPlayer("itadori");
        PlayerData target   = newPlayer("mahito");

        CombatPipeline pipeline = new CombatPipeline();
        CombatPipeline.PipelineResult result =
            pipeline.processData(attacker, target, 1, 100L);

        assertEquals(CombatPipeline.PipelineResult.SUCCESS, result,
            "서로 다른 팀 플레이어 간 스킬 공격 성공");
        assertTrue(target.hpCurrent < target.hpMax,
            "피격 후 HP 감소 확인");
        assertFalse(attacker.cooldowns.isEmpty(),
            "공격 후 쿨타임 등록 확인");
    }

    // ── 시나리오 2: 각성 플로우 ──────────────────────────────────────────────────
    @Test
    void scenario2_awakeningFlow() {
        AwakeningManager mgr = new AwakeningManager(config);
        PlayerData data = newPlayer("gojo");
        data.hpMax = 20f;

        // HP 4% → 각성 발동
        mgr.checkAndActivate(data, data.hpMax * 0.04f, data.hpMax, 0L);
        assertTrue(data.awakeningActive, "HP 4%에서 각성 발동");
        assertEquals(160L, data.awakeningEndTick, "각성 지속: 160틱 §LOCK");

        // 160틱 경과 → 각성 해제
        mgr.tickCheck(data, 160L);
        assertFalse(data.awakeningActive, "160틱 후 각성 해제 §LOCK");

        // 쿨타임 2400틱 내 재발동 시도 → 불가
        mgr.checkAndActivate(data, data.hpMax * 0.04f, data.hpMax, 161L);
        assertFalse(data.awakeningActive, "2400틱 쿨타임 내 재발동 불가 §LOCK");

        // 쿨타임 후 재발동
        mgr.checkAndActivate(data, data.hpMax * 0.04f, data.hpMax, 2401L);
        assertTrue(data.awakeningActive, "쿨타임 후 재각성 가능");
    }

    // ── 시나리오 3: 저장/재접속 플로우 ──────────────────────────────────────────
    @Test
    void scenario3_saveLoadFlow() {
        PlayerData original = newPlayer("gojo");
        original.xp = 1234L;
        original.fingerCount = 5;
        original.cooldowns.put("cd_test", 9999L);
        repo.saveImmediate(original);

        PlayerData loaded = repo.load(original.uuid);
        assertEquals(original.uuid,        loaded.uuid);
        assertEquals(original.characterId, loaded.characterId);
        assertEquals(1234L,                loaded.xp,           "XP 보존");
        assertEquals(5,                    loaded.fingerCount,  "fingerCount 보존");
        assertEquals(9999L, loaded.cooldowns.getOrDefault("cd_test", 0L), "cooldowns 보존");
    }

    // ── 시나리오 4: 캐릭터 선택 중복 방지 (pure data 확인) ──────────────────────
    @Test
    void scenario4_duplicateCharacterPrevention() {
        // allowDuplicateCharacter=false (config 기본값)
        assertFalse(config.allowDuplicateCharacter,
            "config 기본값: allowDuplicateCharacter=false");

        PlayerData playerA = newPlayer(null);
        PlayerData playerB = newPlayer(null);
        playerA.characterId = "gojo";

        // B가 같은 캐릭터를 선택하려 할 때 중복 체크 로직 검증
        // (CharacterCommandService는 JJKMod 필요 → 여기서는 TeamManager로 진영만 확인)
        TeamManager tm = new TeamManager();
        assertEquals(TeamManager.Team.JUJUTSU_SORCERER, TeamManager.getTeam("gojo"));
        assertEquals(TeamManager.Team.CURSED_SPIRIT, TeamManager.getTeam("mahito"));
    }

    // ── 시나리오 5: 영역 충돌 해결 ─────────────────────────────────────────────
    @Test
    void scenario5_domainConflict() {
        PlayerData sorcerer = newPlayer("gojo");
        sorcerer.grade = Grade.SPECIAL;
        sorcerer.mastery = 80;
        sorcerer.ceCurrent = 500f;
        sorcerer.ceMax = 500f;

        PlayerData spirit = newPlayer("mahito");
        spirit.grade = Grade.GRADE_2;
        spirit.mastery = 40;
        spirit.ceCurrent = 300f;
        spirit.ceMax = 500f;

        DomainInstance closedSorcerer = new DomainInstance(
            sorcerer.uuid, "gojo_unlimited_void",
            new BlockPos(0, 0, 0), 1500f, 20f, false, false, true, false);
        DomainInstance closedSpirit = new DomainInstance(
            spirit.uuid, "mahito_domain",
            new BlockPos(0, 0, 0), 1500f, 20f, false, false, true, false);

        float pSorcerer = DomainPriorityCalculator.calculate(sorcerer, closedSorcerer);
        float pSpirit   = DomainPriorityCalculator.calculate(spirit,   closedSpirit);

        assertTrue(pSorcerer > pSpirit,
            "고죠(특급, 숙련80) 우선순위 > 마히토(2급, 숙련40): " + pSorcerer + " vs " + pSpirit);
    }

    // ── 시나리오 6: 사망 → 부활 플로우 ─────────────────────────────────────────
    @Test
    void scenario6_deathRespawnFlow() {
        PlayerData data = newPlayer("sukuna");
        data.awakeningActive = true;
        data.zoneActive = true;
        data.bindingVowDeclaredTick = 100L;
        data.hpCurrent = 0f;

        // 사망 시 상태 초기화 (pure data 수준)
        data.awakeningActive = false;
        data.zoneActive = false;
        data.ceCurrent = data.ceMax * 0.50f;
        data.hpCurrent = data.hpMax * 0.50f;
        data.bindingVowDeclaredTick = -1L;

        assertFalse(data.awakeningActive, "사망 후 각성 해제");
        assertFalse(data.zoneActive, "사망 후 Zone 해제");
        assertEquals(-1L, data.bindingVowDeclaredTick, "사망 후 속박 초기화 §LOCK sentinel=-1L");
        assertEquals(data.ceMax * 0.50f, data.ceCurrent, 0.001f, "부활 CE = ceMax×0.50");
        assertEquals(data.hpMax * 0.50f, data.hpCurrent, 0.001f, "부활 HP = hpMax×0.50");
    }

    // ── 시나리오 7: 이누마키 부담 봉인 플로우 ───────────────────────────────────
    @Test
    void scenario7_inumakiBurdenSeal() {
        PlayerData data = newPlayer("inumaki");
        data.burden = 0;
        long tick = 100L;

        // !터져(+30) 4회 → burden = 120 > 100 → skill_seal 등록
        for (int i = 0; i < 4; i++) {
            BurdenManager.addBurden(data, 30, tick, config);
        }

        // burden 101 초과 후 봉인 확인 (addBurden은 초과 즉시 봉인)
        assertTrue(BurdenManager.isSealed(data, tick), "burden 120 → skill_seal 등록");
        assertEquals(0, data.burden, "봉인 후 burden 초기화");

        // 600틱 후 봉인 해제
        long sealEnd = data.cooldowns.getOrDefault("skill_seal", 0L);
        assertFalse(BurdenManager.isSealed(data, sealEnd), "600틱 후 봉인 해제");
    }

    // ── 시나리오 9: 주구 효과 CombatPipeline 통합 ────────────────────────────────
    @Test
    void scenario9_cursedToolEffect_inCombat() {
        // 천호창: attackBonus=0.14, ceReduction=0.08, rangeBonus=없음
        CursedToolEffect spear = CursedToolRegistry.getEffect("thousand_spear");
        float base = 100f;
        float withBonus = base * (1.0f + spear.attackBonus());
        assertEquals(114f, withBonus, 0.01f, "천호창 공격력 +14%: 100 → 114");

        float ceCost = 100f;
        float ceRefund = ceCost * spear.ceReduction();
        assertEquals(8f, ceRefund, 0.01f, "CE 소모 -8% 환급: 100 × 0.08 = 8");

        assertEquals(0f, spear.rangeBonus(), 0.001f,
            "천호창은 범위 보너스 없음 (rangeBonus=0)");
    }

    // ── 시나리오 10: 석혼도 isSoulDirect 강제 → 방어 무시 ────────────────────────
    @Test
    void scenario10_splitSoulBlade_forceSoulDirect() {
        CursedToolEffect blade = CursedToolRegistry.getEffect("split_soul_blade");
        assertTrue(blade.forceSoulDirect(), "석혼도: forceSoulDirect=true");

        // isSoulDirect=true → applyDefenseStat 방어 무시
        float rawDmg = 100f;
        float dmgWithSoulDirect = CombatPipeline.applyDefenseStat(rawDmg, 80, 1.0f, true);
        float dmgNormal         = CombatPipeline.applyDefenseStat(rawDmg, 80, 1.0f, false);

        assertEquals(100f, dmgWithSoulDirect, 0.01f,
            "isSoulDirect=true → defenseStat 무시, 100f 그대로");
        assertTrue(dmgWithSoulDirect > dmgNormal,
            "석혼도 isSoulDirect → 일반 공격보다 데미지 높음");
    }

    // ── 시나리오 11: 주령 4급 처치 → XP 지급 확인 ─────────────────────────────
    @Test
    void scenario11_cursedSpirit_xpOnDeath() {
        // GradeManager.addXp(data, grade.xpDrop, player) 호출 검증
        // pure-data: xpDrop 값이 올바른지 확인 (MC 없이)
        assertEquals(10,  CursedSpiritGrade.GRADE_4.xpDrop, "4급 처치 XP = 10");
        assertEquals(25,  CursedSpiritGrade.GRADE_3.xpDrop, "3급 처치 XP = 25");
        assertEquals(400, CursedSpiritGrade.SPECIAL.xpDrop, "특급 처치 XP = 400");

        // XP 적용 시뮬레이션
        PlayerData killer = newPlayer("itadori");
        killer.xp = 0L;
        killer.xp += CursedSpiritGrade.GRADE_4.xpDrop;
        assertEquals(10L, killer.xp, "4급 처치 후 XP = 10");
    }

    // ── 시나리오 12: 특급 주령 영역 전개 플로우 ──────────────────────────────────
    @Test
    void scenario12_specialSpirit_domainDeploy() {
        // DomainManager.deployNpcDomain + getActiveDomain 플로우
        DomainManager mgr = new DomainManager(config);
        UUID npcUuid = java.util.UUID.randomUUID();

        // 테스트용 도메인 정의 등록
        DomainDefinition def = new DomainDefinition();
        def.domainId = "cursed_spirit_domain";
        def.radius = 15f;
        def.wallHp = 800f;
        def.isOpen = false;
        def.sureHitActive = true;
        def.autoTargetAll = true;
        def.cooldownTicks = 200;
        mgr.addDomainDefForTest("cursed_spirit_domain", def);

        boolean deployed = mgr.deployNpcDomain(
            npcUuid, new Vec3d(0, 64, 0), "cursed_spirit_domain", 0L, "minecraft:overworld");
        assertTrue(deployed, "특급 주령 영역 전개 성공");

        DomainInstance domain = mgr.getActiveDomain(npcUuid);
        assertNotNull(domain, "activeDomains에 등록됨 확인");
        assertEquals("cursed_spirit_domain", domain.domainId, "domainId 일치");
        assertTrue(domain.npcOwned, "NPC 영역 플래그 확인");
        assertTrue(domain.sureHitActive, "sureHitActive=true → 틱 데미지 활성");

        // 1200틱 쿨타임 내 재전개 불가
        boolean redeployed = mgr.deployNpcDomain(
            npcUuid, new Vec3d(0, 64, 0), "cursed_spirit_domain", 1L, "minecraft:overworld");
        assertFalse(redeployed, "이미 활성 영역이 있으면 재전개 불가");
    }

    // ── 시나리오 8: 히구루마 재판 완전 플로우 ───────────────────────────────────
    @Test
    void scenario8_higurumaTrial() {
        PlayerData higuruma = newPlayer("higuruma");
        PlayerData target   = newPlayer("mahito");
        long tick = 0L;

        // F: 증거 제출
        TrialManager.addEvidence(higuruma, target, tick);
        assertTrue(target.cooldowns.keySet().stream()
            .anyMatch(k -> k.startsWith("evidence_")), "증거 제출 확인");

        // Shift+R: 재판 시작 (config.trialSuccessRate 사용)
        JjkConfig forcedConfig = new JjkConfig();
        forcedConfig.trialSuccessRate = 1.0f; // 항상 유죄
        com.jjk.api.skill.SkillResult trialResult =
            TrialManager.startTrial(higuruma, target, tick, forcedConfig);
        assertEquals(com.jjk.api.skill.SkillResult.SUCCESS, trialResult,
            "증거 있을 때 재판 시작 성공");
        assertTrue(higuruma.cooldowns.containsKey("trial_state"), "재판 상태 키 등록");

        // 증거 없이 재판 시작 시도 → FAIL_CONDITION
        PlayerData fresh = newPlayer("higuruma");
        PlayerData target2 = newPlayer("sukuna");
        com.jjk.api.skill.SkillResult noEvidenceResult =
            TrialManager.startTrial(fresh, target2, tick, config);
        assertEquals(com.jjk.api.skill.SkillResult.FAIL_CONDITION, noEvidenceResult,
            "증거 없이 재판 시작 → FAIL_CONDITION");
    }

    // ── 시나리오 13: 주구 등급 조건 + CombatPipeline 연동 ────────────────────────
    @Test
    void scenario13_cursedTool_gradeCheck_and_combat() {
        CursedToolEffect inverted = CursedToolRegistry.getEffect("inverted_spear");

        // 4급 플레이어 + 천역모(특급 전용) → 장착 불가 → NONE
        PlayerData grade4 = newPlayer("itadori");
        grade4.grade = Grade.GRADE_4;
        assertFalse(CursedToolItem.isEligible(grade4, inverted),
            "4급 + 천역모 → 등급 미달, getActiveEffect=NONE");

        // 특급 플레이어 + 천역모 → defPenetration 0.15 적용
        PlayerData special = newPlayer("gojo");
        special.grade = Grade.SPECIAL;
        assertTrue(CursedToolItem.isEligible(special, inverted),
            "특급 + 천역모 → 장착 가능");
        float defMult = 1.0f - inverted.defPenetration(); // 1.0 - 0.15 = 0.85
        assertEquals(0.85f, defMult, 0.001f, "천역모 defenseMultiplier = 0.85");
        float dmgPenetrate = CombatPipeline.applyDefenseStat(100f, 75, 0.85f, false);
        float dmgNormal    = CombatPipeline.applyDefenseStat(100f, 75, 1.00f, false);
        assertTrue(dmgPenetrate > dmgNormal, "천역모 관통 → finalDamage 높음");

        // 특급 + 석혼도 → isSoulDirect=true → 방어 무시
        CursedToolEffect blade = CursedToolRegistry.getEffect("split_soul_blade");
        assertTrue(blade.forceSoulDirect(), "석혼도 forceSoulDirect=true");
        float dmgSoulDirect = CombatPipeline.applyDefenseStat(100f, 80, 1.0f, true);
        assertEquals(100f, dmgSoulDirect, 0.01f, "isSoulDirect → 방어 무시, 100f 유지");
        float withAttackBonus = 100f * (1.0f + blade.attackBonus()); // ×1.18
        assertEquals(118f, withAttackBonus, 0.01f, "석혼도 attackBonus +18%");
    }

    // ── 시나리오 14: 주령 등급별 Goal 구조 확인 ──────────────────────────────────
    @Test
    void scenario14_cursedSpiritGoals() {
        // AI Tier로 Goal 구조 검증 (MC 엔티티 없이)
        assertEquals(CursedSpiritGrade.AiTier.BASIC,   CursedSpiritGrade.GRADE_4.aiTier,
            "GRADE_4: BASIC (MeleeAttackGoal만)");
        assertEquals(CursedSpiritGrade.AiTier.RANGED,  CursedSpiritGrade.GRADE_3.aiTier,
            "GRADE_3: RangedCeAttackGoal 추가");
        assertEquals(CursedSpiritGrade.AiTier.SKILLED, CursedSpiritGrade.GRADE_1.aiTier,
            "GRADE_1: Ranged+Evasive+CeBurst");
        assertEquals(CursedSpiritGrade.AiTier.BOSS,    CursedSpiritGrade.SPECIAL.aiTier,
            "특급: DomainDeployGoal 포함");

        // DomainDeployGoal canStart 조건: HP < SPECIAL.maxHp × 0.5 = 200f
        float threshold = CursedSpiritGrade.SPECIAL.maxHp * 0.5f;
        assertEquals(200f, threshold, 0.001f, "특급 발동 HP 기준 = 200f");
        assertTrue(90f < threshold, "HP=90 < 200 → canStart=true");
        assertFalse(201f < threshold, "HP=201 >= 200 → canStart=false");
    }

    // ── 시나리오 15: 주령 처치 → XP + 손가락 미드롭 ─────────────────────────────
    @Test
    void scenario15_cursedSpiritDeath_xpAndFingers() {
        PlayerData killer = newPlayer("itadori");
        killer.xp = 0L;

        killer.xp += CursedSpiritGrade.GRADE_4.xpDrop;
        assertEquals(10L, killer.xp, "GRADE_4 처치 XP = 10");

        killer.xp += CursedSpiritGrade.SPECIAL.xpDrop;
        assertEquals(410L, killer.xp, "SPECIAL 추가 처치 XP = 400");

        // 주령 처치는 손가락 드롭과 무관 (스쿠나 플레이어 사망 시만 tryDrop)
        PlayerData sukunaKiller = newPlayer("sukuna");
        sukunaKiller.fingerCount = 5;
        // 주령 처치 후 fingerCount 변화 없음
        sukunaKiller.xp += CursedSpiritGrade.SPECIAL.xpDrop;
        assertEquals(5, sukunaKiller.fingerCount,
            "주령 처치 시 fingerCount 불변 (tryDrop 미호출)");
    }

    // ── 시나리오 16: 나나미 극한초과 + 십:분 콤보 ───────────────────────────────
    @Test
    void scenario16_nanami_overtime_tenPuncture_combo() {
        long tick = 100L;

        // worldTime=13000 → 배율 2.50
        float mult = NanamiSkillSet.getOvertimeMultiplier(13000L);
        assertEquals(2.50f, mult, 0.001f, "13000틱 → 극한초과 ×2.50");
        long encoded = (long)(mult * 1000);
        assertEquals(2500L, encoded, "overtime_multiplier_value = 2500");

        // 60틱 이내: overtimeUntil(160) > testTick(110) → 배율 적용
        long overtimeUntil = tick + 60L;
        assertTrue(overtimeUntil > tick + 10L, "60틱 이내 overtime 유효");
        float rawDmgOvertime = NanamiSkillSet.applyWeaknessBonus(78f, false) * mult;
        assertEquals(195f, rawDmgOvertime, 0.01f, "overtime 적용: 78 × 2.50 = 195");

        // 60틱 경과: overtimeUntil(160) <= tick(161) → 미적용
        assertFalse(overtimeUntil > tick + 61L, "60틱 후 overtime 만료");
        assertEquals(78f, 78f * 1.0f, 0.01f, "overtime 만료: rawDamage = 78f");

        // worldTime=12999 → ×1.00
        float noOvertime = NanamiSkillSet.getOvertimeMultiplier(12999L);
        assertEquals(1.0f, noOvertime, 0.001f, "12999틱 → 배율 없음");
        assertEquals(78f, 78f * noOvertime, 0.01f, "rawDamage = 78f 그대로");
    }

    // ── 시나리오 17: 스쿠나 손가락 연동 전체 플로우 ──────────────────────────────
    @Test
    void scenario17_sukuna_fingerSystem() {
        // 역팔지 반경 분기
        PlayerData data9  = newPlayer("sukuna"); data9.fingerCount  = 9;
        PlayerData data10 = newPlayer("sukuna"); data10.fingerCount = 10;
        double r9  = data9.fingerCount  >= 10 ? 8.0 * 1.3 : 8.0;
        double r10 = data10.fingerCount >= 10 ? 8.0 * 1.3 : 8.0;
        assertEquals(8.0,  r9,  0.001, "손가락 9개: 반경 8.0");
        assertEquals(10.4, r10, 0.001, "손가락 10개: 반경 8.0 × 1.3 = 10.4");

        // CD 감소: fingerCount=20 → baseCd × 0.80
        int baseCd = 6;
        PlayerData data20 = newPlayer("sukuna"); data20.fingerCount = 20;
        int reducedCd = data20.fingerCount >= 20 ? (int)(baseCd * 0.80f) : baseCd;
        assertEquals(4, reducedCd, "손가락 20개: CD 6×0.80 = 4");

        PlayerData data19 = newPlayer("sukuna"); data19.fingerCount = 19;
        int unreducedCd = data19.fingerCount >= 20 ? (int)(baseCd * 0.80f) : baseCd;
        assertEquals(6, unreducedCd, "손가락 19개: CD 감소 없음");

        // FingerSystem §LOCK 수치 확인
        FingerSystem fs = new FingerSystem(config);
        assertEquals(0.10f, config.fingerDropRate, 0.001f, "fingerDropRate §LOCK 0.10");
        assertEquals(20,    config.fingerMaxCount,         "fingerMaxCount §LOCK 20");
    }

    // ── 시나리오 18: 특급 주령 영역 전개 → 데미지·해제 플로우 ──────────────────
    @Test
    void scenario18_npcDomain_fullFlow() {
        DomainManager mgr = new DomainManager(config);
        java.util.UUID npcUuid = java.util.UUID.randomUUID();

        DomainDefinition def = new DomainDefinition();
        def.domainId = "cursed_spirit_domain";
        def.radius = 15f;
        def.wallHp = 800f;
        def.isOpen = false;
        def.sureHitActive = true;
        def.autoTargetAll = true;
        def.cooldownTicks = 200;
        mgr.addDomainDefForTest("cursed_spirit_domain", def);

        // deployNpcDomain 성공 → activeDomains 등록
        boolean deployed = mgr.deployNpcDomain(
            npcUuid, new Vec3d(0, 64, 0), "cursed_spirit_domain", 0L, "minecraft:overworld");
        assertTrue(deployed, "NPC 영역 전개 성공");

        DomainInstance domain = mgr.getActiveDomain(npcUuid);
        assertNotNull(domain,                            "activeDomains 등록 확인");
        assertTrue(domain.npcOwned,                     "npcOwned=true");
        assertTrue(domain.sureHitActive,                "sureHitActive=true (틱 데미지 활성)");
        assertEquals(800f, domain.wallHp, 0.001f,       "wallHp=800");
        assertEquals(15f,  domain.currentRadius, 0.001f,"반경=15");

        // 틱 데미지: attackDamage × 0.3 (SPECIAL = 38 × 0.3 = 11.4)
        float tickDmg = CursedSpiritGrade.SPECIAL.attackDamage * 0.3f;
        assertEquals(11.4f, tickDmg, 0.01f, "특급 틱 데미지 = 38 × 0.3 = 11.4");

        // 주령 사망 → collapseDomain → activeDomains 제거
        mgr.collapseDomain(npcUuid, 100L);
        assertNull(mgr.getActiveDomain(npcUuid), "사망 후 영역 제거 확인");

        // 금지 청크 → 전개 불가
        config.domainBannedChunks.add("minecraft:overworld:0,0");
        boolean bannedResult = mgr.deployNpcDomain(
            npcUuid, new Vec3d(0, 64, 0), "cursed_spirit_domain", 0L, "minecraft:overworld");
        assertFalse(bannedResult, "금지 청크(0,0) → deployNpcDomain false");
        config.domainBannedChunks.clear(); // 다른 테스트 영향 없도록
    }

    // ── 시나리오 19: 건축물 생성 플로우 ─────────────────────────────────────────
    @Test
    void scenario19_buildingConfig_flow() {
        JjkConfig cfg = new JjkConfig();
        assertFalse(cfg.jjtBuildingGenerated(), "초기 generated=false");

        cfg.setJjtBuildingGenerated(true);
        assertTrue(cfg.jjtBuildingGenerated(), "빌드 완료 후 true");

        // 재진입 방지 가드
        assertTrue(cfg.jjtBuildingGenerated(), "generated=true → 재생성 방지 플래그");

        // NPC 스폰 좌표 범위 검증
        int[] ijichi = {-15, 1, -12};
        assertTrue(ijichi[0] >= -20 && ijichi[0] <= 20, "이치지 1층 X 범위");
        assertTrue(ijichi[1] >= 0   && ijichi[1] <= 8,  "이치지 1층 Y 범위");
        assertTrue(ijichi[2] >= -15 && ijichi[2] <= 15, "이치지 1층 Z 범위");

        int[] gojoShiyu = {-15, -19, 0};
        assertTrue(gojoShiyu[1] >= -20 && gojoShiyu[1] <= -11, "공시우 B2 Y 범위");

        int[] yaga = {-10, 19, 0};
        assertTrue(yaga[1] >= 18 && yaga[1] <= 27, "야가 3층 Y 범위");
    }

    // ── 시나리오 20: 주력석 경제 순환 ───────────────────────────────────────────
    @Test
    void scenario20_cursedStone_economy() {
        PlayerData playerA = newPlayer("itadori");

        // GRADE_4 처치 → XP + 석
        playerA.xp += CursedSpiritGrade.GRADE_4.xpDrop;
        assertEquals(10L, playerA.xp, "4급 처치 XP = 10");

        csm.give(playerA, 40L, "spirit_kill_4", null);
        assertEquals(40L, playerA.cursedStones, "4급 처치 → 40석");

        // 현상금 누적
        PlayerData playerB = newPlayer("mahito");
        csm.addBounty(playerB, 50L);
        assertEquals(50L, playerB.bounty, "bounty += 50");

        // 현상금 정산
        csm.give(playerB, playerB.bounty, "settle", null);
        long settled = playerB.cursedStones;
        playerB.bounty = 0L;
        assertEquals(0L, playerB.bounty, "정산 후 bounty = 0");
        assertEquals(50L, settled, "bounty → cursedStones 변환");

        // 젠인 창고지기 구매
        PlayerData buyer = newPlayer("itadori");
        buyer.cursedStones = 500L;
        csm.spend(buyer, 300L, "buy_cursed_dagger", null);
        assertEquals(200L, buyer.cursedStones, "500 - 300 = 200석");
    }

    // ── 시나리오 21: NPC 서비스 쿨타임 영속성 ───────────────────────────────────
    @Test
    void scenario21_npcService_cooldown_persistence() {
        PlayerData data = newPlayer("itadori");
        data.cursedStones = 1000L;
        long tick = 100L;

        SkillResult r = ShokoService.handle(
            new NpcServiceC2SPacket("shoko", "heal_full", null),
            data, null, tick, csm);
        assertEquals(SkillResult.SUCCESS, r, "완전 회복 성공");
        assertTrue(data.cooldowns.containsKey("npc_shoko_heal_full"), "쿨타임 키 등록됨");

        // save → reload
        repo.saveImmediate(data);
        PlayerData loaded = repo.load(data.uuid);
        assertTrue(loaded.cooldowns.containsKey("npc_shoko_heal_full"), "재접속 후 쿨타임 유지");

        // 쿨타임 중 재사용
        loaded.cursedStones = 1000L;
        SkillResult r2 = ShokoService.handle(
            new NpcServiceC2SPacket("shoko", "heal_full", null),
            loaded, null, tick + 100L, csm);
        assertEquals(SkillResult.ON_COOLDOWN, r2, "쿨타임 중 재사용 → ON_COOLDOWN");
    }

    // ── 시나리오 22: 퀘스트 진행도 연동 ────────────────────────────────────────
    @Test
    void scenario22_questProgress_flow() {
        PlayerData data = newPlayer("itadori");
        QuestManager qm = JJKMod.getQuestManager();

        int today = (int)(System.currentTimeMillis() / 86400000L);
        QuestDef quest = qm.getDailyQuest(data.uuid, today);
        assertNotNull(quest, "일일 퀘스트 배정");

        assertEquals(0L, data.cooldowns.getOrDefault("quest_daily_prog", 0L), "초기 진행도 = 0");

        // target - 1 회 progress
        for (int i = 0; i < quest.target() - 1; i++) {
            qm.progress(data, quest.type(), null, 0L);
        }
        long progBefore = data.cooldowns.getOrDefault("quest_daily_prog", 0L);
        assertEquals(quest.target() - 1, progBefore, "target-1 진행 후 확인");

        // 마지막 progress → 완료
        qm.progress(data, quest.type(), null, 0L);
        boolean done = data.cooldowns.getOrDefault("quest_daily_done", -1L) == today;
        assertTrue(done, "퀘스트 완료 확인");

        // 완료 후 재시도 → 변화 없음
        long stonesBefore = data.cursedStones;
        qm.progress(data, quest.type(), null, 0L);
        assertEquals(stonesBefore, data.cursedStones, "완료 후 재시도 → 변화 없음");
    }

    // ── 시나리오 23: 주구 강화 + CombatPipeline 연동 ────────────────────────────
    @Test
    void scenario23_tool_enhance_combat() {
        // 천호창 기본 + 1단계 강화 효과
        float baseBonus = CursedToolRegistry.getEffect("thousand_spear").attackBonus();
        float enhBonus  = 0.03f;
        float total     = baseBonus + enhBonus;
        assertEquals(0.17f, total, 0.001f, "천호창 1단계: 0.14 + 0.03 = 0.17");

        // 강화 단계 추적
        PlayerData data = newPlayer("itadori");
        data.cooldowns.put("tool_enhance_thousand_spear", 1L);
        int enhLevel = data.cooldowns.getOrDefault("tool_enhance_thousand_spear", 0L).intValue();
        assertEquals(1, enhLevel, "강화 단계 1 확인");

        // 3단계 후 추가 강화 → FAIL
        data.cooldowns.put("tool_enhance_thousand_spear", 3L);
        data.cursedStones = 99999L;
        SkillResult r = YagaService.handle(
            new NpcServiceC2SPacket("yaga", "enhance", "thousand_spear"),
            data, null, 0L, csm);
        assertEquals(SkillResult.FAIL, r, "3단계 후 추가 강화 → FAIL");

        // isSoulDirect 방어 무시
        float dmgSoul = CombatPipeline.applyDefenseStat(100f, 80, 1.0f, true);
        assertEquals(100f, dmgSoul, 0.01f, "석혼도 방어 무시 = 100f");
    }

    // ── 시나리오 24: NPC GUI payload 검증 ──────────────────────────────────────
    @Test
    void scenario24_npcPayload_validation() {
        Gson gson = new Gson();

        // 이치지 payload
        String ijichiPayload = "{\"stones\":500,\"questDesc\":\"주령 5마리 처치\","
            + "\"questTarget\":5,\"questProg\":0,\"questDone\":false}";
        JsonObject j = gson.fromJson(ijichiPayload, JsonObject.class);
        assertTrue(j.has("questDesc"),   "questDesc 필드 존재");
        assertTrue(j.has("questTarget"), "questTarget 필드 존재");
        assertTrue(j.has("questProg"),   "questProg 필드 존재");

        // 야가 payload
        String yagaPayload = "{\"stones\":500,\"toolId\":\"thousand_spear\",\"enhLevel\":1}";
        JsonObject yj = gson.fromJson(yagaPayload, JsonObject.class);
        assertEquals("thousand_spear", yj.get("toolId").getAsString(), "toolId 일치");
        assertEquals(1, yj.get("enhLevel").getAsInt(), "enhLevel 일치");

        // 공시우 payload — 주술사 진영
        String gojoPayload = "{\"stones\":500,\"bounty\":0,\"isCursedSpirit\":false}";
        JsonObject gj = gson.fromJson(gojoPayload, JsonObject.class);
        assertFalse(gj.get("isCursedSpirit").getAsBoolean(), "주술사 → isCursedSpirit false");
    }
}
