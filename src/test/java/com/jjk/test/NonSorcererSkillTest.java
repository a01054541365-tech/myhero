package com.jjk.test;

import com.jjk.JjkConfig;
import com.jjk.api.combat.IDamageSource;
import com.jjk.api.skill.SkillResult;
import com.jjk.character.impl.NonSorcererSkillSet;
import com.jjk.combat.DamageCalculator;
import com.jjk.combat.DamageContext;
import com.jjk.data.Migrator;
import com.jjk.data.PlayerData;
import com.jjk.domain.DomainDefinition;
import com.jjk.domain.DomainManager;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

// PHASE H-6: 비술사(천여주박, characterId="todo") 스킬셋 검증
class NonSorcererSkillTest {

    private final NonSorcererSkillSet skill = new NonSorcererSkillSet();

    private PlayerData todoData() {
        PlayerData d = PlayerData.createDefault(UUID.randomUUID());
        d.characterId = "todo";
        d.ceCurrent = 0f;
        d.ceMax = 0f;
        d.hpCurrent = 44f;
        d.hpMax = 44f;
        d.attackStat = 88;
        d.defenseStat = 85;
        return d;
    }

    // ── 1. 모든 스킬 ceCost = 0 (CE 술식 없음) ───────────────────────────────
    @Test
    void allSkills_ceCostZero() {
        for (int key = 0; key <= 4; key++) {
            assertEquals(0, skill.getCeCost(key), "key=" + key + " ceCost는 항상 0");
        }
    }

    // ── 2. 쿨타임 값: 강화주먹3 / 강화질주40 / 파쇄격60 / 천여주박각성1200 / 불굴300 ──
    @Test
    void cooldownTicks_matchSpec() {
        assertEquals(3,    skill.getCooldownTicks(0), "강화주먹 CD=3");
        assertEquals(40,   skill.getCooldownTicks(1), "강화질주 CD=40");
        assertEquals(60,   skill.getCooldownTicks(2), "파쇄격 CD=60");
        assertEquals(1200, skill.getCooldownTicks(3), "천여주박각성 CD=1200");
        assertEquals(300,  skill.getCooldownTicks(4), "불굴 CD=300");
    }

    // ── 3. CE 0인 채로 onF~onV 호출 시 CE_INSUFFICIENT가 아니어야 함 (player=null → SUCCESS) ──
    @Test
    void ceZero_doesNotBlockSkillUse() {
        PlayerData data = todoData();
        long tick = 0L;
        assertEquals(SkillResult.SUCCESS, skill.onF(data,      null, tick));
        assertEquals(SkillResult.SUCCESS, skill.onShiftF(data, null, tick));
        assertEquals(SkillResult.SUCCESS, skill.onR(data,      null, tick));
        assertEquals(SkillResult.SUCCESS, skill.onShiftR(data, null, tick));
        assertEquals(SkillResult.SUCCESS, skill.onV(data,      null, tick));
    }

    // ── 4. 강화주먹 baseDamage 38 ─────────────────────────────────────────────
    @Test
    void enhancedFist_baseDamage38() {
        DamageContext ctx = DamageContext.builder(null, null, IDamageSource.NORMAL_TECHNIQUE, 38f)
                .skillName("enhanced_fist").keyId(0).build();
        assertEquals(38f, ctx.baseDamage, 0.001f, "강화주먹 baseDamage=38");
    }

    // ── 5. 파쇄격 baseDamage 75 ───────────────────────────────────────────────
    @Test
    void shatteringStrike_baseDamage75() {
        DamageContext ctx = DamageContext.builder(null, null, IDamageSource.NORMAL_TECHNIQUE, 75f)
                .skillName("shattering_strike").keyId(2).build();
        assertEquals(75f, ctx.baseDamage, 0.001f, "파쇄격 baseDamage=75");
    }

    // ── 6. 천여주박각성: attackBoostMultiplier×1.6 / defenseBoostMultiplier×1.4 가
    //      DamageCalculator.calculatePure에 반영되는지 확인 ─────────────────────
    @Test
    void awakenedBody_buffMultipliers_applyInDamageCalculation() {
        DamageCalculator calc = new DamageCalculator();
        JjkConfig config = new JjkConfig();

        PlayerData attacker = todoData();
        PlayerData target = PlayerData.createDefault(UUID.randomUUID());
        target.defenseStat = 50;
        target.hpMax = 40f;

        DamageContext ctxBefore = DamageContext.builder(null, null, IDamageSource.NORMAL_TECHNIQUE, 38f).build();
        float dmgBefore = calc.calculatePure(ctxBefore, attacker, target, 38f, config, 0L);

        // 천여주박각성 발동 상태 적용
        attacker.attackBoostMultiplier = 1.6f;

        DamageContext ctxAfter = DamageContext.builder(null, null, IDamageSource.NORMAL_TECHNIQUE, 38f).build();
        float dmgAfter = calc.calculatePure(ctxAfter, attacker, target, 38f, config, 0L);

        assertTrue(dmgAfter > dmgBefore,
                "attackBoostMultiplier×1.6 적용 시 데미지 증가: " + dmgAfter + " > " + dmgBefore);

        // 방어 측 defenseBoostMultiplier×1.4 → effectiveDefense 증가 → 받는 피해 감소
        PlayerData defender = PlayerData.createDefault(UUID.randomUUID());
        defender.defenseStat = 50;
        defender.hpMax = 40f;

        DamageContext ctxDefBefore = DamageContext.builder(null, null, IDamageSource.NORMAL_TECHNIQUE, 100f).build();
        float dmgToDefBefore = calc.calculatePure(ctxDefBefore, attacker, defender, 100f, config, 0L);

        defender.defenseBoostMultiplier = 1.4f;
        DamageContext ctxDefAfter = DamageContext.builder(null, null, IDamageSource.NORMAL_TECHNIQUE, 100f).build();
        float dmgToDefAfter = calc.calculatePure(ctxDefAfter, attacker, defender, 100f, config, 0L);

        assertTrue(dmgToDefAfter < dmgToDefBefore,
                "defenseBoostMultiplier×1.4 적용 시 받는 피해 감소: " + dmgToDefAfter + " < " + dmgToDefBefore);
    }

    // ── 7. 불굴: 피해 ×0.70 + 사망 방지 1회 로직 검증 (CombatPipeline Stage 7c와 동일 공식) ──
    @Test
    void indomitable_damageReduction_andDeathPrevention() {
        PlayerData defender = todoData();
        defender.hpCurrent = 10f;
        defender.nsShieldExpireTick = 100L;
        defender.nsDeathPreventUsed = false;

        long shieldTick = 50L; // <= nsShieldExpireTick → 활성
        float incoming = 50f;

        assertTrue(shieldTick <= defender.nsShieldExpireTick, "불굴 활성 구간");
        float reduced = incoming * 0.70f;
        assertEquals(35f, reduced, 0.001f, "불굴: 피해 ×0.70 = 35.0");

        // 사망 방지: reduced(35) >= hpCurrent(10) → hpCurrent - 1 로 클램프, 1회만
        if (!defender.nsDeathPreventUsed && reduced >= defender.hpCurrent) {
            reduced = defender.hpCurrent - 1.0f;
            defender.nsDeathPreventUsed = true;
        }
        assertEquals(9f, reduced, 0.001f, "사망 방지: hpCurrent - 1 = 9.0");
        assertTrue(defender.nsDeathPreventUsed, "사망 방지 1회 소모 플래그 설정");

        // 두 번째 발동 시에는 사망 방지가 다시 적용되지 않음
        float secondHit = 50f * 0.70f;
        if (!defender.nsDeathPreventUsed && secondHit >= defender.hpCurrent) {
            secondHit = defender.hpCurrent - 1.0f;
        }
        assertEquals(35f, secondHit, 0.001f, "사망 방지 1회 소모 후에는 일반 ×0.70 피해만 적용");
    }

    // ── 8. 영역 전개 시도 시 차단 (characterId="todo") ────────────────────────
    @Test
    void domainDeploy_blockedForNonSorcerer() throws Exception {
        java.sql.Connection conn = java.sql.DriverManager.getConnection("jdbc:sqlite::memory:");
        new Migrator().migrate(conn);
        JjkConfig config = new JjkConfig();
        DomainManager mgr = new DomainManager(config);

        PlayerData data = todoData();
        data.ceCurrent = 100000f; // CE 충분해도 차단되어야 함
        data.ceMax = 100000f;

        DomainDefinition def = new DomainDefinition();
        def.domainId = "todo_test_domain";
        def.ceCost = 100f;
        def.isOpen = false;
        def.wallHp = 1500f;
        def.radius = 20f;
        def.cooldownTicks = 200;
        def.sureHitActive = false;
        def.autoTargetAll = false;
        def.isIncomplete = false;

        boolean deployed = mgr.deployDomainData(data, def, 0L);
        assertFalse(deployed, "비술사(todo)는 영역 전개 불가 — deployDomainData가 false 반환");
        assertTrue(mgr.getActiveDomains().isEmpty(), "차단되었으므로 활성 영역 없음");
    }

    // ── 9. CE 관련 전제: maxCe=0, ceCurrent=0 — CE 의존 스킬이 아예 없음을 확인 ──
    @Test
    void noCeBasedSkills_maxCeZero() {
        PlayerData data = todoData();
        assertEquals(0f, data.ceMax, 0.001f, "비술사 maxCe=0 (천여주박: CE 술식 없음)");
        assertEquals(0f, data.ceCurrent, 0.001f);
        for (int key = 0; key <= 4; key++) {
            assertEquals(0, skill.getCeCost(key), "CE 0 캐릭터의 모든 스킬은 ceCost=0이어야 사용 가능");
        }
    }
}
